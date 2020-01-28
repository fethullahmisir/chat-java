package com.dbe.distributed.systems.client;

import com.dbe.distributed.systems.shared.clock.VectorClock;
import com.dbe.distributed.systems.shared.messages.BaseMessage;
import com.dbe.distributed.systems.shared.messages.ChatMessage;
import com.dbe.distributed.systems.shared.messages.GetMissingChatMessages;
import com.dbe.distributed.systems.shared.network.ConnectionDetails;
import com.dbe.distributed.systems.shared.network.MulticastPublisher;
import com.dbe.distributed.systems.shared.network.MulticastReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class HoldBackQueue implements MulticastReceiver.MessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HoldBackQueue.class);

    private final Map<Long, ChatMessage> chatMessageHistory = new ConcurrentHashMap<>();
    private final PriorityBlockingQueue<ChatMessage> priorityQueue = new PriorityBlockingQueue<>(10,
            new ChatMessageComparator());

    private final PriorityBlockingQueue<ChatMessage> orderedDeliveryQueue = new PriorityBlockingQueue<>(10,
            (o1, o2) -> VectorClock.compare(o1.getVectorClock(), o2.getVectorClock()));


    private final Object LOCK = new Object();

    private AtomicLong lastDeliveredMessage = new AtomicLong();
    private MulticastPublisher publisher;
    private Consumer<ChatMessage> messageDeliveryHandler;

    public HoldBackQueue(MulticastPublisher publisher, Consumer<ChatMessage> messageDeliveryHandler) {
        this.publisher = publisher;
        this.messageDeliveryHandler = messageDeliveryHandler;
    }

    @Override
    public void handle(ConnectionDetails details, BaseMessage message) {
        if (message instanceof ChatMessage) {
            ChatMessage chatMessage = (ChatMessage) message;
            synchronized (LOCK) {
                if (chatMessageHistory.containsKey(chatMessage.getSequence())) {
                    return;
                }
                chatMessageHistory.put(chatMessage.getSequence(), chatMessage);
                priorityQueue.put(chatMessage);
                orderedDeliveryQueue.put(chatMessage);
            }
        }
    }

    @Override
    public List<Class<? extends BaseMessage>> getResponsibleMessageTypes() {
        return List.of(ChatMessage.class);
    }

    public void deliverMessages() {
        if (priorityQueue.size() == 0) {
            return;
        }
        Long queueHeadSequence = priorityQueue.peek().getSequence();
        long diff = queueHeadSequence - lastDeliveredMessage.get();
        if (diff == 1) {
            ChatMessage messageToDeliver = priorityQueue.poll();
            Long sequence = messageToDeliver.getSequence();
            lastDeliveredMessage.set(sequence);

            ChatMessage messageOrdered = orderedDeliveryQueue.poll();
            LOG.info("Delivering message with sequence {}. Ordered message sequence is {}", sequence, messageOrdered.getSequence());
            LOG.info("Vector clock of chat message is {}", messageOrdered.getVectorClock().getReplicaTimestamps());
            messageDeliveryHandler.accept(messageOrdered);
        } else {
            List<Long> missingMessageSequences = getRange(lastDeliveredMessage.get(), queueHeadSequence);
            if (missingMessageSequences.isEmpty()) {
                return;
            }
            LOG.info("There are missing messages for sequences {}. Going to request them.",
                    missingMessageSequences.toString());
            publisher.publish(new GetMissingChatMessages(missingMessageSequences));
        }
    }

    static List<Long> getRange(long startExclusive, long endExclusive) {
        return LongStream.range(startExclusive + 1, endExclusive).boxed().collect(Collectors.toList());
    }

    private static class ChatMessageComparator implements Comparator<ChatMessage> {

        @Override
        public int compare(ChatMessage o1, ChatMessage o2) {
            return o1.getSequence().compareTo(o2.getSequence());
        }
    }

}
