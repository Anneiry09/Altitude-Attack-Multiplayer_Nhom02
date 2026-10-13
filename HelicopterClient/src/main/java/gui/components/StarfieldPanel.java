package gui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * StarfieldPanel - Màn hình nền không gian vũ trụ phong cách Starlight Sci-Fi
 * Tái hiện chính xác tông màu và hiệu ứng từ các hình ảnh Starlight-Mobile-UI:
 * - Nền đen vũ trụ sâu thẳm với quầng sáng tinh vân (Nebula) màu cam/hổ phách ở góc trên bên phải
 * - Hiệu ứng Falling Stars Particle System rơi liên tục theo nhịp 60 FPS
 * - Tương tác chuột 2D đa hướng linh hoạt:
 *   + Di chuột qua phải/trái: các ngôi sao trôi nghiêng theo hướng ngang của chuột.
 *   + Di chuột lên trên: tạo lực nâng nhẹ đẩy các ngôi sao trôi ngược lên trên trong khi vẫn giữ xu hướng rơi.
 *   + Di chuột xuống dưới: tăng tốc độ rơi cuốn theo chuyển động chuột.
 *   + Khi ngừng di chuột: toàn bộ lực đẩy theo các hướng giảm dần mượt mà (smooth decay)
 *     và các ngôi sao trở về tốc độ rơi thẳng đứng bình thường.
 * - Khung tiêu đề công nghệ (Tech Frame) viền Neon phát sáng đặc trưng
 */
public class StarfieldPanel extends JPanel {
    private static class Star {
        float x, y;
        float speed;
        float size;
        float baseAlpha;
        float currentAlpha;
        float twinkleSpeed;
        boolean twinkleUp;
        Color color;
        float driftX;
    }

    private final List<Star> stars = new ArrayList<>();
    private final Random random = new Random();
    private Timer animationTimer;
    private final int starCount = 140;

    // Tương tác chuột 2D và độ trôi quán tính (2D Mouse Drift Physics)
    private int lastScreenX = -1;
    private int lastScreenY = -1;
    private float currentDriftVelocityX = 0.0f;
    private float targetDriftVelocityX = 0.0f;
    private float currentDriftVelocityY = 0.0f;
    private float targetDriftVelocityY = 0.0f;
    private long lastMouseMoveTime = 0;
    private AWTEventListener awtMouseListener = null;

    // Tiêu đề hiển thị
    private String titleText = "ALTITUDE ATTACK";
    private String subtitleText = "SPACE HELICOPTER MULTIPLAYER ARENA • GIAO THỨC UDP/TCP";
    private boolean showTitleBanner = true;

    public StarfieldPanel() {
        this(null);
    }

    public StarfieldPanel(LayoutManager layout) {
        super(layout);
        setOpaque(true);
        setBackground(new Color(7, 10, 16));
        initStars();
        initMouseInteractions();
        initTimer();
    }

    private void initStars() {
        stars.clear();
        Color[] palette = new Color[]{
            Color.WHITE,
            new Color(255, 235, 195), // Ánh vàng kim ấm
            new Color(255, 185, 140), // Ánh cam san hô Starlight
            new Color(195, 235, 255), // Ánh xanh băng
            new Color(225, 245, 255)  // Ánh trắng xanh
        };

        for (int i = 0; i < starCount; i++) {
            Star s = new Star();
            s.x = random.nextFloat() * 1000;
            s.y = random.nextFloat() * 800;
            // Phân tầng tốc độ: sao nhỏ ở xa rơi chậm, sao to ở gần rơi nhanh hơn
            float layer = random.nextFloat();
            if (layer < 0.6f) {
                // Tầng xa (60%): nhỏ, mờ, rơi chậm
                s.size = 0.9f + random.nextFloat() * 0.8f;
                s.speed = 0.25f + random.nextFloat() * 0.45f;
                s.baseAlpha = 0.25f + random.nextFloat() * 0.4f;
            } else if (layer < 0.9f) {
                // Tầng trung (30%): vừa, rơi tốc độ trung bình
                s.size = 1.7f + random.nextFloat() * 0.9f;
                s.speed = 0.65f + random.nextFloat() * 0.65f;
                s.baseAlpha = 0.55f + random.nextFloat() * 0.35f;
            } else {
                // Tầng gần (10%): sao to, sáng rực rỡ, lướt nhanh
                s.size = 2.6f + random.nextFloat() * 1.2f;
                s.speed = 1.25f + random.nextFloat() * 0.95f;
                s.baseAlpha = 0.8f + random.nextFloat() * 0.2f;
            }
            s.currentAlpha = s.baseAlpha;
            s.twinkleSpeed = 0.008f + random.nextFloat() * 0.025f;
            s.twinkleUp = random.nextBoolean();
            s.color = palette[random.nextInt(palette.length)];
            s.driftX = (random.nextFloat() - 0.5f) * 0.12f;
            stars.add(s);
        }
    }

    private void initMouseInteractions() {
        // Lắng nghe chuột trực tiếp trên StarfieldPanel
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleMouseMove(e.getXOnScreen(), e.getYOnScreen());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseMove(e.getXOnScreen(), e.getYOnScreen());
            }
        });
    }

    /**
     * Xử lý chuyển động chuột 2D: Tính toán cả xung lực ngang (dx) và xung lực dọc (dy)
     * - dy < 0 (chuột đẩy lên): tạo xung lực âm đẩy các ngôi sao nhẹ nhàng trôi ngược lên trên
     * - dy > 0 (chuột kéo xuống): tạo xung lực dương đẩy các ngôi sao rơi nhanh hơn
     * - dx: tạo xung lực trôi ngang sang trái/phải
     */
    private void handleMouseMove(int screenX, int screenY) {
        if (lastScreenX != -1 && lastScreenY != -1) {
            int dx = screenX - lastScreenX;
            int dy = screenY - lastScreenY;

            if (dx != 0 || dy != 0) {
                // Xung lực ngang nhẹ nhàng (tinh chỉnh chậm và êm ái)
                float impulseX = Math.max(-2.0f, Math.min(2.0f, dx * 0.055f));
                targetDriftVelocityX += impulseX;
                targetDriftVelocityX = Math.max(-1.8f, Math.min(1.8f, targetDriftVelocityX));

                // Xung lực dọc nhẹ nhàng (đẩy/kéo chậm và tự nhiên)
                float impulseY = Math.max(-2.0f, Math.min(2.0f, dy * 0.055f));
                targetDriftVelocityY += impulseY;
                targetDriftVelocityY = Math.max(-1.8f, Math.min(1.8f, targetDriftVelocityY));

                lastMouseMoveTime = System.currentTimeMillis();
            }
        }
        lastScreenX = screenX;
        lastScreenY = screenY;
    }

    private void initTimer() {
        // Vòng lặp chuyển động ~60 FPS (16ms)
        animationTimer = new Timer(16, e -> updateStars());
    }

    public void startAnimation() {
        if (animationTimer != null && !animationTimer.isRunning()) {
            animationTimer.start();
        }
    }

    public void stopAnimation() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        startAnimation();

        // Đăng ký AWTEventListener toàn cục để bắt được chuyển động chuột ngay cả khi chuột
        // đang rê trên các component con như ô nhập liệu (GameTextField) hay nút bấm (GameButton)
        try {
            if (awtMouseListener == null) {
                awtMouseListener = event -> {
                    if (!isShowing()) return;
                    if (event instanceof MouseEvent me) {
                        int id = me.getID();
                        if (id == MouseEvent.MOUSE_MOVED || id == MouseEvent.MOUSE_DRAGGED) {
                            handleMouseMove(me.getXOnScreen(), me.getYOnScreen());
                        }
                    }
                };
                Toolkit.getDefaultToolkit().addAWTEventListener(awtMouseListener, AWTEvent.MOUSE_MOTION_EVENT_MASK);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        stopAnimation();

        // Hủy listener khi panel không còn hiển thị
        try {
            if (awtMouseListener != null) {
                Toolkit.getDefaultToolkit().removeAWTEventListener(awtMouseListener);
                awtMouseListener = null;
            }
        } catch (Exception ignored) {}
    }

    private void updateStars() {
        int w = getWidth() > 0 ? getWidth() : 880;
        int h = getHeight() > 0 ? getHeight() : 640;
        long now = System.currentTimeMillis();

        // 1. XỬ LÝ QUÁN TÍNH CHUỘT 2D (Triệt tiêu êm dịu khi chuột dừng):
        if (now - lastMouseMoveTime > 40) {
            targetDriftVelocityX *= 0.85f;
            if (Math.abs(targetDriftVelocityX) < 0.01f) {
                targetDriftVelocityX = 0.0f;
            }

            targetDriftVelocityY *= 0.85f;
            if (Math.abs(targetDriftVelocityY) < 0.01f) {
                targetDriftVelocityY = 0.0f;
            }
        }

        // Nội suy mượt mà vận tốc hiện tại bám theo target (Smooth Damping)
        currentDriftVelocityX += (targetDriftVelocityX - currentDriftVelocityX) * 0.14f;
        if (Math.abs(currentDriftVelocityX) < 0.005f) {
            currentDriftVelocityX = 0.0f;
        }

        currentDriftVelocityY += (targetDriftVelocityY - currentDriftVelocityY) * 0.14f;
        if (Math.abs(currentDriftVelocityY) < 0.005f) {
            currentDriftVelocityY = 0.0f;
        }

        // 2. CẬP NHẬT TỌA ĐỘ VÀ TRẠNG THÁI TỪNG NGÔI SAO
        for (Star s : stars) {
            // Hệ số thị sai chiều sâu 3D êm ái
            float depthFactor = Math.max(0.35f, s.speed * 0.70f);

            // Vận tốc Y: Tốc độ rơi tự nhiên + Lực đẩy/kéo nhẹ theo chiều dọc của chuột
            s.y += s.speed + (currentDriftVelocityY * depthFactor);

            // Vận tốc X: Độ trôi tự nhiên + Độ trôi êm ái theo chuột
            s.x += s.driftX + (currentDriftVelocityX * depthFactor);

            // Nhấp nháy nhẹ nhàng
            if (s.twinkleUp) {
                s.currentAlpha += s.twinkleSpeed;
                if (s.currentAlpha >= Math.min(1.0f, s.baseAlpha + 0.3f)) {
                    s.twinkleUp = false;
                }
            } else {
                s.currentAlpha -= s.twinkleSpeed;
                if (s.currentAlpha <= Math.max(0.15f, s.baseAlpha - 0.25f)) {
                    s.twinkleUp = true;
                }
            }

            // Bao phủ biên dọc (Wrap-around khi rơi hết đáy hoặc bị đẩy ngược lên trên mép đỉnh)
            if (s.y > h + 15) {
                s.y = -random.nextFloat() * 15;
                s.x = random.nextFloat() * w;
            } else if (s.y < -20) {
                s.y = h + random.nextFloat() * 15;
                s.x = random.nextFloat() * w;
            }

            // Bao phủ biên ngang (Wrap-around ngang khi sao bị lệch)
            if (s.x < -20) s.x = w + 15;
            if (s.x > w + 20) s.x = -15;
        }

        repaint();
    }

    public void setTitleText(String title, String subtitle) {
        this.titleText = title;
        this.subtitleText = subtitle;
        repaint();
    }

    public void setShowTitleBanner(boolean show) {
        this.showTitleBanner = show;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. NỀN VŨ TRỤ SÂU THẲM (DEEP SPACE BACKGROUND)
        g2.setColor(new Color(7, 10, 16));
        g2.fillRect(0, 0, w, h);

        // 2. QUẦNG SÁNG TINH VÂN (NEBULA GLOW) Ở GÓC TRÊN BÊN PHẢI (Mô phỏng Starlight UI)
        float nebRadius = Math.max(w, h) * 0.72f;
        Point2D nebCenter = new Point2D.Float(w * 0.88f, h * 0.12f);
        RadialGradientPaint nebulaOrange = new RadialGradientPaint(
            nebCenter,
            nebRadius,
            new float[]{0.0f, 0.32f, 0.70f, 1.0f},
            new Color[]{
                new Color(135, 48, 20, 115),  // Cam san hô rực rỡ tâm điểm
                new Color(85, 26, 12, 65),    // Hổ phách thẫm lan tỏa
                new Color(35, 14, 18, 25),    // Đỏ tím không gian mờ
                new Color(7, 10, 16, 0)       // Tiêu biến vào màu đen vũ trụ
            }
        );
        g2.setPaint(nebulaOrange);
        g2.fillRect(0, 0, w, h);

        // Quầng sáng tinh vân thứ cấp xanh thẫm ở góc dưới bên trái tạo chiều sâu vũ trụ
        Point2D nebCenter2 = new Point2D.Float(w * 0.12f, h * 0.88f);
        RadialGradientPaint nebulaBlue = new RadialGradientPaint(
            nebCenter2,
            Math.max(w, h) * 0.55f,
            new float[]{0.0f, 0.5f, 1.0f},
            new Color[]{
                new Color(18, 35, 65, 80),
                new Color(10, 18, 35, 30),
                new Color(7, 10, 16, 0)
            }
        );
        g2.setPaint(nebulaBlue);
        g2.fillRect(0, 0, w, h);

        // 3. VẼ HỆ THỐNG CÁC NGÔI SAO RƠI TỪ TỪ (KÈM HIỆU ỨNG TƯƠNG TÁC 2D THEO CHUỘT)
        for (Star s : stars) {
            int alpha = (int) (Math.max(0.0f, Math.min(1.0f, s.currentAlpha)) * 255);
            g2.setColor(new Color(s.color.getRed(), s.color.getGreen(), s.color.getBlue(), alpha));
            g2.fill(new Ellipse2D.Float(s.x, s.y, s.size, s.size));

            // Hiệu ứng hào quang cho những ngôi sao lớn
            if (s.size >= 2.4f) {
                g2.setColor(new Color(s.color.getRed(), s.color.getGreen(), s.color.getBlue(), alpha / 4));
                g2.fill(new Ellipse2D.Float(s.x - s.size * 0.7f, s.y - s.size * 0.7f, s.size * 2.4f, s.size * 2.4f));
            }
        }

        // 4. VẼ KHUNG TIÊU ĐỀ STARLIGHT SCI-FI ĐẲNG CẤP Ở ĐỈNH MÀN HÌNH (NẾU ĐƯỢC BẬT)
        if (showTitleBanner) {
            drawStarlightTitleBanner(g2, w);
        }

        g2.dispose();
    }

    private void drawStarlightTitleBanner(Graphics2D g2, int w) {
        int bannerW = 540;
        int bannerH = 68;
        int bannerX = (w - bannerW) / 2;
        int bannerY = 36;

        // Khung viền hình học vát góc phong cách Starlight Mobile UI2
        Polygon frame = new Polygon();
        int cut = 14;
        int midPeak = 8;
        // Điểm đỉnh nóc với chóp nhọn nhỏ ở giữa
        frame.addPoint(bannerX + cut, bannerY);
        frame.addPoint(bannerX + bannerW / 2 - 40, bannerY);
        frame.addPoint(bannerX + bannerW / 2, bannerY - midPeak); // Nhô nhẹ lên ở giữa
        frame.addPoint(bannerX + bannerW / 2 + 40, bannerY);
        frame.addPoint(bannerX + bannerW - cut, bannerY);
        // Cạnh phải vát chéo
        frame.addPoint(bannerX + bannerW, bannerY + cut);
        frame.addPoint(bannerX + bannerW, bannerY + bannerH - cut);
        frame.addPoint(bannerX + bannerW - cut, bannerY + bannerH);
        // Đáy với chóp nhọn nhỏ hướng xuống ở giữa
        frame.addPoint(bannerX + bannerW / 2 + 40, bannerY + bannerH);
        frame.addPoint(bannerX + bannerW / 2, bannerY + bannerH + midPeak);
        frame.addPoint(bannerX + bannerW / 2 - 40, bannerY + bannerH);
        frame.addPoint(bannerX + cut, bannerY + bannerH);
        // Cạnh trái vát chéo
        frame.addPoint(bannerX, bannerY + bannerH - cut);
        frame.addPoint(bannerX, bannerY + cut);

        // Nền mờ của khung banner
        g2.setColor(new Color(15, 18, 28, 190));
        g2.fill(frame);

        // Hào quang phát sáng Neon viền ngoài (Orange Glow)
        g2.setColor(new Color(255, 85, 35, 55));
        g2.setStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(frame);

        // Viền Neon Cam sắc nét (Starlight Orange)
        g2.setColor(new Color(255, 105, 45));
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(frame);

        // 4 Góc ngoặc Starlight L-Brackets ở 4 góc của banner
        g2.setColor(new Color(255, 175, 90));
        g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int bl = 16;
        // Góc trên trái
        g2.drawLine(bannerX - 4, bannerY + bl, bannerX - 4, bannerY - 2);
        g2.drawLine(bannerX - 4, bannerY - 2, bannerX + bl, bannerY - 2);
        // Góc trên phải
        g2.drawLine(bannerX + bannerW + 4, bannerY + bl, bannerX + bannerW + 4, bannerY - 2);
        g2.drawLine(bannerX + bannerW + 4, bannerY - 2, bannerX + bannerW - bl, bannerY - 2);
        // Góc dưới trái
        g2.drawLine(bannerX - 4, bannerY + bannerH - bl, bannerX - 4, bannerY + bannerH + 2);
        g2.drawLine(bannerX - 4, bannerY + bannerH + 2, bannerX + bl, bannerY + bannerH + 2);
        // Góc dưới phải
        g2.drawLine(bannerX + bannerW + 4, bannerY + bannerH - bl, bannerX + bannerW + 4, bannerY + bannerH + 2);
        g2.drawLine(bannerX + bannerW + 4, bannerY + bannerH + 2, bannerX + bannerW - bl, bannerY + bannerH + 2);

        // VẼ CHỮ TIÊU ĐỀ CHÍNH VỚI HIỆU ỨNG PHÁT QUANG
        g2.setFont(new Font("Arial", Font.BOLD, 28));
        FontMetrics fm = g2.getFontMetrics();
        int tx = bannerX + (bannerW - fm.stringWidth(titleText)) / 2;
        int ty = bannerY + 40;

        // Bóng hào quang chữ Neon (Orange Glow)
        g2.setColor(new Color(255, 75, 25, 95));
        g2.drawString(titleText, tx - 1, ty);
        g2.drawString(titleText, tx + 1, ty);
        g2.drawString(titleText, tx, ty - 1);
        g2.drawString(titleText, tx, ty + 1);

        // Bóng tối đổ bóng chữ
        g2.setColor(new Color(0, 0, 0, 220));
        g2.drawString(titleText, tx + 2, ty + 2);

        // Chữ chính màu Cam Neon Starlight rực rỡ
        g2.setColor(new Color(255, 120, 50));
        g2.drawString(titleText, tx, ty);

        // Dòng phụ đề công nghệ
        g2.setFont(new Font("Arial", Font.BOLD, 11));
        FontMetrics fmSub = g2.getFontMetrics();
        int subX = (w - fmSub.stringWidth(subtitleText)) / 2;
        int subY = bannerY + bannerH + 28;

        g2.setColor(new Color(180, 205, 240, 210));
        g2.drawString(subtitleText, subX, subY);
    }
}
