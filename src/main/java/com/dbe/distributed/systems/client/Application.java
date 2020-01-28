package com.dbe.distributed.systems.client;


import com.dbe.distributed.systems.shared.ApplicationProperties;
import com.dbe.distributed.systems.shared.Constants;
import com.dbe.distributed.systems.shared.messages.GetMasterMessage;
import com.dbe.distributed.systems.shared.network.MulticastPublisher;
import com.dbe.distributed.systems.shared.network.MulticastReceiver;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Application {

    public static void main(String[] args) throws UnknownHostException, InterruptedException {
        InetAddress multicastAddress = ApplicationProperties.getMulticastAddress();
        var multicastPublisher = new MulticastPublisher(multicastAddress, Constants.MULTICAST_PORT);
        var chatController = new ChatController(new MulticastPublisher(multicastAddress, Constants.MULTICAST_PORT));

        new Thread(new MulticastReceiver(Constants.MULTICAST_PORT, multicastAddress, ApplicationProperties.getIpAddress(), chatController)).start();
        var joinUI = new JoinUI(chatController);
        joinUI.setVisible(true);

        Thread.sleep(2000);
        multicastPublisher.publish(new GetMasterMessage());
    }

}
