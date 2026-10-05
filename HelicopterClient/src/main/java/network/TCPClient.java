package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TCPClient {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private byte myPlayerId;
    private boolean isConnected;

    private TCPListener listener;

    public interface TCPListener {
        void onPlayerListUpdated(String data);
        void onDisconnected(String reason);
        void onCountdown(int seconds);         // Mới: Báo đếm ngược
        void onCountdownCancelled();           // Mới: Hủy đếm ngược
        void onGameStart();                    // Mới: Bắt đầu game
    }

    public void setListener(TCPListener listener) {
        this.listener = listener;
    }

    public boolean connect(String serverIP, String playerName) {
        try {
            socket = new Socket(serverIP, 8888);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            out.writeUTF(playerName);
            out.flush();

            myPlayerId = in.readByte();
            isConnected = true;
            System.out.println("[TCP Client] Connected! Your ID: " + myPlayerId);

            startListening();
            return true;
        } catch (IOException e) {
            System.out.println("[TCP Client] Cannot connect to " + serverIP);
            return false;
        }
    }

    private void startListening() {
        Thread listenThread = new Thread(() -> {
            try {
                while (isConnected) {
                    String message = in.readUTF();

                    if (message.startsWith("PLAYER_LIST:") && listener != null) {
                        listener.onPlayerListUpdated(message);
                    } else if (message.startsWith("COUNTDOWN:")) {
                        int sec = Integer.parseInt(message.split(":")[1]);
                        if (listener != null) listener.onCountdown(sec);
                    } else if (message.equals("START_GAME")) {
                        if (listener != null) listener.onGameStart();
                    } else if (message.equals("CANCEL_COUNTDOWN")) {
                        if (listener != null) listener.onCountdownCancelled();
                    }
                }
            } catch (IOException e) {
                if (isConnected && listener != null) {
                    listener.onDisconnected("Lost connection to Server.");
                }
            }
        });
        listenThread.setDaemon(true);
        listenThread.start();
    }

    public void sendReady() {
        try {
            if (out != null) {
                out.writeUTF("READY");
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        isConnected = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte getMyPlayerId() { return myPlayerId; }
    public boolean isConnected() { return isConnected; }
}
