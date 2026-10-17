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

    public static final int MATCH_DURATION_TICKS = 60 * 20; // Trận đấu 60 giây (1200 ticks)
    public static final float BULLET_MAX_RANGE = 300.0f;     // Tầm bắn tối đa của đạn (300px)

    private volatile boolean isRunning;
    private volatile boolean isGameOver = false;
    private int tick;

    private ConcurrentHashMap<Byte, Player> players;
    private CopyOnWriteArrayList<Bullet> bullets;
    private UDPServer udpServer;
    private java.util.concurrent.ConcurrentLinkedQueue<network.InputPacket> inputQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();

    private static class ScheduledDrop {
        int dropTick;
        byte category; // 1: Khiên, 2: Thuốc Pill
        byte type;
        boolean dropped;

        ScheduledDrop(int dropTick, byte category, byte type) {
            this.dropTick = dropTick;
            this.category = category;
            this.type = type;
            this.dropped = false;
        }
    }
    private java.util.List<ScheduledDrop> scheduledDrops = new java.util.ArrayList<>();
    private CopyOnWriteArrayList<ShieldItem> activeShieldItems = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<PillItem> activePillItems = new CopyOnWriteArrayList<>();
    private int nextItemId = 1;

    public GameEngine(UDPServer udpServer) {
        this.udpServer = udpServer;
        this.players = new ConcurrentHashMap<>();
        this.bullets = new CopyOnWriteArrayList<>();
        this.tick = 0;
        initScheduledDrops();
    }

    public void enqueueInput(network.InputPacket input) {
        inputQueue.offer(input);
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

            if (tick >= MATCH_DURATION_TICKS) {
                if (!isGameOver) {
                    isGameOver = true;
                    int maxScore = -1;
                    int tiedCount = 0;
                    byte winnerId = -1;
                    for (Player p : players.values()) {
                        if (p.score > maxScore) {
                            maxScore = p.score;
                            tiedCount = 1;
                            winnerId = p.id;
                        } else if (p.score == maxScore) {
                            tiedCount++;
                        }
                    }
                    if (tiedCount >= 2) {
                        System.out.println("[GameEngine] MATCH OVER (60s)! KẾT QUẢ: HÒA (DRAW) - " + tiedCount + " người chơi có cùng " + maxScore + " kills!");
                    } else if (winnerId != -1) {
                        System.out.println("[GameEngine] MATCH OVER (60s)! KẾT QUẢ: CHIẾN THẮNG - Player #" + winnerId + " dẫn đầu với " + maxScore + " kills!");
                    } else {
                        System.out.println("[GameEngine] MATCH OVER! Time limit reached (60s).");
                    }
                }
            }

            if (!isGameOver) {
                checkScheduledDrops();
                processInputs();
                updatePhysics();
                checkCollisions();
            } else {
                bullets.clear(); // Hết giờ: xóa đạn còn bay dở dang
            }

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
        network.InputPacket input;
        while ((input = inputQueue.poll()) != null) {
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
                    if (p.weaponBuff == 3) {
                        // Pill Blue: Vòng laser quỹ đạo bảo vệ, máy bay không bắn súng thường được
                    } else if (p.weaponBuff == 1) {
                        // Pill Red: 3 luồng đạn cầu đỏ, tốc độ bắn chậm hơn bình thường (8 ticks = 400ms)
                        p.shotCooldown = 8;

                        float bulletSpeed = 16.0f;
                        float spreadAngle = 0.26f; // ~15 độ

                        // 1. Viên giữa (thẳng theo hướng chuột)
                        float bx0 = p.x + (float) Math.cos(p.angle) * 25.0f;
                        float by0 = p.y + (float) Math.sin(p.angle) * 25.0f;
                        float bvx0 = (float) Math.cos(p.angle) * bulletSpeed;
                        float bvy0 = (float) Math.sin(p.angle) * bulletSpeed;
                        bullets.add(new Bullet(bx0, by0, bvx0, bvy0, p.angle, p.id, BULLET_MAX_RANGE, (byte) 1));

                        // 2. Viên trái (lệch -15 độ)
                        float aLeft = p.angle - spreadAngle;
                        float bx1 = p.x + (float) Math.cos(aLeft) * 25.0f;
                        float by1 = p.y + (float) Math.sin(aLeft) * 25.0f;
                        float bvx1 = (float) Math.cos(aLeft) * bulletSpeed;
                        float bvy1 = (float) Math.sin(aLeft) * bulletSpeed;
                        bullets.add(new Bullet(bx1, by1, bvx1, bvy1, aLeft, p.id, BULLET_MAX_RANGE, (byte) 1));

                        // 3. Viên phải (lệch +15 độ)
                        float aRight = p.angle + spreadAngle;
                        float bx2 = p.x + (float) Math.cos(aRight) * 25.0f;
                        float by2 = p.y + (float) Math.sin(aRight) * 25.0f;
                        float bvx2 = (float) Math.cos(aRight) * bulletSpeed;
                        float bvy2 = (float) Math.sin(aRight) * bulletSpeed;
                        bullets.add(new Bullet(bx2, by2, bvx2, bvy2, aRight, p.id, BULLET_MAX_RANGE, Bullet.TYPE_RED_TRIPLE));
                    } else if (p.weaponBuff == 2) {
                        // Pill Green: Bắn từng nhịp (semi-auto), click chuột từng phát, khóa cơ chế giữ chuột spam
                        if (!p.lastShootingInput) {
                            p.shotCooldown = 5; // Cooldown 250ms giữa 2 nhịp bấm

                            float bulletSpeed = 16.0f;
                            float bx = p.x + (float) Math.cos(p.angle) * 25.0f;
                            float by = p.y + (float) Math.sin(p.angle) * 25.0f;
                            float bvx = (float) Math.cos(p.angle) * bulletSpeed;
                            float bvy = (float) Math.sin(p.angle) * bulletSpeed;

                            // Đạn mẹ màu xanh lá (Bullet.TYPE_GREEN_MAIN = 2)
                            bullets.add(new Bullet(bx, by, bvx, bvy, p.angle, p.id, BULLET_MAX_RANGE, Bullet.TYPE_GREEN_MAIN));
                        }
                    } else {
                        // Bắn thường: 1 viên đạn laser đỏ dài (4 ticks = 200ms)
                        p.shotCooldown = 4;

                        float bulletSpeed = 16.0f;
                        float bx = p.x + (float) Math.cos(p.angle) * 25.0f;
                        float by = p.y + (float) Math.sin(p.angle) * 25.0f;
                        float bvx = (float) Math.cos(p.angle) * bulletSpeed;
                        float bvy = (float) Math.sin(p.angle) * bulletSpeed;

                        bullets.add(new Bullet(bx, by, bvx, bvy, p.angle, p.id, BULLET_MAX_RANGE, Bullet.TYPE_NORMAL));
                    }
                }
                p.lastShootingInput = input.isShooting;
            }
        }
    }

    private void updatePhysics() {
        // Cập nhật người chơi
        for (Player p : players.values()) {
            if (p.shotCooldown > 0) {
                p.shotCooldown--;
            }

            if (p.weaponBuffTicks > 0) {
                p.weaponBuffTicks--;
                if (p.weaponBuffTicks <= 0) {
                    p.weaponBuff = 0;
                    p.weaponBuffTicks = 0;
                    System.out.println("[GameEngine] Player #" + p.id + " weapon buff expired.");
                }
            }

            if (p.blueBurnCooldown > 0) {
                p.blueBurnCooldown--;
            }

            if (p.respawnTicks > 0) {
                p.respawnTicks--;
                if (p.respawnTicks == 0) {
                    respawnPlayerSafely(p);
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

            // Hủy đạn khi: chạm đá, vượt quá tầm bắn (300px), hết thời gian hoặc ra ngoài map
            float distTraveled = (float) Math.hypot(b.x - b.startX, b.y - b.startY);
            if (hitObstacle || distTraveled >= b.maxRange || b.lifeTicks <= 0 || b.x < 0 || b.x > 800 || b.y < 0 || b.y > 600) {
                if (b.type == Bullet.TYPE_GREEN_MAIN && b.x >= 0 && b.x <= 800 && b.y >= 0 && b.y <= 600) {
                    explodeGreenBulletAt(b.x, b.y, b.angle, b.shooterId);
                }
                bullets.remove(b);
            }
        }
    }

    // Nổ đạn mẹ màu xanh lá ra 4 viên đạn con phân mảnh tỏa ra 4 hướng vuông góc
    private void explodeGreenBulletAt(float x, float y, float angle, byte shooterId) {
        float subSpeed = 13.0f;
        float subRange = 130.0f; // Tầm bay của 4 viên đạn con
        float[] subAngleOffsets = {0.0f, (float) (Math.PI / 2), (float) Math.PI, (float) (3 * Math.PI / 2)};

        for (float offset : subAngleOffsets) {
            float subAngle = angle + offset;
            float spawnX = x + (float) Math.cos(subAngle) * 14.0f;
            float spawnY = y + (float) Math.sin(subAngle) * 14.0f;
            float svx = (float) Math.cos(subAngle) * subSpeed;
            float svy = (float) Math.sin(subAngle) * subSpeed;
            bullets.add(new Bullet(spawnX, spawnY, svx, svy, subAngle, shooterId, subRange, Bullet.TYPE_GREEN_SUB));
        }
        System.out.println("[GameEngine] Green bullet exploded into 4 sub-bullets at (" + (int)x + ", " + (int)y + ")");
    }

    private void checkScheduledDrops() {
        for (ScheduledDrop drop : scheduledDrops) {
            if (!drop.dropped && this.tick >= drop.dropTick) {
                drop.dropped = true;
                if (drop.category == 1) {
                    spawnShieldItem(drop.type);
                } else if (drop.category == 2) {
                    spawnPillItem(drop.type);
                }
            }
        }
    }

    private void spawnShieldItem(byte type) {
        java.util.Random rnd = new java.util.Random();
        float bestX = 400;
        float bestY = 300;
        for (int attempt = 0; attempt < 35; attempt++) {
            float candX = 80 + rnd.nextFloat() * (800 - 160);
            float candY = 70 + rnd.nextFloat() * (600 - 140);
            boolean hitObs = false;
            for (float[] obs : OBSTACLES) {
                float ox = obs[0], oy = obs[1], or = obs[2];
                if (Math.hypot(candX - ox, candY - oy) < or + 25.0f) {
                    hitObs = true;
                    break;
                }
            }
            if (!hitObs) {
                bestX = candX;
                bestY = candY;
                break;
            }
        }
        ShieldItem item = new ShieldItem(nextItemId++, type, bestX, bestY);
        activeShieldItems.add(item);
        System.out.println("[GameEngine] Dropped Shield Item (type " + type + ") at (" + (int)bestX + ", " + (int)bestY + ")");
    }

    private void spawnPillItem(byte type) {
        java.util.Random rnd = new java.util.Random();
        float bestX = 400;
        float bestY = 300;
        for (int attempt = 0; attempt < 35; attempt++) {
            float candX = 80 + rnd.nextFloat() * (800 - 160);
            float candY = 70 + rnd.nextFloat() * (600 - 140);
            boolean hitObs = false;
            for (float[] obs : OBSTACLES) {
                float ox = obs[0], oy = obs[1], or = obs[2];
                if (Math.hypot(candX - ox, candY - oy) < or + 25.0f) {
                    hitObs = true;
                    break;
                }
            }
            if (!hitObs) {
                bestX = candX;
                bestY = candY;
                break;
            }
        }
        PillItem item = new PillItem(nextItemId++, type, bestX, bestY);
        activePillItems.add(item);
        System.out.println("[GameEngine] Dropped Pill Item (type " + type + ") at (" + (int)bestX + ", " + (int)bestY + ")");
    }

    private void checkCollisions() {
        // 1. Kiểm tra người chơi nhặt khiên trên map
        for (ShieldItem item : activeShieldItems) {
            for (Player p : players.values()) {
                if (p.hp > 0 && p.respawnTicks <= 0) {
                    float dist = (float) Math.hypot(p.x - item.x, p.y - item.y);
                    if (dist < 32.0f) { // Bán kính nhặt khiên
                        p.shieldType = item.type;
                        if (item.type == ShieldItem.TYPE_BRONZE) {
                            p.shieldHits = 1;
                        } else if (item.type == ShieldItem.TYPE_SILVER) {
                            p.shieldHits = 2;
                        } else if (item.type == ShieldItem.TYPE_GOLD) {
                            p.shieldHits = 3;
                        }
                        activeShieldItems.remove(item);
                        System.out.println("[GameEngine] Player #" + p.id + " picked up Shield (type " + item.type + ", hits: " + p.shieldHits + ")");
                        break;
                    }
                }
            }
        }

        // 1.5. Kiểm tra người chơi nhặt thuốc nâng cấp đạn (Pill) trên map
        for (PillItem pill : activePillItems) {
            for (Player p : players.values()) {
                if (p.hp > 0 && p.respawnTicks <= 0) {
                    float dist = (float) Math.hypot(p.x - pill.x, p.y - pill.y);
                    if (dist < 32.0f) { // Bán kính nhặt thuốc
                        p.weaponBuff = pill.type;
                        p.weaponBuffTicks = 300; // 15 giây (300 ticks * 50ms)
                        activePillItems.remove(pill);
                        System.out.println("[GameEngine] Player #" + p.id + " picked up Pill (type " + pill.type + ", duration: 15s)");
                        break;
                    }
                }
            }
        }

        // 1.8. Xử lý Vòng bảo vệ & Đốt sát thương của Pill Blue (Orbital Laser)
        for (Player p : players.values()) {
            if (p.hp > 0 && p.respawnTicks <= 0 && p.weaponBuff == PillItem.TYPE_BLUE) {
                float orbitRadius = 45.0f;
                float baseOrbitAngle = this.tick * 0.12f;

                // A. Bảo vệ: Triệt tiêu đạn đối phương khi chạm vào vòng đạn bảo vệ / quả cầu xanh
                for (Bullet b : bullets) {
                    if (b.shooterId != p.id) {
                        float distToPlayer = (float) Math.hypot(b.x - p.x, b.y - p.y);
                        boolean vaporized = (distToPlayer <= orbitRadius + 6.0f);
                        if (!vaporized) {
                            for (int k = 0; k < 4; k++) {
                                float orbAngle = baseOrbitAngle + (float) (k * Math.PI / 2);
                                float orbX = p.x + (float) Math.cos(orbAngle) * orbitRadius;
                                float orbY = p.y + (float) Math.sin(orbAngle) * orbitRadius;
                                if (Math.hypot(b.x - orbX, b.y - orbY) < 22.0f) {
                                    vaporized = true;
                                    break;
                                }
                            }
                        }
                        if (vaporized) {
                            bullets.remove(b);
                            System.out.println("[GameEngine] Player #" + p.id + " Blue Orbit vaporized enemy bullet!");
                        }
                    }
                }

                // B. Đốt sát thương đối phương khi áp sát (Damage Over Time: 8 HP mỗi 200ms)
                if (p.blueBurnCooldown <= 0) {
                    for (Player enemy : players.values()) {
                        if (enemy.id != p.id && enemy.hp > 0 && enemy.respawnTicks <= 0) {
                            float distToEnemy = (float) Math.hypot(p.x - enemy.x, p.y - enemy.y);
                            if (distToEnemy < orbitRadius + 24.0f) {
                                p.blueBurnCooldown = 4; // 200ms
                                int burnDmg = 8;

                                if (enemy.shieldHits > 0) {
                                    enemy.shieldHits--;
                                    if (enemy.shieldHits <= 0) {
                                        enemy.shieldHits = 0;
                                        enemy.shieldType = 0;
                                    }
                                    System.out.println("[GameEngine] Player #" + p.id + " Blue Orbit burned Player #" + enemy.id + " shield! Hits left: " + enemy.shieldHits);
                                } else {
                                    enemy.hp = (byte) Math.max(0, enemy.hp - burnDmg);
                                    if (enemy.hp <= 0) {
                                        enemy.hp = 0;
                                        enemy.shieldType = 0;
                                        enemy.shieldHits = 0;
                                        enemy.weaponBuff = 0;
                                        enemy.weaponBuffTicks = 0;
                                        enemy.respawnTicks = 26;
                                        p.score++;
                                        System.out.println("[GameEngine] Player #" + p.id + " Blue Orbit melted Player #" + enemy.id + "!");
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }

        // 2. Kiểm tra đạn bắn trúng người chơi
        for (Bullet b : bullets) {
            for (Player p : players.values()) {
                if (p.id != b.shooterId && p.hp > 0 && p.respawnTicks <= 0) {
                    float dist = (float) Math.hypot(b.x - p.x, b.y - p.y);
                    if (dist < 25.0f) {
                        boolean isGreenMain = (b.type == Bullet.TYPE_GREEN_MAIN);
                        float hitX = b.x;
                        float hitY = b.y;
                        float hitAngle = b.angle;
                        byte shooterId = b.shooterId;

                        bullets.remove(b);

                        int damage = (b.type == Bullet.TYPE_GREEN_SUB) ? 15 : 25; // Đạn con 15 damage, đạn thường/mẹ 25 damage

                        if (p.shieldHits > 0) {
                            // Khiên đỡ được đạn: trừ 1 hit khiên, KHÔNG trừ máu!
                            p.shieldHits--;
                            if (p.shieldHits <= 0) {
                                p.shieldHits = 0;
                                p.shieldType = 0; // Khiên vỡ hoàn toàn
                            }
                            System.out.println("[GameEngine] Player #" + p.id + " shield absorbed hit! Remaining hits: " + p.shieldHits);
                        } else {
                            // Không có khiên: trừ máu
                            p.hp = (byte) Math.max(0, p.hp - damage);
                            if (p.hp <= 0) {
                                p.hp = 0;
                                p.shieldType = 0;
                                p.shieldHits = 0;
                                p.weaponBuff = 0;
                                p.weaponBuffTicks = 0;
                                p.respawnTicks = 26; // 1.3 giây (26 ticks * 50ms = 1300ms)
                                Player shooter = players.get(shooterId);
                                if (shooter != null) {
                                    shooter.score++;
                                }
                            }
                        }

                        if (isGreenMain) {
                            // Đạn mẹ trúng mục tiêu phát nổ ra 4 viên đạn con
                            explodeGreenBulletAt(hitX, hitY, hitAngle, shooterId);
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
        int remainingTicks = Math.max(0, MATCH_DURATION_TICKS - this.tick);
        state.remainingSeconds = (short) (remainingTicks / 20);

        int i = 0;
        for (Player p : players.values()) {
            state.playerIds[i] = p.id;
            state.playerXs[i] = p.x;
            state.playerYs[i] = p.y;
            state.playerAngles[i] = p.angle;
            state.playerHPs[i] = p.hp;
            state.playerScores[i] = p.score;
            state.playerShieldTypes[i] = p.shieldType;
            state.playerShieldHits[i] = p.shieldHits;
            state.playerWeaponBuffs[i] = p.weaponBuff;
            state.playerBuffRemainingSeconds[i] = (byte) ((p.weaponBuffTicks + 19) / 20);
            i++;
        }

        int count = Math.min(bullets.size(), 40);
        state.bulletCount = (byte) count;
        for (int bIdx = 0; bIdx < count; bIdx++) {
            Bullet b = bullets.get(bIdx);
            state.bulletXs[bIdx] = b.x;
            state.bulletYs[bIdx] = b.y;
            state.bulletAngles[bIdx] = b.angle;
            state.bulletTypes[bIdx] = b.type;
        }

        int sCount = Math.min(activeShieldItems.size(), 10);
        state.shieldItemCount = (byte) sCount;
        for (int sIdx = 0; sIdx < sCount; sIdx++) {
            ShieldItem item = activeShieldItems.get(sIdx);
            state.shieldItemTypes[sIdx] = item.type;
            state.shieldItemXs[sIdx] = item.x;
            state.shieldItemYs[sIdx] = item.y;
        }

        int pCount = Math.min(activePillItems.size(), 10);
        state.pillItemCount = (byte) pCount;
        for (int pIdx = 0; pIdx < pCount; pIdx++) {
            PillItem pill = activePillItems.get(pIdx);
            state.pillItemTypes[pIdx] = pill.type;
            state.pillItemXs[pIdx] = pill.x;
            state.pillItemYs[pIdx] = pill.y;
        }

        udpServer.sendStateTo(state.toBytes(), players.keySet());
    }

    // Tìm vị trí ngẫu nhiên an toàn, không kẹt đá và xa kẻ địch còn sống nhất
    private void respawnPlayerSafely(Player p) {
        java.util.Random rnd = new java.util.Random();
        float bestX = p.spawnX;
        float bestY = p.spawnY;
        float maxMinDist = -1;

        // Thử 40 tọa độ ngẫu nhiên để chọn vị trí an toàn nhất
        for (int attempt = 0; attempt < 40; attempt++) {
            float candX = 80 + rnd.nextFloat() * (800 - 160);
            float candY = 70 + rnd.nextFloat() * (600 - 140);

            // 1. Kiểm tra va chạm thiên thạch (phải cách xa mọi khối đá)
            boolean hitObs = false;
            for (float[] obs : OBSTACLES) {
                float ox = obs[0], oy = obs[1], or = obs[2];
                if (Math.hypot(candX - ox, candY - oy) < or + 35.0f) {
                    hitObs = true;
                    break;
                }
            }
            if (hitObs) continue;

            // 2. Tính khoảng cách tới kẻ địch còn sống gần nhất
            float nearestEnemyDist = Float.MAX_VALUE;
            for (Player other : players.values()) {
                if (other.id != p.id && other.hp > 0 && other.respawnTicks <= 0) {
                    float d = (float) Math.hypot(candX - other.x, candY - other.y);
                    if (d < nearestEnemyDist) {
                        nearestEnemyDist = d;
                    }
                }
            }

            // Nếu không có kẻ địch nào còn sống, chọn ngay vị trí này
            if (nearestEnemyDist == Float.MAX_VALUE) {
                bestX = candX;
                bestY = candY;
                break;
            }

            // Chọn vị trí có khoảng cách tới kẻ địch gần nhất là LỚN NHẤT
            if (nearestEnemyDist > maxMinDist) {
                maxMinDist = nearestEnemyDist;
                bestX = candX;
                bestY = candY;
            }
        }

        p.respawn(bestX, bestY);
        System.out.println("[GameEngine] Player #" + p.id + " respawned at safe random location: (" + (int)bestX + ", " + (int)bestY + ")");
    }

    private void initScheduledDrops() {
        this.scheduledDrops.clear();
        java.util.Random rnd = new java.util.Random();
        // 1. Khiên (Category 1: Bronze, Silver, Gold)
        this.scheduledDrops.add(new ScheduledDrop(120 + rnd.nextInt(60), (byte) 1, ShieldItem.TYPE_BRONZE)); // ~6-9s
        this.scheduledDrops.add(new ScheduledDrop(280 + rnd.nextInt(60), (byte) 1, ShieldItem.TYPE_SILVER)); // ~14-17s
        this.scheduledDrops.add(new ScheduledDrop(440 + rnd.nextInt(60), (byte) 1, ShieldItem.TYPE_GOLD));   // ~22-25s
        this.scheduledDrops.add(new ScheduledDrop(600 + rnd.nextInt(60), (byte) 1, ShieldItem.TYPE_BRONZE)); // ~30-33s
        this.scheduledDrops.add(new ScheduledDrop(760 + rnd.nextInt(60), (byte) 1, ShieldItem.TYPE_SILVER)); // ~38-41s
        this.scheduledDrops.add(new ScheduledDrop(920 + rnd.nextInt(60), (byte) 1, ShieldItem.TYPE_GOLD));   // ~46-49s

        // 2. Thuốc nâng cấp (Category 2: Pill Red, Pill Green, Pill Blue rớt 2 lần trong trận)
        this.scheduledDrops.add(new ScheduledDrop(200 + rnd.nextInt(50), (byte) 2, PillItem.TYPE_RED));   // ~10-12s
        this.scheduledDrops.add(new ScheduledDrop(360 + rnd.nextInt(50), (byte) 2, PillItem.TYPE_GREEN)); // ~18-20s
        this.scheduledDrops.add(new ScheduledDrop(520 + rnd.nextInt(50), (byte) 2, PillItem.TYPE_BLUE));  // ~26-28s
        this.scheduledDrops.add(new ScheduledDrop(680 + rnd.nextInt(50), (byte) 2, PillItem.TYPE_RED));   // ~34-36s
        this.scheduledDrops.add(new ScheduledDrop(840 + rnd.nextInt(50), (byte) 2, PillItem.TYPE_GREEN)); // ~42-44s
        this.scheduledDrops.add(new ScheduledDrop(1000 + rnd.nextInt(50), (byte) 2, PillItem.TYPE_BLUE)); // ~50-52s
    }

    public void resetMatch() {
        this.tick = 0;
        this.isGameOver = false;
        this.bullets.clear();
        this.inputQueue.clear();
        this.activeShieldItems.clear();
        this.activePillItems.clear();
        initScheduledDrops();

        for (Player p : players.values()) {
            p.hp = 100;
            p.score = 0;
            p.vx = 0;
            p.vy = 0;
            p.shotCooldown = 0;
            p.respawnTicks = 0;
            p.shieldType = 0;
            p.shieldHits = 0;
            p.weaponBuff = 0;
            p.weaponBuffTicks = 0;
            p.blueBurnCooldown = 0;
            p.lastShootingInput = false;
            p.x = p.spawnX;
            p.y = p.spawnY;
        }
        System.out.println("[GameEngine] Match reset! Ready for fresh 60s match with scheduled shield & pill drops.");
    }

    public void removePlayer(byte playerId) {
        players.remove(playerId);
        System.out.println("[GameEngine] Player #" + playerId + " removed from engine.");
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void stop() {
        isRunning = false;
    }
}
