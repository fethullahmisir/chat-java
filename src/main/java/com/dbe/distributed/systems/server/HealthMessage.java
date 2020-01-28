package com.dbe.distributed.systems.server;

import com.dbe.distributed.systems.shared.messages.BaseMessage;

public class HealthMessage extends BaseMessage {

    public HealthMessage() {
        super(HealthMessage.class.getSimpleName());
    }

    @Override
    public String toString() {
        return "HealthMessage{}";
    }
}
