package game;

import network.ClientHandler;
import network.UDPServer;
import network.TCPServer;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RoomManager {
    private ConcurrentHashMap<Integer, Room> rooms = new ConcurrentHashMap<>();
    private AtomicInteger nextRoomId = new AtomicInteger(101);

    public Room createRoom(String name, ClientHandler host, UDPServer udpServer, TCPServer tcpServer) {
        int roomId = nextRoomId.getAndIncrement();
        if (name == null || name.trim().isEmpty()) {
            name = "Phòng #" + roomId;
        }
        Room room = new Room(roomId, name.trim(), host.getPlayerId(), udpServer, tcpServer);
        rooms.put(roomId, room);
        room.addMember(host);
        return room;
    }

    public Room getRoom(int id) {
        return rooms.get(id);
    }

    public Room getRoomByPlayerId(byte pid) {
        for (Room r : rooms.values()) {
            if (r.hasMember(pid)) return r;
        }
        return null;
    }

    public void removeRoom(int id) {
        rooms.remove(id);
    }

    public Collection<Room> getAllRooms() {
        return rooms.values();
    }

    public String serializeRooms() {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Room r : rooms.values()) {
            if (count > 0) sb.append(";");
            String safeRoomName = (r.getName() != null ? r.getName() : "Room #" + r.getId())
                                  .replace(",", " ").replace(";", " ").replace("|", " ");
            String safeHostName = (r.getHostName() != null ? r.getHostName() : "Unknown")
                                  .replace(",", " ").replace(";", " ").replace("|", " ");
            sb.append(r.getId()).append(",")
              .append(safeRoomName).append(",")
              .append(safeHostName).append(",")
              .append(r.getMembers().size()).append(",")
              .append(Room.MAX_PLAYERS).append(",")
              .append(r.getStatus().name());
            count++;
        }
        return sb.toString();
    }
}
