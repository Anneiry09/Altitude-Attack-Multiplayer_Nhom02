package network;

import game.Room;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler extends Thread {
    public enum PlayerStatus { LOBBY, MATCHMAKING, IN_ROOM, IN_GAME }

    private Socket socket;
    private byte playerId;
    private String playerName;
    private TCPServer server;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean isConnected;
    private boolean isReady;
    private PlayerStatus status = PlayerStatus.LOBBY;
    private Room currentRoom = null;

    public ClientHandler(Socket socket, byte playerId, TCPServer server) {
        this.socket = socket;
        this.playerId = playerId;
        this.server = server;
        this.isConnected = true;
        this.isReady = false;
    }

    @Override
    public void run() {
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            playerName = in.readUTF();
            System.out.println("[TCP Server] Player #" + playerId + " connected with name: " + playerName);

            out.writeByte(playerId);
            out.flush();

            server.broadcastLobbyData();

            while (isConnected) {
                String message = in.readUTF();

                if ("MATCHMAKING_JOIN".equals(message)) {
                    server.joinMatchmaking(this);
                } else if ("MATCHMAKING_CANCEL".equals(message)) {
                    server.cancelMatchmaking(this);
                } else if (message.startsWith("RENAME:")) {
                    String newName = message.substring("RENAME:".length()).trim();
                    if (!newName.isEmpty()) {
                        this.playerName = newName;
                        if (currentRoom != null) {
                            currentRoom.broadcastRoomData();
                        }
                        server.broadcastLobbyData();
                    }
                } else if (message.startsWith("CREATE_ROOM")) {
                    String roomName = "";
                    if (message.contains(":")) {
                        roomName = message.substring(message.indexOf(":") + 1);
                    }
                    server.createRoom(this, roomName);
                } else if (message.startsWith("JOIN_ROOM:")) {
                    try {
                        int rId = Integer.parseInt(message.split(":")[1]);
                        server.joinRoom(this, rId);
                    } catch (NumberFormatException ignored) {}
                } else if ("LEAVE_ROOM".equals(message)) {
                    if (currentRoom != null) {
                        currentRoom.removeMember(this);
                    }
                } else if (message.startsWith("TOGGLE_READY:")) {
                    boolean ready = Boolean.parseBoolean(message.split(":")[1]);
                    if (currentRoom != null) {
                        currentRoom.toggleReady(this, ready);
                    }
                } else if ("READY".equals(message)) {
                    if (currentRoom != null) {
                        currentRoom.toggleReady(this, true);
                    }
                } else if ("REMATCH_READY".equals(message)) {
                    if (currentRoom != null) {
                        currentRoom.handleRematchReady(this);
                    }
                } else if (message.startsWith("INVITE:")) {
                    try {
                        byte targetId = Byte.parseByte(message.split(":")[1]);
                        server.handleInvite(this, targetId);
                    } catch (NumberFormatException ignored) {}
                } else if (message.startsWith("INVITE_RESPONSE:")) {
                    try {
                        String body = message.substring("INVITE_RESPONSE:".length());
                        String[] parts = body.split(",");
                        int rId = Integer.parseInt(parts[0]);
                        boolean acc = "ACCEPT".equalsIgnoreCase(parts[1]);
                        server.handleInviteResponse(this, rId, acc);
                    } catch (Exception ignored) {}
                } else if (message.startsWith("ROOM_CHAT:")) {
                    String chatText = message.substring("ROOM_CHAT:".length());
                    if (currentRoom != null && !chatText.trim().isEmpty()) {
                        currentRoom.broadcast("ROOM_CHAT:" + this.playerId + "," + this.playerName + "," + chatText);
                    }
                } else if (message.startsWith("ROOM_TYPING:")) {
                    String isTyping = message.substring("ROOM_TYPING:".length());
                    if (currentRoom != null) {
                        currentRoom.broadcast("ROOM_TYPING:" + this.playerId + "," + this.playerName + "," + isTyping);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("[TCP Server] Player #" + playerId + " connection closed.");
        } finally {
            disconnect();
        }
    }

    private final Object writeLock = new Object();

    public void sendMessage(String message) {
        synchronized (writeLock) {
            try {
                if (out != null && isConnected) {
                    out.writeUTF(message);
                    out.flush();
                }
            } catch (IOException e) {
                disconnect();
            }
        }
    }

    public synchronized void disconnect() {
        if (!isConnected) return;
        isConnected = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}

        if (status == PlayerStatus.MATCHMAKING) {
            server.cancelMatchmaking(this);
        }
        if (currentRoom != null) {
            currentRoom.removeMember(this);
        }
        server.removeClient(this);
        server.broadcastLobbyData();
    }

    public byte getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public boolean isReady() { return isReady; }
    public void setReady(boolean ready) { this.isReady = ready; }
    public PlayerStatus getStatus() { return status; }
    public void setStatus(PlayerStatus status) { this.status = status; }
    public Room getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(Room room) { this.currentRoom = room; }
}
