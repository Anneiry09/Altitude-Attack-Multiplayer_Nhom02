package main;

import game.GamePanel;
import javax.swing.JFrame;

public class ClientMain extends JFrame {
    public ClientMain() {
        this.setTitle("Trực Thăng Đối Kháng");
        this.setSize(800, 600); // Kích thước cửa sổ
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null); // Hiển thị ra giữa màn hình
        this.setResizable(false); // Không cho phóng to/thu nhỏ để tránh lệch toạ độ

        // Khởi tạo Panel Game và nhét nó vào giữa Cửa sổ
        GamePanel gamePanel = new GamePanel();
        this.add(gamePanel);
    }

    public static void main(String[] args) {
        // Khởi chạy Giao diện (Mở cửa sổ)
        java.awt.EventQueue.invokeLater(() -> {
            new ClientMain().setVisible(true);
        });
    }
}