package com.dbe.distributed.systems.server.cluster;

import com.dbe.distributed.systems.shared.messages.BaseMessage;
import com.dbe.distributed.systems.shared.network.TcpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UnicastMessageSender {

    private static final Logger LOG = LoggerFactory.getLogger(UnicastMessageSender.class);

    private Supplier<Set<Member>> members;
    private Consumer<Member> transmissionFailureHandler;
    private ExecutorService executorService;

    public UnicastMessageSender(Supplier<Set<Member>> members, Consumer<Member> transmissionFailureHandler) {
        this.members = members;
        this.transmissionFailureHandler = transmissionFailureHandler;
        this.executorService = Executors.newFixedThreadPool(10);
    }

    public void sendMessageToAllMembers(BaseMessage baseMessage) {
        members.get().forEach(member -> executorService.submit(() -> sendMessage(member, baseMessage)));
    }

    private void sendMessage(Member member, BaseMessage baseMessage) {
        try {
            var tcpClient = new TcpClient(InetAddress.getByName(member.getAddress()), member.getTcpPort());
            tcpClient.send(baseMessage);
        } catch (Exception e) {
            LOG.info("Member with id {} and address {}:{} was not reachable. Reason is {}.", member.getId(), member.getAddress(), member.getTcpPort(), e.getMessage());
            transmissionFailureHandler.accept(member);
        }
    }


}
