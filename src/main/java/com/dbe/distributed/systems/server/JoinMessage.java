package com.dbe.distributed.systems.server;

import com.dbe.distributed.systems.shared.messages.BaseMessage;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JoinMessage extends BaseMessage {

    private final String id;
    private final int tcpPort;

    public JoinMessage(@JsonProperty("id") String id, @JsonProperty("tcpPort") int tcpPort) {
        super(JoinMessage.class.getSimpleName());
        this.id = id;
        this.tcpPort = tcpPort;
    }

    public String getId() {
        return id;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    @Override
    public String toString() {
        return "JoinMessage{}";
    }
}
