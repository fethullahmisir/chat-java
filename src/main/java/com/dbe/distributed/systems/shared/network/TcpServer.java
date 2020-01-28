package com.dbe.distributed.systems.shared.network;

import com.dbe.distributed.systems.shared.messages.BaseMessage;
import com.dbe.distributed.systems.shared.messages.MessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpServer implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(TcpServer.class);
    private final int port;
    private final ExecutorService executorService;
    private final MessageHandler handler;

    public TcpServer(int port, MessageHandler handler) {
        this.port = port;
        this.executorService = Executors.newFixedThreadPool(20);
        this.handler = handler;
    }

    @Override
    public void run() {
        try {
            LOG.info("Starting tcp server on port {}", port);
            ServerSocket server = new ServerSocket(port);
            while (true) {
                Socket socket = server.accept();
                executorService.execute(new ConnectionHandler(socket, handler));
            }
        } catch (IOException e) {
            LOG.error("TCP server error occurred.", e);
            throw new RuntimeException(e);
        }
    }

    public interface MessageHandler {
        String handleUnicast(ConnectionDetails details, BaseMessage message);

        List<Class<? extends BaseMessage>> getResponsibleUnicastMessageTypes();
    }


    private static class ConnectionHandler implements Runnable {
        private Socket socket;
        private MessageHandler handler;

        private ConnectionHandler(Socket socket, MessageHandler handler) {
            this.socket = socket;
            this.handler = handler;
        }

        @Override
        public void run() {
            try (var socket = this.socket; var writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
                 var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                String receivedMessage = reader.readLine();
                String response = handler.handleUnicast(new ConnectionDetails(socket.getInetAddress(), socket.getPort()),
                        MessageConverter.deserialize(receivedMessage));
                writer.write(response);
                writer.flush();
            } catch (Exception e) {
                LOG.error("Error occurred during tcp request processing.", e);
            }
        }
    }

}
