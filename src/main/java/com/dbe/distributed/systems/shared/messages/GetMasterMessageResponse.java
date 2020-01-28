package com.dbe.distributed.systems.shared.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GetMasterMessageResponse extends BaseMessage {

    private int tcpServerPort;

    public GetMasterMessageResponse(@JsonProperty("tcpServerPort") int tcpServerPort) {
        super(GetMasterMessageResponse.class.getSimpleName());
        this.tcpServerPort = tcpServerPort;
    }

    public int getTcpServerPort() {
        return tcpServerPort;
    }
}
