package network;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class TCPServer {
    private static final int TCP_PORT = 8888;
    private static final int MAX_PLAYERS = 4;

    private ServerSocket serverSocket;
    private CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private boolean isRunning;
    private byte nextPlayerId = 0;
    
    private Thread countdownThread = null;
    private game.GameEngine engine;

    public TCPServer(game.GameEngine engine) {
        this.engine = engine;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(TCP_PORT);
            isRunning = true;
            System.out.println("[TCP Server] Waiting for connections on port " + TCP_PORT + "...");

            while (isRunning) {
                Socket socket = serverSocket.accept();

                if (clients.size() >= MAX_PLAYERS) {
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF("FULL");
                    socket.close();
                    System.out.println("[TCP Server] Connection rejected - Room is full.");
                    continue;
                }

                byte playerId = nextPlayerId++;
                ClientHandler handler = new ClientHandler(socket, playerId, this);
                clients.add(handler);
                handler.start();
                System.out.println("[TCP Server] Player #" + playerId + " connected. Total: " + clients.size());
            }
        } catch (IOException e) {
            System.out.println("[TCP Server] Server stopped.");
        }
    }

    // Kiểm tra nếu tất cả đã sẵn sàng (>= 2 người) thì đếm ngược 5s
    public synchronized void checkAllReady() {
        if (clients.size() < 2) return; // Phải có ít nhất 2 người mới chơi được
        
        boolean allReady = true;
        for (ClientHandler client : clients) {
            if (!client.isReady()) {
                allReady = false;
                break;
            }
        }

        if (allReady && countdownThread == null) {
            countdownThread = new Thread(() -> {
                try {
                    for (int i = 5; i > 0; i--) {
                        broadcastMessage("COUNTDOWN:" + i);
                        System.out.println("[TCP Server] Game starting in " + i + "...");
                        Thread.sleep(1000);
                    }
                    
                    // Nạp người chơi vào GameEngine
                    for(ClientHandler c : clients) {
                        engine.addPlayer(c.getPlayerId());
                    }
                    
                    broadcastMessage("START_GAME");
                    System.out.println("[TCP Server] START_GAME sent to all clients.");
                    
                    // Bắt đầu vòng lặp game
                    new Thread(engine).start();
                } catch (InterruptedException e) {
                    broadcastMessage("CANCEL_COUNTDOWN");
                    System.out.println("[TCP Server] Countdown cancelled.");
                } finally {
                    countdownThread = null;
                }
            });
            countdownThread.start();
        }
    }

    // Nếu ai đó thoát giữa chừng, ngắt đếm ngược
    public synchronized void cancelCountdown() {
        if (countdownThread != null) {
            countdownThread.interrupt();
        }
    }

    public void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public void broadcastPlayerList() {
        StringBuilder sb = new StringBuilder("PLAYER_LIST:");
        for (ClientHandler client : clients) {
            sb.append(client.getPlayerId()).append(",").append(client.getPlayerName()).append(";");
        }
        broadcastMessage(sb.toString());
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("[TCP Server] Player #" + client.getPlayerId() + " disconnected. Total: " + clients.size());
        broadcastPlayerList();
    }
}
