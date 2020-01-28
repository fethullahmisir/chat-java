package com.dbe.distributed.systems.shared.network;

import java.net.InetAddress;

public class ConnectionDetails {

    private final InetAddress from;
    private final int port;

    public ConnectionDetails(InetAddress from, int port) {
        this.from = from;
        this.port = port;
    }


    public InetAddress getFrom() {
        return from;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "Message from=" + from;
    }
}
