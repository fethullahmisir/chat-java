package com.dbe.distributed.systems.server.cluster;

import com.dbe.distributed.systems.shared.messages.BaseMessage;
import com.dbe.distributed.systems.shared.network.ConnectionDetails;
import com.dbe.distributed.systems.shared.network.TcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class UnicastHandler implements TcpServer.MessageHandler {
    private static final Logger LOG = LoggerFactory.getLogger(UnicastHandler.class);

    private final Map<Class<?>, TcpServer.MessageHandler> messageHandlers;

    public UnicastHandler(Map<Class<?>, TcpServer.MessageHandler> messageHandlers) {
        this.messageHandlers = messageHandlers;
    }

    @Override
    public String handleUnicast(ConnectionDetails details, BaseMessage message) {
        TcpServer.MessageHandler messageHandler = messageHandlers.get(message.getClass());
        if (null == messageHandler) {
            throw new IllegalArgumentException(String.format("There is no tcp message handler registered for message type %s.", message.getType()));
        }
        return messageHandler.handleUnicast(details, message);
    }

    @Override
    public List<Class<? extends BaseMessage>> getResponsibleUnicastMessageTypes() {
        throw new UnsupportedOperationException("This operation is not supported for the root multicast message handler.");
    }

}