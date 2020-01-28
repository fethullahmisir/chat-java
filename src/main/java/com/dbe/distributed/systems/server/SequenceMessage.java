package com.dbe.distributed.systems.server;

import com.dbe.distributed.systems.shared.messages.BaseMessage;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SequenceMessage extends BaseMessage {

    private final Long sequence;

    public SequenceMessage(@JsonProperty("sequence") Long sequence) {
        super(SequenceMessage.class.getSimpleName());
        this.sequence = sequence;
    }

    public Long getSequence() {
        return sequence;
    }
}
