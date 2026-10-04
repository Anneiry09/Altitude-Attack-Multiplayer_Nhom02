/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package game;

/**
 *
 * @author an013
 */
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class GamePanel extends JPanel {
    private Timer renderTimer;

    public GamePanel() {
        // Cài đặt màu nền đen cho dễ nhìn
        this.setBackground(Color.BLACK);
        
        // Cho phép Panel nhận sự kiện bàn phím
        this.setFocusable(true);

        // Tạo vòng lặp vẽ lại màn hình 60 lần/giây (1000ms / 60 ≈ 16ms)
        renderTimer = new Timer(16, e -> {
            repaint(); // Hàm này sẽ kích hoạt lại hàm paintComponent ở dưới
        });
        renderTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Xóa hình cũ đi
        Graphics2D g2d = (Graphics2D) g;

        // TẠM THỜI: In thử một dòng chữ để test
        g2d.setColor(Color.GREEN);
        g2d.drawString("Màn hình Game (Vẽ tay) đã sẵn sàng - Đang chạy 60 FPS!", 50, 50);
        
        // Về sau, lệnh vẽ máy bay: g2d.drawImage(...) sẽ được gọi ở đây
    }
}
