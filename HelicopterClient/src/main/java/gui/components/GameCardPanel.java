package gui.components;

import javax.swing.*;
import java.awt.*;

public class GameCardPanel extends JPanel {
    private int cornerRadius = 14;
    private Color backgroundColor = new Color(16, 22, 38, 225);
    private Color borderColor = new Color(48, 68, 100);
    private Color accentColor = null;

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

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Nền kính mờ
        g2.setColor(backgroundColor);
        g2.fillRoundRect(0, 0, w, h, cornerRadius, cornerRadius);

        // 2. Viền ngoài
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);

        // 3. Đường line accent phía trên (nếu có)
        if (accentColor != null) {
            g2.setColor(accentColor);
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawLine(cornerRadius / 2, 1, w - cornerRadius / 2, 1);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
