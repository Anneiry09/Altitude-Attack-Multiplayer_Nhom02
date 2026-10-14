package model;

public class OnlinePlayer {
    public byte id;
    public String name;
    public String status; // LOBBY, MATCHMAKING, IN_ROOM, IN_GAME

    public OnlinePlayer(byte id, String name, String status) {
        this.id = id;
        this.name = name;
        this.status = status;
    }

    public String getStatusDisplay() {
        return switch (status) {
            case "MATCHMAKING" -> "● Đang ghép";
            case "IN_ROOM" -> "● Trong phòng";
            case "IN_GAME" -> "● Đang thi đấu";
            default -> "● Sảnh chờ";
        };
    }
}
