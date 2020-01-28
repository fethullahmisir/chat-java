package com.dbe.distributed.systems.server.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Member {

    private String id;
    private String address;
    private int tcpPort;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Member(@JsonProperty("id") String id, @JsonProperty("address") String address, @JsonProperty("tcpPort") int tcpPort) {
        this.id = id;
        this.address = address;
        this.tcpPort = tcpPort;
    }

    public String getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    @Override
    public String toString() {
        return String.format("id=%s | host=%s:%s", id, address, tcpPort);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Member member = (Member) o;
        return id.equals(member.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
