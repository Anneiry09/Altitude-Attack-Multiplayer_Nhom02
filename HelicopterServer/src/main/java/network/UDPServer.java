package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class UDPServer {
    private static final int UDP_PORT = 9999;
    private DatagramSocket socket;
    private boolean isRunning;

    // Lưu địa chỉ IP & Port của từng Client (được cập nhật khi Client gửi gói tin đầu tiên)
    private ConcurrentHashMap<Byte, InetSocketAddress> clientAddresses = new ConcurrentHashMap<>();
    
    // Hàng đợi chứa các gói Input chờ GameEngine xử lý
    private ConcurrentLinkedQueue<InputPacket> inputQueue = new ConcurrentLinkedQueue<>();

    public void start() {
        try {
            socket = new DatagramSocket(UDP_PORT);
            isRunning = true;
            System.out.println("[UDP Server] Listening on port " + UDP_PORT + "...");

            new Thread(() -> {
                byte[] buffer = new byte[1024];
                while (isRunning) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);

                        // Giải mã gói tin xem ai gửi
                        byte[] data = new byte[packet.getLength()];
                        System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
                        InputPacket input = InputPacket.fromBytes(data);

                        // Lưu/Cập nhật địa chỉ UDP của Client này
                        InetSocketAddress senderAddress = new InetSocketAddress(packet.getAddress(), packet.getPort());
                        clientAddresses.put(input.playerId, senderAddress);

                        // Đưa vào hàng đợi
                        inputQueue.offer(input);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void broadcastState(byte[] stateData) {
        if (socket == null || socket.isClosed()) return;
        
        for (InetSocketAddress address : clientAddresses.values()) {
            try {
                DatagramPacket packet = new DatagramPacket(stateData, stateData.length, address);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public ConcurrentLinkedQueue<InputPacket> getInputQueue() {
        return inputQueue;
    }

    public void stop() {
        isRunning = false;
        if (socket != null) socket.close();
    }
}
