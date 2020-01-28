package com.dbe.distributed.systems.shared.messages;

public class GetMasterMessage extends BaseMessage {
    public GetMasterMessage() {
        super(GetMasterMessage.class.getSimpleName());
    }
}
