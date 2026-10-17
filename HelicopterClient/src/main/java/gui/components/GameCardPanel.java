package gui.components;

import javax.swing.*;
import java.awt.*;

/**
 * GameCardPanel - Khung thẻ Sci-Fi phong cách Starlight Mobile UI
 * Tích hợp 4 góc ngoặc công nghệ (Corner L-Brackets) phát sáng đồng bộ với GameButton.
 */
public class GameCardPanel extends JPanel {
    private int cornerRadius = 14;
    private Color backgroundColor = new Color(14, 18, 30, 230);
    private Color borderColor = new Color(48, 68, 100);
    private Color accentColor = null;
    private boolean showCornerBrackets = true;

    public GameCardPanel() {
        this(new FlowLayout());
    }

    public GameCardPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
        repaint();
    }

    public void setBorderColor(Color color) {
        this.borderColor = color;
        repaint();
    }

    public void setAccentColor(Color color) {
        this.accentColor = color;
        repaint();
    }

    public void setCornerBrackets(boolean show) {
        this.showCornerBrackets = show;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Nền kính mờ bán trong suốt (Glassmorphism)
        g2.setColor(backgroundColor);
        g2.fillRoundRect(2, 2, w - 4, h - 4, cornerRadius, cornerRadius);

        // 2. Viền ngoài mảnh công nghệ
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(2, 2, w - 5, h - 5, cornerRadius, cornerRadius);

        // 3. Đường line accent phía trên (nếu có)
        if (accentColor != null) {
            g2.setColor(accentColor);
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawLine(cornerRadius + 4, 2, w - cornerRadius - 4, 2);
        }

        // 4. 4 Góc ngoặc Starlight L-Brackets phát quang đặc trưng
        if (showCornerBrackets) {
            Color bracketCol = accentColor != null ? accentColor : borderColor;
            g2.setColor(new Color(bracketCol.getRed(), bracketCol.getGreen(), bracketCol.getBlue(), 220));
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int bLen = 12;
            int pad = 2;

            // Top-left ┌
            g2.drawLine(pad, pad + bLen, pad, pad);
            g2.drawLine(pad, pad, pad + bLen, pad);

            // Top-right ┐
            g2.drawLine(w - pad - 1, pad + bLen, w - pad - 1, pad);
            g2.drawLine(w - pad - 1, pad, w - pad - 1 - bLen, pad);

            // Bottom-left └
            g2.drawLine(pad, h - pad - 1 - bLen, pad, h - pad - 1);
            g2.drawLine(pad, h - pad - 1, pad + bLen, h - pad - 1);

            // Bottom-right ┘
            g2.drawLine(w - pad - 1, h - pad - 1 - bLen, w - pad - 1, h - pad - 1);
            g2.drawLine(w - pad - 1, h - pad - 1, w - pad - 1 - bLen, h - pad - 1);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
