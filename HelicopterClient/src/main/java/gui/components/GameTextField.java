package gui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class GameTextField extends JTextField {
    private String placeholder = "";
    private int cornerRadius = 10;
    private boolean isFocused = false;

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
        setBackground(new Color(14, 20, 34, 230));
        setForeground(Color.WHITE);
        setCaretColor(new Color(0, 225, 255));
        setFont(new Font("Consolas", Font.PLAIN, 14));
        setMargin(new Insets(6, 12, 6, 12));

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

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Nền tối bán trong suốt
        g2.setColor(getBackground());
        g2.fillRoundRect(2, 2, w - 4, h - 4, cornerRadius, cornerRadius);

        // 2. Viền phát sáng khi focus hoặc viền xám công nghệ khi bình thường
        if (isFocused) {
            g2.setColor(new Color(0, 210, 255, 75));
            g2.setStroke(new BasicStroke(3.0f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, cornerRadius + 2, cornerRadius + 2);

            g2.setColor(new Color(0, 225, 255));
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawRoundRect(2, 2, w - 5, h - 5, cornerRadius, cornerRadius);
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
            gPlaceholder.setColor(new Color(130, 145, 175, 160));
            gPlaceholder.setFont(getFont().deriveFont(Font.ITALIC));
            Insets insets = getInsets();
            FontMetrics fm = gPlaceholder.getFontMetrics();
            int py = (h - fm.getHeight()) / 2 + fm.getAscent();
            gPlaceholder.drawString(placeholder, insets.left + 2, py);
            gPlaceholder.dispose();
        }
    }
}
