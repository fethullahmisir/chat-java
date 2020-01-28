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
        // upper chat part
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

        // Erzeugung eines JSplitPane-Objektes mit horizontaler Trennung
        JSplitPane splitpane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        // Hier setzen wir links unser rotes JPanel und rechts das gelbe
        //splitpane.setLeftComponent(meinJDialog);
        splitpane.setRightComponent(bottomPanel);

        // Hier fügen wir unserem Dialog unser JSplitPane hinzu
        add(splitpane);
        // Wir lassen unseren Dialog anzeigen
        setVisible(true);

        // starte in der mitte
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width / 2 - this.getSize().width / 2, dim.height / 2 - this.getSize().height / 2);

        // reagiere auf key + button events

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
            //chatMessagesSendArea.requestFocusInWindow();


        });
    }

}
