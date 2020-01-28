package com.dbe.distributed.systems.server;

import com.dbe.distributed.systems.shared.messages.BaseMessage;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LeaderElectedMessage extends BaseMessage {
    private final String electedLeaderId;

    public LeaderElectedMessage(@JsonProperty("electedLeaderId") String electedLeaderId) {
        super(LeaderElectedMessage.class.getSimpleName());
        this.electedLeaderId = electedLeaderId;
    }
    
    public String getElectedLeaderId() {
        return electedLeaderId;
    }
}
