package network;

import game.GameEngine;
import game.Room;
import game.RoomManager;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class TCPServer {
    private static final int TCP_PORT = 8888;
    private static final int MAX_PLAYERS = 32; // Cho phép nhiều người chơi online đồng thời trên server

    private ServerSocket serverSocket;
    private CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<ClientHandler> matchmakingQueue = new CopyOnWriteArrayList<>();
    private RoomManager roomManager = new RoomManager();
    private UDPServer udpServer;
    private boolean isRunning;
    private byte nextPlayerId = 0;

    public TCPServer(UDPServer udpServer) {
        this.udpServer = udpServer;
        // Thiết lập router input UDP trực tiếp tới engine của phòng tương ứng
        if (this.udpServer != null) {
            this.udpServer.setInputRouter(input -> {
                Room room = roomManager.getRoomByPlayerId(input.playerId);
                if (room != null) {
                    GameEngine engine = room.getEngine();
                    if (engine != null && engine.isRunning()) {
                        engine.enqueueInput(input);
                    }
                }
            });
        }
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(TCP_PORT);
            isRunning = true;
            System.out.println("[TCP Server] Waiting for connections on port " + TCP_PORT + "...");

            while (isRunning) {
                Socket socket = serverSocket.accept();

                if (clients.size() >= MAX_PLAYERS) {
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF("FULL");
                    socket.close();
                    System.out.println("[TCP Server] Connection rejected - Server full.");
                    continue;
                }

                byte playerId = nextPlayerId++;
                ClientHandler handler = new ClientHandler(socket, playerId, this);
                clients.add(handler);
                handler.start();
                System.out.println("[TCP Server] Player #" + playerId + " connected. Total online: " + clients.size());
            }
        } catch (IOException e) {
            System.out.println("[TCP Server] Server stopped.");
        }
    }

    // Ghép trận ngẫu nhiên (Matchmaking)
    public synchronized void joinMatchmaking(ClientHandler client) {
        if (client.getCurrentRoom() != null) {
            client.getCurrentRoom().removeMember(client);
        }
        if (!matchmakingQueue.contains(client)) {
            matchmakingQueue.add(client);
            client.setStatus(ClientHandler.PlayerStatus.MATCHMAKING);
            client.sendMessage("MATCHMAKING_STATUS:QUEUED");
            System.out.println("[Matchmaking] Player #" + client.getPlayerId() + " (" + client.getPlayerName() + ") entered matchmaking queue. Queue size: " + matchmakingQueue.size());
            broadcastLobbyData();
        }

        // Nếu có đủ ít nhất 2 người đang tìm trận, tự động ghép vào 1 phòng và đếm ngược 5s
        if (matchmakingQueue.size() >= 2) {
            ClientHandler c1 = matchmakingQueue.remove(0);
            ClientHandler c2 = matchmakingQueue.remove(0);

            Room room = roomManager.createRoom("Trận ngẫu nhiên #" + (System.currentTimeMillis() % 1000), c1, udpServer, this);
            room.addMember(c2);

            c1.sendMessage("MATCHMAKING_STATUS:MATCHED");
            c2.sendMessage("MATCHMAKING_STATUS:MATCHED");

            // Tự động bật Sẵn sàng cho cả 2 để bắt đầu đếm ngược 5s
            c1.setReady(true);
            c2.setReady(true);
            room.checkAllReady();

            System.out.println("[Matchmaking] Paired Player #" + c1.getPlayerId() + " & Player #" + c2.getPlayerId() + " into Room #" + room.getId());
            broadcastLobbyData();
        }
    }

    public synchronized void cancelMatchmaking(ClientHandler client) {
        if (matchmakingQueue.remove(client)) {
            client.setStatus(ClientHandler.PlayerStatus.LOBBY);
            client.sendMessage("MATCHMAKING_STATUS:IDLE");
            System.out.println("[Matchmaking] Player #" + client.getPlayerId() + " cancelled matchmaking.");
            broadcastLobbyData();
        }
    }

    // Quản lý phòng (Room)
    public synchronized void createRoom(ClientHandler client, String roomName) {
        if (client.getCurrentRoom() != null) {
            client.getCurrentRoom().removeMember(client);
        }
        if (matchmakingQueue.contains(client)) {
            cancelMatchmaking(client);
        }

        if (roomName == null || roomName.trim().isEmpty()) {
            roomName = "Phòng của " + client.getPlayerName();
        }

        Room room = roomManager.createRoom(roomName, client, udpServer, this);
        System.out.println("[Room] Player #" + client.getPlayerId() + " created Room #" + room.getId() + " (" + room.getName() + ")");
        broadcastLobbyData();
    }

    public synchronized void joinRoom(ClientHandler client, int roomId) {
        Room room = roomManager.getRoom(roomId);
        if (room == null) {
            client.sendMessage("JOIN_ROOM_FAILED:Phòng không tồn tại.");
            return;
        }
        if (room.getStatus() == Room.Status.PLAYING) {
            client.sendMessage("JOIN_ROOM_FAILED:Trận đấu trong phòng này đã bắt đầu.");
            return;
        }
        if (room.getMembers().size() >= Room.MAX_PLAYERS) {
            client.sendMessage("JOIN_ROOM_FAILED:Phòng đã đầy người (4/4).");
            return;
        }

        if (client.getCurrentRoom() != null) {
            if (client.getCurrentRoom().getId() == roomId) {
                // Đã ở trong phòng này rồi, gửi lại dữ liệu phòng để client chắc chắn hiển thị đúng
                client.setStatus(ClientHandler.PlayerStatus.IN_ROOM);
                room.broadcastRoomData();
                return;
            }
            client.getCurrentRoom().removeMember(client);
        }
        if (matchmakingQueue.contains(client)) {
            cancelMatchmaking(client);
        }

        boolean ok = room.addMember(client);
        if (ok) {
            System.out.println("[Room] Player #" + client.getPlayerId() + " joined Room #" + roomId);
        } else {
            client.sendMessage("JOIN_ROOM_FAILED:Không thể tham gia phòng.");
        }
    }

    // Xử lý lời mời (Invite)
    public void handleInvite(ClientHandler sender, byte targetPlayerId) {
        Room room = sender.getCurrentRoom();
        if (room == null) {
            sender.sendMessage("INVITE_FAILED:Bạn chưa ở trong phòng nào để mời bạn bè.");
            return;
        }
        if (room.getMembers().size() >= Room.MAX_PLAYERS) {
            sender.sendMessage("INVITE_FAILED:Phòng của bạn đã đầy (4/4).");
            return;
        }

        ClientHandler target = getClientById(targetPlayerId);
        if (target == null) {
            sender.sendMessage("INVITE_FAILED:Người chơi không tồn tại hoặc đã ngắt kết nối.");
            return;
        }

        if (target.getStatus() != ClientHandler.PlayerStatus.LOBBY) {
            sender.sendMessage("INVITE_FAILED:Người chơi hiện đang bận (" + target.getStatus().name() + ").");
            return;
        }

        // Gửi lời mời tới target
        target.sendMessage("INVITE_FROM:" + sender.getPlayerId() + "," + sender.getPlayerName() + "," + room.getId() + "," + room.getName());
        sender.sendMessage("INVITE_SENT:Đã gửi lời mời tới " + target.getPlayerName() + ".");
        System.out.println("[Invite] Player #" + sender.getPlayerId() + " invited Player #" + targetPlayerId + " to Room #" + room.getId());
    }

    public void handleInviteResponse(ClientHandler target, int roomId, boolean accept) {
        Room room = roomManager.getRoom(roomId);
        if (!accept) {
            if (room != null) {
                ClientHandler host = getClientById(room.getHostId());
                if (host != null) {
                    host.sendMessage("INVITE_DECLINED:" + target.getPlayerName() + " đã từ chối lời mời.");
                }
            }
            return;
        }

        if (room == null || room.getMembers().size() >= Room.MAX_PLAYERS || room.getStatus() == Room.Status.PLAYING) {
            target.sendMessage("JOIN_ROOM_FAILED:Phòng không còn khả dụng hoặc đã đầy.");
            return;
        }

        if (matchmakingQueue.contains(target)) cancelMatchmaking(target);
        if (target.getCurrentRoom() != null) target.getCurrentRoom().removeMember(target);

        room.addMember(target);
        System.out.println("[Invite] Player #" + target.getPlayerId() + " accepted invite and joined Room #" + roomId);
    }

    public ClientHandler getClientById(byte playerId) {
        for (ClientHandler c : clients) {
            if (c.getPlayerId() == playerId) return c;
        }
        return null;
    }

    // Broadcast dữ liệu sảnh (Online players + Active rooms)
    public void broadcastLobbyData() {
        // Format: LOBBY_DATA:id,name,status;...|roomId,name,hostName,cur,max,status;...
        StringBuilder sbPlayers = new StringBuilder();
        int pCount = 0;
        for (ClientHandler c : clients) {
            if (c.getPlayerName() == null) continue;
            if (pCount > 0) sbPlayers.append(";");
            String safeName = c.getPlayerName().replace(",", " ").replace(";", " ").replace("|", " ");
            sbPlayers.append(c.getPlayerId()).append(",")
                     .append(safeName).append(",")
                     .append(c.getStatus().name());
            pCount++;
        }

        String roomsStr = roomManager.serializeRooms();
        String fullMsg = "LOBBY_DATA:" + sbPlayers.toString() + "|" + roomsStr;

        for (ClientHandler c : clients) {
            c.sendMessage(fullMsg);
        }
    }

    public void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        matchmakingQueue.remove(client);
        System.out.println("[TCP Server] Player #" + client.getPlayerId() + " removed. Online: " + clients.size());
        broadcastLobbyData();
    }

    public RoomManager getRoomManager() {
        return roomManager;
    }

    public UDPServer getUdpServer() {
        return udpServer;
    }
}
