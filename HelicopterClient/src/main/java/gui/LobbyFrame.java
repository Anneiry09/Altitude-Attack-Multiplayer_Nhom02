package gui;

import game.GamePanel;
import model.OnlinePlayer;
import model.RoomInfo;
import model.RoomMember;
import network.TCPClient;
import network.UDPClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LobbyFrame extends JFrame implements TCPClient.TCPListener {
    // TCP & Mạng
    private TCPClient tcpClient;
    private String currentServerIP = "localhost";
    private String myPlayerName = "";
    private byte myPlayerId = -1;
    private Map<Byte, String> playerNamesMap = new HashMap<>();

    // Container điều hướng chính (CardLayout)
    private CardLayout rootCardLayout;
    private JPanel rootPanel;

    // View 1: Đăng nhập / Kết nối
    private JTextField txtServerIP;
    private JTextField txtPlayerName;
    private JButton btnConnect;
    private JLabel lblConnectStatus;

    // View 2: Dashboard (Sảnh chính & Phòng)
    private CardLayout contentCardLayout;
    private JPanel contentPanel;
    private JLabel lblPlayerProfile;
    private JButton btnQuickMatch;
    private boolean isMatchmaking = false;

    // Sảnh chính (Lobby View)
    private DefaultTableModel onlinePlayersModel;
    private JTable onlinePlayersTable;
    private DefaultTableModel roomsModel;
    private JTable roomsTable;

    // Chi tiết phòng (Room View)
    private JLabel lblRoomTitle;
    private JLabel lblRoomHost;
    private JPanel slotsPanel;
    private JLabel lblCountdownBanner;
    private JButton btnReadyToggle;
    private JButton btnLeaveRoom;
    private boolean isMyReady = false;
    private int currentRoomId = -1;

    // Danh sách người chơi online để mời nhanh
    private DefaultListModel<String> inviteListModel;
    private JList<String> inviteList;
    private Map<Integer, Byte> inviteIndexToPid = new HashMap<>();

    // View 3: Trong game
    private JPanel gameContainer;
    private GamePanel gamePanel;

    public LobbyFrame() {
        tcpClient = new TCPClient();
        tcpClient.setListener(this);
        initUI();
    }

    private void initUI() {
        setTitle("Helicopter Combat - Gaming Arena");
        setSize(850, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        rootCardLayout = new CardLayout();
        rootPanel = new JPanel(rootCardLayout);

        // Khởi tạo các View chính
        JPanel loginView = createLoginView();
        JPanel dashboardView = createDashboardView();
        gameContainer = new JPanel(new BorderLayout());

        rootPanel.add(loginView, "LOGIN");
        rootPanel.add(dashboardView, "DASHBOARD");
        rootPanel.add(gameContainer, "GAME");

        add(rootPanel);
        rootCardLayout.show(rootPanel, "LOGIN");
    }

    // ==========================================
    // 1. MÀN HÌNH ĐĂNG NHẬP / KẾT NỐI
    // ==========================================
    private JPanel createLoginView() {
        JPanel panel = new JPanel(null);
        panel.setBackground(new Color(11, 15, 26));

        // Tiêu đề game
        JLabel lblTitle = new JLabel("🚁 HELICOPTER COMBAT", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 28));
        lblTitle.setForeground(new Color(0, 210, 255));
        lblTitle.setBounds(150, 60, 550, 40);
        panel.add(lblTitle);

        JLabel lblSub = new JLabel("ĐẤU TRƯỜNG BẮN MÁY BAY KHÔNG GIAN ONLINE", SwingConstants.CENTER);
        lblSub.setFont(new Font("Arial", Font.BOLD, 13));
        lblSub.setForeground(new Color(160, 180, 210));
        lblSub.setBounds(150, 105, 550, 25);
        panel.add(lblSub);

        // Khung nhập liệu giữa màn hình
        JPanel formBox = new JPanel(null);
        formBox.setBackground(new Color(20, 26, 44));
        formBox.setBorder(new LineBorder(new Color(45, 60, 95), 1, true));
        formBox.setBounds(240, 160, 370, 290);

        JLabel lblIP = new JLabel("Địa chỉ Server IP:");
        lblIP.setForeground(new Color(200, 215, 240));
        lblIP.setFont(new Font("Arial", Font.PLAIN, 13));
        lblIP.setBounds(30, 25, 200, 20);
        formBox.add(lblIP);

        txtServerIP = new JTextField("localhost");
        txtServerIP.setFont(new Font("Arial", Font.PLAIN, 14));
        txtServerIP.setBackground(new Color(12, 17, 30));
        txtServerIP.setForeground(Color.WHITE);
        txtServerIP.setCaretColor(Color.WHITE);
        txtServerIP.setBorder(new LineBorder(new Color(60, 80, 120), 1));
        txtServerIP.setBounds(30, 50, 310, 35);
        formBox.add(txtServerIP);

        JLabel lblName = new JLabel("Tên phi công (Player Name):");
        lblName.setForeground(new Color(200, 215, 240));
        lblName.setFont(new Font("Arial", Font.PLAIN, 13));
        lblName.setBounds(30, 95, 200, 20);
        formBox.add(lblName);

        txtPlayerName = new JTextField("");
        txtPlayerName.setFont(new Font("Arial", Font.PLAIN, 14));
        txtPlayerName.setBackground(new Color(12, 17, 30));
        txtPlayerName.setForeground(Color.WHITE);
        txtPlayerName.setCaretColor(Color.WHITE);
        txtPlayerName.setBorder(new LineBorder(new Color(60, 80, 120), 1));
        txtPlayerName.setBounds(30, 120, 310, 35);
        formBox.add(txtPlayerName);

        btnConnect = new JButton("🚀 KẾT NỐI VÀO SẢNH");
        btnConnect.setFont(new Font("Arial", Font.BOLD, 15));
        btnConnect.setBackground(new Color(0, 150, 255));
        btnConnect.setForeground(Color.WHITE);
        btnConnect.setFocusPainted(false);
        btnConnect.setBounds(30, 180, 310, 42);
        btnConnect.addActionListener(e -> onConnectClicked());
        formBox.add(btnConnect);

        lblConnectStatus = new JLabel("Vui lòng nhập tên để tiếp tục.", SwingConstants.CENTER);
        lblConnectStatus.setFont(new Font("Arial", Font.PLAIN, 12));
        lblConnectStatus.setForeground(Color.GRAY);
        lblConnectStatus.setBounds(20, 235, 330, 25);
        formBox.add(lblConnectStatus);

        panel.add(formBox);
        return panel;
    }

    private void onConnectClicked() {
        String ip = txtServerIP.getText().trim();
        String name = txtPlayerName.getText().trim();

        if (name.isEmpty()) {
            lblConnectStatus.setText("⚠️ Vui lòng nhập tên phi công!");
            lblConnectStatus.setForeground(new Color(255, 100, 100));
            return;
        }

        lblConnectStatus.setText("Đang kết nối tới server...");
        lblConnectStatus.setForeground(new Color(255, 200, 80));
        btnConnect.setEnabled(false);

        new Thread(() -> {
            boolean ok = tcpClient.connect(ip, name);
            SwingUtilities.invokeLater(() -> {
                if (ok) {
                    currentServerIP = ip;
                    myPlayerName = name;
                    lblPlayerProfile.setText("🟢 Phi công: " + name + " (ID: #" + tcpClient.getMyPlayerId() + ")");
                    rootCardLayout.show(rootPanel, "DASHBOARD");
                    showLobbyView();
                } else {
                    lblConnectStatus.setText("❌ Không thể kết nối tới " + ip);
                    lblConnectStatus.setForeground(new Color(255, 80, 80));
                    btnConnect.setEnabled(true);
                }
            });
        }).start();
    }

    // ==========================================
    // 2. DASHBOARD CHÍNH (HEADER + SẢNH / PHÒNG)
    // ==========================================
    private JPanel createDashboardView() {
        JPanel dashboard = new JPanel(new BorderLayout());
        dashboard.setBackground(new Color(11, 15, 26));

        // Top Navigation Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(18, 24, 42));
        header.setBorder(new EmptyBorder(12, 18, 12, 18));

        lblPlayerProfile = new JLabel("🟢 Phi công: ");
        lblPlayerProfile.setFont(new Font("Arial", Font.BOLD, 15));
        lblPlayerProfile.setForeground(new Color(100, 255, 160));
        header.add(lblPlayerProfile, BorderLayout.WEST);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        headerRight.setOpaque(false);

        btnQuickMatch = new JButton("🎲 GHÉP TRẬN NGẪU NHIÊN");
        btnQuickMatch.setFont(new Font("Arial", Font.BOLD, 13));
        btnQuickMatch.setBackground(new Color(255, 140, 0));
        btnQuickMatch.setForeground(Color.WHITE);
        btnQuickMatch.setFocusPainted(false);
        btnQuickMatch.addActionListener(e -> onQuickMatchClicked());
        headerRight.add(btnQuickMatch);

        JButton btnDisconnect = new JButton("🚪 Đăng xuất");
        btnDisconnect.setFont(new Font("Arial", Font.PLAIN, 12));
        btnDisconnect.setBackground(new Color(60, 70, 95));
        btnDisconnect.setForeground(Color.LIGHT_GRAY);
        btnDisconnect.setFocusPainted(false);
        btnDisconnect.addActionListener(e -> onDisconnectClicked());
        headerRight.add(btnDisconnect);

        header.add(headerRight, BorderLayout.EAST);
        dashboard.add(header, BorderLayout.NORTH);

        // Content Area (Switch giữa Sảnh chính và Phòng)
        contentCardLayout = new CardLayout();
        contentPanel = new JPanel(contentCardLayout);
        contentPanel.setBackground(new Color(11, 15, 26));

        JPanel lobbyView = createLobbyContentView();
        JPanel roomView = createRoomContentView();

        contentPanel.add(lobbyView, "LOBBY_VIEW");
        contentPanel.add(roomView, "ROOM_VIEW");

        dashboard.add(contentPanel, BorderLayout.CENTER);
        return dashboard;
    }

    private void onQuickMatchClicked() {
        if (!isMatchmaking) {
            tcpClient.sendMatchmakingJoin();
            setMatchmakingState(true);
        } else {
            tcpClient.sendMatchmakingCancel();
            setMatchmakingState(false);
        }
    }

    private void setMatchmakingState(boolean searching) {
        this.isMatchmaking = searching;
        if (searching) {
            btnQuickMatch.setText("⏳ ĐANG TÌM TRẬN... [HỦY]");
            btnQuickMatch.setBackground(new Color(220, 50, 50));
        } else {
            btnQuickMatch.setText("🎲 GHÉP TRẬN NGẪU NHIÊN");
            btnQuickMatch.setBackground(new Color(255, 140, 0));
        }
    }

    private void onDisconnectClicked() {
        tcpClient.disconnect();
        rootCardLayout.show(rootPanel, "LOGIN");
        btnConnect.setEnabled(true);
        lblConnectStatus.setText("Đã ngắt kết nối.");
        lblConnectStatus.setForeground(Color.GRAY);
    }

    private void showLobbyView() {
        contentCardLayout.show(contentPanel, "LOBBY_VIEW");
        btnQuickMatch.setEnabled(true);
    }

    private void showRoomView() {
        contentCardLayout.show(contentPanel, "ROOM_VIEW");
        btnQuickMatch.setEnabled(false); // Đang trong phòng thì tắt nút tìm trận ngẫu nhiên
    }

    // ==========================================
    // 3. SẢNH CHÍNH (ONLINE PLAYERS & ROOM LIST)
    // ==========================================
    private JPanel createLobbyContentView() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 16, 0));
        panel.setBackground(new Color(11, 15, 26));
        panel.setBorder(new EmptyBorder(16, 18, 16, 18));

        // Cột Trái: Danh sách người chơi Online
        JPanel leftPanel = new JPanel(new BorderLayout(0, 10));
        leftPanel.setBackground(new Color(18, 24, 40));
        leftPanel.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel lblOnlineTitle = new JLabel("👥 NGƯỜI CHƠI ONLINE TRÊN SERVER");
        lblOnlineTitle.setFont(new Font("Arial", Font.BOLD, 14));
        lblOnlineTitle.setForeground(new Color(130, 200, 255));
        leftPanel.add(lblOnlineTitle, BorderLayout.NORTH);

        onlinePlayersModel = new DefaultTableModel(new String[]{"ID", "Tên Người Chơi", "Trạng Thái"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        onlinePlayersTable = createStyledTable(onlinePlayersModel);
        leftPanel.add(new JScrollPane(onlinePlayersTable), BorderLayout.CENTER);

        JButton btnInviteSelected = new JButton("💌 MỜI NGƯỜI CHƠI ĐÃ CHỌN VÀO PHÒNG");
        btnInviteSelected.setFont(new Font("Arial", Font.BOLD, 12));
        btnInviteSelected.setBackground(new Color(40, 120, 220));
        btnInviteSelected.setForeground(Color.WHITE);
        btnInviteSelected.setFocusPainted(false);
        btnInviteSelected.addActionListener(e -> {
            int row = onlinePlayersTable.getSelectedRow();
            if (row >= 0) {
                byte targetId = (byte) onlinePlayersModel.getValueAt(row, 0);
                tcpClient.sendInvite(targetId);
            } else {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 người chơi từ danh sách để gửi lời mời!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        leftPanel.add(btnInviteSelected, BorderLayout.SOUTH);

        // Cột Phải: Danh sách Phòng Đấu
        JPanel rightPanel = new JPanel(new BorderLayout(0, 10));
        rightPanel.setBackground(new Color(18, 24, 40));
        rightPanel.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel lblRoomsTitle = new JLabel("🏠 PHÒNG ĐẤU HIỆN CÓ");
        lblRoomsTitle.setFont(new Font("Arial", Font.BOLD, 14));
        lblRoomsTitle.setForeground(new Color(255, 205, 80));
        rightPanel.add(lblRoomsTitle, BorderLayout.NORTH);

        roomsModel = new DefaultTableModel(new String[]{"ID", "Tên Phòng", "Chủ Phòng", "Số Lượng", "Trạng Thái"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        roomsTable = createStyledTable(roomsModel);
        rightPanel.add(new JScrollPane(roomsTable), BorderLayout.CENTER);

        JPanel roomButtons = new JPanel(new GridLayout(1, 2, 10, 0));
        roomButtons.setOpaque(false);

        JButton btnCreateRoom = new JButton("➕ TẠO PHÒNG");
        btnCreateRoom.setFont(new Font("Arial", Font.BOLD, 13));
        btnCreateRoom.setBackground(new Color(40, 167, 69));
        btnCreateRoom.setForeground(Color.WHITE);
        btnCreateRoom.setFocusPainted(false);
        btnCreateRoom.addActionListener(e -> {
            String rName = JOptionPane.showInputDialog(this, "Nhập tên phòng của bạn:", "Tạo phòng mới", JOptionPane.PLAIN_MESSAGE);
            if (rName != null) {
                tcpClient.sendCreateRoom(rName.trim());
            }
        });
        roomButtons.add(btnCreateRoom);

        JButton btnJoinRoom = new JButton("🚪 VÀO PHÒNG");
        btnJoinRoom.setFont(new Font("Arial", Font.BOLD, 13));
        btnJoinRoom.setBackground(new Color(0, 140, 240));
        btnJoinRoom.setForeground(Color.WHITE);
        btnJoinRoom.setFocusPainted(false);
        btnJoinRoom.addActionListener(e -> {
            int row = roomsTable.getSelectedRow();
            if (row >= 0) {
                int rId = (int) roomsModel.getValueAt(row, 0);
                tcpClient.sendJoinRoom(rId);
            } else {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 phòng trong bảng để tham gia!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        roomButtons.add(btnJoinRoom);

        rightPanel.add(roomButtons, BorderLayout.SOUTH);

        panel.add(leftPanel);
        panel.add(rightPanel);
        return panel;
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setBackground(new Color(13, 18, 30));
        table.setForeground(Color.WHITE);
        table.setGridColor(new Color(30, 42, 68));
        table.setRowHeight(28);
        table.setFont(new Font("Arial", Font.PLAIN, 12));
        table.getTableHeader().setBackground(new Color(24, 32, 54));
        table.getTableHeader().setForeground(new Color(180, 210, 255));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        table.setSelectionBackground(new Color(45, 85, 150));
        table.setSelectionForeground(Color.WHITE);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.setDefaultRenderer(Object.class, center);
        return table;
    }

    // ==========================================
    // 4. CHI TIẾT PHÒNG ĐẤU (ROOM VIEW)
    // ==========================================
    private JPanel createRoomContentView() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(new Color(11, 15, 26));
        panel.setBorder(new EmptyBorder(16, 18, 16, 18));

        // Header thông tin phòng
        JPanel roomHeader = new JPanel(new GridLayout(2, 1, 0, 4));
        roomHeader.setBackground(new Color(18, 24, 42));
        roomHeader.setBorder(new EmptyBorder(10, 16, 10, 16));

        lblRoomTitle = new JLabel("🏠 PHÒNG: [Đang tải...]");
        lblRoomTitle.setFont(new Font("Arial", Font.BOLD, 18));
        lblRoomTitle.setForeground(new Color(255, 215, 0));
        roomHeader.add(lblRoomTitle);

        lblRoomHost = new JLabel("👑 Chủ phòng: [Đang tải...]");
        lblRoomHost.setFont(new Font("Arial", Font.PLAIN, 13));
        lblRoomHost.setForeground(new Color(180, 205, 235));
        roomHeader.add(lblRoomHost);

        panel.add(roomHeader, BorderLayout.NORTH);

        // Trung tâm: 4 Slots người chơi + Bảng mời bạn bè bên cạnh
        JPanel centerGrid = new JPanel(new BorderLayout(16, 0));
        centerGrid.setOpaque(false);

        // 4 Slots người chơi (Trái)
        slotsPanel = new JPanel(new GridLayout(2, 2, 14, 14));
        slotsPanel.setOpaque(false);
        renderEmptySlots();
        centerGrid.add(slotsPanel, BorderLayout.CENTER);

        // Cột Mời nhanh người chơi ở sảnh (Phải)
        JPanel inviteBox = new JPanel(new BorderLayout(0, 8));
        inviteBox.setPreferredSize(new Dimension(280, 0));
        inviteBox.setBackground(new Color(18, 24, 42));
        inviteBox.setBorder(new EmptyBorder(10, 12, 10, 12));

        JLabel lblQuickInvite = new JLabel("💌 MỜI NGƯỜI CHƠI ONLINE");
        lblQuickInvite.setFont(new Font("Arial", Font.BOLD, 13));
        lblQuickInvite.setForeground(new Color(100, 200, 255));
        inviteBox.add(lblQuickInvite, BorderLayout.NORTH);

        inviteListModel = new DefaultListModel<>();
        inviteList = new JList<>(inviteListModel);
        inviteList.setBackground(new Color(12, 17, 30));
        inviteList.setForeground(Color.WHITE);
        inviteList.setFont(new Font("Arial", Font.PLAIN, 13));
        inviteList.setFixedCellHeight(26);
        inviteBox.add(new JScrollPane(inviteList), BorderLayout.CENTER);

        JButton btnSendInvite = new JButton("GỬI LỜI MỜI");
        btnSendInvite.setFont(new Font("Arial", Font.BOLD, 12));
        btnSendInvite.setBackground(new Color(0, 140, 240));
        btnSendInvite.setForeground(Color.WHITE);
        btnSendInvite.setFocusPainted(false);
        btnSendInvite.addActionListener(e -> {
            int idx = inviteList.getSelectedIndex();
            if (idx >= 0 && inviteIndexToPid.containsKey(idx)) {
                byte targetId = inviteIndexToPid.get(idx);
                tcpClient.sendInvite(targetId);
            } else {
                JOptionPane.showMessageDialog(this, "Hãy chọn 1 người chơi trong danh sách để mời!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        inviteBox.add(btnSendInvite, BorderLayout.SOUTH);

        centerGrid.add(inviteBox, BorderLayout.EAST);
        panel.add(centerGrid, BorderLayout.CENTER);

        // Bottom Bar: Đếm ngược & Nút Sẵn sàng / Rời phòng
        JPanel roomBottom = new JPanel(new BorderLayout(0, 8));
        roomBottom.setOpaque(false);

        lblCountdownBanner = new JLabel("", SwingConstants.CENTER);
        lblCountdownBanner.setFont(new Font("Arial", Font.BOLD, 16));
        lblCountdownBanner.setForeground(new Color(255, 200, 0));
        roomBottom.add(lblCountdownBanner, BorderLayout.NORTH);

        JPanel actionBtns = new JPanel(new GridLayout(1, 2, 14, 0));
        actionBtns.setOpaque(false);

        btnReadyToggle = new JButton("✅ SẴN SÀNG");
        btnReadyToggle.setFont(new Font("Arial", Font.BOLD, 15));
        btnReadyToggle.setBackground(new Color(40, 167, 69));
        btnReadyToggle.setForeground(Color.WHITE);
        btnReadyToggle.setFocusPainted(false);
        btnReadyToggle.addActionListener(e -> {
            isMyReady = !isMyReady;
            tcpClient.sendToggleReady(isMyReady);
            updateReadyButtonText();
        });
        actionBtns.add(btnReadyToggle);

        btnLeaveRoom = new JButton("🚪 RỜI PHÒNG");
        btnLeaveRoom.setFont(new Font("Arial", Font.BOLD, 15));
        btnLeaveRoom.setBackground(new Color(180, 50, 50));
        btnLeaveRoom.setForeground(Color.WHITE);
        btnLeaveRoom.setFocusPainted(false);
        btnLeaveRoom.addActionListener(e -> {
            tcpClient.sendLeaveRoom();
            showLobbyView();
        });
        actionBtns.add(btnLeaveRoom);

        roomBottom.add(actionBtns, BorderLayout.SOUTH);
        panel.add(roomBottom, BorderLayout.SOUTH);

        return panel;
    }

    private void updateReadyButtonText() {
        if (isMyReady) {
            btnReadyToggle.setText("❌ HỦY SẴN SÀNG");
            btnReadyToggle.setBackground(new Color(220, 100, 20));
        } else {
            btnReadyToggle.setText("✅ SẴN SÀNG");
            btnReadyToggle.setBackground(new Color(40, 167, 69));
        }
    }

    private void renderEmptySlots() {
        slotsPanel.removeAll();
        for (int i = 1; i <= 4; i++) {
            JPanel slot = new JPanel(new BorderLayout());
            slot.setBackground(new Color(16, 22, 38));
            slot.setBorder(new LineBorder(new Color(35, 48, 76), 1, true));
            JLabel emptyLabel = new JLabel("Slot #" + i + " - Đang trống", SwingConstants.CENTER);
            emptyLabel.setForeground(new Color(100, 120, 150));
            emptyLabel.setFont(new Font("Arial", Font.ITALIC, 13));
            slot.add(emptyLabel, BorderLayout.CENTER);
            slotsPanel.add(slot);
        }
        slotsPanel.revalidate();
        slotsPanel.repaint();
    }

    // ==========================================
    // 5. TCP CLIENT LISTENER CALLBACKS
    // ==========================================
    @Override
    public void onConnected(byte myPlayerId) {
        this.myPlayerId = myPlayerId;
    }

    @Override
    public void onDisconnected(String reason) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, reason, "Mất kết nối", JOptionPane.WARNING_MESSAGE);
            onDisconnectClicked();
        });
    }

    @Override
    public void onLobbyData(List<OnlinePlayer> players, List<RoomInfo> rooms) {
        SwingUtilities.invokeLater(() -> {
            // Cập nhật bảng Người chơi Online
            onlinePlayersModel.setRowCount(0);
            inviteListModel.clear();
            inviteIndexToPid.clear();
            int invIdx = 0;

            for (OnlinePlayer p : players) {
                playerNamesMap.put(p.id, p.name);
                String pText = p.name;
                if (p.id == myPlayerId) pText += " (Bạn)";
                onlinePlayersModel.addRow(new Object[]{p.id, pText, p.getStatusDisplay()});

                // Nếu người chơi này đang ở sảnh và không phải là mình, thêm vào danh sách mời nhanh
                if (p.id != myPlayerId && "LOBBY".equals(p.status)) {
                    inviteListModel.addElement(p.name + " (#" + p.id + ")");
                    inviteIndexToPid.put(invIdx++, p.id);
                }
            }

            // Cập nhật bảng Danh sách Phòng
            roomsModel.setRowCount(0);
            for (RoomInfo r : rooms) {
                roomsModel.addRow(new Object[]{
                    r.id,
                    r.name,
                    r.hostName,
                    r.currentPlayers + " / " + r.maxPlayers,
                    r.getStatusDisplay()
                });
            }
        });
    }

    @Override
    public void onMatchmakingStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            if ("QUEUED".equals(status)) {
                setMatchmakingState(true);
            } else if ("IDLE".equals(status)) {
                setMatchmakingState(false);
            } else if ("MATCHED".equals(status)) {
                setMatchmakingState(false);
                showRoomView();
            }
        });
    }

    @Override
    public void onRoomData(RoomInfo roomInfo, List<RoomMember> members) {
        SwingUtilities.invokeLater(() -> {
            currentRoomId = roomInfo.id;
            lblRoomTitle.setText("🏠 PHÒNG #" + roomInfo.id + ": " + roomInfo.name);
            lblRoomHost.setText("👑 Chủ phòng: " + roomInfo.hostName + " | Trạng thái: " + roomInfo.getStatusDisplay());

            // Vẽ lại 4 slots người chơi
            slotsPanel.removeAll();
            for (int i = 0; i < 4; i++) {
                JPanel slot = new JPanel(new BorderLayout(0, 4));
                slot.setBorder(new EmptyBorder(10, 12, 10, 12));

                if (i < members.size()) {
                    RoomMember m = members.get(i);
                    playerNamesMap.put(m.id, m.name);

                    if (m.id == myPlayerId) {
                        isMyReady = m.isReady;
                        updateReadyButtonText();
                    }

                    slot.setBackground(m.isReady ? new Color(20, 45, 30) : new Color(24, 32, 54));
                    slot.setBorder(new LineBorder(m.isReady ? new Color(40, 180, 80) : new Color(60, 80, 120), 1, true));

                    JLabel lblName = new JLabel((m.isHost ? "👑 " : "🚁 ") + m.name + (m.id == myPlayerId ? " (BẠN)" : ""));
                    lblName.setFont(new Font("Arial", Font.BOLD, 14));
                    lblName.setForeground(m.isHost ? new Color(255, 215, 0) : Color.WHITE);
                    slot.add(lblName, BorderLayout.NORTH);

                    JLabel lblState = new JLabel(m.isReady ? "✅ ĐÃ SẴN SÀNG" : "⏳ CHƯA SẴN SÀNG");
                    lblState.setFont(new Font("Arial", Font.BOLD, 12));
                    lblState.setForeground(m.isReady ? new Color(100, 255, 140) : new Color(255, 170, 70));
                    slot.add(lblState, BorderLayout.SOUTH);
                } else {
                    slot.setBackground(new Color(15, 20, 34));
                    slot.setBorder(new LineBorder(new Color(30, 40, 60), 1, true));
                    JLabel emptyLabel = new JLabel("Slot #" + (i + 1) + " (Đang trống)", SwingConstants.CENTER);
                    emptyLabel.setForeground(new Color(90, 110, 140));
                    emptyLabel.setFont(new Font("Arial", Font.ITALIC, 13));
                    slot.add(emptyLabel, BorderLayout.CENTER);
                }
                slotsPanel.add(slot);
            }
            slotsPanel.revalidate();
            slotsPanel.repaint();

            showRoomView();
        });
    }

    @Override
    public void onLeftRoom() {
        SwingUtilities.invokeLater(() -> {
            currentRoomId = -1;
            isMyReady = false;
            lblCountdownBanner.setText("");
            updateReadyButtonText();
            if (gamePanel == null) {
                showLobbyView();
            }
        });
    }

    @Override
    public void onInviteReceived(byte fromId, String fromName, int roomId, String roomName) {
        SwingUtilities.invokeLater(() -> {
            int opt = JOptionPane.showConfirmDialog(
                this,
                "🔔 LỜI MỜI THAM GIA PHÒNG ĐẤU\n\nNgười chơi [" + fromName + "] đã mời bạn vào:\n" + roomName + " (Phòng #" + roomId + ")!\n\nBạn có muốn tham gia không?",
                "Lời mời từ " + fromName,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );

            if (opt == JOptionPane.YES_OPTION) {
                tcpClient.sendInviteResponse(roomId, true);
            } else {
                tcpClient.sendInviteResponse(roomId, false);
            }
        });
    }

    @Override
    public void onInviteDeclined(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, "Lời mời bị từ chối", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    @Override
    public void onNotification(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    @Override
    public void onError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.WARNING_MESSAGE);
        });
    }

    @Override
    public void onCountdown(int seconds) {
        SwingUtilities.invokeLater(() -> {
            if (gamePanel != null) {
                gamePanel.setCountdown(seconds);
            } else {
                lblCountdownBanner.setText("🚀 TẤT CẢ ĐÃ SẴN SÀNG! VÀO TRẬN ĐẤU TRONG " + seconds + " GIÂY...");
            }
        });
    }

    @Override
    public void onCountdownCancelled() {
        SwingUtilities.invokeLater(() -> {
            if (gamePanel != null) {
                gamePanel.setCountdown(-1);
            } else {
                lblCountdownBanner.setText("⚠️ Đã hủy đếm ngược do có người chơi thay đổi sẵn sàng.");
            }
        });
    }

    @Override
    public void onGameStart() {
        SwingUtilities.invokeLater(() -> {
            lblCountdownBanner.setText("");
            rootCardLayout.show(rootPanel, "GAME");

            if (gamePanel == null) {
                UDPClient udpClient = new UDPClient();
                udpClient.connect(currentServerIP);

                gamePanel = new GamePanel(udpClient, tcpClient, playerNamesMap);
                gamePanel.setGamePanelListener(this::onReturnToLobbyFromGame);
                gameContainer.removeAll();
                gameContainer.add(gamePanel, BorderLayout.CENTER);
            } else {
                gamePanel.resetForRematch();
            }

            gameContainer.revalidate();
            gameContainer.repaint();
            gamePanel.requestFocus();
        });
    }

    private void onReturnToLobbyFromGame() {
        SwingUtilities.invokeLater(() -> {
            if (gamePanel != null) {
                gamePanel.stopGame();
                gameContainer.removeAll();
                gamePanel = null;
            }
            rootCardLayout.show(rootPanel, "DASHBOARD");
            showLobbyView();
            revalidate();
            repaint();
        });
    }
}
