package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler extends Thread {
    private Socket socket;
    private byte playerId;
    private String playerName;
    private TCPServer server;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean isConnected;
    private boolean isReady; // Thêm trạng thái Ready

    public ClientHandler(Socket socket, byte playerId, TCPServer server) {
        this.socket = socket;
        this.playerId = playerId;
        this.server = server;
        this.isConnected = true;
        this.isReady = false;
    }

    @Override
    public void run() {
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            playerName = in.readUTF();
            System.out.println("[TCP Server] Player #" + playerId + " name: " + playerName);

            out.writeByte(playerId);
            out.flush();

            server.broadcastPlayerList();

            while (isConnected) {
                String message = in.readUTF();

                if ("READY".equals(message)) {
                    System.out.println("[TCP Server] Player #" + playerId + " is ready.");
                    isReady = true;
                    server.checkAllReady(); // Kích hoạt kiểm tra xem tất cả đã sẵn sàng chưa
                }
            }
        } catch (IOException e) {
            System.out.println("[TCP Server] Player #" + playerId + " lost connection.");
        } finally {
            disconnect();
        }
    }

    public void sendMessage(String message) {
        try {
            if (out != null && isConnected) {
                out.writeUTF(message);
                out.flush();
            }
        } catch (IOException e) {
            disconnect();
        }
    }

    public void disconnect() {
        if (!isConnected) return;
        isConnected = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        server.cancelCountdown(); // Hủy đếm ngược nếu có người thoát
        server.removeClient(this);
    }

    public byte getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public boolean isReady() { return isReady; }
}
