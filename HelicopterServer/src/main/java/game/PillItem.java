package game;

public class PillItem {
    public static final byte TYPE_RED = 1;   // 3 tia đạn cầu đỏ, bắn chậm
    public static final byte TYPE_GREEN = 2; // Bắn từng nhịp, đạn nổ 4 mảnh
    public static final byte TYPE_BLUE = 3;  // Vòng laser bảo hộ, đốt dame

    public int id;
    public byte type; // 1, 2, 3
    public float x, y;

    public PillItem(int id, byte type, float x, float y) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
    }
}
