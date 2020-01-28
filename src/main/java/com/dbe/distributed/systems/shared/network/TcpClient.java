package com.dbe.distributed.systems.shared.network;

import com.dbe.distributed.systems.shared.messages.BaseMessage;
import com.dbe.distributed.systems.shared.messages.MessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TcpClient {
    private static Logger logger = LoggerFactory.getLogger(TcpClient.class);
    private final InetAddress address;
    private final int port;

    private static final int MAX_RETRY_COUNT = 3;
    private static final int RETRY_WAIT_MILLIS = 150;

    public TcpClient(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public String send(BaseMessage message) throws IOException {
        boolean retry = true;
        int retryCount = 1;
        while (retry) {
            try (var socket = new Socket()) {
                socket.connect(new InetSocketAddress(address.getHostAddress(), port), 100);
                socket.setSoTimeout(100);
                try (var writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
                     var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                    logger.info("Going to send message {} to {}:{}", message, address.getHostAddress(), port);
                    retry = false;
                    writer.write(MessageConverter.serialize(message) + "\n");
                    writer.flush();

                    return reader.readLine();
                }
            } catch (IOException e) {
                if (retryCount >= MAX_RETRY_COUNT) {
                    logger.info("Failed to connect after {} retries.", retryCount);
                    throw e;
                }
                logger.info("Failed to connect. Current retry count {} Retrying ...", retryCount);
                retryCount = retryCount + 1;
                try {
                    Thread.sleep(RETRY_WAIT_MILLIS);
                } catch (InterruptedException ex) {
                    logger.info("Sleeping thread failed", ex);
                }
            }
        }
        throw new ConnectException("Connection could not be established after retries.");
    }
}