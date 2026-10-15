package gui;

import audio.SoundManager;
import gui.components.GameButton;
import gui.components.GameCardPanel;
import gui.components.GameSliderUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Consumer;

/**
 * SettingsDialog - Hộp thoại cài đặt hệ thống (Âm thanh, Đổi tên, Đăng xuất)
 * Tách biệt khỏi LobbyFrame để code gọn gàng, module hóa theo nguyên tắc Clean Code.
 */
public class SettingsDialog {

    public static void show(
        JFrame parent,
        String currentName,
        byte playerId,
        Consumer<String> onRename,
        Runnable onLogout
    ) {
        JDialog dialog = new JDialog(parent, "Cài đặt hệ thống", true);
        dialog.setUndecorated(true);
        dialog.setSize(460, 360);
        dialog.setLocationRelativeTo(parent);

        GameCardPanel rootCard = new GameCardPanel(new BorderLayout());
        rootCard.setBackgroundColor(new Color(10, 14, 25, 250));
        rootCard.setBorderColor(new Color(55, 75, 115));
        rootCard.setAccentColor(new Color(255, 120, 50));
        rootCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Header Dialog
        JPanel headerPnl = new JPanel(new BorderLayout());
        headerPnl.setOpaque(false);

        JLabel lblTitle = new JLabel("⚙ CÀI ĐẶT HỆ THỐNG", SwingConstants.LEFT);
        lblTitle.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
        lblTitle.setForeground(new Color(255, 180, 80));
        headerPnl.add(lblTitle, BorderLayout.WEST);

        GameButton btnClose = new GameButton("✕", GameButton.Style.DARK);
        btnClose.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
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
        btnMusicMenu.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
        btnMusicMenu.setPreferredSize(new Dimension(0, 48));

        // Mục 2: THÔNG TIN PHI CÔNG & ĐỔI TÊN
        final String[] nameHolder = new String[]{currentName};
        GameButton btnProfileMenu = new GameButton("👤 ĐỔI TÊN PHI CÔNG (" + nameHolder[0] + ")", GameButton.Style.DARK);
        btnProfileMenu.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
        btnProfileMenu.setPreferredSize(new Dimension(0, 48));
        btnProfileMenu.addActionListener(e -> {
            String newName = JOptionPane.showInputDialog(dialog, "Nhập tên phi công mới của bạn:", nameHolder[0]);
            if (newName != null && !newName.trim().isEmpty() && !newName.trim().equals(nameHolder[0])) {
                String trimmed = newName.trim();
                nameHolder[0] = trimmed;
                btnProfileMenu.setText("👤 ĐỔI TÊN PHI CÔNG (" + trimmed + ")");
                SoundManager.getInstance().playClick();
                if (onRename != null) {
                    onRename.accept(trimmed);
                }
            }
        });

        // Mục 3: ĐĂNG XUẤT
        GameButton btnLogoutMenu = new GameButton("✕ ĐĂNG XUẤT KHỎI SERVER", GameButton.Style.RED);
        btnLogoutMenu.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
        btnLogoutMenu.setPreferredSize(new Dimension(0, 48));
        btnLogoutMenu.addActionListener(e -> {
            dialog.dispose();
            if (onLogout != null) {
                onLogout.run();
            }
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

        // Thanh kéo âm lượng BGM
        JPanel volRow = new JPanel(new BorderLayout(10, 0));
        volRow.setOpaque(false);
        JLabel lblVolTitle = new JLabel("Âm lượng:");
        lblVolTitle.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
        lblVolTitle.setForeground(new Color(220, 230, 245));
        lblVolTitle.setPreferredSize(new Dimension(80, 26));
        volRow.add(lblVolTitle, BorderLayout.WEST);

        JSlider volSlider = new JSlider(0, 100, (int) (SoundManager.getInstance().getBgmVolume() * 100));
        volSlider.setOpaque(false);
        volSlider.setUI(new GameSliderUI(volSlider));
        JLabel lblVolVal = new JLabel(volSlider.getValue() + "%", SwingConstants.RIGHT);
        lblVolVal.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
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
        btnMuteToggle.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
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
        lblSongTitle.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
        lblSongTitle.setForeground(new Color(220, 230, 245));
        lblSongTitle.setPreferredSize(new Dimension(80, 26));
        songRow.add(lblSongTitle, BorderLayout.WEST);

        String[] bgmTracks = new String[]{"Darkwave.wav", "OverworldDay.wav"};
        JComboBox<String> comboTracks = new JComboBox<>(bgmTracks);
        comboTracks.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
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
        btnBackToMain.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
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
}
