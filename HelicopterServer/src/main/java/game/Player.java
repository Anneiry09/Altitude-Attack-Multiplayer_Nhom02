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
    }

    public void respawn() {
        this.x = spawnX;
        this.y = spawnY;
        this.vx = 0;
        this.vy = 0;
        this.hp = 100;
        this.respawnTicks = 0;
    }
}
