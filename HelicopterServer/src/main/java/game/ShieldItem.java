package game;

public class ShieldItem {
    public static final byte TYPE_BRONZE = 1; // Đồng: đỡ 1 phát đạn
    public static final byte TYPE_SILVER = 2; // Bạc: đỡ 2 phát đạn
    public static final byte TYPE_GOLD = 3;   // Vàng: đỡ 3 phát đạn

    public int id;
    public byte type; // 1, 2, 3
    public float x, y;

    public ShieldItem(int id, byte type, float x, float y) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
    }
}
