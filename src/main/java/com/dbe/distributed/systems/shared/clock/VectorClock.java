package com.dbe.distributed.systems.shared.clock;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class VectorClock {
    private Map<String, Long> replicaTimestamps = new ConcurrentHashMap<>();

    public VectorClock() {
    }

    public VectorClock(@JsonProperty("replicaTimestamps") Map<String, Long> replicaTimestamps) {
        this.replicaTimestamps = new ConcurrentHashMap<>(replicaTimestamps);
    }

    public Map<String, Long> getReplicaTimestamps() {
        return replicaTimestamps;
    }


    public Long getTimestampForReplica(String replicaId) {
        return replicaTimestamps.get(replicaId);
    }

    public void setReplicaTimestamp(String replicaId, long timestamp) {
        replicaTimestamps.put(replicaId, timestamp);
    }

    public VectorClock copy() {
        return new VectorClock(Map.copyOf(replicaTimestamps));
    }

    public void merge(VectorClock other) {
        for (Entry<String, Long> entry : other.replicaTimestamps.entrySet()) {
            final String replicaId = entry.getKey();
            final long mergingTimestamp = entry.getValue();
            final long localTimestamp = replicaTimestamps.getOrDefault(replicaId, Long.MIN_VALUE);
            replicaTimestamps.put(replicaId, Math.max(localTimestamp, mergingTimestamp));
        }
    }

    public boolean isAfter(VectorClock other) {
        return compare(this, other) == 1;
    }


    public static int compare(VectorClock left, VectorClock right) {
        Map<String, Long> leftVersions = left.replicaTimestamps;
        Map<String, Long> rightVersions = right.replicaTimestamps;
        Set<String> union = new HashSet<>(leftVersions.keySet());
        union.addAll(rightVersions.keySet());

        int leftGreater = 0;
        int rightGreater = 0;
        for (String key : union) {
            long leftValue = leftVersions.getOrDefault(key, 0L);
            long rightValue = rightVersions.getOrDefault(key, 0L);

            if (leftValue > rightValue) {
                leftGreater++;
            } else if (leftValue < rightValue) {
                rightGreater++;
            }
        }
        //before -1
        //after 1
        // concurrent 0
        return Long.compare(leftGreater, rightGreater);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        VectorClock that = (VectorClock) o;

        return replicaTimestamps.equals(that.replicaTimestamps);
    }

    @Override
    public int hashCode() {
        return replicaTimestamps.hashCode();
    }

    @Override
    public String toString() {
        return replicaTimestamps.toString();
    }

}