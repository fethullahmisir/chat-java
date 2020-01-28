package com.dbe.distributed.systems.shared.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class GetMissingChatMessages extends BaseMessage {
    private final List<Long> missingChatMessageSequences;

    public GetMissingChatMessages(@JsonProperty("missingChatMessageSequences") List<Long> missingChatMessageSequences) {
        super(GetMissingChatMessages.class.getSimpleName());
        this.missingChatMessageSequences = missingChatMessageSequences;
    }

    public List<Long> getMissingChatMessageSequences() {
        return missingChatMessageSequences;
    }
}
