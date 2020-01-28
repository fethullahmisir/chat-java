package com.dbe.distributed.systems.server;

import com.dbe.distributed.systems.server.cluster.Member;
import com.dbe.distributed.systems.shared.clock.VectorClock;
import com.dbe.distributed.systems.shared.messages.BaseMessage;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public class MemberMessage extends BaseMessage {
    private final VectorClock vectorClock;
    private final Set<Member> members;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public MemberMessage(@JsonProperty("members") Set<Member> member,
                         @JsonProperty("vectorClock") VectorClock vectorClock) {
        super(MemberMessage.class.getSimpleName());
        this.members = member;
        this.vectorClock = vectorClock;
    }


    public Set<Member> getMembers() {
        return members;
    }

    public VectorClock getVectorClock() {
        return vectorClock;
    }

    @Override
    public String toString() {
        return "MemberMessage{" +
                "member=" + members.toString() +
                "}";
    }
}
