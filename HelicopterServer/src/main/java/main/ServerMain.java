package main;

import network.TCPServer;
import network.UDPServer;

public class ServerMain {
    public static void main(String[] args) {
        System.out.println("=== HELICOPTER COMBAT - SERVER ===");

        // Khởi động UDP Server
        UDPServer udpServer = new UDPServer();
        udpServer.start();

        // Khởi động TCP Server để đón người chơi vào sảnh, quản lý phòng & ghép trận
        TCPServer tcpServer = new TCPServer(udpServer);
        tcpServer.start();
    }
}
