package com.dbe.distributed.systems.server;

import com.dbe.distributed.systems.server.cluster.ClusterState;
import com.dbe.distributed.systems.shared.messages.BaseMessage;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JoinAckMessage extends BaseMessage {
    private final ClusterState clusterState;

    public JoinAckMessage(@JsonProperty("clusterState") ClusterState clusterState) {
        super(JoinAckMessage.class.getSimpleName());
        this.clusterState = clusterState;
    }

    public ClusterState getClusterState() {
        return clusterState;
    }

    @Override
    public String toString() {
        return "JoinAckMessage{}";
    }
}