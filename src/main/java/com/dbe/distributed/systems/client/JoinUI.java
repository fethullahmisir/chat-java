package com.dbe.distributed.systems.client;

import com.dbe.distributed.systems.shared.messages.ChatMessage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class JoinUI extends JFrame {

    private ChatController chatController = null;
    private JButton joinButton = new JButton("Join");
    private JTextArea chatName = new JTextArea(1, 10);
    private JLabel nameLabel = new JLabel("Nickname");
    JPanel meinJDialog = new JPanel();

    public JoinUI(ChatController chatController) {
        this.chatController = chatController;

        chatName.setLineWrap (true);
        chatName.setWrapStyleWord (true); //default
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Chat Client Application");
        setSize(300, 200);

        meinJDialog.setLayout(new GridLayout(2, 2));

        add(nameLabel);
        var bottomPanel = new JPanel();
        add(bottomPanel);
        joinButton.setSize(100, 50);
        bottomPanel.add(nameLabel);
        bottomPanel.add(chatName);
        bottomPanel.add(joinButton);

        meinJDialog.setVisible(true);

        // starte in der mitte
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);

        // reagiere auf key + button events
        registerKey(chatName);
        registerHandlers();

    }

    private void registerHandlers() {

        joinButton.addActionListener(l -> {
        var chatUi = new ChatUi(chatController, chatName.getText() );
        chatUi.setVisible(true);
        setVisible(false);
        });

    }

    private void registerKey(JTextArea chatName) {
        chatName.addKeyListener(new KeyListener(){
            @Override
            public void keyPressed(KeyEvent e){
                if(e.getKeyCode() == KeyEvent.VK_ENTER){
                    var chatUi = new ChatUi(chatController, chatName.getText() );
                    System.out.println(chatName.getText() + " joined the chat");
                    chatUi.setVisible(true);
                    setVisible(false);
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });

    }

}
