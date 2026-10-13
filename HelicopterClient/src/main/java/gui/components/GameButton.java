package gui.components;

import audio.SoundManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GameButton extends JButton {
    public enum Style {
        CYAN,    // Màu chủ đạo (Neon Cyan)
        GREEN,   // Sẵn sàng, Thành công (Neon Green)
        RED,     // Rời phòng, Nguy hiểm (Neon Red)
        GOLD,    // Tìm trận, Cảnh báo (Neon Gold)
        DARK     // Thứ cấp, Nút tối (Dark Slate)
    }

    private Style style = Style.CYAN;
    private int cornerRadius = 12;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private boolean soundEnabled = true;

    public GameButton(String text) {
        this(text, Style.CYAN);
    }

    public GameButton(String text, Icon icon, Style style) {
        super(text, icon);
        this.style = style;
        initButton();
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
        setFont(new Font("Arial", Font.BOLD, 13));
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
                        if (style == Style.RED) {
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

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
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

        // 1. Phối màu theo Style
        Color cTop, cBottom, cBorder, cGlow;
        switch (style) {
            case GREEN -> {
                cTop = isHovered ? new Color(46, 235, 120) : new Color(34, 190, 95);
                cBottom = isHovered ? new Color(20, 160, 75) : new Color(14, 125, 60);
                cBorder = new Color(70, 255, 145);
                cGlow = new Color(46, 235, 120, 90);
            }
            case RED -> {
                cTop = isHovered ? new Color(255, 85, 100) : new Color(230, 55, 75);
                cBottom = isHovered ? new Color(185, 30, 50) : new Color(150, 20, 38);
                cBorder = new Color(255, 120, 135);
                cGlow = new Color(255, 75, 95, 90);
            }
            case GOLD -> {
                cTop = isHovered ? new Color(255, 185, 30) : new Color(240, 155, 15);
                cBottom = isHovered ? new Color(195, 110, 5) : new Color(160, 85, 0);
                cBorder = new Color(255, 215, 75);
                cGlow = new Color(255, 180, 25, 90);
            }
            case DARK -> {
                cTop = isHovered ? new Color(65, 80, 110) : new Color(45, 55, 75);
                cBottom = isHovered ? new Color(38, 48, 68) : new Color(25, 32, 46);
                cBorder = isHovered ? new Color(100, 130, 175) : new Color(60, 80, 110);
                cGlow = new Color(80, 115, 165, 60);
            }
            default -> { // CYAN (PRIMARY)
                cTop = isHovered ? new Color(0, 225, 255) : new Color(0, 175, 240);
                cBottom = isHovered ? new Color(0, 130, 210) : new Color(0, 95, 170);
                cBorder = new Color(100, 235, 255);
                cGlow = new Color(0, 210, 255, 95);
            }
        }

        if (!isEnabled()) {
            cTop = new Color(60, 65, 75);
            cBottom = new Color(40, 45, 52);
            cBorder = new Color(80, 85, 95);
            cGlow = new Color(0, 0, 0, 0);
        }

        int offset = (isPressed && isEnabled()) ? 1 : 0;

        // 2. Viền phát sáng Neon Glow (khi hover)
        if (isHovered && isEnabled()) {
            g2.setColor(cGlow);
            g2.setStroke(new BasicStroke(3.5f));
            g2.drawRoundRect(1, 1 + offset, w - 3, h - 3, cornerRadius + 2, cornerRadius + 2);
        }

        // 3. Nền Gradient
        GradientPaint gp = new GradientPaint(0, offset, cTop, 0, h + offset, cBottom);
        g2.setPaint(gp);
        g2.fillRoundRect(2, 2 + offset, w - 4, h - 4, cornerRadius, cornerRadius);

        // 4. Viền sắc nét (Border)
        g2.setColor(cBorder);
        g2.setStroke(new BasicStroke(1.4f));
        g2.drawRoundRect(2, 2 + offset, w - 5, h - 5, cornerRadius, cornerRadius);

        // 5. Hiệu ứng ánh sáng bóng gương trên đỉnh nút (Gloss highlight)
        if (isEnabled()) {
            g2.setColor(new Color(255, 255, 255, isHovered ? 45 : 25));
            g2.fillRoundRect(3, 3 + offset, w - 6, (h - 4) / 2, cornerRadius - 2, cornerRadius - 2);
        }

        // 6. Vẽ Text và Icon căn giữa
        Icon icon = getIcon();
        String text = getText() != null ? getText() : "";
        FontMetrics fm = g2.getFontMetrics();
        int textW = !text.isEmpty() ? fm.stringWidth(text) : 0;
        int textH = fm.getAscent();

        int iconW = (icon != null) ? icon.getIconWidth() : 0;
        int iconH = (icon != null) ? icon.getIconHeight() : 0;
        int gap = (icon != null && !text.isEmpty()) ? 8 : 0;
        int totalContentW = iconW + gap + textW;

        int startX = (w - totalContentW) / 2;
        if (icon != null) {
            int iconY = (h - iconH) / 2 + offset;
            icon.paintIcon(this, g2, startX, iconY);
        }

        if (!text.isEmpty()) {
            int tx = startX + iconW + gap;
            int ty = (h + textH) / 2 - 2 + offset;

            // Bóng chữ mờ
            g2.setColor(new Color(0, 0, 0, 160));
            g2.drawString(text, tx + 1, ty + 1);

            // Chữ chính
            g2.setColor(isEnabled() ? getForeground() : new Color(140, 145, 155));
            g2.drawString(text, tx, ty);
        }

        g2.dispose();
    }
}
