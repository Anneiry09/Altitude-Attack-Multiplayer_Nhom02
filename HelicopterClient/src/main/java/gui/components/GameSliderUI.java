package gui.components;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;

/**
 * GameSliderUI - Thanh trượt phong cách Starlight Sci-Fi
 * Sử dụng thanh trượt màu cam/hổ phách với hiệu ứng phát quang và con chạy góc cạnh
 */
public class GameSliderUI extends BasicSliderUI {
    private Color trackBg = new Color(20, 28, 48);
    private Color trackFilled = new Color(255, 120, 50);
    private Color thumbColor = new Color(255, 160, 60);

    public GameSliderUI(JSlider b) {
        super(b);
    }

    @Override
    public void paintTrack(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cy = trackRect.y + (trackRect.height / 2);
        int trackHeight = 6;
        int arc = 6;

        // Track nền
        g2.setColor(trackBg);
        g2.fillRoundRect(trackRect.x, cy - (trackHeight / 2), trackRect.width, trackHeight, arc, arc);

        // Track đã kéo (fill)
        int fillW = thumbRect.x + (thumbRect.width / 2) - trackRect.x;
        if (fillW > 0) {
            g2.setColor(trackFilled);
            g2.fillRoundRect(trackRect.x, cy - (trackHeight / 2), fillW, trackHeight, arc, arc);
        }

        // Viền thanh
        g2.setColor(new Color(55, 80, 120));
        g2.drawRoundRect(trackRect.x, cy - (trackHeight / 2), trackRect.width, trackHeight, arc, arc);

        g2.dispose();
    }

    @Override
    public void paintThumb(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int x = thumbRect.x;
        int y = thumbRect.y;
        int w = thumbRect.width;
        int h = thumbRect.height;

        // Quầng phát quang mờ
        g2.setColor(new Color(255, 120, 50, 80));
        g2.fillOval(x - 2, y - 2, w + 4, h + 4);

        // Con chạy (Thumb)
        g2.setColor(thumbColor);
        g2.fillOval(x, y, w, h);

        // Tâm sáng trắng
        g2.setColor(Color.WHITE);
        g2.fillOval(x + w / 4, y + h / 4, w / 2, h / 2);

        // Viền
        g2.setColor(new Color(255, 220, 120));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(x, y, w, h);

        g2.dispose();
    }

    @Override
    protected Dimension getThumbSize() {
        return new Dimension(16, 16);
    }
}
