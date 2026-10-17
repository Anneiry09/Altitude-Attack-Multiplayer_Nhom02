package model;

public class RoomMember {
    public byte id;
    public String name;
    public boolean isReady;
    public boolean isHost;

    public RoomMember(byte id, String name, boolean isReady, boolean isHost) {
        this.id = id;
        this.name = name;
        this.isReady = isReady;
        this.isHost = isHost;
    }
}
