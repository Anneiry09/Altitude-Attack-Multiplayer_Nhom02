package game;

public class Player {
    public byte id;
    public float x, y;
    public float vx, vy;
    public float angle;
    public byte hp;

    public Player(byte id, float startX, float startY) {
        this.id = id;
        this.x = startX;
        this.y = startY;
        this.vx = 0;
        this.vy = 0;
        this.angle = 0;
        this.hp = 100;
    }
}
