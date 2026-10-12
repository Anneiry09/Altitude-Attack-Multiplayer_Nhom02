package game;

public class Bullet {
    public float x, y;
    public float startX, startY; // Tọa độ lúc bắn ra để tính quãng đường bay
    public float vx, vy;
    public float angle;
    public byte shooterId;
    public int lifeTicks;
    public float maxRange; // Tầm bắn giới hạn tối đa
    public byte type; // 0: Laser đỏ thường, 1: Cầu đỏ 3 tia, 2: Cầu xanh lá mẹ, 3: Cầu xanh lá con, 4: Cầu xanh dương quỹ đạo

    public static final byte TYPE_NORMAL = 0;
    public static final byte TYPE_RED_TRIPLE = 1;
    public static final byte TYPE_GREEN_MAIN = 2;
    public static final byte TYPE_GREEN_SUB = 3;
    public static final byte TYPE_BLUE_ORBIT = 4;

    public Bullet(float x, float y, float vx, float vy, float angle, byte shooterId, float maxRange) {
        this(x, y, vx, vy, angle, shooterId, maxRange, (byte) 0);
    }

    public Bullet(float x, float y, float vx, float vy, float angle, byte shooterId, float maxRange, byte type) {
        this.x = x;
        this.y = y;
        this.startX = x;
        this.startY = y;
        this.vx = vx;
        this.vy = vy;
        this.angle = angle;
        this.shooterId = shooterId;
        this.maxRange = maxRange;
        this.type = type;
        this.lifeTicks = 25;
    }
}
