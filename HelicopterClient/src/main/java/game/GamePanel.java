package game;

import network.InputPacket;
import network.StatePacket;
import network.UDPClient;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ConcurrentHashMap;

public class GamePanel extends JPanel {
    private UDPClient udpClient;
    private byte myPlayerId;
    
    // Lưu phím đang bấm
    private boolean keyW, keyA, keyS, keyD;
    private float mouseX, mouseY;
    private boolean isRunning = true;

    // Delta Time
    private long lastTime;

    // Lớp nội bộ để lưu tọa độ hiển thị (Nội suy) tách biệt với tọa độ Server
    private class ClientPlayer {
        float x, y;
    }
    private ConcurrentHashMap<Byte, ClientPlayer> renderPlayers = new ConcurrentHashMap<>();

    public GamePanel(UDPClient udpClient, byte myPlayerId) {
        this.udpClient = udpClient;
        this.myPlayerId = myPlayerId;
        
        this.setBackground(Color.BLACK);
        this.setFocusable(true);

        setupInputListeners();

        // 1. Thread gửi Input (Đóng gói phím bấm gửi Server với tốc độ ổn định 60Hz)
        new Thread(() -> {
            while (isRunning) {
                sendInput();
                try { Thread.sleep(16); } catch (Exception e) {}
            }
        }).start();

        // 2. Thread Render (Mở khóa FPS - Phụ thuộc vào phần cứng của người chơi)
        new Thread(() -> {
            lastTime = System.nanoTime();
            while (isRunning) {
                repaint(); // Ép vẽ lại liên tục
                try { Thread.sleep(2); } catch (Exception e) {} // Nghỉ 2ms để chống cháy CPU
            }
        }).start();
    }

    private void setupInputListeners() {
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch(e.getKeyCode()) {
                    case KeyEvent.VK_W -> keyW = true;
                    case KeyEvent.VK_A -> keyA = true;
                    case KeyEvent.VK_S -> keyS = true;
                    case KeyEvent.VK_D -> keyD = true;
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
                switch(e.getKeyCode()) {
                    case KeyEvent.VK_W -> keyW = false;
                    case KeyEvent.VK_A -> keyA = false;
                    case KeyEvent.VK_S -> keyS = false;
                    case KeyEvent.VK_D -> keyD = false;
                }
            }
        });

        this.addMouseMotionListener(new MouseAdapter() {
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
        });
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

        input.mouseAngle = 0; 
        input.isShooting = false;
        
        udpClient.sendInput(input);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Tính toán Delta Time (Khoảng thời gian trôi qua giữa 2 lần vẽ, đơn vị: Giây)
        long now = System.nanoTime();
        float deltaTime = (now - lastTime) / 1_000_000_000.0f;
        lastTime = now;

        StatePacket state = udpClient.latestState;
        if (state == null) {
            g2d.setColor(Color.WHITE);
            g2d.drawString("Waiting for server state...", 50, 50);
            return;
        }

        // Tốc độ bám theo tọa độ gốc (Càng cao thì trượt càng nhanh)
        float lerpSpeed = 15.0f; 

        // Cập nhật và nội suy tọa độ
        for (int i = 0; i < state.playerCount; i++) {
            byte pid = state.playerIds[i];
            float targetX = state.playerXs[i];
            float targetY = state.playerYs[i];
            
            // Lấy tọa độ hiển thị hiện tại. Nếu mới xuất hiện thì đặt thẳng tại Target
            ClientPlayer rp = renderPlayers.computeIfAbsent(pid, k -> {
                ClientPlayer newPlayer = new ClientPlayer();
                newPlayer.x = targetX;
                newPlayer.y = targetY;
                return newPlayer;
            });

            // THUẬT TOÁN LERP BÙ TRỪ DELTA TIME:
            // Tọa_độ_vẽ = Tọa_độ_vẽ + Khoảng_cách * Tốc_độ_trượt * Thời_gian_trôi_qua
            rp.x += (targetX - rp.x) * lerpSpeed * deltaTime;
            rp.y += (targetY - rp.y) * lerpSpeed * deltaTime;

            // Vẽ khối vuông
            if (pid == myPlayerId) {
                g2d.setColor(Color.GREEN);
            } else {
                g2d.setColor(Color.RED);
            }
            g2d.fillRect((int)rp.x, (int)rp.y, 30, 30);
            
            g2d.setColor(Color.WHITE);
            g2d.drawString("P" + pid, rp.x, rp.y - 5);
        }
    }
}
