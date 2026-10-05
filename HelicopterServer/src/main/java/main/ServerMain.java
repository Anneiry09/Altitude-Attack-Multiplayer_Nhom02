package main;

import game.GameEngine;
import network.TCPServer;
import network.UDPServer;

public class ServerMain {
    public static void main(String[] args) {
        System.out.println("=== HELICOPTER COMBAT - SERVER ===");

        // Khởi động UDP Server
        UDPServer udpServer = new UDPServer();
        udpServer.start();

        // Khởi tạo Game Engine (truyền UDP vào để mượn đường gửi/nhận)
        GameEngine engine = new GameEngine(udpServer);

        // Khởi động TCP Server để đón người chơi vào sảnh
        TCPServer tcpServer = new TCPServer(engine);
        tcpServer.start();
    }
}
