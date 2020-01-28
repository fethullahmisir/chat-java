package com.dbe.distributed.systems.server.cluster;

import com.dbe.distributed.systems.server.SequenceMessage;
import com.dbe.distributed.systems.shared.Constants;
import com.dbe.distributed.systems.shared.HoldBackQueue;
import com.dbe.distributed.systems.shared.clock.VectorClock;
import com.dbe.distributed.systems.shared.messages.BaseMessage;
import com.dbe.distributed.systems.shared.messages.ChatMessage;
import com.dbe.distributed.systems.shared.messages.GetMissingChatMessages;
import com.dbe.distributed.systems.shared.network.ConnectionDetails;
import com.dbe.distributed.systems.shared.network.MulticastPublisher;
import com.dbe.distributed.systems.shared.network.MulticastReceiver;
import com.dbe.distributed.systems.shared.network.TcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class ChatService implements MulticastReceiver.MessageHandler, TcpServer.MessageHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ChatService.class);

    private static final List<Class<? extends BaseMessage>> RESPONSIBLE_UNICAST_MESSAGE_TYPES = List.of(ChatMessage.class, SequenceMessage.class);

    private static final List<Class<? extends BaseMessage>> RESPONSIBLE_MULTICAST_MESSAGE_TYPES =
            List.of(ChatMessage.class, GetMissingChatMessages.class);

    private HoldBackQueue chatMessageHoldBackQueue;

    private final VectorClock clock;
    private final String replicaId;
    private final MulticastPublisher multicastPublisher;
    private final Supplier<Boolean> isMaster;
    private final UnicastMessageSender unicastMessageSender;

    private AtomicLong sequence = new AtomicLong(0);

    public ChatService(String replicaId, Supplier<Boolean> isMaster, MulticastPublisher multicastPublisher,
                       UnicastMessageSender unicastMessageSender) {
        this.clock = new VectorClock();
        this.isMaster = isMaster;
        this.multicastPublisher = multicastPublisher;
        this.replicaId = replicaId;
        this.clock.setReplicaTimestamp(replicaId, BigDecimal.ZERO.longValue());
        this.unicastMessageSender = unicastMessageSender;
        this.chatMessageHoldBackQueue = new HoldBackQueue(multicastPublisher);
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(chatMessageHoldBackQueue::getMissingMessages,
                2000, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    public void handle(ConnectionDetails details, BaseMessage message) {

        if (isMaster()) {
            handleMasterMulticastMessages(message);
            return;
        }

        if (message instanceof ChatMessage) {
            incrementClock();
            ChatMessage chatMessage = ((ChatMessage) message);
            clock.merge(chatMessage.getVectorClock());
            chatMessageHoldBackQueue.add(chatMessage);
            return;
        }
    }

    private void handleMasterMulticastMessages(BaseMessage message) {
        if (message instanceof GetMissingChatMessages) {
            var chatMessages = chatMessageHoldBackQueue.getChatMessages();
            var retransmissionMessage = (GetMissingChatMessages) message;
            retransmissionMessage.getMissingChatMessageSequences().stream()
                    .filter(chatMessages::containsKey)
                    .map(chatMessages::get)
                    .forEach(multicastPublisher::publish);
        }
    }

    @Override
    public List<Class<? extends BaseMessage>> getResponsibleMessageTypes() {
        return RESPONSIBLE_MULTICAST_MESSAGE_TYPES;
    }

    @Override
    public String handleUnicast(ConnectionDetails details, BaseMessage message) {
        if (isMaster()) {
            return handleMasterMessages(message);
        }

        if (message instanceof SequenceMessage) {
            var sequenceOfMessage = ((SequenceMessage) message).getSequence();
            setSquenceWhenGreater(sequenceOfMessage);
        }
        return Constants.OK_RESPONSE;
    }

    private void setSquenceWhenGreater(final Long sequenceToSet) {
        sequence.updateAndGet(internalSequence -> {
            if (internalSequence > sequenceToSet) {
                LOG.info("Received sequence {} is smaller then the internal sequence {} will be ignored.", sequenceToSet, internalSequence);
                return internalSequence;
            }
            LOG.info("Received sequence {} is greate then the internal sequence {} will be set.", sequenceToSet, internalSequence);
            return sequenceToSet;
        });
    }

    public void setClusterState(ClusterState clusterState) {
        sequence.set(clusterState.getSequence());
        chatMessageHoldBackQueue.addAll(clusterState.getChatMessages());
    }

    private String handleMasterMessages(BaseMessage message) {
        if (message instanceof ChatMessage) {
            incrementClock();
            long nextSequence = sequence.incrementAndGet();
            unicastMessageSender.sendMessageToAllMembers(new SequenceMessage(nextSequence));
            var chatMessage = ((ChatMessage) message).withSequence(nextSequence);
            clock.merge(chatMessage.getVectorClock());
            incrementClock();
            ChatMessage messageToSend = chatMessage.withVectorClock(clock.copy());
            chatMessageHoldBackQueue.add(messageToSend);
            multicastPublisher.publish(messageToSend);
            return Constants.OK_RESPONSE;
        }
        LOG.info("Message of type {} is not handled by the leader.", message.getType());
        return Constants.OK_RESPONSE;
    }

    @Override
    public List<Class<? extends BaseMessage>> getResponsibleUnicastMessageTypes() {
        return RESPONSIBLE_UNICAST_MESSAGE_TYPES;
    }

    private boolean isMaster() {
        return isMaster.get();
    }

    public ClusterState getClusterState() {
        return new ClusterState(chatMessageHoldBackQueue.getChatMessages(), sequence.get());
    }

    private synchronized void incrementClock() {
        long replicaSequence = this.clock.getTimestampForReplica(replicaId) + 1;
        this.clock.setReplicaTimestamp(replicaId, replicaSequence);
    }

}
