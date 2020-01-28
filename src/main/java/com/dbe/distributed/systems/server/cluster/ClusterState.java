package com.dbe.distributed.systems.server.cluster;

import com.dbe.distributed.systems.shared.messages.ChatMessage;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.TreeSet;

public class ClusterState {
    private final Map<Long, ChatMessage> chatMessages;
    private final Long sequence;

    public ClusterState(@JsonProperty("chatMessages") Map<Long, ChatMessage> chatMessages, @JsonProperty("sequence") Long sequence) {
        this.chatMessages = chatMessages;
        this.sequence = sequence;
    }

    public Map<Long, ChatMessage> getChatMessages() {
        return chatMessages;
    }

    public Long getSequence() {
        return sequence;
    }

    @JsonIgnore
    public ChatMessage getLastChatMessage() {
        if (chatMessages.isEmpty()) {
            return null;
        }
        Long last = new TreeSet<>(chatMessages.keySet()).last();
        return chatMessages.get(last);
    }
}
