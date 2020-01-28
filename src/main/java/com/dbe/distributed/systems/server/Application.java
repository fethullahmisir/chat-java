package com.dbe.distributed.systems.server;


import com.dbe.distributed.systems.server.cluster.*;
import com.dbe.distributed.systems.shared.ApplicationProperties;
import com.dbe.distributed.systems.shared.Constants;
import com.dbe.distributed.systems.shared.network.MulticastPublisher;
import com.dbe.distributed.systems.shared.network.MulticastReceiver;
import com.dbe.distributed.systems.shared.network.TcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Application {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);


    public static void main(String[] args) throws Exception {
        InetAddress localhost = ApplicationProperties.getIpAddress();
        int tcpPort = ApplicationProperties.getTcpServerPort();
        Member member = new Member(ApplicationProperties.getMemberId(), localhost.getHostAddress(), tcpPort);
        LOG.info("Starting application with ip address {}", localhost);

        var multicastAddress = ApplicationProperties.getMulticastAddress();
        //InetAddress.getByName("169.254.25.135")
        var multicastPublisher = new MulticastPublisher(multicastAddress, Constants.MULTICAST_PORT);

        var serverType = new ServerType();
        var memberService = new MemberService(member, serverType, multicastPublisher);

        var leaderElectionService = new LeaderElectionService(member, serverType, memberService::getMembers, multicastPublisher);
        var chatService = new ChatService(member.getAddress(), serverType::isMaster, multicastPublisher, memberService.getUnicastMessageSender());

        memberService.setClusterState(chatService::getClusterState);
        memberService.setClusterStateReceiveHandler(chatService::setClusterState);

        var multicastHandlers = toMulticastHandlers(memberService, chatService, leaderElectionService);
        var unicastHandlers = toUnicastHandlers(memberService, chatService, leaderElectionService);

        new Thread(new TcpServer(member.getTcpPort(), new UnicastHandler(unicastHandlers))).start();
        new Thread(new MulticastReceiver(Constants.MULTICAST_PORT, multicastAddress, localhost,
                new MulticastHandler(member, multicastHandlers))).start();

        Thread.sleep(2000);
        memberService.sendJoinMessage();
    }


    private static Map<Class<?>, MulticastReceiver.MessageHandler> toMulticastHandlers(MulticastReceiver.MessageHandler... handlers) {
        return Stream.of(handlers)
                .map(handler -> handler.getResponsibleMessageTypes()
                        .stream()
                        .collect(Collectors.toMap(hel -> hel, hel -> handler)))
                .collect(DisallowDuplicateKeyHashMap::new, Map::putAll, Map::putAll);
    }

    private static Map<Class<?>, TcpServer.MessageHandler> toUnicastHandlers(TcpServer.MessageHandler... handlers) {
        return Stream.of(handlers)
                .map(handler -> handler.getResponsibleUnicastMessageTypes()
                        .stream()
                        .collect(Collectors.toMap(hel -> hel, hel -> handler)))
                .collect(DisallowDuplicateKeyHashMap::new, Map::putAll, Map::putAll);
    }

    private static class DisallowDuplicateKeyHashMap<K, V> extends HashMap<K, V> {

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            List<? extends K> duplicates = m.keySet().stream()
                    .filter(this::containsKey)
                    .collect(Collectors.toList());
            if (duplicates.isEmpty()) {
                m.forEach(this::put);
                return;
            }
            throw new IllegalArgumentException(String.format("Duplicate keys are not allowed. Duplicates are %s",
                    duplicates.toString()));
        }
    }

}
