package com.dbe.distributed.systems.client;

import com.dbe.distributed.systems.shared.clock.VectorClock;
import com.dbe.distributed.systems.shared.messages.BaseMessage;
import com.dbe.distributed.systems.shared.messages.ChatMessage;
import com.dbe.distributed.systems.shared.messages.GetMasterMessage;
import com.dbe.distributed.systems.shared.messages.GetMasterMessageResponse;
import com.dbe.distributed.systems.shared.network.ConnectionDetails;
import com.dbe.distributed.systems.shared.network.MulticastPublisher;
import com.dbe.distributed.systems.shared.network.MulticastReceiver;
import com.dbe.distributed.systems.shared.network.TcpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ChatController implements MulticastReceiver.MessageHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ChatController.class);

    private final HoldBackQueue holdBackQueue;
    private final MulticastPublisher multicastPublisher;
    private Consumer<ChatMessage> chatMessageConsumer;
    private boolean messageDeliveryStarted = false;
    private InetAddress serverAddress;
    private int tcpServerPort;
    private VectorClock clock;
    private final String id;

    public ChatController(MulticastPublisher multicastPublisher) throws UnknownHostException {
        this.multicastPublisher = multicastPublisher;
        this.id = UUID.randomUUID().toString();
        this.holdBackQueue = new HoldBackQueue(multicastPublisher, message -> {
            if (chatMessageConsumer != null) {
                // clock.merge(message.getVectorClock());
                chatMessageConsumer.accept(message);
            }
        });
        this.clock = new VectorClock();
        clock.setReplicaTimestamp(id, 0);
    }

    public void setChatMessageHandler(Consumer<ChatMessage> chatMessageHandler) {
        this.chatMessageConsumer = chatMessageHandler;
        startMessageDelivery();
    }

    private void startMessageDelivery() {
        if (messageDeliveryStarted) {
            return;
        }
        messageDeliveryStarted = true;
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(holdBackQueue::deliverMessages,
                200, 200, TimeUnit.MILLISECONDS);
    }

    public void sendTextMessage(ChatMessage chatMessage) {
        incrementClock();
        boolean success = false;
        ChatMessage chatMessageToSend = chatMessage.withVectorClock(clock.copy());
        int retryCount = 0;
        while (!success) {
            success = sendChatMessage(chatMessageToSend);
            retryCount = retryCount + 1;
            if (retryCount == 3) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public boolean sendChatMessage(ChatMessage chatMessage) {
        if (serverAddress == null) {
            throw new IllegalArgumentException("Server address must not be null.");
        }
        try {
            var tcpClient = new TcpClient(serverAddress, tcpServerPort);
            tcpClient.send(chatMessage);
            return true;
        } catch (Exception e) {
            LOG.info("Failed to send chat message to master due to {}. Going to discover new server.", e.getMessage());
            multicastPublisher.publish(new GetMasterMessage());
            return false;
        }
    }


    private synchronized void incrementClock() {
        long replicaSequence = this.clock.getTimestampForReplica(id) + 1;
        this.clock.setReplicaTimestamp(id, replicaSequence);
    }

    @Override
    public void handle(ConnectionDetails details, BaseMessage message) {
        if (message instanceof ChatMessage) {
            incrementClock();
            clock.merge(((ChatMessage) message).getVectorClock());
            holdBackQueue.handle(details, message);
        }
        if (message instanceof GetMasterMessageResponse) {
            LOG.info("Setting server address to {}", details.getFrom());
            var masterMessageResponse = (GetMasterMessageResponse) message;
            serverAddress = details.getFrom();
            tcpServerPort = masterMessageResponse.getTcpServerPort();
        }
    }

    @Override
    public List<Class<? extends BaseMessage>> getResponsibleMessageTypes() {
        return null;
    }

}
