package com.dbe.distributed.systems.server.cluster;

import com.dbe.distributed.systems.server.HealthMessage;
import com.dbe.distributed.systems.server.LeaderElectedMessage;
import com.dbe.distributed.systems.server.LeaderElectionMessage;
import com.dbe.distributed.systems.shared.Constants;
import com.dbe.distributed.systems.shared.messages.BaseMessage;
import com.dbe.distributed.systems.shared.network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LeaderElectionService implements TcpServer.MessageHandler, MulticastReceiver.MessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LeaderElectionMessage.class);

    private static final List<Class<? extends BaseMessage>> RESPONSIBLE_UNICAST_MESSAGE_TYPES =
            List.of(LeaderElectionMessage.class, HealthMessage.class);

    private static final List<Class<? extends BaseMessage>> RESPONSIBLE_MULTICAST_MESSAGE_TYPES =
            List.of(LeaderElectedMessage.class);


    private volatile boolean isElectionRunning = false;
    private final String id;
    private final ServerType serverType;
    private final Supplier<Set<Member>> members;
    private final MulticastPublisher multicastPublisher;
    private final FailureDetector failureDetector;

    public LeaderElectionService(Member thisInstanceMember,
                                 ServerType serverType,
                                 Supplier<Set<Member>> members,
                                 MulticastPublisher multicastPublisher) {
        this.id = thisInstanceMember.getId();
        this.serverType = serverType;
        this.members = members;
        this.multicastPublisher = multicastPublisher;
        this.failureDetector = new FailureDetector(new FailureDetectionListener());
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(failureDetector, 2000, 100, TimeUnit.MILLISECONDS);
    }


    @Override
    public String handleUnicast(ConnectionDetails details, BaseMessage message) {
        if (message instanceof LeaderElectionMessage) {
            var electionMessage = (LeaderElectionMessage) message;

            int electionIdCompareResult = electionMessage.getId().compareTo(id);
            LOG.info("The election id comparision between this member with id {} and the received id {} is {}.",
                    id, electionMessage.getId(), electionIdCompareResult);
            // When this instance receives it's own id, the election is finished.
            if (electionIdCompareResult == 0) {
                multicastPublisher.publish(new LeaderElectedMessage(id));
            }
            Set<Member> members = this.members.get();
            // When the received id is bigger then the the id of this instance the received message will be send to
            // the next member.
            if (electionIdCompareResult > 0) {
                sendElectionMessage(members, id, electionMessage);
                return Constants.OK_RESPONSE;
            }
            // When the received id is smaller, then the id of this instance will be send to the next member.
            if (electionIdCompareResult < 0) {
                sendElectionMessage(members, id, new LeaderElectionMessage(id));
                return Constants.OK_RESPONSE;
            }
        }

        if (message instanceof HealthMessage) {
            failureDetector.onHealthMessageReceived();
            LOG.info("Received HealthMessage responding with OK.");
            return Constants.OK_RESPONSE;
        }
        return String.format("%s : Message of type %s is not supported by the %s handler. ",
                Constants.ERROR_RESPONSE, message.getType(), this.getClass().getSimpleName());
    }

    @Override
    public List<Class<? extends BaseMessage>> getResponsibleUnicastMessageTypes() {
        return RESPONSIBLE_UNICAST_MESSAGE_TYPES;
    }

    @Override
    public void handle(ConnectionDetails details, BaseMessage message) {
        if (message instanceof LeaderElectedMessage) {
            isElectionRunning = false;
            var electedMessage = (LeaderElectedMessage) message;
            String electedLeaderId = electedMessage.getElectedLeaderId();
            LOG.info("The member with address {} is elected as new leader.", electedLeaderId);
            serverType.setIsMaster(electedLeaderId.equals(this.id));
        }
    }

    @Override
    public List<Class<? extends BaseMessage>> getResponsibleMessageTypes() {
        return RESPONSIBLE_MULTICAST_MESSAGE_TYPES;
    }

    public void startLeaderElection() {
        if (isElectionRunning) {
            return;
        }
        isElectionRunning = true;
        Set<Member> members = this.members.get();
        sendElectionMessage(members, id, new LeaderElectionMessage(id));
    }

    public void sendElectionMessage(Set<Member> members, String targetMemberId, LeaderElectionMessage electionMessage) {
        Member targetMember = getNextMemberAfter(members, targetMemberId);
        try {
            LOG.info("Sending election message with id {} to the next member with address {}.",
                    electionMessage, targetMember.getAddress());
            var tcpClient = new TcpClient(InetAddress.getByName(targetMember.getAddress()), targetMember.getTcpPort());
            tcpClient.send(electionMessage);
        } catch (IOException e) {
            LOG.info("Member {} was not reachable for the leader election message and will be removed. Leader election message will send to next member.", targetMember);
            members.remove(targetMember);
            sendElectionMessage(members, targetMemberId, electionMessage);
        }
    }

    static Member getNextMemberAfter(Set<Member> members, String id) {
        return members.stream()
                .filter(val -> val.getId().compareTo(id) > 0)
                .findFirst()
                .orElse(members.iterator().next());
    }

    private class FailureDetectionListener implements Consumer<Long> {

        @Override
        public void accept(Long aLong) {
            LOG.info("Nothing heard from master since {} seconds. Going to start leader election.", aLong);
            startLeaderElection();
        }
    }

    private class FailureDetector implements Runnable {

        private long lastTimeHealthMessageReceived = System.currentTimeMillis();
        private Consumer<Long> onFailureDetected;

        public FailureDetector(Consumer<Long> onFailureDetected) {
            this.onFailureDetected = onFailureDetected;
        }

        public void onHealthMessageReceived() {
            lastTimeHealthMessageReceived = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        }

        @Override
        public void run() {
            if (serverType.isMaster()) {
                return;
            }
            long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - lastTimeHealthMessageReceived;
            if (diffInSeconds > 2) {
                onFailureDetected.accept(diffInSeconds);
            }
        }
    }
}
