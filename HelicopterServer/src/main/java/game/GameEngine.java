package game;

import network.InputPacket;
import network.StatePacket;
import network.UDPServer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameEngine implements Runnable {
    // Phân bố thiên thạch ngẫu nhiên tự nhiên (Raindrop Scatter) khắp bản đồ 800x600 {X, Y, Bán kính, Loại ảnh, Góc xoay}
    public static final float[][] OBSTACLES = {
        {243f, 102f, 35f, 6f, 5.39f},
        {612f, 61f,  35f, 6f, 0.89f},
        {308f, 499f, 21f, 0f, 3.18f},
        {733f, 88f,  21f, 1f, 2.51f},
        {326f, 273f, 21f, 1f, 4.50f},
        {458f, 196f, 14f, 2f, 6.10f},
        {406f, 110f, 14f, 3f, 0.08f},
        {266f, 181f, 14f, 3f, 5.58f},
        {195f, 362f, 14f, 3f, 0.27f},
        {532f, 395f, 14f, 2f, 1.54f},
        {668f, 249f, 9f,  4f, 3.86f},
        {142f, 55f,  9f,  4f, 3.55f},
        {727f, 359f, 9f,  4f, 5.97f},
        {579f, 307f, 9f,  4f, 0.15f},
        {679f, 541f, 9f,  4f, 4.20f},
        {350f, 354f, 9f,  5f, 1.81f},
        {428f, 514f, 9f,  5f, 2.72f},
        {113f, 320f, 9f,  5f, 4.62f}
    };

    private boolean isRunning;
    private int tick;

    private ConcurrentHashMap<Byte, Player> players;
    private CopyOnWriteArrayList<Bullet> bullets;
    private UDPServer udpServer;

    public GameEngine(UDPServer udpServer) {
        this.udpServer = udpServer;
        this.players = new ConcurrentHashMap<>();
        this.bullets = new CopyOnWriteArrayList<>();
        this.tick = 0;
    }

    public void addPlayer(byte id) {
        float startX = 150 + (id % 2) * 500;
        float startY = 150 + (id / 2) * 300;
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
                Thread.sleep(50); // 20 tick/s = 50ms
            } catch (InterruptedException e) {
                break;
            }
        }
        System.out.println("[GameEngine] Game loop stopped.");
    }

    private void processInputs() {
        InputPacket input;
        while ((input = udpServer.getInputQueue().poll()) != null) {
            Player p = players.get(input.playerId);
            if (p != null && p.hp > 0 && p.respawnTicks <= 0) {
                // Di chuyển dứt khoát: nhả phím là dừng ngay lập tức, không trượt sân băng
                float speed = 6.0f;
                if (input.moveX != 0 && input.moveY != 0) {
                    float diagSpeed = speed * 0.7071f; // Chuẩn hóa tốc độ khi đi chéo
                    p.vx = input.moveX * diagSpeed;
                    p.vy = input.moveY * diagSpeed;
                } else {
                    p.vx = input.moveX * speed;
                    p.vy = input.moveY * speed;
                }

                p.angle = input.mouseAngle;

                if (input.isShooting && p.shotCooldown <= 0) {
                    p.shotCooldown = 4; // Cooldown 4 ticks = 200ms

                    float bulletSpeed = 16.0f;
                    float bx = p.x + (float) Math.cos(p.angle) * 25.0f;
                    float by = p.y + (float) Math.sin(p.angle) * 25.0f;
                    float bvx = (float) Math.cos(p.angle) * bulletSpeed;
                    float bvy = (float) Math.sin(p.angle) * bulletSpeed;

                    bullets.add(new Bullet(bx, by, bvx, bvy, p.angle, p.id));
                }
            }
        }
    }

    private void updatePhysics() {
        // Cập nhật người chơi
        for (Player p : players.values()) {
            if (p.shotCooldown > 0) {
                p.shotCooldown--;
            }

            if (p.respawnTicks > 0) {
                p.respawnTicks--;
                if (p.respawnTicks == 0) {
                    p.respawn();
                }
                continue;
            }

            p.x += p.vx;
            p.y += p.vy;

            // Va chạm với toàn bộ các khối thiên thạch rải rác
            for (float[] obs : OBSTACLES) {
                float ox = obs[0], oy = obs[1], or = obs[2];
                float dist = (float) Math.hypot(p.x - ox, p.y - oy);
                float minDist = or + 18.0f;
                if (dist < minDist && dist > 0) {
                    float pushAngle = (float) Math.atan2(p.y - oy, p.x - ox);
                    p.x = ox + (float) Math.cos(pushAngle) * minDist;
                    p.y = oy + (float) Math.sin(pushAngle) * minDist;
                    p.vx = 0;
                    p.vy = 0;
                }
            }

            // Giới hạn biên màn hình 800x600
            if (p.x < 25) { p.x = 25; p.vx = 0; }
            if (p.x > 775) { p.x = 775; p.vx = 0; }
            if (p.y < 25) { p.y = 25; p.vy = 0; }
            if (p.y > 575) { p.y = 575; p.vy = 0; }
        }

        // Cập nhật đạn
        for (Bullet b : bullets) {
            b.x += b.vx;
            b.y += b.vy;
            b.lifeTicks--;

            boolean hitObstacle = false;
            for (float[] obs : OBSTACLES) {
                float ox = obs[0], oy = obs[1], or = obs[2];
                if (Math.hypot(b.x - ox, b.y - oy) < or) {
                    hitObstacle = true;
                    break;
                }
            }

            if (hitObstacle || b.lifeTicks <= 0 || b.x < 0 || b.x > 800 || b.y < 0 || b.y > 600) {
                bullets.remove(b);
            }
        }
    }

    private void checkCollisions() {
        for (Bullet b : bullets) {
            for (Player p : players.values()) {
                if (p.id != b.shooterId && p.hp > 0 && p.respawnTicks <= 0) {
                    float dist = (float) Math.hypot(b.x - p.x, b.y - p.y);
                    if (dist < 25.0f) {
                        p.hp -= 25;
                        bullets.remove(b);

                        if (p.hp <= 0) {
                            p.hp = 0;
                            p.respawnTicks = 60;
                            Player shooter = players.get(b.shooterId);
                            if (shooter != null) {
                                shooter.score++;
                            }
                        }
                        break;
                    }
                }
            }
        }
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
            state.playerScores[i] = p.score;
            i++;
        }

        int count = Math.min(bullets.size(), 30);
        state.bulletCount = (byte) count;
        for (int bIdx = 0; bIdx < count; bIdx++) {
            Bullet b = bullets.get(bIdx);
            state.bulletXs[bIdx] = b.x;
            state.bulletYs[bIdx] = b.y;
            state.bulletAngles[bIdx] = b.angle;
        }

        udpServer.broadcastState(state.toBytes());
    }

    public void stop() {
        isRunning = false;
    }
}
