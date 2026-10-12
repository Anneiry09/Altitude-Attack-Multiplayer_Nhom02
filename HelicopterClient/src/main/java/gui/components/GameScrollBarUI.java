package gui.components;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public class GameScrollBarUI extends BasicScrollBarUI {
    private static final int THUMB_SIZE = 8;
    private static final Color TRACK_COLOR = new Color(12, 17, 30, 200);
    private static final Color THUMB_NORMAL = new Color(50, 72, 105);
    private static final Color THUMB_HOVER = new Color(75, 115, 170);

    @Override
    protected void configureScrollBarColors() {
        this.thumbColor = THUMB_NORMAL;
        this.trackColor = TRACK_COLOR;
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createZeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createZeroButton();
    }

    private JButton createZeroButton() {
        JButton btn = new JButton();
        btn.setPreferredSize(new Dimension(0, 0));
        btn.setMinimumSize(new Dimension(0, 0));
        btn.setMaximumSize(new Dimension(0, 0));
        return btn;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(TRACK_COLOR);
        g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        g2.dispose();
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color col = isThumbRollover() ? THUMB_HOVER : THUMB_NORMAL;
        g2.setColor(col);

        int arc = 6;
        int x = thumbBounds.x + 2;
        int y = thumbBounds.y + 2;
        int w = Math.max(4, thumbBounds.width - 4);
        int h = Math.max(4, thumbBounds.height - 4);

        g2.fillRoundRect(x, y, w, h, arc, arc);
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return new Dimension(THUMB_SIZE, THUMB_SIZE);
    }
}
