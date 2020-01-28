package com.dbe.distributed.systems.shared;

import com.dbe.distributed.systems.shared.messages.ChatMessage;
import com.dbe.distributed.systems.shared.messages.GetMissingChatMessages;
import com.dbe.distributed.systems.shared.network.MulticastPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class HoldBackQueue {

    private static final Logger LOG = LoggerFactory.getLogger(HoldBackQueue.class);

    private final Map<Long, ChatMessage> chatMessageHistory = new ConcurrentHashMap<>();
    private final PriorityBlockingQueue<ChatMessage> priorityQueue = new PriorityBlockingQueue<>(10,
            new ChatMessageComparator());


    private final Object LOCK = new Object();

    private AtomicLong lastDeliveredMessage = new AtomicLong();
    private MulticastPublisher publisher;

    public HoldBackQueue(MulticastPublisher publisher) {
        this.publisher = publisher;
    }

    public void add(ChatMessage chatMessage) {
        synchronized (LOCK) {
            chatMessageHistory.put(chatMessage.getSequence(), chatMessage);
            priorityQueue.put(chatMessage);
            LOG.info("{} added.", chatMessage.toString());
        }
    }

    public void addAll(Map<Long, ChatMessage> chatMessages) {
        if (chatMessages.isEmpty()) {
            return;
        }
        Long last = new TreeSet<>(chatMessages.keySet()).last();
        LOG.info("Adding {} chat messages, and setting last delivered message sequence to {}.", chatMessages.size(), last);
        lastDeliveredMessage.set(last);
        chatMessageHistory.putAll(chatMessages);
    }

    public Map<Long, ChatMessage> getChatMessages() {
        return chatMessageHistory;
    }

    public void getMissingMessages() {
        if (priorityQueue.size() == 0) {
            return;
        }
        Long queueHeadSequence = priorityQueue.peek().getSequence();
        long diff = queueHeadSequence - lastDeliveredMessage.get();
        if (diff == 1) {
            ChatMessage messageToDeliver = priorityQueue.poll();
            Long sequence = messageToDeliver.getSequence();
            lastDeliveredMessage.set(sequence);
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
