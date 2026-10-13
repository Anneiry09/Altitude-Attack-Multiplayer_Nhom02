package gui;

import audio.SoundManager;
import game.GamePanel;
import gui.components.GameButton;
import gui.components.GameCardPanel;
import gui.components.GameScrollBarUI;
import gui.components.GameSliderUI;
import gui.components.GameTextField;
import gui.components.StarfieldPanel;
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
import java.awt.geom.Point2D;
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
    private StarfieldPanel loginView;
    private GameTextField txtServerIP;
    private GameTextField txtPlayerName;
    private GameButton btnConnect;
    private JLabel lblConnectStatus;

    // View 2: Dashboard (Sảnh chính & Phòng)
    private StarfieldPanel dashboardView;
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
        loginView = createLoginView();
        dashboardView = createDashboardView();
        gameContainer = new JPanel(new BorderLayout());

        rootPanel.add(loginView, "LOGIN");
        rootPanel.add(dashboardView, "DASHBOARD");
        rootPanel.add(gameContainer, "GAME");

        add(rootPanel);
        rootCardLayout.show(rootPanel, "LOGIN");
    }

    // ==========================================
    // 1. MÀN HÌNH ĐĂNG NHẬP / KẾT NỐI (STARLIGHT SPACE LOGIN)
    // ==========================================
    private StarfieldPanel createLoginView() {
        StarfieldPanel panel = new StarfieldPanel(null);

        // Card đăng nhập kính mờ chính giữa mang phong cách Starlight Sci-Fi
        GameCardPanel formBox = new GameCardPanel(null);
        formBox.setBackgroundColor(new Color(12, 16, 28, 235));
        formBox.setBorderColor(new Color(85, 45, 25));
        formBox.setAccentColor(new Color(255, 115, 45)); // Quầng sáng cam Starlight
        formBox.setCornerBrackets(true);
        formBox.setBounds(255, 142, 370, 350);

        JLabel lblFormTitle = new JLabel("KẾT NỐI MÁY CHỦ", SwingConstants.CENTER);
        lblFormTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblFormTitle.setForeground(new Color(255, 145, 80));
        lblFormTitle.setBounds(20, 22, 330, 25);
        formBox.add(lblFormTitle);

        JLabel lblIP = new JLabel("Địa chỉ Server IP:");
        lblIP.setForeground(new Color(215, 225, 245));
        lblIP.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblIP.setBounds(30, 60, 200, 20);
        formBox.add(lblIP);

        txtServerIP = new GameTextField("localhost", "Ví dụ: 127.0.0.1");
        txtServerIP.setFocusColor(new Color(255, 120, 50));
        txtServerIP.setBounds(30, 82, 310, 38);
        formBox.add(txtServerIP);

        JLabel lblName = new JLabel("Tên phi công (Player Name):");
        lblName.setForeground(new Color(215, 225, 245));
        lblName.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblName.setBounds(30, 132, 200, 20);
        formBox.add(lblName);

        txtPlayerName = new GameTextField("", "Nhập tên phi công của bạn...");
        txtPlayerName.setFocusColor(new Color(255, 120, 50));
        txtPlayerName.setBounds(30, 154, 310, 38);
        formBox.add(txtPlayerName);

        // Nút kết nối phong cách Starlight Coral Red-Orange với góc ngoặc phát quang
        btnConnect = new GameButton("▶ VÀO SẢNH CHIẾN ĐẤU", GameButton.Style.CORAL);
        btnConnect.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnConnect.setBounds(30, 218, 310, 46);
        btnConnect.addActionListener(e -> onConnectClicked());
        formBox.add(btnConnect);

        lblConnectStatus = new JLabel("Nhập tên để sẵn sàng cất cánh", SwingConstants.CENTER);
        lblConnectStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblConnectStatus.setForeground(new Color(160, 175, 205));
        lblConnectStatus.setBounds(20, 285, 330, 25);
        formBox.add(lblConnectStatus);

        panel.add(formBox);

        // Footer version công nghệ
        JLabel lblVer = new JLabel("Altitude Attack • Starlight Space Arena v2.5 • UDP/TCP Network", SwingConstants.CENTER);
        lblVer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblVer.setForeground(new Color(95, 110, 140));
        lblVer.setBounds(200, 555, 480, 20);
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
                    if (loginView != null) loginView.stopAnimation();
                    if (dashboardView != null) dashboardView.startAnimation();
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
    private StarfieldPanel createDashboardView() {
        StarfieldPanel dashboard = new StarfieldPanel(new BorderLayout());
        dashboard.setShowTitleBanner(false); // Sảnh chờ dùng StarfieldPanel làm nền vũ trụ động với sao rơi

        // Top Navigation Header
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                // Đường viền neon ngăn cách mỏng bên dưới
                g2.setColor(new Color(40, 60, 95, 180));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.setColor(new Color(255, 140, 50, 120));
                g2.drawLine(getWidth() / 4, getHeight() - 1, getWidth() * 3 / 4, getHeight() - 1);
                g2.dispose();
            }
        };
        header.setBackground(new Color(10, 14, 25, 205)); // Màu đục mờ thấy sao rơi phía sau
        header.setBorder(new EmptyBorder(10, 20, 10, 20));

        // Profile bên trái
        JPanel profilePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        profilePanel.setOpaque(false);

        lblPlayerProfile = new JLabel("● PHI CÔNG: ");
        lblPlayerProfile.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblPlayerProfile.setForeground(new Color(80, 245, 160));
        profilePanel.add(lblPlayerProfile);
        header.add(profilePanel, BorderLayout.WEST);

        // Bộ nút bên phải
        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setOpaque(false);

        // Nút Cài đặt (Bên trái nút Ghép trận nhanh)
        GameButton btnSettings = new GameButton("⚙ CÀI ĐẶT", GameButton.Style.DARK);
        btnSettings.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSettings.setPreferredSize(new Dimension(115, 36));
        btnSettings.addActionListener(e -> showSettingsDialog());
        headerRight.add(btnSettings);

        btnQuickMatch = new GameButton("⚡ GHÉP TRẬN NHANH", GameButton.Style.CORAL);
        btnQuickMatch.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnQuickMatch.setPreferredSize(new Dimension(210, 36));
        btnQuickMatch.addActionListener(e -> onQuickMatchClicked());
        headerRight.add(btnQuickMatch);

        header.add(headerRight, BorderLayout.EAST);
        dashboard.add(header, BorderLayout.NORTH);

        // Content Area (Switch giữa Sảnh chính và Phòng)
        contentCardLayout = new CardLayout();
        contentPanel = new JPanel(contentCardLayout);
        contentPanel.setOpaque(false);

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
            btnQuickMatch.setStyle(GameButton.Style.DARK);
        } else {
            btnQuickMatch.setText("⚡ GHÉP TRẬN NHANH");
            btnQuickMatch.setStyle(GameButton.Style.CORAL);
        }
    }

    private void onDisconnectClicked() {
        tcpClient.disconnect();
        if (dashboardView != null) dashboardView.stopAnimation();
        if (loginView != null) loginView.startAnimation();
        rootCardLayout.show(rootPanel, "LOGIN");
        btnConnect.setEnabled(true);
        lblConnectStatus.setText("Đã ngắt kết nối.");
        lblConnectStatus.setForeground(Color.GRAY);
        SoundManager.getInstance().playCancel();
    }

    // ==========================================
    // CỬA SỔ CÀI ĐẶT (SETTINGS DIALOG CÓ HIỆU ỨNG TRƯỢT SANG TRÁI)
    // ==========================================
    private void showSettingsDialog() {
        JDialog dialog = new JDialog(this, "Cài đặt hệ thống", true);
        dialog.setUndecorated(true);
        dialog.setSize(460, 360);
        dialog.setLocationRelativeTo(this);

        GameCardPanel rootCard = new GameCardPanel(new BorderLayout());
        rootCard.setBackgroundColor(new Color(10, 14, 25, 250));
        rootCard.setBorderColor(new Color(55, 75, 115));
        rootCard.setAccentColor(new Color(255, 120, 50));
        rootCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Header Dialog
        JPanel headerPnl = new JPanel(new BorderLayout());
        headerPnl.setOpaque(false);

        JLabel lblTitle = new JLabel("⚙ CÀI ĐẶT HỆ THỐNG", SwingConstants.LEFT);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(new Color(255, 180, 80));
        headerPnl.add(lblTitle, BorderLayout.WEST);

        GameButton btnClose = new GameButton("✕", GameButton.Style.DARK);
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnClose.setPreferredSize(new Dimension(36, 32));
        btnClose.addActionListener(e -> dialog.dispose());
        headerPnl.add(btnClose, BorderLayout.EAST);

        rootCard.add(headerPnl, BorderLayout.NORTH);

        // Slide Container: Chứa Menu chính và Trang con cài đặt nhạc
        int viewportW = 420;
        int viewportH = 265;
        JPanel sliderViewport = new JPanel(null);
        sliderViewport.setOpaque(false);
        sliderViewport.setPreferredSize(new Dimension(viewportW, viewportH));

        JPanel sliderTrack = new JPanel(null);
        sliderTrack.setOpaque(false);
        sliderTrack.setBounds(0, 0, viewportW * 2, viewportH);

        // Panel 1: MENU CHÍNH
        JPanel pnlMainMenu = new JPanel(new GridLayout(3, 1, 0, 12));
        pnlMainMenu.setOpaque(false);
        pnlMainMenu.setBounds(0, 10, viewportW, viewportH - 20);

        // Mục 1: ÂM NHẠC & ÂM LƯỢNG
        GameButton btnMusicMenu = new GameButton("♫ ÂM THANH & NHẠC NỀN ▶", GameButton.Style.CORAL);
        btnMusicMenu.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnMusicMenu.setPreferredSize(new Dimension(0, 48));

        // Mục 2: THÔNG TIN PHI CÔNG & ĐỔI TÊN
        GameButton btnProfileMenu = new GameButton("👤 ĐỔI TÊN PHI CÔNG (" + myPlayerName + ")", GameButton.Style.DARK);
        btnProfileMenu.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnProfileMenu.setPreferredSize(new Dimension(0, 48));
        btnProfileMenu.addActionListener(e -> {
            String newName = JOptionPane.showInputDialog(dialog, "Nhập tên phi công mới của bạn:", myPlayerName);
            if (newName != null && !newName.trim().isEmpty() && !newName.trim().equals(myPlayerName)) {
                String trimmed = newName.trim();
                myPlayerName = trimmed;
                tcpClient.sendRename(trimmed);
                lblPlayerProfile.setText("● PHI CÔNG: " + trimmed + " (ID: #" + myPlayerId + ")");
                btnProfileMenu.setText("👤 ĐỔI TÊN PHI CÔNG (" + trimmed + ")");
                SoundManager.getInstance().playClick();
            }
        });

        // Mục 3: ĐĂNG XUẤT
        GameButton btnLogoutMenu = new GameButton("✕ ĐĂNG XUẤT KHỎI SERVER", GameButton.Style.RED);
        btnLogoutMenu.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnLogoutMenu.setPreferredSize(new Dimension(0, 48));
        btnLogoutMenu.addActionListener(e -> {
            dialog.dispose();
            onDisconnectClicked();
        });

        pnlMainMenu.add(btnMusicMenu);
        pnlMainMenu.add(btnProfileMenu);
        pnlMainMenu.add(btnLogoutMenu);
        sliderTrack.add(pnlMainMenu);

        // Panel 2: CÀI ĐẶT NHẠC CHI TIẾT
        JPanel pnlMusicDetails = new JPanel(new BorderLayout(0, 10));
        pnlMusicDetails.setOpaque(false);
        pnlMusicDetails.setBounds(viewportW, 10, viewportW, viewportH - 20);

        JPanel musicControlsBox = new JPanel(new GridLayout(4, 1, 0, 8));
        musicControlsBox.setOpaque(false);

        // Thanh kéo âm lượng
        JPanel volRow = new JPanel(new BorderLayout(10, 0));
        volRow.setOpaque(false);
        JLabel lblVolTitle = new JLabel("Âm lượng:");
        lblVolTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblVolTitle.setForeground(new Color(220, 230, 245));
        lblVolTitle.setPreferredSize(new Dimension(80, 26));
        volRow.add(lblVolTitle, BorderLayout.WEST);

        JSlider volSlider = new JSlider(0, 100, (int) (SoundManager.getInstance().getBgmVolume() * 100));
        volSlider.setOpaque(false);
        volSlider.setUI(new GameSliderUI(volSlider));
        JLabel lblVolVal = new JLabel(volSlider.getValue() + "%", SwingConstants.RIGHT);
        lblVolVal.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblVolVal.setForeground(new Color(255, 180, 80));
        lblVolVal.setPreferredSize(new Dimension(45, 26));

        volSlider.addChangeListener(ce -> {
            int v = volSlider.getValue();
            lblVolVal.setText(v + "%");
            SoundManager.getInstance().setBgmVolume(v / 100.0f);
        });
        volRow.add(volSlider, BorderLayout.CENTER);
        volRow.add(lblVolVal, BorderLayout.EAST);
        musicControlsBox.add(volRow);

        // Nút Bật/Tắt tiếng nhanh
        GameButton btnMuteToggle = new GameButton(
            SoundManager.getInstance().isBgmMuted() ? "🔈 MỞ TIẾNG BGM" : "🔇 TẮT TIẾNG BGM",
            GameButton.Style.DARK
        );
        btnMuteToggle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnMuteToggle.setPreferredSize(new Dimension(0, 34));
        btnMuteToggle.addActionListener(e -> {
            SoundManager.getInstance().toggleBgmMute();
            boolean muted = SoundManager.getInstance().isBgmMuted();
            btnMuteToggle.setText(muted ? "🔈 MỞ TIẾNG BGM" : "🔇 TẮT TIẾNG BGM");
        });
        musicControlsBox.add(btnMuteToggle);

        // Chọn danh sách bài nhạc nền (Darkwave.wav & OverworldDay.wav)
        JPanel songRow = new JPanel(new BorderLayout(10, 0));
        songRow.setOpaque(false);
        JLabel lblSongTitle = new JLabel("Bản nhạc:");
        lblSongTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSongTitle.setForeground(new Color(220, 230, 245));
        lblSongTitle.setPreferredSize(new Dimension(80, 26));
        songRow.add(lblSongTitle, BorderLayout.WEST);

        String[] bgmTracks = new String[]{"Darkwave.wav", "OverworldDay.wav"};
        JComboBox<String> comboTracks = new JComboBox<>(bgmTracks);
        comboTracks.setFont(new Font("Segoe UI", Font.BOLD, 12));
        comboTracks.setBackground(new Color(18, 25, 45));
        comboTracks.setForeground(new Color(255, 205, 80));
        comboTracks.setSelectedItem(SoundManager.getInstance().getCurrentBgmFileName());
        comboTracks.addActionListener(e -> {
            String selected = (String) comboTracks.getSelectedItem();
            if (selected != null) {
                SoundManager.getInstance().switchBGM(selected);
            }
        });
        songRow.add(comboTracks, BorderLayout.CENTER);
        musicControlsBox.add(songRow);

        // Nút quay lại menu chính
        GameButton btnBackToMain = new GameButton("◀ QUAY LẠI CÀI ĐẶT", GameButton.Style.DARK);
        btnBackToMain.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnBackToMain.setPreferredSize(new Dimension(0, 36));

        pnlMusicDetails.add(musicControlsBox, BorderLayout.CENTER);
        pnlMusicDetails.add(btnBackToMain, BorderLayout.SOUTH);
        sliderTrack.add(pnlMusicDetails);

        sliderViewport.add(sliderTrack);
        rootCard.add(sliderViewport, BorderLayout.CENTER);

        // ==========================================
        // HIỆU ỨNG TRƯỢT SANG TRÁI / PHẢI (SLIDE ANIMATION)
        // ==========================================
        final Timer[] slideTimer = new Timer[1];
        btnMusicMenu.addActionListener(e -> {
            // Đẩy menu chính qua trái (-viewportW), đẩy phần cài đặt nhạc vào từ bên phải
            if (slideTimer[0] != null && slideTimer[0].isRunning()) slideTimer[0].stop();
            slideTimer[0] = new Timer(15, null);
            slideTimer[0].addActionListener(te -> {
                int curX = sliderTrack.getX();
                int targetX = -viewportW;
                int diff = targetX - curX;
                if (Math.abs(diff) <= 8) {
                    sliderTrack.setLocation(targetX, 0);
                    slideTimer[0].stop();
                } else {
                    sliderTrack.setLocation(curX + (int) (diff * 0.28f), 0);
                }
                sliderViewport.repaint();
            });
            slideTimer[0].start();
        });

        btnBackToMain.addActionListener(e -> {
            // Trượt ngược lại menu chính (0)
            if (slideTimer[0] != null && slideTimer[0].isRunning()) slideTimer[0].stop();
            slideTimer[0] = new Timer(15, null);
            slideTimer[0].addActionListener(te -> {
                int curX = sliderTrack.getX();
                int targetX = 0;
                int diff = targetX - curX;
                if (Math.abs(diff) <= 8) {
                    sliderTrack.setLocation(targetX, 0);
                    slideTimer[0].stop();
                } else {
                    sliderTrack.setLocation(curX + (int) (diff * 0.28f), 0);
                }
                sliderViewport.repaint();
            });
            slideTimer[0].start();
        });

        dialog.setContentPane(rootCard);
        dialog.setVisible(true);
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
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(14, 18, 16, 18));

        // Cột Trái: Danh sách người chơi Online (Đồng bộ khung và màu chữ với Phòng Đấu Hoạt Động, nền đục mờ để lộ sao rơi)
        GameCardPanel leftPanel = new GameCardPanel(new BorderLayout(0, 10));
        leftPanel.setBackgroundColor(new Color(11, 16, 28, 168)); // Nền đục mờ thấy background falling stars
        leftPanel.setBorderColor(new Color(55, 75, 115));
        leftPanel.setAccentColor(new Color(255, 120, 50)); // Cùng màu nhấn Starlight Coral/Orange
        leftPanel.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel lblOnlineTitle = new JLabel("● PHI CÔNG TRỰC TUYẾN");
        lblOnlineTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblOnlineTitle.setForeground(new Color(255, 205, 80)); // Cùng màu vàng hổ phách với tiêu đề Phòng Đấu Hoạt Động
        leftPanel.add(lblOnlineTitle, BorderLayout.NORTH);

        onlinePlayersModel = new DefaultTableModel(new String[]{"ID", "Tên Phi Công", "Trạng Thái"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        onlinePlayersTable = createStyledTable(onlinePlayersModel);
        leftPanel.add(createStyledScrollPane(onlinePlayersTable), BorderLayout.CENTER);

        GameButton btnInviteSelected = new GameButton("✉ MỜI PHI CÔNG ĐÃ CHỌN", GameButton.Style.CORAL);
        btnInviteSelected.setPreferredSize(new Dimension(0, 38));
        btnInviteSelected.setFont(new Font("Segoe UI", Font.BOLD, 12));
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
        rightPanel.setBackgroundColor(new Color(11, 16, 28, 168)); // Nền đục mờ thấy background falling stars
        rightPanel.setBorderColor(new Color(55, 75, 115));
        rightPanel.setAccentColor(new Color(255, 120, 50));
        rightPanel.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel lblRoomsTitle = new JLabel("◆ PHÒNG ĐẤU HOẠT ĐỘNG");
        lblRoomsTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblRoomsTitle.setForeground(new Color(255, 205, 80));
        rightPanel.add(lblRoomsTitle, BorderLayout.NORTH);

        roomsModel = new DefaultTableModel(new String[]{"ID", "Tên Phòng", "Chỉ Huy", "Số Phi Đội", "Trạng Thái"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        roomsTable = createStyledTable(roomsModel);
        rightPanel.add(createStyledScrollPane(roomsTable), BorderLayout.CENTER);

        JPanel roomButtons = new JPanel(new GridLayout(1, 2, 12, 0));
        roomButtons.setOpaque(false);

        GameButton btnCreateRoom = new GameButton("+ TẠO PHÒNG MỚI", GameButton.Style.CORAL);
        btnCreateRoom.setPreferredSize(new Dimension(0, 38));
        btnCreateRoom.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCreateRoom.addActionListener(e -> {
            String rName = JOptionPane.showInputDialog(this, "Nhập tên phòng của bạn:", "Tạo phòng mới", JOptionPane.PLAIN_MESSAGE);
            if (rName != null && !rName.trim().isEmpty()) {
                tcpClient.sendCreateRoom(rName.trim());
            }
        });
        roomButtons.add(btnCreateRoom);

        GameButton btnJoinRoom = new GameButton("▶ THAM CHIẾN", GameButton.Style.CORAL);
        btnJoinRoom.setPreferredSize(new Dimension(0, 38));
        btnJoinRoom.setFont(new Font("Segoe UI", Font.BOLD, 12));
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
        table.setOpaque(false);
        table.setForeground(new Color(230, 240, 255));
        table.setGridColor(new Color(35, 50, 75, 120));
        table.setRowHeight(32);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setBackground(new Color(18, 26, 46, 215));
        table.getTableHeader().setForeground(new Color(255, 180, 80));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setSelectionBackground(new Color(255, 120, 40, 160));
        table.setSelectionForeground(Color.WHITE);
        table.setShowVerticalLines(false);

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, val, isSelected, hasFocus, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? new Color(11, 16, 28, 140) : new Color(16, 24, 42, 175));
                }
                if (val != null) {
                    String str = val.toString();
                    if (str.contains("Đang chờ") || str.contains("Rảnh") || str.contains("LOBBY") || str.contains("Sẵn sàng")) {
                        c.setForeground(new Color(80, 240, 140));
                    } else if (str.contains("Trong trận") || str.contains("Bận") || str.contains("PLAYING") || str.contains("Đầy")) {
                        c.setForeground(new Color(255, 100, 100));
                    } else if (str.contains("(BẠN)")) {
                        c.setForeground(new Color(255, 215, 0));
                    } else {
                        c.setForeground(isSelected ? Color.WHITE : new Color(210, 225, 245));
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
        sp.setBorder(new LineBorder(new Color(45, 65, 100, 180), 1, true));
        sp.getVerticalScrollBar().setUI(new GameScrollBarUI());
        sp.getHorizontalScrollBar().setUI(new GameScrollBarUI());
        return sp;
    }

    // ==========================================
    // 4. CHI TIẾT PHÒNG ĐẤU (ROOM VIEW - 4 SLOTS PHI THUYỀN)
    // ==========================================
    private JPanel createRoomContentView() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(14, 18, 16, 18));

        // Header thông tin phòng
        GameCardPanel roomHeader = new GameCardPanel(new GridLayout(2, 1, 0, 4));
        roomHeader.setBackgroundColor(new Color(11, 16, 28, 175));
        roomHeader.setBorderColor(new Color(55, 75, 115));
        roomHeader.setAccentColor(new Color(255, 120, 50));
        roomHeader.setBorder(new EmptyBorder(10, 18, 10, 18));

        lblRoomTitle = new JLabel("◆ PHÒNG: [Đang tải...]");
        lblRoomTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblRoomTitle.setForeground(new Color(255, 215, 80));
        roomHeader.add(lblRoomTitle);

        lblRoomHost = new JLabel("★ Chỉ huy phòng: [Đang tải...]");
        lblRoomHost.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblRoomHost.setForeground(new Color(180, 210, 245));
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
        inviteBox.setPreferredSize(new Dimension(280, 0));
        inviteBox.setBackgroundColor(new Color(11, 16, 28, 175));
        inviteBox.setBorderColor(new Color(55, 75, 115));
        inviteBox.setAccentColor(new Color(255, 120, 50));
        inviteBox.setBorder(new EmptyBorder(10, 12, 10, 12));

        JLabel lblQuickInvite = new JLabel("✉ MỜI PHI CÔNG ONLINE");
        lblQuickInvite.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblQuickInvite.setForeground(new Color(255, 205, 80));
        inviteBox.add(lblQuickInvite, BorderLayout.NORTH);

        inviteListModel = new DefaultListModel<>();
        inviteList = new JList<>(inviteListModel);
        inviteList.setOpaque(false);
        inviteList.setBackground(new Color(10, 15, 26, 160));
        inviteList.setForeground(Color.WHITE);
        inviteList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        inviteList.setFixedCellHeight(30);
        inviteBox.add(createStyledScrollPane(inviteList), BorderLayout.CENTER);

        GameButton btnSendInvite = new GameButton("✉ GỬI LỜI MỜI", GameButton.Style.CORAL);
        btnSendInvite.setPreferredSize(new Dimension(0, 36));
        btnSendInvite.setFont(new Font("Segoe UI", Font.BOLD, 12));
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
        JPanel roomBottom = new JPanel(new BorderLayout(0, 10));
        roomBottom.setOpaque(false);

        lblCountdownBanner = new JLabel("", SwingConstants.CENTER);
        lblCountdownBanner.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblCountdownBanner.setForeground(new Color(255, 215, 0));
        roomBottom.add(lblCountdownBanner, BorderLayout.NORTH);

        JPanel actionBtns = new JPanel(new GridLayout(1, 2, 16, 0));
        actionBtns.setOpaque(false);

        btnReadyToggle = new GameButton("✓ SẴN SÀNG CHIẾN ĐẤU", GameButton.Style.GREEN);
        btnReadyToggle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnReadyToggle.setPreferredSize(new Dimension(0, 46));
        btnReadyToggle.addActionListener(e -> {
            isMyReady = !isMyReady;
            tcpClient.sendToggleReady(isMyReady);
            updateReadyButtonText();
        });
        actionBtns.add(btnReadyToggle);

        btnLeaveRoom = new GameButton("✕ RỜI PHÒNG CHIẾN", GameButton.Style.RED);
        btnLeaveRoom.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnLeaveRoom.setPreferredSize(new Dimension(0, 46));
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
            emptySlot.setBackgroundColor(new Color(11, 15, 26, 145));
            emptySlot.setBorderColor(new Color(35, 50, 75));
            JLabel emptyLabel = new JLabel("⬚ PHI ĐỘI #" + (i + 1) + " (ĐANG TRỐNG)", SwingConstants.CENTER);
            emptyLabel.setForeground(new Color(85, 110, 145));
            emptyLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            emptySlot.add(emptyLabel, BorderLayout.CENTER);
            slotsPanel.add(emptySlot);
        }
    }

    private void updateReadyButtonText() {
        if (isMyReady) {
            btnReadyToggle.setText("✕ HỦY SẴN SÀNG");
            btnReadyToggle.setStyle(GameButton.Style.GOLD);
        } else {
            btnReadyToggle.setText("✓ SẴN SÀNG CHIẾN ĐẤU");
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
            lblRoomHost.setText("★ Chỉ huy: " + roomInfo.hostName + " | Trạng thái: " + roomInfo.getStatusDisplay());

            // Vẽ lại 4 slots người chơi dạng Card phi thuyền phong cách Starlight
            slotsPanel.removeAll();
            for (int i = 0; i < 4; i++) {
                GameCardPanel slot = new GameCardPanel(new BorderLayout(14, 6));
                slot.setBorder(new EmptyBorder(12, 16, 12, 16));

                if (i < members.size()) {
                    RoomMember m = members.get(i);
                    playerNamesMap.put(m.id, m.name);

                    if (m.id == myPlayerId) {
                        isMyReady = m.isReady;
                        updateReadyButtonText();
                    }

                    if (m.isReady) {
                        slot.setBackgroundColor(new Color(16, 44, 30, 185));
                        slot.setBorderColor(new Color(45, 215, 100));
                        slot.setAccentColor(new Color(45, 215, 100));
                    } else {
                        slot.setBackgroundColor(new Color(14, 22, 38, 175));
                        slot.setBorderColor(new Color(55, 75, 115));
                        slot.setAccentColor(new Color(255, 120, 50));
                    }

                    // Icon phi thuyền đại diện cho slot - nâng kích thước rõ nét
                    BufferedImage shipImg = (i < shipImages.length) ? shipImages[i] : null;
                    if (shipImg != null) {
                        JLabel shipIcon = new JLabel(new ImageIcon(shipImg.getScaledInstance(48, 42, Image.SCALE_SMOOTH)));
                        slot.add(shipIcon, BorderLayout.WEST);
                    }

                    // Thông tin tên và trạng thái
                    JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 4));
                    infoPanel.setOpaque(false);

                    String hostTag = m.isHost ? "👑 " : "🚀 ";
                    JLabel lblName = new JLabel(hostTag + m.name + (m.id == myPlayerId ? " (BẠN)" : ""));
                    lblName.setFont(new Font("Segoe UI", Font.BOLD, 15));
                    lblName.setForeground(m.isHost ? new Color(255, 215, 0) : Color.WHITE);
                    infoPanel.add(lblName);

                    JLabel lblState = new JLabel(m.isReady ? "● ĐÃ SẴN SÀNG" : "○ ĐANG CHUẨN BỊ...");
                    lblState.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    lblState.setForeground(m.isReady ? new Color(80, 245, 140) : new Color(255, 175, 75));
                    infoPanel.add(lblState);

                    slot.add(infoPanel, BorderLayout.CENTER);
                } else {
                    slot.setBackgroundColor(new Color(11, 15, 26, 145));
                    slot.setBorderColor(new Color(35, 50, 75));
                    slot.setAccentColor(null);
                    JLabel emptyLabel = new JLabel("⬚ PHI ĐỘI #" + (i + 1) + " (ĐANG TRỐNG)", SwingConstants.CENTER);
                    emptyLabel.setForeground(new Color(85, 110, 145));
                    emptyLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
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
            dialog.setSize(460, 220);
            dialog.setLocationRelativeTo(this);

            GameCardPanel pnl = new GameCardPanel(new BorderLayout(0, 14));
            pnl.setBackgroundColor(new Color(13, 18, 32, 252));
            pnl.setBorderColor(new Color(255, 120, 40));
            pnl.setAccentColor(new Color(255, 120, 40));
            pnl.setBorder(new EmptyBorder(18, 22, 18, 22));

            JLabel lblTitle = new JLabel("✉ LỜI MỜI THAM CHIẾN PHÒNG ĐẤU", SwingConstants.CENTER);
            lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
            lblTitle.setForeground(new Color(255, 160, 50));
            pnl.add(lblTitle, BorderLayout.NORTH);

            JLabel lblMsg = new JLabel("<html><div style='text-align: center; color: #E0E8F5; font-family: Segoe UI;'>"
                + "Phi công <b style='color: #FFD700; font-size: 15px;'>" + fromName + "</b> muốn mời bạn tham gia đội ngũ:<br><br>"
                + "<span style='font-size: 14px; color: #64FFDA;'>◆ " + roomName + " (Phòng #" + roomId + ")</span>"
                + "</div></html>", SwingConstants.CENTER);
            pnl.add(lblMsg, BorderLayout.CENTER);

            JPanel btnPnl = new JPanel(new GridLayout(1, 2, 14, 0));
            btnPnl.setOpaque(false);

            GameButton btnAccept = new GameButton("✓ CHẤP NHẬN", GameButton.Style.GREEN);
            btnAccept.setPreferredSize(new Dimension(0, 40));
            btnAccept.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnAccept.addActionListener(e -> {
                dialog.dispose();
                tcpClient.sendInviteResponse(roomId, true);
            });

            GameButton btnDecline = new GameButton("✕ TỪ CHỐI", GameButton.Style.DARK);
            btnDecline.setPreferredSize(new Dimension(0, 40));
            btnDecline.setFont(new Font("Segoe UI", Font.BOLD, 13));
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
            if (dashboardView != null) dashboardView.stopAnimation();

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
            if (dashboardView != null) dashboardView.startAnimation();

            rootCardLayout.show(rootPanel, "DASHBOARD");
            showLobbyView();
            revalidate();
            repaint();
        });
    }
}
