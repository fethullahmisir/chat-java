package com.dbe.distributed.systems.client;

import com.dbe.distributed.systems.shared.messages.ChatMessage;

import javax.swing.*;
import java.awt.*;

public class ChatUi extends JFrame {

    private final ChatController chatController;
    private String chatName;

    private JTextArea chatMessagesShowArea = new JTextArea(30, 30);
    private JButton sendButton = new JButton("Send");
    private JTextArea chatMessagesSendArea = new JTextArea(5, 30);
    private JLabel nameLabel = new JLabel("Nickname");

    public ChatUi(ChatController chatController, String chatName) {
        this.chatController = chatController;
        this.chatName = chatName;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("Chat Client Application");
        setSize(450, 300);


        setLayout(new GridLayout(2, 2));
        chatMessagesShowArea.setEditable(false);
        add(chatMessagesShowArea);
        JScrollPane scrollPaneShowArea = new JScrollPane(chatMessagesShowArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(scrollPaneShowArea);

        var bottomPanel = new JPanel();
        sendButton.setSize(100, 50);
        nameLabel.setText(chatName);
        bottomPanel.add(nameLabel);
        bottomPanel.add(chatMessagesSendArea);
        bottomPanel.add(sendButton);

        add(bottomPanel);

        JSplitPane splitpane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitpane.setRightComponent(bottomPanel);

        add(splitpane);
        setVisible(true);

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width / 2 - this.getSize().width / 2, dim.height / 2 - this.getSize().height / 2);
        registerHandlers();
    }


    private void registerHandlers() {
        chatController.setChatMessageHandler(chatMessage -> {
                    chatMessagesShowArea.append(String.format("%s : %s \n", chatMessage.getName(), chatMessage.getText()));
                    chatMessagesShowArea.setCaretPosition(chatMessagesShowArea.getDocument().getLength());
                }
        );

        sendButton.addActionListener(l -> {
            String text = chatMessagesSendArea.getText();
            if (text == null || text.trim().isEmpty()) {
                return;
            }
            var chatMessage = ChatMessage.of(text, chatName);
            chatController.sendTextMessage(chatMessage);
            chatMessagesSendArea.setText("");
            chatMessagesSendArea.requestFocus(true);
        });
    }

}
