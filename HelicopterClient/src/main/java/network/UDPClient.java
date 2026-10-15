package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPClient {
    private static final int SERVER_PORT = 9999;
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private volatile boolean isRunning;
    
    // Trạng thái mới nhất nhận từ Server để GamePanel lấy ra vẽ
    public volatile StatePacket latestState = null;

    public boolean connect(String serverIP) {
        try {
            socket = new DatagramSocket();
            serverAddress = InetAddress.getByName(serverIP);
            isRunning = true;
            
            // Bắt đầu thread lắng nghe trạng thái từ Server
            startListening();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void startListening() {
        Thread listenThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (isRunning) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    byte[] data = new byte[packet.getLength()];
                    System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
                    
                    StatePacket state = StatePacket.fromBytes(data);
                    
                    // Nếu gói mới hơn gói hiện tại, hoặc Server vừa reset trận mới (tick nhỏ hơn hẳn)
                    if (latestState == null || state.tick > latestState.tick || state.tick < latestState.tick - 100) {
                        latestState = state;
                    }
                } catch (IOException e) {
                    if (isRunning) e.printStackTrace();
                }
            }
        }, "UDP-Client-Receiver");
        listenThread.setDaemon(true);
        listenThread.start();
    }

    public void resetState() {
        this.latestState = null;
    }

    public void sendInput(InputPacket input) {
        if (socket == null || socket.isClosed()) return;
        
        byte[] data = input.toBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, SERVER_PORT);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        isRunning = false;
        if (socket != null) socket.close();
    }
}
