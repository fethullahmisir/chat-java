package com.dbe.distributed.systems.server.cluster;

import com.dbe.distributed.systems.shared.messages.BaseMessage;
import com.dbe.distributed.systems.shared.network.ConnectionDetails;
import com.dbe.distributed.systems.shared.network.MulticastReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class MulticastHandler implements MulticastReceiver.MessageHandler {
    private static final Logger LOG = LoggerFactory.getLogger(MulticastHandler.class.getName());

    private Map<Class<?>, MulticastReceiver.MessageHandler> multicastHandlers;
    private Member member;

    public MulticastHandler(Member member, Map<Class<?>, MulticastReceiver.MessageHandler> handlers) {
        this.multicastHandlers = handlers;
        this.member = member;
    }

    @Override
    public void handle(ConnectionDetails details, BaseMessage message) {
        LOG.info("Multicast received with type {}", message.getType());
        MulticastReceiver.MessageHandler messageHandler = multicastHandlers.get(message.getClass());
        if (null == messageHandler) {
            LOG.info("There is no multicast handler registered for message type {}.", message.getType());
            return;
        }
        try {
            messageHandler.handle(details, message);
        } catch (Exception e) {
            LOG.error("Failed to handle message with type {}", message.getType(), e);
        }
    }

    @Override
    public List<Class<? extends BaseMessage>> getResponsibleMessageTypes() {
        throw new UnsupportedOperationException("This operation is not supported for the root multicast message handler.");
    }

}