package com.dbe.distributed.systems.shared.network;

import com.dbe.distributed.systems.shared.messages.BaseMessage;
import com.dbe.distributed.systems.shared.messages.MessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class MulticastPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(MulticastPublisher.class);
    private InetAddress group;
    private final int multiCastPort;

    private DatagramSocket socket;

    public MulticastPublisher(InetAddress group, int multiCastPort) {
        this.group = group;
        this.multiCastPort = multiCastPort;
    }

    public void publish(BaseMessage message) {
        try {
            if (socket == null) {
                socket = new DatagramSocket();
            }
            String messageToSend = MessageConverter.serialize(message);
            byte[] buf = messageToSend.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, group, multiCastPort);
            LOG.info("Sending a Multicast message of type {}", message.getType());
            LOG.info("Raw message is {}", messageToSend);
            socket.send(packet);
        } catch (IOException e) {
            throw new RuntimeException("Failed to send multicast message.", e);
        }
    }

    public void close() {
        if (null != socket && !socket.isClosed()) {
            socket.close();
        }
    }

}