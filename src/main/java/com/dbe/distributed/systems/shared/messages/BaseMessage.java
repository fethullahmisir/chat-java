package com.dbe.distributed.systems.shared.messages;

public abstract class BaseMessage {
    private final String type;

    protected BaseMessage(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

}
