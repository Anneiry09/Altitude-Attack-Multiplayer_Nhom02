package gui;

import game.GamePanel;
import network.TCPClient;
import javax.swing.*;
import java.awt.*;

public class LobbyFrame extends JFrame implements TCPClient.TCPListener {
    private JTextField txtServerIP;
    private JTextField txtPlayerName;
    private JButton btnConnect;
    private JButton btnReady;
    private DefaultListModel<String> playerListModel;
    private JList<String> playerList;
    private JLabel lblStatus;

    private TCPClient tcpClient;

    public LobbyFrame() {
        tcpClient = new TCPClient();
        tcpClient.setListener(this);
        initUI();
    }

    private void initUI() {
        setTitle("Helicopter Combat - Lobby");
        setSize(400, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel();
        panel.setLayout(null);

        JLabel lblIP = new JLabel("Server IP:");
        lblIP.setBounds(20, 20, 80, 25);
        panel.add(lblIP);

        txtServerIP = new JTextField("localhost");
        txtServerIP.setBounds(100, 20, 160, 25);
        panel.add(txtServerIP);

        JLabel lblName = new JLabel("Your Name:");
        lblName.setBounds(20, 55, 80, 25);
        panel.add(lblName);

        txtPlayerName = new JTextField("");
        txtPlayerName.setBounds(100, 55, 160, 25);
        panel.add(txtPlayerName);

        btnConnect = new JButton("Connect");
        btnConnect.setBounds(270, 20, 100, 60);
        btnConnect.addActionListener(e -> onConnectClicked());
        panel.add(btnConnect);

        JLabel lblPlayers = new JLabel("Players in room:");
        lblPlayers.setBounds(20, 95, 200, 25);
        panel.add(lblPlayers);

        playerListModel = new DefaultListModel<>();
        playerList = new JList<>(playerListModel);
        JScrollPane scrollPane = new JScrollPane(playerList);
        scrollPane.setBounds(20, 120, 350, 120);
        panel.add(scrollPane);

        btnReady = new JButton("Ready");
        btnReady.setBounds(20, 255, 350, 35);
        btnReady.setEnabled(false);
        btnReady.addActionListener(e -> onReadyClicked());
        panel.add(btnReady);

        lblStatus = new JLabel("Not connected.");
        lblStatus.setBounds(20, 295, 350, 20);
        panel.add(lblStatus);

        add(panel);
    }

    private void onConnectClicked() {
        String ip = txtServerIP.getText().trim();
        String name = txtPlayerName.getText().trim();

        if (name.isEmpty()) {
            lblStatus.setText("Please enter your name!");
            return;
        }

        lblStatus.setText("Connecting...");
        btnConnect.setEnabled(false);

        new Thread(() -> {
            boolean ok = tcpClient.connect(ip, name);
            SwingUtilities.invokeLater(() -> {
                if (ok) {
                    lblStatus.setText("Connected! Your ID: " + tcpClient.getMyPlayerId());
                    txtServerIP.setEnabled(false);
                    txtPlayerName.setEnabled(false);
                    btnReady.setEnabled(true);
                } else {
                    lblStatus.setText("Cannot connect to " + ip);
                    btnConnect.setEnabled(true);
                }
            });
        }).start();
    }

    private void onReadyClicked() {
        tcpClient.sendReady();
        btnReady.setEnabled(false);
        lblStatus.setText("Ready! Waiting for other players...");
    }

    @Override
    public void onPlayerListUpdated(String data) {
        SwingUtilities.invokeLater(() -> {
            playerListModel.clear();
            String body = data.replace("PLAYER_LIST:", "");
            String[] entries = body.split(";");
            for (String entry : entries) {
                if (!entry.isEmpty()) {
                    String[] parts = entry.split(",");
                    if (parts.length == 2) {
                        playerListModel.addElement("Player #" + parts[0] + " - " + parts[1]);
                    }
                }
            }
        });
    }

    @Override
    public void onDisconnected(String reason) {
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText(reason);
            btnConnect.setEnabled(true);
            txtServerIP.setEnabled(true);
            txtPlayerName.setEnabled(true);
            btnReady.setEnabled(false);
            playerListModel.clear();
        });
    }

    @Override
    public void onCountdown(int seconds) {
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText("Game starts in " + seconds + " seconds...");
        });
    }

    @Override
    public void onCountdownCancelled() {
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText("Countdown cancelled. Waiting for players...");
            // Cho phép bấm lại nút Ready nếu muốn, nhưng ở đây ai đã Ready rồi thì giữ nguyên
        });
    }

    @Override
    public void onGameStart() {
        SwingUtilities.invokeLater(() -> {
            // Đóng băng trạng thái Lobby
            lblStatus.setText("Game Started!");
            
            // Xóa sạch các component giao diện kéo thả (Lobby)
            this.getContentPane().removeAll();
            
            // Phóng to cửa sổ ra 800x600 để chơi game
            this.setSize(800, 600);
            // Khởi tạo UDPClient và kết nối
            network.UDPClient udpClient = new network.UDPClient();
            udpClient.connect(txtServerIP.getText().trim());

            // Khởi tạo và nhét GamePanel vào
            GamePanel gamePanel = new GamePanel(udpClient, tcpClient.getMyPlayerId());
            this.getContentPane().add(gamePanel);
            
            // Refresh lại cửa sổ để vẽ lại GamePanel
            this.revalidate();
            this.repaint();
            
            // Chuyển focus vào GamePanel để nó bắt được sự kiện bàn phím
            gamePanel.requestFocus();
        });
    }
}
