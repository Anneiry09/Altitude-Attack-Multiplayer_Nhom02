package gui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * GameTextField - Ô nhập liệu Sci-Fi phong cách Starlight
 * Bo góc, viền phát sáng Neon khi Focus, hỗ trợ Placeholder và 4 góc ngoặc công nghệ.
 */
public class GameTextField extends JTextField {
    private String placeholder = "";
    private int cornerRadius = 10;
    private boolean isFocused = false;
    private Color focusColor = new Color(255, 120, 50); // Starlight Coral/Orange Glow

    public GameTextField() {
        this("", "");
    }

    public GameTextField(String text) {
        this(text, "");
    }

    public GameTextField(String text, String placeholder) {
        super(text);
        this.placeholder = placeholder;
        initField();
    }

    private void initField() {
        setOpaque(false);
        setBackground(new Color(11, 15, 26, 235));
        setForeground(Color.WHITE);
        setCaretColor(new Color(255, 150, 75));
        setFont(new Font("Segoe UI", Font.PLAIN, 14));
        setMargin(new Insets(6, 14, 6, 14));

        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                isFocused = true;
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                isFocused = false;
                repaint();
            }
        });
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    public void setFocusColor(Color color) {
        this.focusColor = color;
        setCaretColor(color);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Nền tối bán trong suốt sâu thẳm
        g2.setColor(getBackground());
        g2.fillRoundRect(2, 2, w - 4, h - 4, cornerRadius, cornerRadius);

        // 2. Viền phát sáng khi focus hoặc viền xám công nghệ khi bình thường
        if (isFocused) {
            // Hào quang Neon mờ ngoài
            g2.setColor(new Color(focusColor.getRed(), focusColor.getGreen(), focusColor.getBlue(), 80));
            g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRoundRect(1, 1, w - 3, h - 3, cornerRadius + 2, cornerRadius + 2);

            // Viền chính
            g2.setColor(focusColor);
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawRoundRect(2, 2, w - 5, h - 5, cornerRadius, cornerRadius);

            // 4 Góc ngoặc Starlight khi focus
            int bLen = 8;
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(255, 200, 140));
            g2.drawLine(2, 2 + bLen, 2, 2);
            g2.drawLine(2, 2, 2 + bLen, 2);
            g2.drawLine(w - 4, 2 + bLen, w - 4, 2);
            g2.drawLine(w - 4, 2, w - 4 - bLen, 2);
            g2.drawLine(2, h - 4 - bLen, 2, h - 4);
            g2.drawLine(2, h - 4, 2 + bLen, h - 4);
            g2.drawLine(w - 4, h - 4 - bLen, w - 4, h - 4);
            g2.drawLine(w - 4, h - 4, w - 4 - bLen, h - 4);
        } else {
            g2.setColor(new Color(55, 75, 105));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(2, 2, w - 5, h - 5, cornerRadius, cornerRadius);
        }

        g2.dispose();
        super.paintComponent(g);

        // 3. Vẽ placeholder text nếu đang trống
        if (getText().isEmpty() && !isFocused && placeholder != null && !placeholder.isEmpty()) {
            Graphics2D gPlaceholder = (Graphics2D) g.create();
            gPlaceholder.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            gPlaceholder.setColor(new Color(140, 155, 185, 170));
            gPlaceholder.setFont(getFont().deriveFont(Font.ITALIC));
            Insets insets = getInsets();
            FontMetrics fm = gPlaceholder.getFontMetrics();
            int py = (h - fm.getHeight()) / 2 + fm.getAscent();
            gPlaceholder.drawString(placeholder, insets.left + 2, py);
            gPlaceholder.dispose();
        }
    }
}
