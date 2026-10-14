package game;

import network.InputPacket;
import network.StatePacket;
import network.TCPClient;
import network.UDPClient;

import javax.imageio.ImageIO;
import audio.SoundManager;
import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GamePanel extends JPanel {
    // Phân bố thiên thạch ngẫu nhiên tự nhiên (Raindrop Scatter) đồng bộ với Server {X, Y, Bán kính, Loại ảnh, Góc xoay}
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

    private UDPClient udpClient;
    private TCPClient tcpClient;
    private byte myPlayerId;
    private final Map<Byte, String> playerNames = new ConcurrentHashMap<>();

    private boolean keyW, keyA, keyS, keyD;
    private boolean isShooting;
    private float mouseX, mouseY;
    private boolean isRunning = true;

    private long lastTime;
    private byte lastHp = 100;
    private byte lastMyShieldHits = 0;
    private long lastShotSoundTime = 0;
    private boolean hasPlayedGameOverSound = false;

    // Trạng thái Rematch (Chơi lại)
    private boolean isRematchReady = false;
    private int rematchCountdown = -1;

    // Hình ảnh tài nguyên
    private BufferedImage bgImage;
    private BufferedImage[] shipImages = new BufferedImage[4];
    private BufferedImage bulletImage;
    private BufferedImage[] meteorImages;
    private BufferedImage[] shieldImages = new BufferedImage[4]; // 1: Bronze, 2: Silver, 3: Gold
    private BufferedImage[] pillImages = new BufferedImage[4];   // 1: Red, 2: Green, 3: Blue
    private BufferedImage laserRedSphereImage;   // laserRed08.png
    private BufferedImage laserGreenSphereImage; // laserGreen14.png
    private BufferedImage laserBlueSphereImage;  // laserBlue08.png

    // Lớp nội bộ lưu tọa độ hiển thị (Nội suy)
    private class ClientPlayer {
        float x, y, angle;
    }
    private ConcurrentHashMap<Byte, ClientPlayer> renderPlayers = new ConcurrentHashMap<>();

    // Quản lý âm thanh & hiệu ứng Buff
    private int lastGreenSubCount = 0;
    private int lastGreenMainCount = 0;
    private byte lastMyWeaponBuff = 0;

    // Lớp hiệu ứng nổ và sóng xung kích (Visual Explosion Effects)
    private static class VisualEffect {
        float x, y;
        float maxRadius;
        Color color;
        long startTime;
        long durationMs;

        VisualEffect(float x, float y, float maxRadius, Color color, long durationMs) {
            this.x = x;
            this.y = y;
            this.maxRadius = maxRadius;
            this.color = color;
            this.startTime = System.currentTimeMillis();
            this.durationMs = durationMs;
        }
    }
    private final List<VisualEffect> visualEffects = new CopyOnWriteArrayList<>();

    private void addExplosionEffect(float x, float y, Color col) {
        visualEffects.add(new VisualEffect(x, y, 42.0f, col, 280));
    }

    public GamePanel(UDPClient udpClient, byte myPlayerId) {
        this(udpClient, null, new HashMap<>());
    }

    public GamePanel(UDPClient udpClient, byte myPlayerId, Map<Byte, String> playerNames) {
        this(udpClient, null, playerNames);
    }

    public GamePanel(UDPClient udpClient, TCPClient tcpClient, Map<Byte, String> playerNames) {
        this.udpClient = udpClient;
        this.tcpClient = tcpClient;
        this.myPlayerId = (tcpClient != null) ? tcpClient.getMyPlayerId() : 0;
        if (playerNames != null) {
            this.playerNames.putAll(playerNames);
        }

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

    public void setCountdown(int seconds) {
        this.rematchCountdown = seconds;
        repaint();
    }

    public void resetForRematch() {
        this.isRematchReady = false;
        this.rematchCountdown = -1;
        this.hasPlayedGameOverSound = false;
        this.lastHp = 100;
        this.lastMyShieldHits = 0;
        this.keyW = false;
        this.keyA = false;
        this.keyS = false;
        this.keyD = false;
        this.isShooting = false;
        this.lastGreenSubCount = 0;
        this.lastGreenMainCount = 0;
        this.lastMyWeaponBuff = 0;
        this.visualEffects.clear();
        this.renderPlayers.clear();
        if (udpClient != null) {
            udpClient.resetState();
        }
        repaint();
    }

    public interface GamePanelListener {
        void onReturnToLobby();
    }
    private GamePanelListener panelListener;

    public void updatePlayerNames(Map<Byte, String> names) {
        if (names != null) {
            this.playerNames.putAll(names);
        }
    }

    public void setGamePanelListener(GamePanelListener listener) {
        this.panelListener = listener;
    }

    public void stopGame() {
        this.isRunning = false;
        if (udpClient != null) {
            udpClient.disconnect();
        }
    }

    public void returnToLobby() {
        stopGame();
        if (tcpClient != null) {
            tcpClient.sendLeaveRoom();
        }
        if (panelListener != null) {
            panelListener.onReturnToLobby();
        }
    }

    private void triggerRematchReady() {
        StatePacket state = udpClient.latestState;
        if (state != null && state.remainingSeconds <= 0 && !isRematchReady) {
            isRematchReady = true;
            if (tcpClient != null) {
                tcpClient.sendRematchReady();
            }
            repaint();
        }
    }

    private File findAssetFile(String relativePath) {
        File f1 = new File(relativePath);
        if (f1.exists()) return f1;
        File f2 = new File("HelicopterClient/" + relativePath);
        if (f2.exists()) return f2;
        return f1;
    }

    private void loadAssets() {
        try {
            File bgFile = findAssetFile("assets/images/black.png");
            if (bgFile.exists()) bgImage = ImageIO.read(bgFile);

            String[] shipFiles = {
                "playerShip1_green.png",
                "playerShip1_red.png",
                "playerShip1_blue.png",
                "playerShip1_orange.png"
            };
            for (int i = 0; i < 4; i++) {
                File f = findAssetFile("assets/images/" + shipFiles[i]);
                if (f.exists()) shipImages[i] = ImageIO.read(f);
            }

            File bFile = findAssetFile("assets/images/laserRed01.png");
            if (bFile.exists()) bulletImage = ImageIO.read(bFile);

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
                File mFile = findAssetFile("assets/images/" + meteorFiles[i]);
                if (mFile.exists()) meteorImages[i] = ImageIO.read(mFile);
            }

            String[] shieldFiles = {
                "",
                "shield_bronze.png", // type 1
                "shield_silver.png", // type 2
                "shield_gold.png"    // type 3
            };
            for (int i = 1; i <= 3; i++) {
                File sFile = findAssetFile("assets/images/" + shieldFiles[i]);
                if (sFile.exists()) shieldImages[i] = ImageIO.read(sFile);
            }

            String[] pillFiles = {
                "",
                "pill_red.png",   // type 1
                "pill_green.png", // type 2
                "pill_blue.png"   // type 3
            };
            for (int i = 1; i <= 3; i++) {
                File pFile = findAssetFile("assets/images/" + pillFiles[i]);
                if (pFile.exists()) pillImages[i] = ImageIO.read(pFile);
            }

            File lrFile = findAssetFile("assets/images/laserRed08.png");
            if (lrFile.exists()) laserRedSphereImage = ImageIO.read(lrFile);

            File lgFile = findAssetFile("assets/images/laserGreen14.png");
            if (lgFile.exists()) laserGreenSphereImage = ImageIO.read(lgFile);

            File lbFile = findAssetFile("assets/images/laserBlue08.png");
            if (lbFile.exists()) laserBlueSphereImage = ImageIO.read(lbFile);
        } catch (Exception e) {
            System.err.println("Warning: Cannot load some images: " + e.getMessage());
        }
    }

    private void playSound(String fileName) {
        SoundManager.getInstance().playSfx(fileName);
    }

    private byte getMyWeaponBuff() {
        StatePacket curState = udpClient.latestState;
        if (curState != null) {
            for (int i = 0; i < curState.playerCount; i++) {
                if (curState.playerIds[i] == myPlayerId) {
                    return (i < curState.playerWeaponBuffs.length) ? curState.playerWeaponBuffs[i] : 0;
                }
            }
        }
        return 0;
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
                    case KeyEvent.VK_R -> triggerRematchReady(); // Phím tắt R để sẵn sàng chơi lại
                    case KeyEvent.VK_SPACE -> {
                        isShooting = true;
                        byte buff = getMyWeaponBuff();
                        if (buff != 3) { // Buff Blue: Không bắn súng, không phát âm thanh bắn
                            long now = System.currentTimeMillis();
                            long shotCd = (buff == 1) ? 400 : 200;
                            if (now - lastShotSoundTime >= shotCd) {
                                if (buff == 2) {
                                    playSound("pill_green_shoot.wav");
                                } else {
                                    playSound("shoot.wav");
                                }
                                lastShotSoundTime = now;
                            }
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
                // Kiểm tra click vào nút Rematch hoặc nút Về sảnh khi trận đấu đã kết thúc
                StatePacket state = udpClient.latestState;
                if (state != null && state.remainingSeconds <= 0) {
                    int boardW = 480;
                    int boardH = 380;
                    int boardX = (getWidth() - boardW) / 2;
                    int boardY = (getHeight() - boardH) / 2;
                    int btnH = 42;
                    int btnY = boardY + boardH - 58;
                    int btnRematchW = 250;
                    int btnLobbyW = 140;
                    int gap = 14;
                    int totalW = btnRematchW + gap + btnLobbyW;
                    int btnRematchX = boardX + (boardW - totalW) / 2;
                    int btnLobbyX = btnRematchX + btnRematchW + gap;

                    // Click nút Rematch
                    if (e.getX() >= btnRematchX && e.getX() <= btnRematchX + btnRematchW &&
                        e.getY() >= btnY && e.getY() <= btnY + btnH) {
                        triggerRematchReady();
                        return;
                    }

                    // Click nút Về sảnh
                    if (e.getX() >= btnLobbyX && e.getX() <= btnLobbyX + btnLobbyW &&
                        e.getY() >= btnY && e.getY() <= btnY + btnH) {
                        returnToLobby();
                        return;
                    }
                }

                if (e.getButton() == MouseEvent.BUTTON1) {
                    isShooting = true;
                    byte buff = getMyWeaponBuff();
                    if (buff != 3) { // Buff Blue: Không bắn súng, không phát âm thanh bắn
                        long now = System.currentTimeMillis();
                        long shotCd = (buff == 1) ? 400 : 200;
                        if (now - lastShotSoundTime >= shotCd) {
                            if (buff == 2) {
                                playSound("pill_green_shoot.wav");
                            } else {
                                playSound("shoot.wav");
                            }
                            lastShotSoundTime = now;
                        }
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
        // Nếu đã hết giờ: khóa hoàn toàn điều khiển
        if (udpClient.latestState != null && udpClient.latestState.remainingSeconds <= 0) {
            InputPacket input = new InputPacket();
            input.playerId = myPlayerId;
            input.moveX = 0;
            input.moveY = 0;
            input.mouseAngle = 0;
            input.isShooting = false;
            udpClient.sendInput(input);
            return;
        }

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
            byte myBuff = getMyWeaponBuff();
            // Khi có Buff Green (bắn từng nhịp click) hoặc Buff Blue (không bắn thường): không phát âm thanh liên tục khi giữ chuột
            if (myBuff != 2 && myBuff != 3) {
                long now = System.currentTimeMillis();
                long shotCd = (myBuff == 1) ? 400 : 200;
                if (now - lastShotSoundTime >= shotCd) {
                    playSound("shoot.wav");
                    lastShotSoundTime = now;
                }
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

        // A. Kiểm tra nhặt thuốc nâng cấp (Pill Pickup Sound)
        byte curMyBuff = 0;
        byte myBuffRemainingSec = 0;
        for (int pIdx = 0; pIdx < state.playerCount; pIdx++) {
            if (state.playerIds[pIdx] == myPlayerId) {
                curMyBuff = (pIdx < state.playerWeaponBuffs.length) ? state.playerWeaponBuffs[pIdx] : 0;
                myBuffRemainingSec = (pIdx < state.playerBuffRemainingSeconds.length) ? state.playerBuffRemainingSeconds[pIdx] : 0;
                break;
            }
        }
        if (curMyBuff > 0 && curMyBuff != lastMyWeaponBuff) {
            playSound("pill_pickup.wav");
        }
        lastMyWeaponBuff = curMyBuff;

        // B. Phát âm thanh khi có đạn xanh lá được bắn ra trên bản đồ
        int curGreenMainCount = 0;
        int curGreenSubCount = 0;
        for (int b = 0; b < state.bulletCount; b++) {
            byte bType = (b < state.bulletTypes.length) ? state.bulletTypes[b] : 0;
            if (bType == 2) curGreenMainCount++;
            else if (bType == 3) curGreenSubCount++;
        }

        if (curGreenMainCount > lastGreenMainCount) {
            if (System.currentTimeMillis() - lastShotSoundTime >= 100) {
                playSound("pill_green_shoot.wav");
            }
        }
        lastGreenMainCount = curGreenMainCount;

        // C. Phát âm thanh nổ đạn xanh & tạo hiệu ứng nổ (Cluster Explosion)
        if (curGreenSubCount > lastGreenSubCount) {
            playSound("pill_green_explode.wav");
            for (int b = 0; b < state.bulletCount; b++) {
                byte bType = (b < state.bulletTypes.length) ? state.bulletTypes[b] : 0;
                if (bType == 3) {
                    addExplosionEffect(state.bulletXs[b], state.bulletYs[b], new Color(60, 255, 100));
                    break;
                }
            }
        }
        lastGreenSubCount = curGreenSubCount;

        // 2.5. Vẽ các vật phẩm Khiên xuất hiện trên bản đồ (Rớt ngẫu nhiên theo đợt)
        float itemPulse = (float) Math.sin(System.currentTimeMillis() * 0.006f) * 3.0f;
        for (int sIdx = 0; sIdx < state.shieldItemCount; sIdx++) {
            byte sType = state.shieldItemTypes[sIdx];
            float sx = state.shieldItemXs[sIdx];
            float sy = state.shieldItemYs[sIdx];

            Color glowCol;
            if (sType == 3) glowCol = new Color(255, 215, 0);       // Vàng
            else if (sType == 2) glowCol = new Color(180, 230, 255); // Bạc
            else glowCol = new Color(205, 127, 50);                 // Đồng

            // Vòng hào quang phát sáng dưới đất
            g2d.setColor(new Color(glowCol.getRed(), glowCol.getGreen(), glowCol.getBlue(), 45));
            g2d.fillOval((int) (sx - 20 - itemPulse), (int) (sy - 20 - itemPulse), (int) ((20 + itemPulse) * 2), (int) ((20 + itemPulse) * 2));
            g2d.setColor(new Color(glowCol.getRed(), glowCol.getGreen(), glowCol.getBlue(), 180));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawOval((int) (sx - 17 - itemPulse * 0.5f), (int) (sy - 17 - itemPulse * 0.5f), (int) ((17 + itemPulse * 0.5f) * 2), (int) ((17 + itemPulse * 0.5f) * 2));

            // Hiệu ứng lơ lửng nhẹ và vẽ Icon
            float hoverY = (float) Math.sin(System.currentTimeMillis() * 0.005f + sIdx) * 3.0f;
            BufferedImage sImg = (sType >= 1 && sType <= 3) ? shieldImages[sType] : null;
            if (sImg != null) {
                int sw = sImg.getWidth();
                int sh = sImg.getHeight();
                g2d.drawImage(sImg, (int) (sx - sw / 2.0f), (int) (sy - sh / 2.0f + hoverY), null);
            } else {
                g2d.setColor(glowCol);
                g2d.fillRoundRect((int) (sx - 12), (int) (sy - 12 + hoverY), 24, 24, 6, 6);
            }
        }

        // 2.6. Vẽ các vật phẩm Thuốc nâng cấp (Pills) xuất hiện trên bản đồ
        for (int pIdx = 0; pIdx < state.pillItemCount; pIdx++) {
            byte pType = state.pillItemTypes[pIdx];
            float px = state.pillItemXs[pIdx];
            float py = state.pillItemYs[pIdx];

            Color glowCol;
            if (pType == 1) glowCol = new Color(255, 60, 60);       // Đỏ (Pill Red)
            else if (pType == 2) glowCol = new Color(60, 255, 100);  // Xanh lá (Pill Green)
            else glowCol = new Color(60, 160, 255);                 // Xanh dương (Pill Blue)

            // Vòng hào quang phát sáng dưới đất
            g2d.setColor(new Color(glowCol.getRed(), glowCol.getGreen(), glowCol.getBlue(), 55));
            g2d.fillOval((int) (px - 18 - itemPulse), (int) (py - 18 - itemPulse), (int) ((18 + itemPulse) * 2), (int) ((18 + itemPulse) * 2));
            g2d.setColor(new Color(glowCol.getRed(), glowCol.getGreen(), glowCol.getBlue(), 190));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawOval((int) (px - 15 - itemPulse * 0.5f), (int) (py - 15 - itemPulse * 0.5f), (int) ((15 + itemPulse * 0.5f) * 2), (int) ((15 + itemPulse * 0.5f) * 2));

            // Hiệu ứng lơ lửng nhẹ và vẽ Icon thuốc (22x21)
            float hoverY = (float) Math.sin(System.currentTimeMillis() * 0.005f + pIdx * 1.5f) * 3.0f;
            BufferedImage pImg = (pType >= 1 && pType <= 3) ? pillImages[pType] : null;
            if (pImg != null) {
                int pw = pImg.getWidth();
                int ph = pImg.getHeight();
                g2d.drawImage(pImg, (int) (px - pw / 2.0f), (int) (py - ph / 2.0f + hoverY), null);
            } else {
                g2d.setColor(glowCol);
                g2d.fillRoundRect((int) (px - 10), (int) (py - 10 + hoverY), 20, 20, 5, 5);
            }
        }

        // 3. Vẽ tất cả viên đạn (Xoay dọc chính xác theo hướng bay hoặc đạn khối cầu)
        for (int bIdx = 0; bIdx < state.bulletCount; bIdx++) {
            float bx = state.bulletXs[bIdx];
            float by = state.bulletYs[bIdx];
            float bAngle = state.bulletAngles[bIdx];
            byte bType = (bIdx < state.bulletTypes.length) ? state.bulletTypes[bIdx] : 0;

            if (bType == 1) { // Đạn cầu đỏ (Pill Red - 3 luồng đạn)
                if (laserRedSphereImage != null) {
                    int sphereSize = 24; // Kích thước hiển thị gọn gàng, sắc nét
                    AffineTransform old = g2d.getTransform();
                    g2d.translate(bx, by);
                    g2d.rotate(bAngle + Math.PI / 2);
                    g2d.drawImage(laserRedSphereImage, -sphereSize / 2, -sphereSize / 2, sphereSize, sphereSize, null);
                    g2d.setTransform(old);
                } else {
                    g2d.setColor(new Color(255, 60, 60));
                    g2d.fillOval((int) bx - 6, (int) by - 6, 12, 12);
                }
            } else if (bType == 2) { // Đạn cầu xanh lá mẹ (Pill Green - Cluster Main)
                if (laserGreenSphereImage != null) {
                    int sphereSize = 26; // Kích thước đạn mẹ 26px
                    AffineTransform old = g2d.getTransform();
                    g2d.translate(bx, by);
                    g2d.rotate(bAngle + Math.PI / 2);
                    // Hào quang xanh lá quanh quả cầu nén
                    g2d.setColor(new Color(60, 255, 100, 60));
                    g2d.fillOval(-16, -16, 32, 32);
                    g2d.drawImage(laserGreenSphereImage, -sphereSize / 2, -sphereSize / 2, sphereSize, sphereSize, null);
                    g2d.setTransform(old);
                } else {
                    g2d.setColor(new Color(60, 255, 100));
                    g2d.fillOval((int) bx - 7, (int) by - 7, 14, 14);
                }
            } else if (bType == 3) { // Đạn cầu xanh lá con (Pill Green - Frag Sub 4 mảnh)
                if (laserGreenSphereImage != null) {
                    int sphereSize = 17; // Kích thước đạn con nhỏ gọn 17px
                    AffineTransform old = g2d.getTransform();
                    g2d.translate(bx, by);
                    g2d.rotate(bAngle + Math.PI / 2);
                    g2d.drawImage(laserGreenSphereImage, -sphereSize / 2, -sphereSize / 2, sphereSize, sphereSize, null);
                    g2d.setTransform(old);
                } else {
                    g2d.setColor(new Color(120, 255, 140));
                    g2d.fillOval((int) bx - 4, (int) by - 4, 8, 8);
                }
            } else { // Đạn laser đỏ dài thông thường (type 0)
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
        }

        // 3.5. Vẽ hiệu ứng nổ và sóng xung kích (Visual Explosion Effects)
        long currentTime = System.currentTimeMillis();
        for (VisualEffect eff : visualEffects) {
            float progress = (currentTime - eff.startTime) / (float) eff.durationMs;
            if (progress >= 1.0f) {
                visualEffects.remove(eff);
                continue;
            }

            float currentRadius = eff.maxRadius * (float) Math.sin(progress * Math.PI / 2);
            int alpha = (int) (220 * (1.0f - progress));

            // Vòng sóng xung kích phát sáng nở rộng
            g2d.setColor(new Color(eff.color.getRed(), eff.color.getGreen(), eff.color.getBlue(), alpha));
            g2d.setStroke(new BasicStroke(2.5f * (1.0f - progress * 0.5f)));
            g2d.drawOval((int) (eff.x - currentRadius), (int) (eff.y - currentRadius), (int) (currentRadius * 2), (int) (currentRadius * 2));

            // Quầng sáng tâm vụ nổ
            int coreAlpha = (int) (alpha * 0.45f);
            g2d.setColor(new Color(eff.color.getRed(), eff.color.getGreen(), eff.color.getBlue(), coreAlpha));
            float coreR = currentRadius * 0.6f;
            g2d.fillOval((int) (eff.x - coreR), (int) (eff.y - coreR), (int) (coreR * 2), (int) (coreR * 2));

            // 6 tia plasma phân tán ra xung quanh
            for (int s = 0; s < 6; s++) {
                double sparkAngle = s * (Math.PI / 3) + progress * 0.8;
                float sparkDist = currentRadius * 1.15f;
                float spX = eff.x + (float) Math.cos(sparkAngle) * sparkDist;
                float spY = eff.y + (float) Math.sin(sparkAngle) * sparkDist;
                g2d.setColor(new Color(220, 255, 230, alpha));
                g2d.fillOval((int) spX - 2, (int) spY - 2, 4, 4);
            }
        }

        // 4. Vẽ tất cả máy bay (Interpolation)
        float lerpSpeed = 25.0f;
        for (int i = 0; i < state.playerCount; i++) {
            byte pid = state.playerIds[i];
            float targetX = state.playerXs[i];
            float targetY = state.playerYs[i];
            float targetAngle = state.playerAngles[i];
            byte hp = state.playerHPs[i];
            byte score = state.playerScores[i];
            byte sType = (i < state.playerShieldTypes.length) ? state.playerShieldTypes[i] : 0;
            byte sHits = (i < state.playerShieldHits.length) ? state.playerShieldHits[i] : 0;
            byte wBuff = (i < state.playerWeaponBuffs.length) ? state.playerWeaponBuffs[i] : 0;

            if (pid == myPlayerId) {
                if (sHits > lastMyShieldHits) {
                    playSound("sfx_shieldUp.wav");
                } else if (sHits < lastMyShieldHits) {
                    playSound("sfx_shieldDown.wav");
                }
                lastMyShieldHits = sHits;

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

            // 4.0. Hiệu ứng nòng súng khi có Buff
            if (wBuff == 1) { // Pill Red: 3 nòng phát sáng đỏ
                AffineTransform oldXf = g2d.getTransform();
                g2d.translate(rp.x, rp.y);
                g2d.rotate(rp.angle + Math.PI / 2);
                g2d.setColor(new Color(255, 60, 60, 210));
                g2d.fillOval(-18, -14, 5, 5); // Nòng trái
                g2d.fillOval(13, -14, 5, 5);  // Nòng phải
                g2d.fillOval(-3, -24, 6, 6);   // Nòng chính giữa
                g2d.setTransform(oldXf);
            } else if (wBuff == 2) { // Pill Green: Nạp quả cầu plasma xanh lá ở mũi tàu
                AffineTransform oldXf = g2d.getTransform();
                g2d.translate(rp.x, rp.y);
                g2d.rotate(rp.angle + Math.PI / 2);
                g2d.setColor(new Color(60, 255, 100, 220));
                g2d.fillOval(-4, -26, 8, 8);
                g2d.setColor(new Color(180, 255, 200, 240));
                g2d.fillOval(-2, -24, 4, 4);
                g2d.setTransform(oldXf);
            } else if (wBuff == 3) { // Pill Blue: Vòng laser 4 quả cầu xanh xoay tròn quanh phi thuyền bảo vệ
                float orbitRadius = 45.0f;
                float baseOrbitAngle = (System.currentTimeMillis() % 100000) * 0.0035f;

                // A. Hào quang laser nền bán trong suốt quanh phi thuyền
                g2d.setColor(new Color(60, 160, 255, 30));
                g2d.fillOval((int) (rp.x - orbitRadius - 5), (int) (rp.y - orbitRadius - 5), (int) ((orbitRadius + 5) * 2), (int) ((orbitRadius + 5) * 2));

                // B. Vòng quỹ đạo lực trường công nghệ đứt đoạn xoay tròn
                AffineTransform oldRingXf = g2d.getTransform();
                g2d.translate(rp.x, rp.y);
                g2d.rotate(baseOrbitAngle * 0.5f);
                g2d.setColor(new Color(100, 200, 255, 150));
                g2d.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{8, 8}, 0));
                g2d.drawOval((int) -orbitRadius, (int) -orbitRadius, (int) (orbitRadius * 2), (int) (orbitRadius * 2));
                g2d.setTransform(oldRingXf);

                // C. Tính tọa độ 4 quả cầu xanh trên quỹ đạo
                float[] orbXs = new float[4];
                float[] orbYs = new float[4];
                for (int k = 0; k < 4; k++) {
                    float orbAngle = baseOrbitAngle + (float) (k * Math.PI / 2);
                    orbXs[k] = rp.x + (float) Math.cos(orbAngle) * orbitRadius;
                    orbYs[k] = rp.y + (float) Math.sin(orbAngle) * orbitRadius;
                }

                // D. Tia liên kết năng lượng laser hình kim cương nối 4 quả cầu
                g2d.setColor(new Color(120, 210, 255, 80));
                g2d.setStroke(new BasicStroke(1.2f));
                for (int k = 0; k < 4; k++) {
                    int nextK = (k + 1) % 4;
                    g2d.drawLine((int) orbXs[k], (int) orbYs[k], (int) orbXs[nextK], (int) orbYs[nextK]);
                }

                // E. Vẽ 4 quả cầu laser xanh (laserBlue08.png) xoay đều quanh tàu
                int sphereSize = 24;
                for (int k = 0; k < 4; k++) {
                    float ox = orbXs[k];
                    float oy = orbYs[k];

                    // Vầng sáng tỏa ra từ mỗi quả cầu
                    g2d.setColor(new Color(80, 180, 255, 75));
                    g2d.fillOval((int) ox - 14, (int) oy - 14, 28, 28);

                    if (laserBlueSphereImage != null) {
                        AffineTransform oldOrbXf = g2d.getTransform();
                        g2d.translate(ox, oy);
                        float orbSpin = baseOrbitAngle * 3.0f + k * (float) Math.PI;
                        g2d.rotate(orbSpin);
                        g2d.drawImage(laserBlueSphereImage, -sphereSize / 2, -sphereSize / 2, sphereSize, sphereSize, null);
                        g2d.setTransform(oldOrbXf);
                    } else {
                        g2d.setColor(new Color(80, 200, 255));
                        g2d.fillOval((int) ox - 8, (int) oy - 8, 16, 16);
                    }
                }

                // F. Điểm lõi năng lượng phòng thủ tại mũi tàu
                AffineTransform oldCoreXf = g2d.getTransform();
                g2d.translate(rp.x, rp.y);
                g2d.rotate(rp.angle + Math.PI / 2);
                g2d.setColor(new Color(60, 180, 255, 220));
                g2d.fillOval(-4, -20, 8, 8);
                g2d.setColor(new Color(220, 245, 255, 240));
                g2d.fillOval(-2, -18, 4, 4);
                g2d.setTransform(oldCoreXf);
            }

            // 4.1. Vẽ Hào quang Khiên bảo vệ (Aura Shield) bao quanh tàu nếu có khiên
            if (sHits > 0) {
                float pulse = (float) Math.sin(System.currentTimeMillis() * 0.008f) * 2.5f;
                float auraR = 28.0f + pulse;
                Color auraBase;
                Color auraBorder;
                if (sType == 3) { // Vàng (3 lượt đỡ)
                    auraBase = new Color(255, 215, 0, 45);
                    auraBorder = new Color(255, 225, 70, 210);
                } else if (sType == 2) { // Bạc (2 lượt đỡ)
                    auraBase = new Color(160, 220, 255, 45);
                    auraBorder = new Color(200, 240, 255, 210);
                } else { // Đồng (1 lượt đỡ)
                    auraBase = new Color(205, 127, 50, 45);
                    auraBorder = new Color(245, 165, 85, 210);
                }

                // Lớp màng năng lượng hình cầu bán trong suốt
                g2d.setColor(auraBase);
                g2d.fillOval((int) (rp.x - auraR), (int) (rp.y - auraR), (int) (auraR * 2), (int) (auraR * 2));

                // Viền khiên phát sáng
                g2d.setColor(auraBorder);
                g2d.setStroke(new BasicStroke(2.0f));
                g2d.drawOval((int) (rp.x - auraR), (int) (rp.y - auraR), (int) (auraR * 2), (int) (auraR * 2));

                // Vòng xoay công nghệ lực trường đứt đoạn (Energy Shield Ring)
                float ringAngle = (System.currentTimeMillis() % 4000) * 0.09f;
                AffineTransform oldXf = g2d.getTransform();
                g2d.translate(rp.x, rp.y);
                g2d.rotate(Math.toRadians(ringAngle));
                g2d.setColor(new Color(auraBorder.getRed(), auraBorder.getGreen(), auraBorder.getBlue(), 130));
                g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{10, 8}, 0));
                g2d.drawOval((int) -(auraR + 3), (int) -(auraR + 3), (int) ((auraR + 3) * 2), (int) ((auraR + 3) * 2));
                g2d.setTransform(oldXf);
            }

            // 4.2. Vẽ thanh máu (HP Bar)
            int barW = 44;
            int barH = 5;
            int barX = (int) rp.x - barW / 2;
            int barY = (int) rp.y - shipH / 2 - 10;

            g2d.setColor(new Color(220, 45, 45));
            g2d.fillRect(barX, barY, barW, barH);
            g2d.setColor(new Color(50, 230, 95));
            int currentHpW = (int) (barW * (Math.max(0, hp) / 100.0f));
            g2d.fillRect(barX, barY, currentHpW, barH);
            g2d.setColor(new Color(15, 20, 30));
            g2d.drawRect(barX, barY, barW, barH);

            // 4.3. Vẽ các nấc khiên (Shield Pips) phía trên thanh máu (nếu có khiên)
            if (sHits > 0) {
                Color pipColor = (sType == 3) ? new Color(255, 215, 0) : (sType == 2 ? new Color(180, 230, 255) : new Color(225, 145, 75));
                int pipW = 8;
                int pipH = 3;
                int pipGap = 3;
                int totalPipW = sHits * pipW + (sHits - 1) * pipGap;
                int pipX = (int) rp.x - totalPipW / 2;
                int pipY = barY - 5;
                for (int pIdx = 0; pIdx < sHits; pIdx++) {
                    g2d.setColor(pipColor);
                    g2d.fillRect(pipX + pIdx * (pipW + pipGap), pipY, pipW, pipH);
                    g2d.setColor(Color.WHITE);
                    g2d.drawRect(pipX + pIdx * (pipW + pipGap), pipY, pipW, pipH);
                }
            }

            // 4.4. TÊN PHI CÔNG HIỂN THỊ CĂN GIỮA PHÍA TRÊN PHI THUYỀN
            g2d.setFont(new Font(Font.DIALOG, Font.BOLD, 11));
            FontMetrics fmPilot = g2d.getFontMetrics();
            String pName = playerNames.getOrDefault(pid, "Pilot #" + pid);
            String pilotDisplay = (pid == myPlayerId ? "[YOU] " : "") + pName;
            int pilotW = fmPilot.stringWidth(pilotDisplay);
            int pilotX = (int) rp.x - pilotW / 2;
            int pilotY = (sHits > 0) ? barY - 8 : barY - 4;

            // Đổ bóng chữ đen
            g2d.setColor(new Color(0, 0, 0, 230));
            g2d.drawString(pilotDisplay, pilotX + 1, pilotY + 1);

            // Màu chữ phi công
            if (pid == myPlayerId) {
                g2d.setColor(new Color(60, 240, 255)); // Neon Cyan cho bạn
            } else {
                g2d.setColor(new Color(245, 245, 250)); // Trắng sáng cho đối thủ
            }
            g2d.drawString(pilotDisplay, pilotX, pilotY);

            // Icon thuốc mini bên cạnh thanh máu nếu có buff
            if (wBuff >= 1 && wBuff <= 3 && pillImages[wBuff] != null) {
                g2d.drawImage(pillImages[wBuff], barX + barW + 5, barY - 4, 13, 12, null);
            }
        }

        // 5. Đồng hồ đếm ngược trận đấu 60 giây (HUD Timer ở giữa trên cùng)
        int remSec = Math.max(0, (int) state.remainingSeconds);
        int minutes = remSec / 60;
        int seconds = remSec % 60;
        String timeStr = String.format("%02d:%02d", minutes, seconds);

        int timerW = 120;
        int timerH = 34;
        int timerX = (getWidth() - timerW) / 2;
        int timerY = 12;

        g2d.setColor(new Color(15, 20, 35, 210));
        g2d.fillRoundRect(timerX, timerY, timerW, timerH, 10, 10);

        if (remSec <= 10 && remSec > 0) {
            g2d.setColor(new Color(255, 60, 60));
        } else {
            g2d.setColor(new Color(70, 130, 220));
        }
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawRoundRect(timerX, timerY, timerW, timerH, 10, 10);

        g2d.setFont(new Font("Consolas", Font.BOLD, 18));
        g2d.setColor(remSec <= 10 && remSec > 0 ? new Color(255, 90, 90) : Color.WHITE);
        FontMetrics fmTime = g2d.getFontMetrics();
        int tx = timerX + (timerW - fmTime.stringWidth(timeStr)) / 2;
        int ty = timerY + ((timerH - fmTime.getHeight()) / 2) + fmTime.getAscent();
        g2d.drawString(timeStr, tx, ty);

        // 5.1. Bảng điểm góc phải thời gian thực (In-Game Real-Time Scoreboard)
        drawInGameScoreboard(g2d, state);

        // 5.5. Bảng thông báo Buff vũ khí góc dưới bên trái (Weapon Buff Status HUD)
        if (curMyBuff >= 1 && curMyBuff <= 3) {
            int hudX = 20;
            int hudY = getHeight() - 76;
            int hudW = 240;
            int hudH = 56;

            Color themeCol;
            String titleText;
            String descText;
            if (curMyBuff == 1) {
                themeCol = new Color(255, 75, 75);
                titleText = "3-WAY SPREAD SHOT";
                descText = "3 luồng đạn plasma đỏ cực mạnh";
            } else if (curMyBuff == 2) {
                themeCol = new Color(50, 230, 90);
                titleText = "CLUSTER FRAG BOMB";
                descText = "Click từng nhịp • Nổ 4 đạn phân mảnh";
            } else {
                themeCol = new Color(40, 180, 255);
                titleText = "ORBITAL LASER BARRIER";
                descText = "Chắn đạn bảo vệ • Thiêu đốt áp sát";
            }

            // Nền hộp kính mờ
            g2d.setColor(new Color(15, 20, 35, 220));
            g2d.fillRoundRect(hudX, hudY, hudW, hudH, 12, 12);

            // Viền phát sáng theo màu buff
            g2d.setColor(themeCol);
            g2d.setStroke(new BasicStroke(1.8f));
            g2d.drawRoundRect(hudX, hudY, hudW, hudH, 12, 12);

            // Icon thuốc
            if (pillImages[curMyBuff] != null) {
                g2d.drawImage(pillImages[curMyBuff], hudX + 10, hudY + 12, 30, 28, null);
            }

            // Tên Buff
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.setColor(themeCol);
            g2d.drawString(titleText, hudX + 48, hudY + 22);

            // Mô tả
            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            g2d.setColor(new Color(190, 205, 225));
            g2d.drawString(descText, hudX + 48, hudY + 36);

            // Thanh tiến trình đếm ngược thời gian buff (15 giây)
            int barWidth = 145;
            int barHeight = 4;
            int barProgX = hudX + 48;
            int barProgY = hudY + 43;
            float timeRatio = Math.max(0.0f, Math.min(1.0f, myBuffRemainingSec / 15.0f));

            g2d.setColor(new Color(40, 45, 60));
            g2d.fillRect(barProgX, barProgY, barWidth, barHeight);
            g2d.setColor(themeCol);
            g2d.fillRect(barProgX, barProgY, (int) (barWidth * timeRatio), barHeight);

            // Số giây còn lại
            g2d.setFont(new Font("Consolas", Font.BOLD, 12));
            g2d.setColor(Color.WHITE);
            g2d.drawString(myBuffRemainingSec + "s", hudX + hudW - 32, hudY + 48);
        }

        // 6. Màn hình BẢNG XẾP HẠNG TỔNG KẾT & NÚT CHƠI LẠI (Khi hết giờ - remSec == 0)
        if (state.remainingSeconds <= 0) {
            if (!hasPlayedGameOverSound) {
                hasPlayedGameOverSound = true;
                playSound("explosion.wav");
            }

            // Phủ mờ toàn màn hình
            g2d.setColor(new Color(5, 10, 20, 225));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // Xếp hạng danh sách theo Score giảm dần
            class PlayerRank {
                byte id;
                byte score;
                PlayerRank(byte id, byte score) { this.id = id; this.score = score; }
            }
            List<PlayerRank> ranks = new ArrayList<>();
            for (int i = 0; i < state.playerCount; i++) {
                ranks.add(new PlayerRank(state.playerIds[i], state.playerScores[i]));
            }
            ranks.sort((a, b) -> Integer.compare(b.score, a.score));

            // Kiểm tra cơ chế HÒA (DRAW): khi có 2 hoặc nhiều người cùng đạt số điểm cao nhất
            int maxScore = ranks.isEmpty() ? 0 : ranks.get(0).score;
            int tiedTopCount = 0;
            for (PlayerRank pr : ranks) {
                if (pr.score == maxScore) tiedTopCount++;
            }
            boolean isMatchDraw = (tiedTopCount >= 2);

            // Khung Bảng xếp hạng chính giữa
            int boardW = 480;
            int boardH = 380;
            int boardX = (getWidth() - boardW) / 2;
            int boardY = (getHeight() - boardH) / 2;

            g2d.setColor(new Color(20, 25, 45, 250));
            g2d.fillRoundRect(boardX, boardY, boardW, boardH, 20, 20);

            // Viền phát quang và 4 góc Starlight theo kết quả Hòa hay Thắng
            Color themeColor;
            if (isMatchDraw) {
                themeColor = (maxScore > 0) ? new Color(255, 175, 50) : new Color(140, 210, 255);
            } else {
                themeColor = new Color(255, 215, 0); // Vàng kim Gold
            }
            g2d.setColor(themeColor);
            g2d.setStroke(new BasicStroke(2.5f));
            g2d.drawRoundRect(boardX, boardY, boardW, boardH, 20, 20);
            drawButtonBrackets(g2d, boardX, boardY, boardW, boardH, themeColor);

            // Tiêu đề & Phụ đề thông báo kết quả
            String title;
            Color titleColor;
            String sub;
            if (isMatchDraw) {
                if (maxScore > 0) {
                    title = "⚔️ MATCH DRAW - BẤT PHÂN THẮNG BẠI ⚔️";
                    titleColor = new Color(255, 175, 50); // Hổ phách Starlight Amber
                    sub = "Kết quả Hòa! " + tiedTopCount + " phi công có cùng " + maxScore + " mạng tiêu diệt!";
                } else {
                    title = "🤝 MATCH DRAW - HÒA NHAU (0 - 0) 🤝";
                    titleColor = new Color(140, 210, 255); // Cyan mềm
                    sub = "Trận đấu hòa: Không có phi công nào bị tiêu diệt sau 60 giây!";
                }
            } else {
                title = "🏆 VICTORY - LEADERBOARD 🏆";
                titleColor = new Color(255, 215, 0); // Vàng kim Gold
                String winnerName = ranks.isEmpty() ? "None" : playerNames.getOrDefault(ranks.get(0).id, "Pilot #" + ranks.get(0).id);
                sub = "Vinh danh " + winnerName + " đã xuất sắc giành chiến thắng với " + maxScore + " Kills!";
            }

            g2d.setFont(new Font(Font.DIALOG, Font.BOLD, 20));
            g2d.setColor(titleColor);
            FontMetrics fmTitle = g2d.getFontMetrics();
            g2d.drawString(title, boardX + (boardW - fmTitle.stringWidth(title)) / 2, boardY + 42);

            g2d.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
            g2d.setColor(new Color(185, 200, 225));
            FontMetrics fmSub = g2d.getFontMetrics();
            g2d.drawString(sub, boardX + (boardW - fmSub.stringWidth(sub)) / 2, boardY + 66);

            g2d.setColor(new Color(60, 75, 110));
            g2d.drawLine(boardX + 30, boardY + 80, boardX + boardW - 30, boardY + 80);

            int rowY = boardY + 108;
            for (int r = 0; r < ranks.size(); r++) {
                PlayerRank pr = ranks.get(r);
                boolean isMe = (pr.id == myPlayerId);
                boolean isTopTied = (isMatchDraw && pr.score == maxScore);
                boolean isClearWinner = (!isMatchDraw && r == 0 && maxScore > 0);

                if (isClearWinner) {
                    g2d.setColor(new Color(255, 215, 0, 45)); // Hạng 1 thắng tuyệt đối
                } else if (isTopTied) {
                    g2d.setColor(new Color(255, 160, 40, 40)); // Đồng hạng 1 (Hòa)
                } else if (isMe) {
                    g2d.setColor(new Color(0, 220, 255, 30)); // Bản thân
                } else {
                    g2d.setColor(new Color(255, 255, 255, 15));
                }
                g2d.fillRoundRect(boardX + 30, rowY - 18, boardW - 60, 36, 10, 10);

                // Viền phát sáng cho người thắng hoặc hòa hạng 1
                if (isClearWinner) {
                    g2d.setColor(new Color(255, 215, 0, 180));
                    g2d.setStroke(new BasicStroke(1.4f));
                    g2d.drawRoundRect(boardX + 30, rowY - 18, boardW - 60, 36, 10, 10);
                } else if (isTopTied) {
                    g2d.setColor(new Color(255, 165, 50, 180));
                    g2d.setStroke(new BasicStroke(1.4f));
                    g2d.drawRoundRect(boardX + 30, rowY - 18, boardW - 60, 36, 10, 10);
                }

                // Huy hiệu Thứ hạng
                String rankBadge;
                Color rankColor;
                if (isTopTied) {
                    rankBadge = "🤝 DRAW";
                    rankColor = new Color(255, 185, 60);
                } else if (isClearWinner) {
                    rankBadge = "🥇 1st";
                    rankColor = new Color(255, 215, 0);
                } else {
                    // Tính thứ hạng thực tế dựa trên điểm số
                    int actualRank = 1;
                    for (int j = 0; j < r; j++) {
                        if (ranks.get(j).score > pr.score) actualRank++;
                    }
                    if (actualRank == 2) {
                        rankBadge = "🥈 2nd";
                        rankColor = new Color(215, 225, 240);
                    } else if (actualRank == 3) {
                        rankBadge = "🥉 3rd";
                        rankColor = new Color(215, 145, 75);
                    } else {
                        rankBadge = "   " + actualRank + "th";
                        rankColor = new Color(160, 175, 195);
                    }
                }

                g2d.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
                g2d.setColor(rankColor);
                g2d.drawString(rankBadge, boardX + 45, rowY + 6);

                // Tên người chơi
                String name = playerNames.getOrDefault(pr.id, "Pilot #" + pr.id);
                if (isMe) name += " (YOU)";
                g2d.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
                if (isMe) {
                    g2d.setColor(new Color(60, 240, 255));
                } else if (isTopTied || isClearWinner) {
                    g2d.setColor(new Color(255, 235, 170));
                } else {
                    g2d.setColor(Color.WHITE);
                }
                g2d.drawString(name, boardX + 130, rowY + 6);

                // Số mạng tiêu diệt (Kills) & Danh hiệu
                String killText = pr.score + " Kills";
                Color killColor = new Color(200, 215, 235);
                if (isClearWinner) {
                    killText += "  👑 WINNER";
                    killColor = new Color(255, 215, 0);
                } else if (isTopTied) {
                    killText += "  🤝 TIED";
                    killColor = new Color(255, 185, 60);
                }

                g2d.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
                g2d.setColor(killColor);
                g2d.drawString(killText, boardX + 300, rowY + 6);

                rowY += 44;
            }

            // 7. Nút Rematch / Chơi lại và Nút Về sảnh ở đáy bảng xếp hạng
            int btnH = 42;
            int btnY = boardY + boardH - 58;
            int btnRematchW = 250;
            int btnLobbyW = 140;
            int gap = 14;
            int totalW = btnRematchW + gap + btnLobbyW;
            int btnRematchX = boardX + (boardW - totalW) / 2;
            int btnLobbyX = btnRematchX + btnRematchW + gap;

            // NÚT 1: CHƠI LẠI (REMATCH) - PHONG CÁCH STARLIGHT
            if (rematchCountdown > 0) {
                // Đang đếm ngược vào trận mới (Amber Gold)
                GradientPaint gpCd = new GradientPaint(btnRematchX, btnY, new Color(255, 185, 30), btnRematchX, btnY + btnH, new Color(195, 110, 5));
                g2d.setPaint(gpCd);
                g2d.fillRoundRect(btnRematchX, btnY, btnRematchW, btnH, 10, 10);
                g2d.setColor(new Color(255, 225, 100));
                g2d.setStroke(new BasicStroke(1.6f));
                g2d.drawRoundRect(btnRematchX, btnY, btnRematchW, btnH, 10, 10);

                drawButtonBrackets(g2d, btnRematchX, btnY, btnRematchW, btnH, new Color(255, 240, 150));

                g2d.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
                g2d.setColor(Color.BLACK);
                String cdText = "🚀 TRẬN MỚI TRONG " + rematchCountdown + "s...";
                FontMetrics fmBtn = g2d.getFontMetrics();
                g2d.drawString(cdText, btnRematchX + (btnRematchW - fmBtn.stringWidth(cdText)) / 2, btnY + 26);
            } else if (isRematchReady) {
                // Bản thân đã sẵn sàng, chờ đối thủ (Lime Green)
                GradientPaint gpReady = new GradientPaint(btnRematchX, btnY, new Color(165, 215, 45), btnRematchX, btnY + btnH, new Color(110, 160, 20));
                g2d.setPaint(gpReady);
                g2d.fillRoundRect(btnRematchX, btnY, btnRematchW, btnH, 10, 10);
                g2d.setColor(new Color(205, 255, 90));
                g2d.setStroke(new BasicStroke(1.6f));
                g2d.drawRoundRect(btnRematchX, btnY, btnRematchW, btnH, 10, 10);

                drawButtonBrackets(g2d, btnRematchX, btnY, btnRematchW, btnH, new Color(225, 255, 130));

                g2d.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
                g2d.setColor(Color.WHITE);
                String readyText = "✓ ĐÃ SẴN SÀNG! ĐỢI ĐỐI THỦ...";
                FontMetrics fmBtn = g2d.getFontMetrics();
                g2d.drawString(readyText, btnRematchX + (btnRematchW - fmBtn.stringWidth(readyText)) / 2, btnY + 26);
            } else {
                // Nút bấm Sẵn sàng chơi lại (Starlight Coral Red-Orange)
                GradientPaint gpReplay = new GradientPaint(btnRematchX, btnY, new Color(255, 95, 45), btnRematchX, btnY + btnH, new Color(195, 42, 16));
                g2d.setPaint(gpReplay);
                g2d.fillRoundRect(btnRematchX, btnY, btnRematchW, btnH, 10, 10);
                g2d.setColor(new Color(255, 125, 75));
                g2d.setStroke(new BasicStroke(1.6f));
                g2d.drawRoundRect(btnRematchX, btnY, btnRematchW, btnH, 10, 10);

                // Vệt sáng gương
                g2d.setColor(new Color(255, 255, 255, 32));
                g2d.fillRoundRect(btnRematchX + 2, btnY + 2, btnRematchW - 4, btnH / 2, 8, 8);

                drawButtonBrackets(g2d, btnRematchX, btnY, btnRematchW, btnH, new Color(255, 165, 110));

                g2d.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
                g2d.setColor(Color.WHITE);
                String replayText = "⚔ CHƠI LẠI HOẶC [R]";
                FontMetrics fmBtn = g2d.getFontMetrics();
                g2d.drawString(replayText, btnRematchX + (btnRematchW - fmBtn.stringWidth(replayText)) / 2, btnY + 26);
            }

            // NÚT 2: VỀ SẢNH CHỜ (Dark Slate Glass)
            GradientPaint gpLobby = new GradientPaint(btnLobbyX, btnY, new Color(60, 75, 102), btnLobbyX, btnY + btnH, new Color(34, 42, 60));
            g2d.setPaint(gpLobby);
            g2d.fillRoundRect(btnLobbyX, btnY, btnLobbyW, btnH, 10, 10);
            g2d.setColor(new Color(110, 140, 185));
            g2d.setStroke(new BasicStroke(1.6f));
            g2d.drawRoundRect(btnLobbyX, btnY, btnLobbyW, btnH, 10, 10);

            drawButtonBrackets(g2d, btnLobbyX, btnY, btnLobbyW, btnH, new Color(135, 165, 215));

            g2d.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
            g2d.setColor(Color.WHITE);
            String lobbyText = "🚪 VỀ SẢNH";
            FontMetrics fmLobby = g2d.getFontMetrics();
            g2d.drawString(lobbyText, btnLobbyX + (btnLobbyW - fmLobby.stringWidth(lobbyText)) / 2, btnY + 26);
        }
    }

    // 5.2. BẢNG ĐIỂM GÓC PHẢI THỜI GIAN THỰC (IN-GAME REAL-TIME SCOREBOARD)
    private void drawInGameScoreboard(Graphics2D g2d, StatePacket state) {
        if (state == null || state.playerCount <= 0 || state.remainingSeconds <= 0) return;

        // 1. Thu thập & Sắp xếp người chơi theo số mạng tiêu diệt (Score giảm dần)
        class ScoreEntry {
            byte id;
            String name;
            byte score;
            byte hp;
            ScoreEntry(byte id, String name, byte score, byte hp) {
                this.id = id;
                this.name = name;
                this.score = score;
                this.hp = hp;
            }
        }

        List<ScoreEntry> list = new ArrayList<>();
        for (int i = 0; i < state.playerCount; i++) {
            byte pid = state.playerIds[i];
            byte sc = state.playerScores[i];
            byte php = state.playerHPs[i];
            String pName = playerNames.getOrDefault(pid, "Pilot #" + pid);
            list.add(new ScoreEntry(pid, pName, sc, php));
        }
        list.sort((a, b) -> Integer.compare(b.score, a.score));

        // Kiểm tra hòa dẫn đầu trong trận (Live Tie for First Place)
        int maxScore = list.isEmpty() ? 0 : list.get(0).score;
        int topTiedCount = 0;
        for (ScoreEntry se : list) {
            if (se.score == maxScore) topTiedCount++;
        }
        boolean isTopTied = (maxScore > 0 && topTiedCount >= 2);

        int boardW = 215;
        int headerH = 26;
        int rowH = 22;
        int boardH = headerH + (state.playerCount * rowH) + 6;
        int boardX = getWidth() - boardW - 14;
        int boardY = 12;

        // 2. Nền kính mờ bán trong suốt (Glassmorphism)
        g2d.setColor(new Color(11, 15, 26, 215));
        g2d.fillRoundRect(boardX, boardY, boardW, boardH, 10, 10);

        // 3. Viền ngoài công nghệ
        g2d.setColor(new Color(40, 65, 100));
        g2d.setStroke(new BasicStroke(1.2f));
        g2d.drawRoundRect(boardX, boardY, boardW, boardH, 10, 10);

        // 4. 4 Góc ngoặc Starlight L-Brackets phát quang
        Color bracketColor = isTopTied ? new Color(255, 185, 60) : (maxScore > 0 ? new Color(255, 215, 0) : new Color(100, 160, 230));
        drawButtonBrackets(g2d, boardX, boardY, boardW, boardH, bracketColor);

        // 5. Header bảng điểm
        g2d.setColor(new Color(18, 25, 45, 230));
        g2d.fillRoundRect(boardX + 2, boardY + 2, boardW - 4, headerH - 2, 8, 8);

        // Tiêu đề: SCOREBOARD hoặc TIED BATTLE
        g2d.setFont(new Font(Font.DIALOG, Font.BOLD, 11));
        if (isTopTied) {
            g2d.setColor(new Color(255, 185, 60)); // Amber hòa dẫn đầu
            g2d.drawString("🤝 TIED (" + maxScore + " KILLS)", boardX + 10, boardY + 17);
        } else if (maxScore > 0) {
            g2d.setColor(new Color(255, 215, 0)); // Vàng Gold
            g2d.drawString("★ SCOREBOARD", boardX + 10, boardY + 17);
        } else {
            g2d.setColor(new Color(175, 210, 245)); // Xanh nhạt
            g2d.drawString("★ SCOREBOARD", boardX + 10, boardY + 17);
        }

        // Cột: KILLS
        g2d.setFont(new Font(Font.DIALOG, Font.BOLD, 10));
        g2d.setColor(new Color(140, 175, 220));
        g2d.drawString("KILLS", boardX + boardW - 44, boardY + 17);

        // 6. Vẽ từng dòng người chơi
        g2d.setFont(new Font(Font.DIALOG, Font.BOLD, 11));
        FontMetrics fm = g2d.getFontMetrics();

        // Bảng màu chấm tròn đại diện theo phi thuyền
        Color[] shipColors = new Color[]{
            new Color(80, 225, 120),  // Green
            new Color(255, 85, 85),   // Red
            new Color(75, 175, 255),  // Blue
            new Color(255, 170, 50)   // Orange
        };

        for (int r = 0; r < list.size(); r++) {
            ScoreEntry entry = list.get(r);
            int rowY = boardY + headerH + (r * rowH) + 4;
            boolean isMe = (entry.id == myPlayerId);

            // Tính thứ hạng thực tế dựa trên competition rank (1, 1, 3, ...)
            int actualRank = 1;
            for (int j = 0; j < r; j++) {
                if (list.get(j).score > entry.score) actualRank++;
            }

            boolean isTopLeaderTied = (isTopTied && entry.score == maxScore);
            boolean isSoloLeader = (!isTopTied && r == 0 && entry.score > 0);

            // Nền highlight nếu là bản thân
            if (isMe) {
                g2d.setColor(new Color(0, 180, 230, 45));
                g2d.fillRoundRect(boardX + 4, rowY, boardW - 8, rowH - 2, 6, 6);
            }

            // Thứ hạng: #1, #2, #3, #4
            String rankText = "#" + actualRank;
            Color rankColor;
            if (isTopLeaderTied) {
                rankColor = new Color(255, 185, 60); // Amber tie
            } else if (isSoloLeader) {
                rankColor = new Color(255, 215, 0); // Gold solo leader
            } else {
                rankColor = new Color(160, 180, 205);
            }
            g2d.setColor(rankColor);
            g2d.drawString(rankText, boardX + 8, rowY + 14);

            // Chấm màu phi thuyền
            Color shipCol = shipColors[entry.id % 4];
            g2d.setColor(shipCol);
            g2d.fillOval(boardX + 28, rowY + 5, 8, 8);

            // Tên người chơi
            String displayName = entry.name + (isMe ? " (YOU)" : "");
            if (entry.hp <= 0) {
                displayName += " [X]";
            }
            if (displayName.length() > 14) {
                displayName = displayName.substring(0, 12) + "..";
            }

            if (isMe) {
                g2d.setColor(new Color(80, 240, 255));
            } else if (entry.hp <= 0) {
                g2d.setColor(new Color(130, 140, 155));
            } else if (isTopLeaderTied) {
                g2d.setColor(new Color(255, 220, 140)); // Amber gold highlight for tied leaders
            } else if (isSoloLeader) {
                g2d.setColor(new Color(255, 235, 140)); // Bright gold for solo leader
            } else {
                g2d.setColor(new Color(230, 235, 245));
            }
            g2d.drawString(displayName, boardX + 42, rowY + 14);

            // Số Kills bên phải
            String killStr = String.valueOf(entry.score);
            int killX = boardX + boardW - 24 - fm.stringWidth(killStr);
            if (isTopLeaderTied) {
                g2d.setColor(new Color(255, 185, 60));
            } else if (entry.score > 0) {
                g2d.setColor(new Color(255, 215, 0));
            } else {
                g2d.setColor(new Color(150, 165, 185));
            }
            g2d.drawString(killStr, killX, rowY + 14);
        }
    }

    private void drawButtonBrackets(Graphics2D g2, int x, int y, int w, int h, Color col) {
        g2.setColor(col);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int bl = 8;
        // Top-left ┌
        g2.drawLine(x + 2, y + 2 + bl, x + 2, y + 2);
        g2.drawLine(x + 2, y + 2, x + 2 + bl, y + 2);
        // Top-right ┐
        g2.drawLine(x + w - 3, y + 2 + bl, x + w - 3, y + 2);
        g2.drawLine(x + w - 3, y + 2, x + w - 3 - bl, y + 2);
        // Bottom-left └
        g2.drawLine(x + 2, y + h - 3 - bl, x + 2, y + h - 3);
        g2.drawLine(x + 2, y + h - 3, x + 2 + bl, y + h - 3);
        // Bottom-right ┘
        g2.drawLine(x + w - 3, y + h - 3 - bl, x + w - 3, y + h - 3);
        g2.drawLine(x + w - 3, y + h - 3, x + w - 3 - bl, y + h - 3);
    }
}
