package com.dbe.distributed.systems.server.cluster;

import com.dbe.distributed.systems.server.HealthMessage;
import com.dbe.distributed.systems.server.JoinAckMessage;
import com.dbe.distributed.systems.server.JoinMessage;
import com.dbe.distributed.systems.server.MemberMessage;
import com.dbe.distributed.systems.shared.Constants;
import com.dbe.distributed.systems.shared.clock.VectorClock;
import com.dbe.distributed.systems.shared.messages.BaseMessage;
import com.dbe.distributed.systems.shared.messages.ChatMessage;
import com.dbe.distributed.systems.shared.messages.GetMasterMessage;
import com.dbe.distributed.systems.shared.messages.GetMasterMessageResponse;
import com.dbe.distributed.systems.shared.network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MemberService implements MulticastReceiver.MessageHandler, TcpServer.MessageHandler {
    private static final Logger LOG = LoggerFactory.getLogger(MemberService.class);

    private static final List<Class<? extends BaseMessage>> RESPONSIBLE_UNICAST_MESSAGE_TYPES = List.of(MemberMessage.class, JoinAckMessage.class);
    private final List<Class<? extends BaseMessage>> RESPONSIBLE_MULTICAST_MESSAGE_TYPES = List.of(
            JoinMessage.class, GetMasterMessage.class);

    private final Map<String, Member> members = new ConcurrentHashMap<>();
    private final ServerType serverType;
    private final VectorClock clock;
    private final String id;
    private final Member member;
    private final MulticastPublisher multicastPublisher;
    private final UnicastMessageSender unicastMessageSender;
    private Supplier<ClusterState> clusterState;
    private Consumer<ClusterState> onReceiveClusterState;

    public MemberService(Member member, ServerType serverType, MulticastPublisher multicastPublisher) {
        this.clock = new VectorClock();
        this.serverType = serverType;
        this.multicastPublisher = multicastPublisher;
        this.id = member.getId();
        this.member = member;
        this.clock.setReplicaTimestamp(id, BigDecimal.ZERO.longValue());
        this.unicastMessageSender = new UnicastMessageSender(this::getReplicas, new TransmissionFailureHandler());
        this.members.put(id, member);
        startFailureDetector();
    }

    public UnicastMessageSender getUnicastMessageSender() {
        return unicastMessageSender;
    }

    private void startFailureDetector() {
        Runnable failureDetector = () -> {
            if (serverType.isMaster()) {
                unicastMessageSender.sendMessageToAllMembers(new HealthMessage());
            }
        };
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(failureDetector, 2000, 1000, TimeUnit.MILLISECONDS);
    }

    public Set<Member> getReplicas() {
        var replicas = new HashMap<>(members);
        replicas.remove(id);
        return new HashSet<>(new TreeMap<>(replicas).values());
    }

    public Set<Member> getMembers() {
        return new HashSet<>(new TreeMap<>(members).values());
    }

    private synchronized void incrementClock() {
        long replicaSequence = this.clock.getTimestampForReplica(id) + 1;
        this.clock.setReplicaTimestamp(id, replicaSequence);
    }

    private synchronized void addAll(MemberMessage memberMessage) {
        if (clock.isAfter(memberMessage.getVectorClock())) {
            LOG.info("Members message is after current clock and will be ignored. Member in message is {}.",
                    memberMessage.toString());
            return;
        }
        clock.merge(memberMessage.getVectorClock());
        LOG.info("Current clock state is {}", clock);
        this.members.clear();
        memberMessage.getMembers().forEach(member -> members.put(member.getId(), member));
        LOG.info("Members are: {}", getMemberIds());
    }

    private Set<String> getMemberIds() {
        return getMembers().stream().map(Member::toString).collect(Collectors.toSet());
    }

    @Override
    public void handle(ConnectionDetails details, BaseMessage message) {
        if (serverType.isMaster()) {
            handleMasterMessages(details, message);
        }
    }

    @Override
    public List<Class<? extends BaseMessage>> getResponsibleMessageTypes() {
        return RESPONSIBLE_MULTICAST_MESSAGE_TYPES;
    }

    private void handleMasterMessages(ConnectionDetails details, BaseMessage message) {
        if (message instanceof JoinMessage) {
            var joinMessage = (JoinMessage) message;
            if (joinMessage.getId().equals(id)) {
                LOG.info("JoinMessage has same id as this instance and will be ignored.");
                return;
            }
            // 1. Create the member
            Member member = new Member(joinMessage.getId(), details.getFrom().getHostAddress(), joinMessage.getTcpPort());
            var tcpClient = new TcpClient(details.getFrom(), joinMessage.getTcpPort());
            try {
                //2. Check if member is healthy
                tcpClient.send(new JoinAckMessage(clusterState.get()));
            } catch (IOException e) {
                LOG.info("Member with id {} and address {}:{} was not reachable and will be removed.", member.getId(), member.getAddress(), member.getTcpPort());
            }
            //3. Add member to the members list
            members.put(member.getId(), member);
            //4. Multicast the new members list
            incrementClock();
            unicastMessageSender.sendMessageToAllMembers(new MemberMessage(getMembers(), clock.copy()));
            LOG.info("Members are: {}", getMemberIds());
            return;
        }

        if (message instanceof GetMasterMessage) {
            LOG.info("Sending GetMasterMessageResponse over multicast.");
            multicastPublisher.publish(new GetMasterMessageResponse(member.getTcpPort()));
            ChatMessage lastChatMessage = clusterState.get().getLastChatMessage();
            if (lastChatMessage != null) {
                multicastPublisher.publish(lastChatMessage);
            }
            return;
        }
        throw new UnsupportedOperationException("Message of type " + message.getType() + " is not handled.");
    }

    public void setClusterState(Supplier<ClusterState> clusterState) {
        this.clusterState = clusterState;
    }

    public void setClusterStateReceiveHandler(Consumer<ClusterState> onReceiveClusterState) {
        this.onReceiveClusterState = onReceiveClusterState;
    }

    @Override
    public String handleUnicast(ConnectionDetails details, BaseMessage message) {
        if (message instanceof JoinAckMessage) {
            // When a acknowledgement for a Join message is received
            // a master is already available. Therefore this instance is going to be marked as replica.
            LOG.info("Master already available. Going to be a replica.");
            serverType.setIsMaster(false);
            JoinAckMessage ackMessage = (JoinAckMessage) message;
            onReceiveClusterState.accept(ackMessage.getClusterState());
            return Constants.OK_RESPONSE;
        }

        if (serverType.isMaster()) {
            return Constants.OK_RESPONSE;
        }
        if (message instanceof MemberMessage) {
            incrementClock();
            addAll((MemberMessage) message);
        }
        return Constants.ERROR_RESPONSE;
    }

    @Override
    public List<Class<? extends BaseMessage>> getResponsibleUnicastMessageTypes() {
        return RESPONSIBLE_UNICAST_MESSAGE_TYPES;
    }

    public void sendJoinMessage() {
        multicastPublisher.publish(new JoinMessage(member.getId(), member.getTcpPort()));
    }

    private class TransmissionFailureHandler implements Consumer<Member> {

        @Override
        public void accept(Member member) {
            LOG.info("Member with id {} and address {}:{} was not reachable and will be removed.", member.getId(), member.getAddress(), member.getTcpPort());
            members.remove(member.getId());
            unicastMessageSender.sendMessageToAllMembers(new MemberMessage(getMembers(), clock));
        }
    }

}
