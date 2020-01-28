package com.dbe.distributed.systems.shared.messages;

import com.dbe.distributed.systems.shared.clock.VectorClock;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatMessage extends BaseMessage {
    private final Long sequence;
    private final String text;
    private final String name;
    private final VectorClock vectorClock;

    public ChatMessage(@JsonProperty("sequence") Long sequence, @JsonProperty("text") String text, @JsonProperty("name") String name,
                       @JsonProperty("vectorClock") VectorClock vectorClock) {
        super(ChatMessage.class.getSimpleName());
        this.text = text;
        this.name = name;
        this.sequence = sequence;
        this.vectorClock = vectorClock;
    }

    public static ChatMessage of(String text, String name) {
        return new ChatMessage(0L, text, name, new VectorClock());
    }

    public ChatMessage withSequence(Long sequence) {
        return new ChatMessage(sequence, this.text, this.name, this.vectorClock);
    }

    public ChatMessage withVectorClock(VectorClock vectorClock) {
        return new ChatMessage(this.sequence, this.text, this.name, vectorClock);
    }

    public Long getSequence() {
        return sequence;
    }

    public String getText() {
        return text;
    }

    public String getName() {
        return name;
    }

    public VectorClock getVectorClock() {
        return vectorClock;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "sequence=" + sequence +
                ", text='" + text + '\'' +
                ", name='" + name + '\'' +
                "} ";
    }
}
