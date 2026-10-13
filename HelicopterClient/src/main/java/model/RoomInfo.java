package model;

public class RoomInfo {
    public int id;
    public String name;
    public String hostName;
    public int currentPlayers;
    public int maxPlayers;
    public String status; // WAITING, COUNTDOWN, PLAYING

    public RoomInfo(int id, String name, String hostName, int currentPlayers, int maxPlayers, String status) {
        this.id = id;
        this.name = name;
        this.hostName = hostName;
        this.currentPlayers = currentPlayers;
        this.maxPlayers = maxPlayers;
        this.status = status;
    }

    public String getStatusDisplay() {
        return switch (status) {
            case "COUNTDOWN" -> "STARTING";
            case "PLAYING" -> "IN BATTLE";
            default -> "WAITING";
        };
    }
}
