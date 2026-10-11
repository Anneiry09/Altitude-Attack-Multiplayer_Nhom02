package game;

public class Bullet {
    public float x, y;
    public float vx, vy;
    public float angle;
    public byte shooterId;
    public int lifeTicks;

    public Bullet(float x, float y, float vx, float vy, float angle, byte shooterId) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.angle = angle;
        this.shooterId = shooterId;
        this.lifeTicks = 40;
    }
}
