package game;

import network.InputPacket;
import network.StatePacket;
import network.UDPClient;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

public class GamePanel extends JPanel {
    // Phân bố thiên thạch ngẫu nhiên tự nhiên (Raindrop Scatter) đồng bộ với Server {X, Y, Bán kính, Loại ảnh, Góc xoay}
    public static final float[][] OBSTACLES = {
        {243f, 102f, 35f, 6f, 5.39f}, // Tảng vừa-lớn
        {612f, 61f,  35f, 6f, 0.89f}, // Tảng vừa-lớn
        {308f, 499f, 21f, 0f, 3.18f}, // Tảng vừa
        {733f, 88f,  21f, 1f, 2.51f}, // Tảng vừa
        {326f, 273f, 21f, 1f, 4.50f}, // Tảng vừa
        {458f, 196f, 14f, 2f, 6.10f}, // Tảng nhỏ
        {406f, 110f, 14f, 3f, 0.08f}, // Tảng nhỏ
        {266f, 181f, 14f, 3f, 5.58f}, // Tảng nhỏ
        {195f, 362f, 14f, 3f, 0.27f}, // Tảng nhỏ
        {532f, 395f, 14f, 2f, 1.54f}, // Tảng nhỏ
        {668f, 249f, 9f,  4f, 3.86f}, // Mảnh vụn
        {142f, 55f,  9f,  4f, 3.55f}, // Mảnh vụn
        {727f, 359f, 9f,  4f, 5.97f}, // Mảnh vụn
        {579f, 307f, 9f,  4f, 0.15f}, // Mảnh vụn
        {679f, 541f, 9f,  4f, 4.20f}, // Mảnh vụn
        {350f, 354f, 9f,  5f, 1.81f}, // Mảnh vụn
        {428f, 514f, 9f,  5f, 2.72f}, // Mảnh vụn
        {113f, 320f, 9f,  5f, 4.62f}  // Mảnh vụn
    };

    private UDPClient udpClient;
    private byte myPlayerId;

    private boolean keyW, keyA, keyS, keyD;
    private boolean isShooting;
    private float mouseX, mouseY;
    private boolean isRunning = true;

    private long lastTime;
    private byte lastHp = 100;
    private long lastShotSoundTime = 0;

    // Hình ảnh tài nguyên
    private BufferedImage bgImage;
    private BufferedImage[] shipImages = new BufferedImage[4];
    private BufferedImage bulletImage;
    private BufferedImage[] meteorImages;

    // Lớp nội bộ lưu tọa độ hiển thị (Nội suy)
    private class ClientPlayer {
        float x, y, angle;
    }
    private ConcurrentHashMap<Byte, ClientPlayer> renderPlayers = new ConcurrentHashMap<>();

    public GamePanel(UDPClient udpClient, byte myPlayerId) {
        this.udpClient = udpClient;
        this.myPlayerId = myPlayerId;

        this.setBackground(Color.BLACK);
        this.setFocusable(true);

        loadAssets();
        setupInputListeners();

        // 1. Thread gửi Input 60Hz
        new Thread(() -> {
            while (isRunning) {
                sendInput();
                try { Thread.sleep(16); } catch (Exception e) {}
            }
        }).start();

        // 2. Thread Render mở khóa FPS
        new Thread(() -> {
            lastTime = System.nanoTime();
            while (isRunning) {
                repaint();
                try { Thread.sleep(2); } catch (Exception e) {}
            }
        }).start();
    }

    private File getAssetFile(String relativePath) {
        File f = new File(relativePath);
        if (f.exists()) return f;
        f = new File("HelicopterClient", relativePath);
        if (f.exists()) return f;
        return null;
    }

    private void loadAssets() {
        try {
            File bgFile = getAssetFile("assets/images/black.png");
            if (bgFile != null && bgFile.exists()) bgImage = ImageIO.read(bgFile);

            String[] shipFiles = {
                "playerShip1_green.png",
                "playerShip1_red.png",
                "playerShip1_blue.png",
                "playerShip1_orange.png"
            };
            for (int i = 0; i < 4; i++) {
                File f = getAssetFile("assets/images/" + shipFiles[i]);
                if (f != null && f.exists()) shipImages[i] = ImageIO.read(f);
            }

            File bFile = getAssetFile("assets/images/laserRed01.png");
            if (bFile != null && bFile.exists()) bulletImage = ImageIO.read(bFile);

            String[] meteorFiles = {
                "meteorBrown_med1.png",   // type 0
                "meteorBrown_med3.png",   // type 1
                "meteorBrown_small1.png", // type 2
                "meteorBrown_small2.png", // type 3
                "meteorBrown_tiny1.png",  // type 4
                "meteorBrown_tiny2.png",  // type 5
                "meteorBrown_big3.png",   // type 6
                "meteorBrown_big4.png"    // type 7
            };
            meteorImages = new BufferedImage[meteorFiles.length];
            for (int i = 0; i < meteorFiles.length; i++) {
                File mFile = getAssetFile("assets/images/" + meteorFiles[i]);
                if (mFile != null && mFile.exists()) meteorImages[i] = ImageIO.read(mFile);
            }
        } catch (Exception e) {
            System.err.println("Warning: Cannot load some images: " + e.getMessage());
        }
    }

    private void playSound(String fileName) {
        new Thread(() -> {
            try {
                File soundFile = getAssetFile("assets/sounds/" + fileName);
                if (soundFile != null && soundFile.exists()) {
                    AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioIn);
                    clip.start();
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void setupInputListeners() {
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W -> keyW = true;
                    case KeyEvent.VK_A -> keyA = true;
                    case KeyEvent.VK_S -> keyS = true;
                    case KeyEvent.VK_D -> keyD = true;
                    case KeyEvent.VK_SPACE -> {
                        isShooting = true;
                        long now = System.currentTimeMillis();
                        if (now - lastShotSoundTime >= 200) {
                            playSound("shoot.wav");
                            lastShotSoundTime = now;
                        }
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W -> keyW = false;
                    case KeyEvent.VK_A -> keyA = false;
                    case KeyEvent.VK_S -> keyS = false;
                    case KeyEvent.VK_D -> keyD = false;
                    case KeyEvent.VK_SPACE -> isShooting = false;
                }
            }
        });

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    isShooting = true;
                    long now = System.currentTimeMillis();
                    if (now - lastShotSoundTime >= 200) {
                        playSound("shoot.wav");
                        lastShotSoundTime = now;
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    isShooting = false;
                }
            }
        };

        this.addMouseMotionListener(mouseHandler);
        this.addMouseListener(mouseHandler);
    }

    private void sendInput() {
        InputPacket input = new InputPacket();
        input.playerId = myPlayerId;

        if (keyW) input.moveY = -1;
        else if (keyS) input.moveY = 1;
        else input.moveY = 0;

        if (keyA) input.moveX = -1;
        else if (keyD) input.moveX = 1;
        else input.moveX = 0;

        ClientPlayer myPlayer = renderPlayers.get(myPlayerId);
        if (myPlayer != null) {
            input.mouseAngle = (float) Math.atan2(mouseY - myPlayer.y, mouseX - myPlayer.x);
        } else {
            input.mouseAngle = 0;
        }

        input.isShooting = isShooting;

        if (isShooting) {
            long now = System.currentTimeMillis();
            if (now - lastShotSoundTime >= 200) {
                playSound("shoot.wav");
                lastShotSoundTime = now;
            }
        }

        udpClient.sendInput(input);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        long now = System.nanoTime();
        float deltaTime = (now - lastTime) / 1_000_000_000.0f;
        lastTime = now;

        // 1. Vẽ nền (Lát gạch 256x256)
        if (bgImage != null) {
            int bw = bgImage.getWidth();
            int bh = bgImage.getHeight();
            for (int x = 0; x < getWidth(); x += bw) {
                for (int y = 0; y < getHeight(); y += bh) {
                    g2d.drawImage(bgImage, x, y, null);
                }
            }
        } else {
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }

        // 2. Vẽ toàn bộ thiên thạch rải rác tự nhiên dạng hạt mưa
        for (float[] obs : OBSTACLES) {
            float ox = obs[0], oy = obs[1], or = obs[2];
            int type = (int) obs[3];
            float rot = obs[4];
            BufferedImage mImg = (meteorImages != null && type < meteorImages.length) ? meteorImages[type] : null;

            if (mImg != null) {
                int mw = mImg.getWidth();
                int mh = mImg.getHeight();
                AffineTransform old = g2d.getTransform();
                g2d.translate(ox, oy);
                g2d.rotate(rot);
                g2d.drawImage(mImg, -mw / 2, -mh / 2, null);
                g2d.setTransform(old);
            } else {
                g2d.setColor(new Color(110, 70, 45));
                g2d.fillOval((int) (ox - or), (int) (oy - or), (int) (or * 2), (int) (or * 2));
            }
        }

        StatePacket state = udpClient.latestState;
        if (state == null) {
            g2d.setColor(Color.WHITE);
            g2d.drawString("Waiting for server state...", 50, 50);
            return;
        }

        // 3. Vẽ tất cả viên đạn (Xoay dọc chính xác theo hướng bay)
        for (int bIdx = 0; bIdx < state.bulletCount; bIdx++) {
            float bx = state.bulletXs[bIdx];
            float by = state.bulletYs[bIdx];
            float bAngle = state.bulletAngles[bIdx];

            if (bulletImage != null) {
                int bw = bulletImage.getWidth();
                int bh = bulletImage.getHeight();

                AffineTransform old = g2d.getTransform();
                g2d.translate(bx, by);
                g2d.rotate(bAngle + Math.PI / 2);
                g2d.drawImage(bulletImage, -bw / 2, -bh / 2, null);
                g2d.setTransform(old);
            } else {
                g2d.setColor(Color.YELLOW);
                g2d.fillOval((int) bx - 3, (int) by - 3, 6, 6);
            }
        }

        // 4. Vẽ tất cả máy bay (Interpolation nhanh, dứt khoát)
        float lerpSpeed = 25.0f;
        for (int i = 0; i < state.playerCount; i++) {
            byte pid = state.playerIds[i];
            float targetX = state.playerXs[i];
            float targetY = state.playerYs[i];
            float targetAngle = state.playerAngles[i];
            byte hp = state.playerHPs[i];
            byte score = state.playerScores[i];

            if (pid == myPlayerId) {
                if (hp < lastHp) {
                    if (hp == 0) playSound("explosion.wav");
                    else playSound("hit.wav");
                }
                lastHp = hp;
            }

            ClientPlayer rp = renderPlayers.computeIfAbsent(pid, k -> {
                ClientPlayer np = new ClientPlayer();
                np.x = targetX;
                np.y = targetY;
                np.angle = targetAngle;
                return np;
            });

            rp.x += (targetX - rp.x) * lerpSpeed * deltaTime;
            rp.y += (targetY - rp.y) * lerpSpeed * deltaTime;
            rp.angle = targetAngle;

            if (hp <= 0) {
                g2d.setColor(Color.GRAY);
                g2d.drawString("RESPAWNING...", (int) rp.x - 40, (int) rp.y);
                continue;
            }

            BufferedImage ship = shipImages[pid % 4];
            int shipW = 45;
            int shipH = 40;

            AffineTransform old = g2d.getTransform();
            g2d.translate(rp.x, rp.y);
            g2d.rotate(rp.angle + Math.PI / 2);

            if (ship != null) {
                g2d.drawImage(ship, -shipW / 2, -shipH / 2, shipW, shipH, null);
            } else {
                g2d.setColor(pid == myPlayerId ? Color.GREEN : Color.RED);
                g2d.fillRect(-15, -15, 30, 30);
            }
            g2d.setTransform(old);

            // Vẽ thanh máu (HP Bar)
            int barW = 40;
            int barH = 5;
            int barX = (int) rp.x - barW / 2;
            int barY = (int) rp.y - shipH / 2 - 12;

            g2d.setColor(Color.RED);
            g2d.fillRect(barX, barY, barW, barH);
            g2d.setColor(Color.GREEN);
            int currentHpW = (int) (barW * (Math.max(0, hp) / 100.0f));
            g2d.fillRect(barX, barY, currentHpW, barH);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(barX, barY, barW, barH);

            // Tên + Điểm
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 11));
            String info = (pid == myPlayerId ? "[YOU] " : "") + "P" + pid + " (" + score + " pts)";
            g2d.drawString(info, barX - 5, barY - 3);
        }

        // 5. Vẽ HUD: Đồng hồ đếm ngược trận đấu 60 giây ở đỉnh giữa màn hình
        int timeLeft = state.remainingTime;
        int mins = timeLeft / 60;
        int secs = timeLeft % 60;
        String timeStr = String.format("%02d:%02d", mins, secs);

        int timerBoxW = 120;
        int timerBoxH = 34;
        int timerBoxX = getWidth() / 2 - timerBoxW / 2;
        int timerBoxY = 15;

        g2d.setColor(new Color(20, 25, 35, 210));
        g2d.fillRoundRect(timerBoxX, timerBoxY, timerBoxW, timerBoxH, 16, 16);
        g2d.setColor(timeLeft <= 10 ? new Color(255, 60, 60) : new Color(0, 180, 255));
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawRoundRect(timerBoxX, timerBoxY, timerBoxW, timerBoxH, 16, 16);

        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        FontMetrics fm = g2d.getFontMetrics();
        int textW = fm.stringWidth("⏱ " + timeStr);
        int textH = fm.getAscent();
        g2d.setColor(timeLeft <= 10 ? new Color(255, 80, 80) : Color.WHITE);
        g2d.drawString("⏱ " + timeStr, timerBoxX + (timerBoxW - textW) / 2, timerBoxY + (timerBoxH + textH) / 2 - 4);

        // 6. Mini Leaderboard thời gian thực ở góc trên bên phải
        int boardW = 170;
        int boardH = 28 + (state.playerCount * 20);
        int boardX = getWidth() - boardW - 15;
        int boardY = 15;

        g2d.setColor(new Color(20, 25, 35, 190));
        g2d.fillRoundRect(boardX, boardY, boardW, boardH, 12, 12);
        g2d.setColor(new Color(255, 215, 0, 180));
        g2d.setStroke(new BasicStroke(1.2f));
        g2d.drawRoundRect(boardX, boardY, boardW, boardH, 12, 12);

        g2d.setColor(new Color(255, 215, 0));
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("🏆 BẢNG ĐIỂM", boardX + 12, boardY + 18);

        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        for (int i = 0; i < state.playerCount; i++) {
            byte pid = state.playerIds[i];
            byte score = state.playerScores[i];
            byte hp = state.playerHPs[i];
            boolean isMe = (pid == myPlayerId);
            g2d.setColor(isMe ? Color.CYAN : Color.WHITE);
            String pStr = (isMe ? "➤ " : "  ") + "P" + pid + " : " + score + " pts" + (hp <= 0 ? " (Respawn)" : "");
            g2d.drawString(pStr, boardX + 10, boardY + 38 + (i * 18));
        }
    }

    public void stop() {
        this.isRunning = false;
    }
}
