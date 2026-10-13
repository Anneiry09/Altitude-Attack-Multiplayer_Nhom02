package gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class UIAssetManager {
    private static Font kenneyFutureFont;
    private static Font kenneyNarrowFont;
    private static final Map<String, ImageIcon> iconCache = new HashMap<>();

    static {
        loadFonts();
    }

    private static File findFile(String relativePath) {
        File f1 = new File(relativePath);
        if (f1.exists()) return f1;
        File f2 = new File("HelicopterClient/" + relativePath);
        if (f2.exists()) return f2;
        return f1;
    }

    private static void loadFonts() {
        try {
            File fFuture = findFile("assets/Font/Kenney Future.ttf");
            if (fFuture.exists()) {
                kenneyFutureFont = Font.createFont(Font.TRUETYPE_FONT, fFuture);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(kenneyFutureFont);
            }
        } catch (Exception e) {
            System.err.println("[UIAssetManager] Could not load Kenney Future font: " + e.getMessage());
        }

        try {
            File fNarrow = findFile("assets/Font/Kenney Future Narrow.ttf");
            if (fNarrow.exists()) {
                kenneyNarrowFont = Font.createFont(Font.TRUETYPE_FONT, fNarrow);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(kenneyNarrowFont);
            }
        } catch (Exception e) {
            System.err.println("[UIAssetManager] Could not load Kenney Narrow font: " + e.getMessage());
        }
    }

    public static Font getFont(float size) {
        if (kenneyFutureFont != null) {
            return kenneyFutureFont.deriveFont(Font.PLAIN, size);
        }
        return new Font("Segoe UI", Font.BOLD, (int) size);
    }

    public static Font getNarrowFont(float size) {
        if (kenneyNarrowFont != null) {
            return kenneyNarrowFont.deriveFont(Font.PLAIN, size);
        }
        return new Font("Segoe UI", Font.PLAIN, (int) size);
    }

    public static ImageIcon getIcon(String relativePath, int width, int height) {
        String cacheKey = relativePath + "@" + width + "x" + height;
        if (iconCache.containsKey(cacheKey)) {
            return iconCache.get(cacheKey);
        }

        try {
            File file = findFile(relativePath);
            if (file.exists()) {
                BufferedImage original = ImageIO.read(file);
                if (original != null) {
                    Image scaled = original.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    ImageIcon icon = new ImageIcon(scaled);
                    iconCache.put(cacheKey, icon);
                    return icon;
                }
            }
        } catch (Exception e) {
            System.err.println("[UIAssetManager] Error loading icon: " + relativePath + " - " + e.getMessage());
        }
        return null;
    }

    // Convenience Icon Getters
    public static ImageIcon getStarIcon(int size) {
        return getIcon("assets/PNG/Yellow/Default/star.png", size, size);
    }

    public static ImageIcon getCheckmarkIcon(int size) {
        return getIcon("assets/PNG/Green/Default/icon_checkmark.png", size, size);
    }

    public static ImageIcon getCrossIcon(int size) {
        return getIcon("assets/PNG/Red/Default/icon_cross.png", size, size);
    }

    public static ImageIcon getOutlineCrossIcon(int size) {
        return getIcon("assets/PNG/Grey/Default/icon_outline_cross.png", size, size);
    }

    public static ImageIcon getPlayIcon(int size) {
        return getIcon("assets/PNG/Extra/Default/icon_play_light.png", size, size);
    }

    public static ImageIcon getRepeatIcon(int size) {
        return getIcon("assets/PNG/Extra/Default/icon_repeat_light.png", size, size);
    }

    public static ImageIcon getArrowIcon(int size) {
        return getIcon("assets/PNG/Blue/Default/arrow_basic_e.png", size, size);
    }

    public static ImageIcon getStatusDot(String status, int size) {
        if (status == null) status = "LOBBY";
        return switch (status.toUpperCase()) {
            case "MATCHMAKING" -> getIcon("assets/PNG/Yellow/Default/icon_circle.png", size, size);
            case "IN_ROOM" -> getIcon("assets/PNG/Blue/Default/icon_circle.png", size, size);
            case "IN_GAME" -> getIcon("assets/PNG/Red/Default/icon_circle.png", size, size);
            default -> getIcon("assets/PNG/Green/Default/icon_circle.png", size, size); // LOBBY
        };
    }
}
