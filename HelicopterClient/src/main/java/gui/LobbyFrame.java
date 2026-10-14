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

    private JPanel lobbyPanel;
    private GamePanel gamePanel;

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

        lobbyPanel = new JPanel();
        lobbyPanel.setLayout(null);

        JLabel lblIP = new JLabel("Server IP:");
        lblIP.setBounds(20, 20, 80, 25);
        lobbyPanel.add(lblIP);

        txtServerIP = new JTextField("localhost");
        txtServerIP.setBounds(100, 20, 160, 25);
        lobbyPanel.add(txtServerIP);

        JLabel lblName = new JLabel("Your Name:");
        lblName.setBounds(20, 55, 80, 25);
        lobbyPanel.add(lblName);

        txtPlayerName = new JTextField("");
        txtPlayerName.setBounds(100, 55, 160, 25);
        lobbyPanel.add(txtPlayerName);

        btnConnect = new JButton("Connect");
        btnConnect.setBounds(270, 20, 100, 60);
        btnConnect.addActionListener(e -> onConnectClicked());
        lobbyPanel.add(btnConnect);

        JLabel lblPlayers = new JLabel("Players in room:");
        lblPlayers.setBounds(20, 95, 200, 25);
        lobbyPanel.add(lblPlayers);

        playerListModel = new DefaultListModel<>();
        playerList = new JList<>(playerListModel);
        JScrollPane scrollPane = new JScrollPane(playerList);
        scrollPane.setBounds(20, 120, 350, 120);
        lobbyPanel.add(scrollPane);

        btnReady = new JButton("Ready");
        btnReady.setBounds(20, 255, 350, 35);
        btnReady.setEnabled(false);
        btnReady.addActionListener(e -> onReadyClicked());
        lobbyPanel.add(btnReady);

        lblStatus = new JLabel("Not connected.");
        lblStatus.setBounds(20, 295, 350, 20);
        lobbyPanel.add(lblStatus);

        setContentPane(lobbyPanel);
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
            if (gamePanel != null) {
                gamePanel.stop();
                gamePanel = null;
            }
            returnToLobby();
            JOptionPane.showMessageDialog(this, reason, "Thông báo ngắt kết nối", JOptionPane.WARNING_MESSAGE);
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
        });
    }

    @Override
    public void onGameStart() {
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText("Game Started!");
            this.setTitle("Helicopter Combat - Arena");

            // Phóng to cửa sổ ra 800x600 để chơi game
            this.setSize(800, 600);
            this.setLocationRelativeTo(null);

            // Khởi tạo UDPClient và kết nối
            network.UDPClient udpClient = new network.UDPClient();
            udpClient.connect(txtServerIP.getText().trim());

            // Khởi tạo và gắn GamePanel vào
            gamePanel = new GamePanel(udpClient, tcpClient.getMyPlayerId());
            this.setContentPane(gamePanel);
            this.revalidate();
            this.repaint();
            gamePanel.requestFocus();
        });
    }

    @Override
    public void onGameOver(String leaderboardData) {
        SwingUtilities.invokeLater(() -> {
            if (gamePanel != null) {
                gamePanel.stop();
                gamePanel = null;
            }
            showGameOverScreen(leaderboardData);
        });
    }

    private void showGameOverScreen(String data) {
        this.setTitle("Helicopter Combat - Kết Quả Trận Đấu");
        this.setSize(600, 500);
        this.setLocationRelativeTo(null);

        JPanel overPanel = new JPanel();
        overPanel.setBackground(new Color(25, 30, 45));
        overPanel.setLayout(new BorderLayout(15, 15));
        overPanel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        // Phân tích cú pháp dữ liệu: winnerId,winnerName;id1,name1,score1;id2,name2,score2...
        String[] sections = data.split(";");
        String[] winnerInfo = sections[0].split(",");
        byte winnerId = Byte.parseByte(winnerInfo[0]);
        String winnerName = winnerInfo.length > 1 ? winnerInfo[1] : "";

        // 1. Header: Banner Victory / Defeat / Draw
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel();
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 30));

        JLabel lblSub = new JLabel();
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblSub.setFont(new Font("Arial", Font.PLAIN, 15));
        lblSub.setForeground(Color.LIGHT_GRAY);

        if (winnerId == tcpClient.getMyPlayerId()) {
            lblTitle.setText("🏆 CHIẾN THẮNG (VICTORY)!");
            lblTitle.setForeground(new Color(255, 215, 0)); // Vàng Gold
            lblSub.setText("Chúc mừng bạn đã giành điểm số cao nhất ván đấu!");
        } else if (winnerId == -2) {
            lblTitle.setText("⚖️ BẤT PHÂN THẮNG BẠI (DRAW)!");
            lblTitle.setForeground(new Color(0, 229, 255)); // Cyan
            lblSub.setText("Trận đấu kết thúc với kết quả hòa!");
        } else {
            lblTitle.setText("💀 THẤT BẠI (DEFEAT)!");
            lblTitle.setForeground(new Color(255, 75, 75)); // Đỏ
            lblSub.setText("Người chiến thắng ván đấu: " + winnerName);
        }

        headerPanel.add(lblTitle);
        headerPanel.add(Box.createVerticalStrut(8));
        headerPanel.add(lblSub);
        overPanel.add(headerPanel, BorderLayout.NORTH);

        // 2. Bảng xếp hạng thành tích (Leaderboard Table)
        String[] columns = {"Hạng", "Mã", "Tên Người Chơi", "Điểm Tiêu Diệt"};
        String[][] tableData = new String[sections.length - 1][4];

        for (int i = 1; i < sections.length; i++) {
            String[] p = sections[i].split(",");
            byte pid = Byte.parseByte(p[0]);
            String pname = p[1];
            String score = p[2];

            String rankIcon = switch (i) {
                case 1 -> "🥇 Hạng 1";
                case 2 -> "🥈 Hạng 2";
                case 3 -> "🥉 Hạng 3";
                default -> "    Hạng " + i;
            };

            tableData[i - 1][0] = rankIcon;
            tableData[i - 1][1] = "P#" + pid + (pid == tcpClient.getMyPlayerId() ? " (BẠN)" : "");
            tableData[i - 1][2] = pname;
            tableData[i - 1][3] = score + " pts";
        }

        JTable table = new JTable(tableData, columns);
        table.setRowHeight(32);
        table.setFont(new Font("Arial", Font.BOLD, 13));
        table.setForeground(Color.WHITE);
        table.setBackground(new Color(35, 42, 60));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(45, 55, 78));
        table.getTableHeader().setForeground(Color.CYAN);
        table.setEnabled(false);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.getViewport().setBackground(new Color(35, 42, 60));
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(60, 75, 105), 1));
        overPanel.add(tableScroll, BorderLayout.CENTER);

        // 3. Nút bấm quay về sảnh
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        JButton btnBackLobby = new JButton("Quay về Phòng Chờ (Lobby)");
        btnBackLobby.setFont(new Font("Arial", Font.BOLD, 14));
        btnBackLobby.setBackground(new Color(40, 140, 220));
        btnBackLobby.setForeground(Color.WHITE);
        btnBackLobby.setFocusPainted(false);
        btnBackLobby.setPreferredSize(new Dimension(240, 40));
        btnBackLobby.addActionListener(e -> returnToLobby());
        bottomPanel.add(btnBackLobby);
        overPanel.add(bottomPanel, BorderLayout.SOUTH);

        this.setContentPane(overPanel);
        this.revalidate();
        this.repaint();
    }

    private void returnToLobby() {
        if (gamePanel != null) {
            gamePanel.stop();
            gamePanel = null;
        }
        setTitle("Helicopter Combat - Lobby");
        setSize(400, 350);
        setLocationRelativeTo(null);
        setContentPane(lobbyPanel);
        btnReady.setEnabled(true);
        lblStatus.setText("Ván đấu đã kết thúc. Sẵn sàng cho trận mới!");
        revalidate();
        repaint();
    }
}
