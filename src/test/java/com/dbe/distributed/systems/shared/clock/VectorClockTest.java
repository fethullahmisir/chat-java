package com.dbe.distributed.systems.shared.clock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VectorClockTest {

    @Test
    public void testConcurrentVectorClockState() {
        var vectorClock = new VectorClock();
        vectorClock.setReplicaTimestamp("replica1", 2);

        var vectorClock2 = new VectorClock();
        vectorClock2.setReplicaTimestamp("replica2", 2);

        int compare = VectorClock.compare(vectorClock, vectorClock2);
        assertEquals(0, compare);
    }

    @Test
    public void testBeforeVectorClockState() {
        var vectorClock = new VectorClock();
        vectorClock.setReplicaTimestamp("replica1", 2);

        var vectorClock2 = new VectorClock();
        vectorClock2.setReplicaTimestamp("replica1", 3);

        int compare = VectorClock.compare(vectorClock, vectorClock2);
        assertEquals(-1, compare);
    }


    @Test
    public void testAfterVectorClockState() {
        var vectorClock = new VectorClock();
        vectorClock.setReplicaTimestamp("replica1", 3);

        var vectorClock2 = new VectorClock();
        vectorClock2.setReplicaTimestamp("replica1", 2);

        int compare = VectorClock.compare(vectorClock, vectorClock2);
        assertEquals(1, compare);
    }

}