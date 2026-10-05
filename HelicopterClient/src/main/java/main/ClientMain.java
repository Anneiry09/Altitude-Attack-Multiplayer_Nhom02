package main;

import gui.LobbyFrame;

public class ClientMain {
    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> {
            new LobbyFrame().setVisible(true);
        });
    }
}