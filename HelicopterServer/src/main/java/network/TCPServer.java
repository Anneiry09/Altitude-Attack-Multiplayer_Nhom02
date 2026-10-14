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
        this.engine.setTCPServer(this);
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
                    
                    // Reset GameEngine trước khi nạp người chơi vào ván mới
                    engine.reset();
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

    // Kết thúc ván đấu sau 60s: tổng hợp điểm, tìm người thắng và broadcast GAME_OVER
    public synchronized void onMatchFinished(java.util.concurrent.ConcurrentHashMap<Byte, game.Player> players) {
        byte winnerId = -1;
        String winnerName = "Không có";
        int maxScore = -1;
        boolean isDraw = false;

        class PlayerRank {
            byte id;
            String name;
            int score;
            PlayerRank(byte id, String name, int score) {
                this.id = id; this.name = name; this.score = score;
            }
        }
        java.util.List<PlayerRank> ranks = new java.util.ArrayList<>();

        for (ClientHandler client : clients) {
            byte pid = client.getPlayerId();
            game.Player p = players.get(pid);
            int score = (p != null) ? p.score : 0;
            ranks.add(new PlayerRank(pid, client.getPlayerName(), score));

            if (score > maxScore) {
                maxScore = score;
                winnerId = pid;
                winnerName = client.getPlayerName();
                isDraw = false;
            } else if (score == maxScore && maxScore >= 0) {
                isDraw = true;
            }
        }

        // Sắp xếp bảng xếp hạng điểm giảm dần
        ranks.sort((a, b) -> Integer.compare(b.score, a.score));

        if (isDraw && ranks.size() > 1 && ranks.get(0).score == ranks.get(1).score) {
            winnerId = -2; // -2 đại diện cho kết quả Hòa
            winnerName = "HÒA (TIE)";
        }

        StringBuilder sb = new StringBuilder("GAME_OVER:");
        sb.append(winnerId).append(",").append(winnerName);
        for (PlayerRank pr : ranks) {
            sb.append(";").append(pr.id).append(",").append(pr.name).append(",").append(pr.score);
        }

        String finalResult = sb.toString();
        System.out.println("[TCP Server] Broadcast Match Results: " + finalResult);
        broadcastMessage(finalResult);

        // Reset trạng thái sẵn sàng của tất cả Client
        for (ClientHandler client : clients) {
            client.setReady(false);
        }
    }

    // Xử lý khi Client bị ngắt kết nối do quá 5 giây timeout Heartbeat
    public void handlePlayerTimeout(byte playerId) {
        for (ClientHandler client : clients) {
            if (client.getPlayerId() == playerId) {
                System.out.println("[TCP Server] Disconnecting timed out player #" + playerId);
                client.sendMessage("TIMEOUT:Mất kết nối Heartbeat quá 5 giây.");
                client.disconnect();
                break;
            }
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
        engine.removePlayer(client.getPlayerId());
        System.out.println("[TCP Server] Player #" + client.getPlayerId() + " disconnected. Total: " + clients.size());
        broadcastPlayerList();
    }
}
