package gui.components;

import audio.SoundManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * GameButton - Nút bấm Sci-Fi phong cách Starlight Mobile UI
 * Thiết kế chuẩn theo các hình ảnh Starlight-Mobile-UI:
 * - 4 góc ngoặc phát quang đặc trưng (Corner L-Brackets)
 * - Đa tầng hào quang Neon phát sáng khi Hover
 * - Nền Gradient bán trong suốt công nghệ (Glassmorphism)
 * - Tông màu chính: Coral/Red-Orange, Amber Gold, Lime Green, Electric Cyan, Neon Purple, Dark Slate
 */
public class GameButton extends JButton {
    public enum Style {
        CYAN,    // Xanh điện quang (Thông tin / Thao tác chính)
        GREEN,   // Xanh chanh Starlight Lime (Sẵn sàng / Tạo phòng / Thành công)
        RED,     // Đỏ san hô Starlight Coral (Nguy hiểm / Rời phòng / Hủy)
        CORAL,   // Cam đỏ san hô Starlight nguyên bản
        ORANGE,  // Cam rực rỡ
        GOLD,    // Vàng hổ phách Starlight Amber (Tìm trận / Cảnh báo)
        PURPLE,  // Tím Neon Cyberpunk
        DARK     // Xám đen Stealth (Thứ cấp / Đăng xuất)
    }

    private Style style = Style.CYAN;
    private int cornerRadius = 10;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private boolean soundEnabled = true;
    private boolean showCornerBrackets = true;

    public GameButton(String text) {
        this(text, Style.CYAN);
    }

    public GameButton(String text, Style style) {
        super(text);
        this.style = style;
        initButton();
    }

    private void initButton() {
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setFont(new Font("Segoe UI", Font.BOLD, 13));
        setForeground(Color.WHITE);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    isHovered = true;
                    if (soundEnabled) {
                        SoundManager.getInstance().playHover();
                    }
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    isPressed = true;
                    if (soundEnabled) {
                        if (style == Style.RED || style == Style.CORAL) {
                            SoundManager.getInstance().playCancel();
                        } else {
                            SoundManager.getInstance().playClick();
                        }
                    }
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    public void setStyle(Style style) {
        this.style = style;
        repaint();
    }

    public Style getStyle() {
        return style;
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    public void setCornerBrackets(boolean show) {
        this.showCornerBrackets = show;
        repaint();
    }

    public void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Phối màu theo Style chuẩn Starlight UI
        Color cTop, cBottom, cBorder, cGlow, cBracket;
        switch (style) {
            case GREEN -> {
                // Starlight Lime Green (từ Starlight-Mobile-UI12)
                cTop = isHovered ? new Color(175, 225, 50) : new Color(150, 205, 35);
                cBottom = isHovered ? new Color(115, 170, 20) : new Color(95, 145, 15);
                cBorder = new Color(205, 255, 80);
                cGlow = new Color(170, 230, 45, 115);
                cBracket = new Color(225, 255, 120);
            }
            case RED, CORAL -> {
                // Starlight Coral Red-Orange (từ Starlight-Mobile-UI2 & UI12)
                cTop = isHovered ? new Color(255, 95, 45) : new Color(245, 75, 30);
                cBottom = isHovered ? new Color(195, 42, 16) : new Color(165, 32, 12);
                cBorder = new Color(255, 120, 70);
                cGlow = new Color(255, 80, 35, 115);
                cBracket = new Color(255, 165, 110);
            }
            case ORANGE -> {
                // Vibrant Orange
                cTop = isHovered ? new Color(255, 145, 35) : new Color(245, 120, 20);
                cBottom = isHovered ? new Color(200, 75, 5) : new Color(170, 55, 0);
                cBorder = new Color(255, 175, 75);
                cGlow = new Color(255, 130, 25, 115);
                cBracket = new Color(255, 205, 110);
            }
            case GOLD -> {
                // Starlight Amber Gold (từ Starlight-Mobile-UI12 nút giữa)
                cTop = isHovered ? new Color(255, 185, 30) : new Color(245, 160, 20);
                cBottom = isHovered ? new Color(195, 110, 5) : new Color(165, 90, 0);
                cBorder = new Color(255, 220, 80);
                cGlow = new Color(255, 175, 25, 115);
                cBracket = new Color(255, 235, 120);
            }
            case PURPLE -> {
                // Cyberpunk Neon Violet
                cTop = isHovered ? new Color(200, 85, 255) : new Color(165, 50, 240);
                cBottom = isHovered ? new Color(135, 25, 210) : new Color(105, 15, 170);
                cBorder = new Color(230, 145, 255);
                cGlow = new Color(195, 75, 255, 115);
                cBracket = new Color(245, 180, 255);
            }
            case DARK -> {
                // Dark Slate Glass (Nút phụ / Đăng xuất)
                cTop = isHovered ? new Color(60, 75, 102) : new Color(42, 52, 72);
                cBottom = isHovered ? new Color(34, 42, 60) : new Color(24, 30, 44);
                cBorder = isHovered ? new Color(110, 140, 185) : new Color(70, 90, 125);
                cGlow = new Color(85, 120, 170, 80);
                cBracket = new Color(135, 165, 215);
            }
            default -> { // CYAN (ELECTRIC CYAN)
                cTop = isHovered ? new Color(0, 230, 255) : new Color(0, 185, 245);
                cBottom = isHovered ? new Color(0, 135, 215) : new Color(0, 105, 180);
                cBorder = new Color(105, 240, 255);
                cGlow = new Color(0, 215, 255, 115);
                cBracket = new Color(165, 248, 255);
            }
        }

        if (!isEnabled()) {
            cTop = new Color(55, 60, 72);
            cBottom = new Color(38, 42, 50);
            cBorder = new Color(75, 80, 92);
            cGlow = new Color(0, 0, 0, 0);
            cBracket = new Color(90, 95, 108);
        }

        int offset = (isPressed && isEnabled()) ? 1 : 0;

        // 2. HÀO QUANG PHÁT SÁNG NEON GLOW (ĐA TẦNG KHI HOVER)
        if (isHovered && isEnabled()) {
            // Tầng ngoài khuếch tán mềm
            g2.setColor(new Color(cGlow.getRed(), cGlow.getGreen(), cGlow.getBlue(), 35));
            g2.setStroke(new BasicStroke(5.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRoundRect(1, 1 + offset, w - 3, h - 3, cornerRadius + 2, cornerRadius + 2);

            // Tầng trong tập trung
            g2.setColor(cGlow);
            g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRoundRect(2, 2 + offset, w - 5, h - 5, cornerRadius, cornerRadius);
        }

        // 3. THÂN NÚT GRADIENT KÍNH MỜ
        GradientPaint gp = new GradientPaint(0, offset, cTop, 0, h + offset, cBottom);
        g2.setPaint(gp);
        g2.fillRoundRect(3, 3 + offset, w - 6, h - 6, cornerRadius, cornerRadius);

        // 4. ĐƯỜNG VIỀN SẮC NÉT (NEON OUTLINE)
        g2.setColor(cBorder);
        g2.setStroke(new BasicStroke(1.4f));
        g2.drawRoundRect(3, 3 + offset, w - 7, h - 7, cornerRadius, cornerRadius);

        // 5. HIỆU ỨNG ÁNH SÁNG BÓNG GƯƠNG (GLOSS SHIMMER HIGHLIGHT)
        if (isEnabled()) {
            g2.setColor(new Color(255, 255, 255, isHovered ? 45 : 22));
            g2.fillRoundRect(4, 4 + offset, w - 8, Math.max(2, (h - 6) / 2), cornerRadius - 2, cornerRadius - 2);
        }

        // 6. 4 GÓC NGOẶC STARLIGHT L-BRACKETS ĐẶC TRƯNG (TỪ STARLIGHT MOBILE UI)
        if (showCornerBrackets && isEnabled()) {
            g2.setColor(isHovered ? cBracket : new Color(cBracket.getRed(), cBracket.getGreen(), cBracket.getBlue(), 210));
            g2.setStroke(new BasicStroke(isHovered ? 2.2f : 1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int bLen = Math.min(8, Math.min(w, h) / 3);
            int pad = 2;

            // Góc trên-trái ┌
            g2.drawLine(pad, pad + offset + bLen, pad, pad + offset);
            g2.drawLine(pad, pad + offset, pad + bLen, pad + offset);

            // Góc trên-phải ┐
            g2.drawLine(w - pad - 1, pad + offset + bLen, w - pad - 1, pad + offset);
            g2.drawLine(w - pad - 1, pad + offset, w - pad - 1 - bLen, pad + offset);

            // Góc dưới-trái └
            g2.drawLine(pad, h - pad - 1 + offset - bLen, pad, h - pad - 1 + offset);
            g2.drawLine(pad, h - pad - 1 + offset, pad + bLen, h - pad - 1 + offset);

            // Góc dưới-phải ┘
            g2.drawLine(w - pad - 1, h - pad - 1 + offset - bLen, w - pad - 1, h - pad - 1 + offset);
            g2.drawLine(w - pad - 1, h - pad - 1 + offset, w - pad - 1 - bLen, h - pad - 1 + offset);
        }

        // 7. VẼ NỘI DUNG CHỮ CĂN GIỮA VỚI BÓNG ĐỔ VÀ HÀO QUANG
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(getText());
        int textH = fm.getAscent();
        int tx = (w - textW) / 2;
        int ty = (h + textH) / 2 - 2 + offset;

        // Bóng chữ mờ đậm
        g2.setColor(new Color(0, 0, 0, 185));
        g2.drawString(getText(), tx + 1, ty + 1);

        // Hào quang chữ khi hover
        if (isHovered && isEnabled()) {
            g2.setColor(new Color(cBorder.getRed(), cBorder.getGreen(), cBorder.getBlue(), 65));
            g2.drawString(getText(), tx - 1, ty);
            g2.drawString(getText(), tx + 1, ty);
        }

        // Chữ chính
        g2.setColor(isEnabled() ? getForeground() : new Color(135, 140, 150));
        g2.drawString(getText(), tx, ty);

        g2.dispose();
    }
}
