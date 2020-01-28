package com.dbe.distributed.systems.shared.network;

import com.dbe.distributed.systems.shared.messages.BaseMessage;
import com.dbe.distributed.systems.shared.messages.MessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.List;

public class MulticastReceiver implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(MulticastReceiver.class.getName());

    private InetAddress networkInterfaceAddress;
    private MessageHandler messageHandler;
    private int multicastPort;
    private InetAddress group;

    public MulticastReceiver(int multicastPort, InetAddress group, InetAddress networkInterfaceAddress, MessageHandler messageHandler) {
        this.multicastPort = multicastPort;
        this.group = group;
        this.networkInterfaceAddress = networkInterfaceAddress;
        this.messageHandler = messageHandler;
    }


    public interface MessageHandler {
        void handle(ConnectionDetails details, BaseMessage message);

        List<Class<? extends BaseMessage>> getResponsibleMessageTypes();
    }

    public void run() {
        try {
            MulticastSocket socket = new MulticastSocket(multicastPort);
            socket.setLoopbackMode(false);
            socket.setBroadcast(true);
            //TODO add network interface ip address
            socket.setInterface(networkInterfaceAddress);
            //set -Djava.net.preferIPv4Stack=true
            socket.setTimeToLive(1);
            //   InetSocketAddress inetSocketAddress = new InetSocketAddress(group, multicastPort);
            //socket.joinGroup(inetSocketAddress, NetworkInterface.getByName("192.168.0.192"));
            socket.joinGroup(group);
            logger.info("Listening for multicast messages on port {} and group {}.", multicastPort, group.getHostAddress());
            while (true) {
                byte[] buf = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                messageHandler.handle(new ConnectionDetails(packet.getAddress(), packet.getPort()), MessageConverter.deserialize(receivedMessage));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
