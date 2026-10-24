package gui;

import java.io.File;

/**
 * AssetUtils - Tiện ích tìm kiếm tài nguyên hình ảnh và âm thanh
 * Tự động phân giải đường dẫn khi chạy từ thư mục gốc hoặc từ thư mục HelicopterClient
 */
public final class AssetUtils {
    private AssetUtils() {}

    public static File findAsset(String relativePath) {
        File f1 = new File(relativePath);
        if (f1.exists()) return f1;
        File f2 = new File("HelicopterClient/" + relativePath);
        if (f2.exists()) return f2;
        try {
            File jarDir = new File(AssetUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
            if (jarDir != null && jarDir.isDirectory()) {
                File f3 = new File(jarDir, relativePath);
                if (f3.exists()) return f3;
            }
        } catch (Exception ignored) {}
        return f1;
    }

    public static File findSound(String fileName) {
        return findAsset("assets/sounds/" + fileName);
    }
}
