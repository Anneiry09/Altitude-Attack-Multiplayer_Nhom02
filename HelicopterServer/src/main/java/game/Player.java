package game;

public class Player {
    public byte id;
    public float x, y;
    public float vx, vy;
    public float angle;
    public byte hp;
    public byte score;
    public int shotCooldown;
    public int respawnTicks;
    public float spawnX, spawnY;
    public byte shieldType; // 0=Không có khiên, 1=Đồng (1 hit), 2=Bạc (2 hits), 3=Vàng (3 hits)
    public byte shieldHits; // Số lần đỡ đạn còn lại
    public byte weaponBuff; // 0=None, 1=Pill Red (3 luồng), 2=Pill Green (Nổ 4 mảnh), 3=Pill Blue (Vòng bảo vệ)
    public int weaponBuffTicks; // Số tick còn lại của hiệu ứng thuốc
    public boolean lastShootingInput; // Trạng thái bắn của tick trước (hỗ trợ bán tự động từng nhịp)
    public int blueBurnCooldown; // Cooldown gây sát thương thiêu đốt đối thủ của vòng Blue Orbit

    public Player(byte id, float startX, float startY) {
        this.id = id;
        this.x = startX;
        this.y = startY;
        this.spawnX = startX;
        this.spawnY = startY;
        this.vx = 0;
        this.vy = 0;
        this.angle = 0;
        this.hp = 100;
        this.score = 0;
        this.shotCooldown = 0;
        this.respawnTicks = 0;
        this.shieldType = 0;
        this.shieldHits = 0;
        this.weaponBuff = 0;
        this.weaponBuffTicks = 0;
        this.lastShootingInput = false;
        this.blueBurnCooldown = 0;
    }

    public void respawn() {
        respawn(this.spawnX, this.spawnY);
    }

    public void respawn(float newX, float newY) {
        this.x = newX;
        this.y = newY;
        this.vx = 0;
        this.vy = 0;
        this.hp = 100;
        this.respawnTicks = 0;
        this.shieldType = 0;
        this.shieldHits = 0;
        this.weaponBuff = 0;
        this.weaponBuffTicks = 0;
        this.lastShootingInput = false;
        this.blueBurnCooldown = 0;
    }
}
