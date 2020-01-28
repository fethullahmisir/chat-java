package com.dbe.distributed.systems.client;

import com.dbe.distributed.systems.shared.messages.ChatMessage;
import com.dbe.distributed.systems.shared.network.MulticastPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class HoldBackQueueTest {
    @Test
    public void testPriorityQueue() throws InterruptedException {
        HoldBackQueue holdBackQueue = new HoldBackQueue(Mockito.mock(MulticastPublisher.class),
                Mockito.mock(Consumer.class));

        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(holdBackQueue::deliverMessages, 1, 1, TimeUnit.SECONDS);

        holdBackQueue.handle(null, new ChatMessage(1L, "Test", "Test", null));
        holdBackQueue.handle(null, new ChatMessage(4L, "Test", "Test", null));

        Thread.sleep(2000);
        holdBackQueue.handle(null, new ChatMessage(2L, "Test", "Test", null));
        Thread.sleep(8000);
        holdBackQueue.handle(null, new ChatMessage(3L, "Test", "Test", null));

        Thread.sleep(10000);
    }

    @Test
    public void testGetRangeExpectValidValuesInRange() {
        List<Long> range1 = HoldBackQueue.getRange(0, 3);
        assertThat(range1).containsOnly(1L, 2L);

        List<Long> range2 = HoldBackQueue.getRange(0, 3);
        assertThat(range2).containsOnly(1L, 2L);

        List<Long> range3 = HoldBackQueue.getRange(123, 128);
        assertThat(range3).containsOnly(124L, 125L, 126L, 127L);
    }

}