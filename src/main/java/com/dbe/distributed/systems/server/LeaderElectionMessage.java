package com.dbe.distributed.systems.server;

import com.dbe.distributed.systems.shared.messages.BaseMessage;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LeaderElectionMessage extends BaseMessage {

    private final String id;

    public LeaderElectionMessage(@JsonProperty("id") String id) {
        super(LeaderElectionMessage.class.getSimpleName());
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
