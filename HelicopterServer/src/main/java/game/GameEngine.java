package game;

import network.InputPacket;
import network.StatePacket;
import network.UDPServer;

import java.util.concurrent.ConcurrentHashMap;

public class GameEngine implements Runnable {
    private boolean isRunning;
    private int tick;
    
    private ConcurrentHashMap<Byte, Player> players;
    private UDPServer udpServer;

    public GameEngine(UDPServer udpServer) {
        this.udpServer = udpServer;
        this.players = new ConcurrentHashMap<>();
        this.tick = 0;
    }

    public void addPlayer(byte id) {
        // Tạm thời xếp người chơi ở các vị trí ngẫu nhiên
        float startX = 100 + (id * 150);
        float startY = 300;
        players.put(id, new Player(id, startX, startY));
    }

    @Override
    public void run() {
        isRunning = true;
        System.out.println("[GameEngine] Game loop started (20Hz).");

        while (isRunning) {
            tick++;

            processInputs();
            updatePhysics();
            checkCollisions();
            broadcastState();

            try {
                Thread.sleep(50); // 1000ms / 20 = 50ms mỗi tick
            } catch (InterruptedException e) {
                break;
            }
        }
        System.out.println("[GameEngine] Game loop stopped.");
    }

    private void processInputs() {
        InputPacket input;
        // Rút tất cả gói input ra khỏi hàng đợi
        while ((input = udpServer.getInputQueue().poll()) != null) {
            Player p = players.get(input.playerId);
            if (p != null) {
                // Tốc độ di chuyển cơ bản
                float speed = 5.0f;
                p.vx = input.moveX * speed;
                p.vy = input.moveY * speed;
                p.angle = input.mouseAngle;
                
                // TODO: Xử lý bắn súng sau
            }
        }
    }

    private void updatePhysics() {
        for (Player p : players.values()) {
            p.x += p.vx;
            p.y += p.vy;
            
            // Giới hạn bản đồ (tạm thời)
            if (p.x < 0) p.x = 0;
            if (p.x > 800) p.x = 800;
            if (p.y < 0) p.y = 0;
            if (p.y > 600) p.y = 600;
        }
    }

    private void checkCollisions() {
        // Giai đoạn 4
    }

    private void broadcastState() {
        StatePacket state = new StatePacket();
        state.tick = this.tick;
        state.playerCount = (byte) players.size();
        
        int i = 0;
        for (Player p : players.values()) {
            state.playerIds[i] = p.id;
            state.playerXs[i] = p.x;
            state.playerYs[i] = p.y;
            state.playerAngles[i] = p.angle;
            state.playerHPs[i] = p.hp;
            i++;
        }
        
        udpServer.broadcastState(state.toBytes());
    }

    public void stop() {
        isRunning = false;
    }
}
