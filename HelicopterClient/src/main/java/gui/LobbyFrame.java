package gui;

import audio.SoundManager;
import game.GamePanel;
import gui.components.GameButton;
import gui.components.GameCardPanel;
import gui.components.GameScrollBarUI;
import gui.components.GameTextField;
import model.OnlinePlayer;
import model.RoomInfo;
import model.RoomMember;
import network.TCPClient;
import network.UDPClient;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
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
    private GameTextField txtServerIP;
    private GameTextField txtPlayerName;
    private GameButton btnConnect;
    private JLabel lblConnectStatus;

    // View 2: Dashboard (Sảnh chính & Phòng)
    private CardLayout contentCardLayout;
    private JPanel contentPanel;
    private JLabel lblPlayerProfile;
    private GameButton btnQuickMatch;
    private GameButton btnAudioToggle;
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
    private GameButton btnReadyToggle;
    private GameButton btnLeaveRoom;
    private boolean isMyReady = false;
    private int currentRoomId = -1;

    // Danh sách người chơi online để mời nhanh
    private DefaultListModel<String> inviteListModel;
    private JList<String> inviteList;
    private Map<Integer, Byte> inviteIndexToPid = new HashMap<>();
    private java.util.List<OnlinePlayer> cachedOnlinePlayers = new java.util.ArrayList<>();

    // View 3: Trong game
    private JPanel gameContainer;
    private GamePanel gamePanel;

    // Hình ảnh phi thuyền cho 4 slot
    private BufferedImage[] shipImages = new BufferedImage[4];

    public LobbyFrame() {
        tcpClient = new TCPClient();
        tcpClient.setListener(this);
        loadShipAssets();
        initUI();
        // Bắt đầu phát nhạc nền sảnh
        SoundManager.getInstance().playBGM("Darkwave.wav");
    }

    private File findAssetFile(String relativePath) {
        File f1 = new File(relativePath);
        if (f1.exists()) return f1;
        File f2 = new File("HelicopterClient/" + relativePath);
        if (f2.exists()) return f2;
        return f1;
    }

    private void loadShipAssets() {
        String[] shipFiles = {
            "playerShip1_green.png",
            "playerShip1_red.png",
            "playerShip1_blue.png",
            "playerShip1_orange.png"
        };
        for (int i = 0; i < 4; i++) {
            try {
                File f = findAssetFile("assets/images/" + shipFiles[i]);
                if (f.exists()) {
                    shipImages[i] = ImageIO.read(f);
                }
            } catch (Exception ignored) {}
        }
    }

    private void initUI() {
        setTitle("Helicopter Combat - Sci-Fi Gaming Arena");
        setSize(880, 640);
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
    // 1. MÀN HÌNH ĐĂNG NHẬP / KẾT NỐI (SCI-FI LOGIN)
    // ==========================================
    private JPanel createLoginView() {
        JPanel panel = new JPanel(null);
        panel.setBackground(new Color(11, 15, 26));

        // Tiêu đề game lớn với hiệu ứng Neon
        JLabel lblTitle = new JLabel("HELICOPTER COMBAT", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 30));
        lblTitle.setForeground(new Color(0, 225, 255));
        lblTitle.setBounds(150, 48, 580, 42);
        panel.add(lblTitle);

        JLabel lblSub = new JLabel("ĐẤU TRƯỜNG BẮN MÁY BAY KHÔNG GIAN ONLINE", SwingConstants.CENTER);
        lblSub.setFont(new Font("Arial", Font.BOLD, 12));
        lblSub.setForeground(new Color(140, 175, 220));
        lblSub.setBounds(150, 92, 580, 22);
        panel.add(lblSub);

        // Card đăng nhập kính mờ chính giữa
        GameCardPanel formBox = new GameCardPanel(null);
        formBox.setBackgroundColor(new Color(18, 24, 42, 240));
        formBox.setBorderColor(new Color(45, 75, 120));
        formBox.setAccentColor(new Color(0, 210, 255));
        formBox.setBounds(255, 140, 370, 340);

        JLabel lblFormTitle = new JLabel("KẾT NỐI MÁY CHỦ", SwingConstants.CENTER);
        lblFormTitle.setFont(new Font("Arial", Font.BOLD, 15));
        lblFormTitle.setForeground(new Color(100, 215, 255));
        lblFormTitle.setBounds(20, 20, 330, 25);
        formBox.add(lblFormTitle);

        JLabel lblIP = new JLabel("Địa chỉ Server IP:");
        lblIP.setForeground(new Color(200, 215, 240));
        lblIP.setFont(new Font("Arial", Font.PLAIN, 12));
        lblIP.setBounds(30, 58, 200, 20);
        formBox.add(lblIP);

        txtServerIP = new GameTextField("localhost", "Ví dụ: 127.0.0.1");
        txtServerIP.setBounds(30, 80, 310, 38);
        formBox.add(txtServerIP);

        JLabel lblName = new JLabel("Tên phi công (Player Name):");
        lblName.setForeground(new Color(200, 215, 240));
        lblName.setFont(new Font("Arial", Font.PLAIN, 12));
        lblName.setBounds(30, 130, 200, 20);
        formBox.add(lblName);

        txtPlayerName = new GameTextField("", "Nhập tên phi công của bạn...");
        txtPlayerName.setBounds(30, 152, 310, 38);
        formBox.add(txtPlayerName);

        btnConnect = new GameButton("▶ VÀO SẢNH CHỜ", GameButton.Style.CYAN);
        btnConnect.setFont(new Font("Arial", Font.BOLD, 14));
        btnConnect.setBounds(30, 215, 310, 44);
        btnConnect.addActionListener(e -> onConnectClicked());
        formBox.add(btnConnect);

        lblConnectStatus = new JLabel("Nhập tên để sẵn sàng cất cánh", SwingConstants.CENTER);
        lblConnectStatus.setFont(new Font("Arial", Font.ITALIC, 12));
        lblConnectStatus.setForeground(new Color(150, 165, 190));
        lblConnectStatus.setBounds(20, 275, 330, 25);
        formBox.add(lblConnectStatus);

        panel.add(formBox);

        // Footer version
        JLabel lblVer = new JLabel("Helicopter Combat v2.4 • Multiplayer UDP/TCP Arena", SwingConstants.CENTER);
        lblVer.setFont(new Font("Arial", Font.PLAIN, 11));
        lblVer.setForeground(new Color(75, 95, 125));
        lblVer.setBounds(200, 550, 480, 20);
        panel.add(lblVer);

        return panel;
    }

    private void onConnectClicked() {
        String ip = txtServerIP.getText().trim();
        String name = txtPlayerName.getText().trim();

        if (name.isEmpty()) {
            lblConnectStatus.setText("[!] Vui lòng nhập tên phi công!");
            lblConnectStatus.setForeground(new Color(255, 100, 100));
            SoundManager.getInstance().playCancel();
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
                    lblPlayerProfile.setText("● Phi công: " + name + " (ID: #" + tcpClient.getMyPlayerId() + ")");
                    rootCardLayout.show(rootPanel, "DASHBOARD");
                    showLobbyView();
                } else {
                    lblConnectStatus.setText("[x] Không thể kết nối tới " + ip);
                    lblConnectStatus.setForeground(new Color(255, 80, 80));
                    btnConnect.setEnabled(true);
                    SoundManager.getInstance().playCancel();
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
        header.setBackground(new Color(16, 22, 38));
        header.setBorder(new EmptyBorder(10, 18, 10, 18));

        // Profile bên trái
        JPanel profilePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        profilePanel.setOpaque(false);

        lblPlayerProfile = new JLabel("● Phi công: ");
        lblPlayerProfile.setFont(new Font("Arial", Font.BOLD, 14));
        lblPlayerProfile.setForeground(new Color(100, 255, 160));
        profilePanel.add(lblPlayerProfile);
        header.add(profilePanel, BorderLayout.WEST);

        // Bộ nút bên phải
        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setOpaque(false);

        // Nút Bật/Tắt Nhạc nền
        btnAudioToggle = new GameButton("♫ NHẠC: BẬT", GameButton.Style.DARK);
        btnAudioToggle.setFont(new Font("Arial", Font.BOLD, 12));
        btnAudioToggle.setPreferredSize(new Dimension(135, 34));
        btnAudioToggle.addActionListener(e -> {
            SoundManager.getInstance().toggleBgmMute();
            boolean muted = SoundManager.getInstance().isBgmMuted();
            btnAudioToggle.setText(muted ? "♫ NHẠC: TẮT" : "♫ NHẠC: BẬT");
        });
        headerRight.add(btnAudioToggle);

        btnQuickMatch = new GameButton("⚡ GHÉP TRẬN NGẪU NHIÊN", GameButton.Style.GOLD);
        btnQuickMatch.setFont(new Font("Arial", Font.BOLD, 13));
        btnQuickMatch.setPreferredSize(new Dimension(215, 34));
        btnQuickMatch.addActionListener(e -> onQuickMatchClicked());
        headerRight.add(btnQuickMatch);

        GameButton btnDisconnect = new GameButton("✕ Đăng xuất", GameButton.Style.DARK);
        btnDisconnect.setFont(new Font("Arial", Font.PLAIN, 12));
        btnDisconnect.setPreferredSize(new Dimension(105, 34));
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
            SoundManager.getInstance().playCancel();
        }
    }

    private void setMatchmakingState(boolean searching) {
        this.isMatchmaking = searching;
        if (searching) {
            btnQuickMatch.setText("[!] ĐANG TÌM TRẬN... [HỦY]");
            btnQuickMatch.setStyle(GameButton.Style.RED);
        } else {
            btnQuickMatch.setText("⚡ GHÉP TRẬN NGẪU NHIÊN");
            btnQuickMatch.setStyle(GameButton.Style.GOLD);
        }
    }

    private void onDisconnectClicked() {
        tcpClient.disconnect();
        rootCardLayout.show(rootPanel, "LOGIN");
        btnConnect.setEnabled(true);
        lblConnectStatus.setText("Đã ngắt kết nối.");
        lblConnectStatus.setForeground(Color.GRAY);
        SoundManager.getInstance().playCancel();
    }

    private void showLobbyView() {
        contentCardLayout.show(contentPanel, "LOBBY_VIEW");
        btnQuickMatch.setEnabled(true);
    }

    private void showRoomView() {
        contentCardLayout.show(contentPanel, "ROOM_VIEW");
        btnQuickMatch.setEnabled(false); // Đang trong phòng thì tắt nút tìm trận ngẫu nhiên
        updateInviteList();
    }

    private void updateInviteList() {
        inviteListModel.clear();
        inviteIndexToPid.clear();
        int inviteIdx = 0;
        for (OnlinePlayer p : cachedOnlinePlayers) {
            if (p.id != myPlayerId && ("LOBBY".equalsIgnoreCase(p.status) || "IDLE".equalsIgnoreCase(p.status))) {
                inviteListModel.addElement("● " + p.name + " (#" + p.id + ")");
                inviteIndexToPid.put(inviteIdx++, p.id);
            }
        }
    }

    // ==========================================
    // 3. SẢNH CHÍNH (ONLINE PLAYERS & ROOM LIST)
    // ==========================================
    private JPanel createLobbyContentView() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 16, 0));
        panel.setBackground(new Color(11, 15, 26));
        panel.setBorder(new EmptyBorder(14, 18, 16, 18));

        // Cột Trái: Danh sách người chơi Online
        GameCardPanel leftPanel = new GameCardPanel(new BorderLayout(0, 10));
        leftPanel.setBackgroundColor(new Color(16, 22, 38, 230));
        leftPanel.setBorderColor(new Color(40, 60, 95));
        leftPanel.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel lblOnlineTitle = new JLabel("● NGƯỜI CHƠI ONLINE TRÊN SERVER");
        lblOnlineTitle.setFont(new Font("Arial", Font.BOLD, 13));
        lblOnlineTitle.setForeground(new Color(100, 210, 255));
        leftPanel.add(lblOnlineTitle, BorderLayout.NORTH);

        onlinePlayersModel = new DefaultTableModel(new String[]{"ID", "Tên Người Chơi", "Trạng Thái"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        onlinePlayersTable = createStyledTable(onlinePlayersModel);
        leftPanel.add(createStyledScrollPane(onlinePlayersTable), BorderLayout.CENTER);

        GameButton btnInviteSelected = new GameButton("✉ MỜI NGƯỜI CHƠI ĐÃ CHỌN", GameButton.Style.CYAN);
        btnInviteSelected.setPreferredSize(new Dimension(0, 36));
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
        GameCardPanel rightPanel = new GameCardPanel(new BorderLayout(0, 10));
        rightPanel.setBackgroundColor(new Color(16, 22, 38, 230));
        rightPanel.setBorderColor(new Color(40, 60, 95));
        rightPanel.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel lblRoomsTitle = new JLabel("◆ PHÒNG ĐẤU HIỆN CÓ");
        lblRoomsTitle.setFont(new Font("Arial", Font.BOLD, 13));
        lblRoomsTitle.setForeground(new Color(255, 205, 80));
        rightPanel.add(lblRoomsTitle, BorderLayout.NORTH);

        roomsModel = new DefaultTableModel(new String[]{"ID", "Tên Phòng", "Chủ Phòng", "Số Lượng", "Trạng Thái"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        roomsTable = createStyledTable(roomsModel);
        rightPanel.add(createStyledScrollPane(roomsTable), BorderLayout.CENTER);

        JPanel roomButtons = new JPanel(new GridLayout(1, 2, 10, 0));
        roomButtons.setOpaque(false);

        GameButton btnCreateRoom = new GameButton("+ TẠO PHÒNG", GameButton.Style.GREEN);
        btnCreateRoom.setPreferredSize(new Dimension(0, 36));
        btnCreateRoom.addActionListener(e -> {
            String rName = JOptionPane.showInputDialog(this, "Nhập tên phòng của bạn:", "Tạo phòng mới", JOptionPane.PLAIN_MESSAGE);
            if (rName != null && !rName.trim().isEmpty()) {
                tcpClient.sendCreateRoom(rName.trim());
            }
        });
        roomButtons.add(btnCreateRoom);

        GameButton btnJoinRoom = new GameButton("▶ VÀO PHÒNG", GameButton.Style.CYAN);
        btnJoinRoom.setPreferredSize(new Dimension(0, 36));
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
        table.setBackground(new Color(13, 18, 32));
        table.setForeground(Color.WHITE);
        table.setGridColor(new Color(25, 36, 58));
        table.setRowHeight(30);
        table.setFont(new Font("Arial", Font.PLAIN, 12));
        table.getTableHeader().setBackground(new Color(22, 30, 52));
        table.getTableHeader().setForeground(new Color(100, 210, 255));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        table.setSelectionBackground(new Color(0, 140, 220, 180));
        table.setSelectionForeground(Color.WHITE);
        table.setShowVerticalLines(false);

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, val, isSelected, hasFocus, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? new Color(14, 20, 36) : new Color(18, 25, 45));
                }
                if (val != null) {
                    String str = val.toString();
                    if (str.contains("Đang chờ") || str.contains("Rảnh")) {
                        c.setForeground(new Color(80, 240, 140));
                    } else if (str.contains("Trong trận") || str.contains("Bận")) {
                        c.setForeground(new Color(255, 100, 100));
                    } else {
                        c.setForeground(isSelected ? Color.WHITE : new Color(220, 230, 245));
                    }
                }
                return c;
            }
        };
        table.setDefaultRenderer(Object.class, cellRenderer);
        return table;
    }

    private JScrollPane createStyledScrollPane(Component comp) {
        JScrollPane sp = new JScrollPane(comp);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(new LineBorder(new Color(35, 50, 75), 1, true));
        sp.getVerticalScrollBar().setUI(new GameScrollBarUI());
        sp.getHorizontalScrollBar().setUI(new GameScrollBarUI());
        return sp;
    }

    // ==========================================
    // 4. CHI TIẾT PHÒNG ĐẤU (ROOM VIEW - 4 SLOTS PHI THUYỀN)
    // ==========================================
    private JPanel createRoomContentView() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(new Color(11, 15, 26));
        panel.setBorder(new EmptyBorder(14, 18, 16, 18));

        // Header thông tin phòng
        GameCardPanel roomHeader = new GameCardPanel(new GridLayout(2, 1, 0, 4));
        roomHeader.setBackgroundColor(new Color(18, 25, 45, 240));
        roomHeader.setBorderColor(new Color(45, 75, 120));
        roomHeader.setBorder(new EmptyBorder(10, 16, 10, 16));

        lblRoomTitle = new JLabel("◆ PHÒNG: [Đang tải...]");
        lblRoomTitle.setFont(new Font("Arial", Font.BOLD, 18));
        lblRoomTitle.setForeground(new Color(255, 215, 0));
        roomHeader.add(lblRoomTitle);

        lblRoomHost = new JLabel("★ Chủ phòng: [Đang tải...]");
        lblRoomHost.setFont(new Font("Arial", Font.PLAIN, 13));
        lblRoomHost.setForeground(new Color(180, 205, 235));
        roomHeader.add(lblRoomHost);

        panel.add(roomHeader, BorderLayout.NORTH);

        // Trung tâm: 4 Slots người chơi + Bảng mời bạn bè bên cạnh
        JPanel centerGrid = new JPanel(new BorderLayout(16, 0));
        centerGrid.setOpaque(false);

        // 4 Slots người chơi dạng Thẻ phi thuyền (Trái)
        slotsPanel = new JPanel(new GridLayout(2, 2, 14, 14));
        slotsPanel.setOpaque(false);
        renderEmptySlots();
        centerGrid.add(slotsPanel, BorderLayout.CENTER);

        // Cột Mời nhanh người chơi ở sảnh (Phải)
        GameCardPanel inviteBox = new GameCardPanel(new BorderLayout(0, 8));
        inviteBox.setPreferredSize(new Dimension(270, 0));
        inviteBox.setBackgroundColor(new Color(16, 22, 38, 230));
        inviteBox.setBorderColor(new Color(40, 60, 95));
        inviteBox.setBorder(new EmptyBorder(10, 12, 10, 12));

        JLabel lblQuickInvite = new JLabel("✉ MỜI NGƯỜI CHƠI ONLINE");
        lblQuickInvite.setFont(new Font("Arial", Font.BOLD, 13));
        lblQuickInvite.setForeground(new Color(100, 200, 255));
        inviteBox.add(lblQuickInvite, BorderLayout.NORTH);

        inviteListModel = new DefaultListModel<>();
        inviteList = new JList<>(inviteListModel);
        inviteList.setBackground(new Color(12, 17, 30));
        inviteList.setForeground(Color.WHITE);
        inviteList.setFont(new Font("Arial", Font.PLAIN, 13));
        inviteList.setFixedCellHeight(28);
        inviteBox.add(createStyledScrollPane(inviteList), BorderLayout.CENTER);

        GameButton btnSendInvite = new GameButton("✉ GỬI LỜI MỜI", GameButton.Style.CYAN);
        btnSendInvite.setPreferredSize(new Dimension(0, 34));
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
        lblCountdownBanner.setFont(new Font("Arial", Font.BOLD, 17));
        lblCountdownBanner.setForeground(new Color(255, 215, 0));
        roomBottom.add(lblCountdownBanner, BorderLayout.NORTH);

        JPanel actionBtns = new JPanel(new GridLayout(1, 2, 14, 0));
        actionBtns.setOpaque(false);

        btnReadyToggle = new GameButton("✓ SẴN SÀNG", GameButton.Style.GREEN);
        btnReadyToggle.setFont(new Font("Arial", Font.BOLD, 15));
        btnReadyToggle.setPreferredSize(new Dimension(0, 44));
        btnReadyToggle.addActionListener(e -> {
            isMyReady = !isMyReady;
            tcpClient.sendToggleReady(isMyReady);
            updateReadyButtonText();
        });
        actionBtns.add(btnReadyToggle);

        btnLeaveRoom = new GameButton("✕ RỜI PHÒNG", GameButton.Style.RED);
        btnLeaveRoom.setFont(new Font("Arial", Font.BOLD, 15));
        btnLeaveRoom.setPreferredSize(new Dimension(0, 44));
        btnLeaveRoom.addActionListener(e -> {
            tcpClient.sendLeaveRoom();
            showLobbyView();
            SoundManager.getInstance().playCancel();
        });
        actionBtns.add(btnLeaveRoom);

        roomBottom.add(actionBtns, BorderLayout.SOUTH);
        panel.add(roomBottom, BorderLayout.SOUTH);
        return panel;
    }

    private void renderEmptySlots() {
        slotsPanel.removeAll();
        for (int i = 0; i < 4; i++) {
            GameCardPanel emptySlot = new GameCardPanel(new BorderLayout());
            emptySlot.setBackgroundColor(new Color(14, 18, 30, 200));
            emptySlot.setBorderColor(new Color(30, 42, 65));
            JLabel emptyLabel = new JLabel("Slot #" + (i + 1) + " (Đang trống)", SwingConstants.CENTER);
            emptyLabel.setForeground(new Color(85, 105, 135));
            emptyLabel.setFont(new Font("Arial", Font.ITALIC, 13));
            emptySlot.add(emptyLabel, BorderLayout.CENTER);
            slotsPanel.add(emptySlot);
        }
    }

    private void updateReadyButtonText() {
        if (isMyReady) {
            btnReadyToggle.setText("✕ HỦY SẴN SÀNG");
            btnReadyToggle.setStyle(GameButton.Style.GOLD);
        } else {
            btnReadyToggle.setText("✓ SẴN SÀNG");
            btnReadyToggle.setStyle(GameButton.Style.GREEN);
        }
    }

    // ==========================================
    // 5. CÁC HÀM XỬ LÝ SỰ KIỆN TỪ SERVER (TCPListener)
    // ==========================================
    @Override
    public void onConnected(byte myPlayerId) {
        this.myPlayerId = myPlayerId;
    }

    @Override
    public void onDisconnected(String reason) {
        SwingUtilities.invokeLater(() -> {
            lblConnectStatus.setText("Mất kết nối: " + reason);
            lblConnectStatus.setForeground(Color.RED);
            btnConnect.setEnabled(true);
            rootCardLayout.show(rootPanel, "LOGIN");
        });
    }

    @Override
    public void onLobbyData(List<OnlinePlayer> players, List<RoomInfo> rooms) {
        SwingUtilities.invokeLater(() -> {
            this.cachedOnlinePlayers = new java.util.ArrayList<>(players);

            // Cập nhật danh sách người chơi online
            onlinePlayersModel.setRowCount(0);
            for (OnlinePlayer p : players) {
                onlinePlayersModel.addRow(new Object[]{
                    p.id,
                    p.name + (p.id == myPlayerId ? " (BẠN)" : ""),
                    p.getStatusDisplay()
                });
            }

            updateInviteList();

            // Cập nhật danh sách phòng
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
            lblRoomTitle.setText("◆ PHÒNG #" + roomInfo.id + ": " + roomInfo.name);
            lblRoomHost.setText("★ Chủ phòng: " + roomInfo.hostName + " | Trạng thái: " + roomInfo.getStatusDisplay());

            // Vẽ lại 4 slots người chơi dạng Card phi thuyền
            slotsPanel.removeAll();
            for (int i = 0; i < 4; i++) {
                GameCardPanel slot = new GameCardPanel(new BorderLayout(10, 4));
                slot.setBorder(new EmptyBorder(10, 14, 10, 14));

                if (i < members.size()) {
                    RoomMember m = members.get(i);
                    playerNamesMap.put(m.id, m.name);

                    if (m.id == myPlayerId) {
                        isMyReady = m.isReady;
                        updateReadyButtonText();
                    }

                    if (m.isReady) {
                        slot.setBackgroundColor(new Color(18, 48, 30, 240));
                        slot.setBorderColor(new Color(50, 220, 110));
                        slot.setAccentColor(new Color(50, 220, 110));
                    } else {
                        slot.setBackgroundColor(new Color(22, 30, 50, 240));
                        slot.setBorderColor(new Color(60, 85, 130));
                        slot.setAccentColor(null);
                    }

                    // Icon phi thuyền đại diện cho slot
                    BufferedImage shipImg = (i < shipImages.length) ? shipImages[i] : null;
                    if (shipImg != null) {
                        JLabel shipIcon = new JLabel(new ImageIcon(shipImg.getScaledInstance(38, 34, Image.SCALE_SMOOTH)));
                        slot.add(shipIcon, BorderLayout.WEST);
                    }

                    // Thông tin tên và trạng thái
                    JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 2));
                    infoPanel.setOpaque(false);

                    JLabel lblName = new JLabel((m.isHost ? "★ " : "") + m.name + (m.id == myPlayerId ? " (BẠN)" : ""));
                    lblName.setFont(new Font("Arial", Font.BOLD, 14));
                    lblName.setForeground(m.isHost ? new Color(255, 215, 0) : Color.WHITE);
                    infoPanel.add(lblName);

                    JLabel lblState = new JLabel(m.isReady ? "✓ ĐÃ SẴN SÀNG" : "... CHƯA SẴN SÀNG");
                    lblState.setFont(new Font("Arial", Font.BOLD, 12));
                    lblState.setForeground(m.isReady ? new Color(90, 255, 140) : new Color(255, 175, 75));
                    infoPanel.add(lblState);

                    slot.add(infoPanel, BorderLayout.CENTER);
                } else {
                    slot.setBackgroundColor(new Color(14, 18, 30, 180));
                    slot.setBorderColor(new Color(30, 42, 65));
                    JLabel emptyLabel = new JLabel("Slot #" + (i + 1) + " (Đang trống)", SwingConstants.CENTER);
                    emptyLabel.setForeground(new Color(85, 105, 135));
                    emptyLabel.setFont(new Font("Arial", Font.ITALIC, 13));
                    slot.add(emptyLabel, BorderLayout.CENTER);
                }
                slotsPanel.add(slot);
            }
            slotsPanel.revalidate();
            slotsPanel.repaint();

            updateInviteList();
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
            SoundManager.getInstance().playInvite();

            // Custom Sci-Fi Invite Dialog
            JDialog dialog = new JDialog(this, "Lời mời vào phòng", true);
            dialog.setUndecorated(true);
            dialog.setSize(440, 210);
            dialog.setLocationRelativeTo(this);

            GameCardPanel pnl = new GameCardPanel(new BorderLayout(0, 14));
            pnl.setBackgroundColor(new Color(18, 25, 45, 250));
            pnl.setBorderColor(new Color(0, 210, 255));
            pnl.setAccentColor(new Color(0, 225, 255));
            pnl.setBorder(new EmptyBorder(16, 20, 16, 20));

            JLabel lblTitle = new JLabel("✉ LỜI MỜI THAM GIA PHÒNG ĐẤU", SwingConstants.CENTER);
            lblTitle.setFont(new Font("Arial", Font.BOLD, 15));
            lblTitle.setForeground(new Color(0, 220, 255));
            pnl.add(lblTitle, BorderLayout.NORTH);

            JLabel lblMsg = new JLabel("<html><div style='text-align: center; color: #E0E8F5;'>"
                + "Phi công <b style='color: #FFD700; font-size: 14px;'>" + fromName + "</b> muốn mời bạn tham chiến tại:<br><br>"
                + "<span style='font-size: 13px; color: #64FFDA;'>◆ " + roomName + " (Phòng #" + roomId + ")</span>"
                + "</div></html>", SwingConstants.CENTER);
            pnl.add(lblMsg, BorderLayout.CENTER);

            JPanel btnPnl = new JPanel(new GridLayout(1, 2, 14, 0));
            btnPnl.setOpaque(false);

            GameButton btnAccept = new GameButton("✓ THAM GIA", GameButton.Style.GREEN);
            btnAccept.setPreferredSize(new Dimension(0, 38));
            btnAccept.addActionListener(e -> {
                dialog.dispose();
                tcpClient.sendInviteResponse(roomId, true);
            });

            GameButton btnDecline = new GameButton("✕ TỪ CHỐI", GameButton.Style.DARK);
            btnDecline.setPreferredSize(new Dimension(0, 38));
            btnDecline.addActionListener(e -> {
                dialog.dispose();
                tcpClient.sendInviteResponse(roomId, false);
                SoundManager.getInstance().playCancel();
            });

            btnPnl.add(btnAccept);
            btnPnl.add(btnDecline);
            pnl.add(btnPnl, BorderLayout.SOUTH);

            dialog.setContentPane(pnl);
            dialog.setVisible(true);
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
            SoundManager.getInstance().playCancel();
        });
    }

    @Override
    public void onCountdown(int seconds) {
        SwingUtilities.invokeLater(() -> {
            if (seconds > 0) {
                SoundManager.getInstance().playCountdown();
            }
            if (gamePanel != null) {
                gamePanel.setCountdown(seconds);
            } else {
                lblCountdownBanner.setText("● TẤT CẢ ĐÃ SẴN SÀNG! VÀO TRẬN ĐẤU TRONG " + seconds + " GIÂY...");
            }
        });
    }

    @Override
    public void onCountdownCancelled() {
        SwingUtilities.invokeLater(() -> {
            SoundManager.getInstance().playCancel();
            if (gamePanel != null) {
                gamePanel.setCountdown(-1);
            } else {
                lblCountdownBanner.setText("[!] Đã hủy đếm ngược do có người chơi thay đổi sẵn sàng.");
            }
        });
    }

    @Override
    public void onGameStart() {
        SwingUtilities.invokeLater(() -> {
            // Tạm dừng BGM sảnh khi vào trận đấu chiến đấu
            SoundManager.getInstance().pauseBGM();

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
            // Tiếp tục phát BGM khi quay lại sảnh
            SoundManager.getInstance().resumeBGM();

            rootCardLayout.show(rootPanel, "DASHBOARD");
            showLobbyView();
            revalidate();
            repaint();
        });
    }
}
