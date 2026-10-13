package network;

import model.OnlinePlayer;
import model.RoomInfo;
import model.RoomMember;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class TCPClient {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private byte myPlayerId;
    private boolean isConnected;

    private TCPListener listener;

    public interface TCPListener {
        void onConnected(byte myPlayerId);
        void onDisconnected(String reason);

        // Dữ liệu sảnh & ghép trận
        void onLobbyData(List<OnlinePlayer> players, List<RoomInfo> rooms);
        void onMatchmakingStatus(String status); // QUEUED, IDLE, MATCHED

        // Dữ liệu phòng
        void onRoomData(RoomInfo roomInfo, List<RoomMember> members);
        void onLeftRoom();

        // Lời mời
        void onInviteReceived(byte fromId, String fromName, int roomId, String roomName);
        void onInviteDeclined(String message);
        void onNotification(String message);
        void onError(String message);

        // Trận đấu
        void onCountdown(int seconds);
        void onCountdownCancelled();
        void onGameStart();
    }

    public void setListener(TCPListener listener) {
        this.listener = listener;
    }

    public boolean connect(String serverIP, String playerName) {
        try {
            socket = new Socket(serverIP, 8888);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            out.writeUTF(playerName);
            out.flush();

            myPlayerId = in.readByte();
            isConnected = true;
            System.out.println("[TCP Client] Connected! Your ID: " + myPlayerId);

            if (listener != null) {
                listener.onConnected(myPlayerId);
            }

            startListening();
            return true;
        } catch (IOException e) {
            System.out.println("[TCP Client] Cannot connect to " + serverIP);
            return false;
        }
    }

    private void startListening() {
        Thread listenThread = new Thread(() -> {
            try {
                while (isConnected) {
                    String message = in.readUTF();

                    try {
                        if (message.startsWith("LOBBY_DATA:")) {
                            parseLobbyData(message.substring("LOBBY_DATA:".length()));
                        } else if (message.startsWith("ROOM_DATA:")) {
                            parseRoomData(message.substring("ROOM_DATA:".length()));
                        } else if (message.equals("LEFT_ROOM")) {
                            if (listener != null) listener.onLeftRoom();
                        } else if (message.startsWith("MATCHMAKING_STATUS:")) {
                            String st = message.split(":")[1];
                            if (listener != null) listener.onMatchmakingStatus(st);
                        } else if (message.startsWith("INVITE_FROM:")) {
                            String body = message.substring("INVITE_FROM:".length());
                            String[] parts = body.split(",");
                            if (parts.length >= 4 && listener != null) {
                                byte fromId = Byte.parseByte(parts[0]);
                                String fromName = parts[1];
                                int rId = Integer.parseInt(parts[2]);
                                String rName = parts[3];
                                listener.onInviteReceived(fromId, fromName, rId, rName);
                            }
                        } else if (message.startsWith("INVITE_DECLINED:")) {
                            if (listener != null) listener.onInviteDeclined(message.substring("INVITE_DECLINED:".length()));
                        } else if (message.startsWith("INVITE_SENT:") || message.startsWith("NOTICE:")) {
                            String msg = message.substring(message.indexOf(":") + 1);
                            if (listener != null) listener.onNotification(msg);
                        } else if (message.startsWith("INVITE_FAILED:") || message.startsWith("JOIN_ROOM_FAILED:")) {
                            String err = message.substring(message.indexOf(":") + 1);
                            if (listener != null) listener.onError(err);
                        } else if (message.startsWith("COUNTDOWN:")) {
                            int sec = Integer.parseInt(message.split(":")[1]);
                            if (listener != null) listener.onCountdown(sec);
                        } else if (message.equals("CANCEL_COUNTDOWN")) {
                            if (listener != null) listener.onCountdownCancelled();
                        } else if (message.equals("START_GAME")) {
                            if (listener != null) listener.onGameStart();
                        }
                    } catch (Exception parseEx) {
                        System.err.println("[TCP Client] Error processing message: " + message + " - " + parseEx.getMessage());
                    }
                }
            } catch (IOException e) {
                if (isConnected && listener != null) {
                    listener.onDisconnected("Mất kết nối tới Server.");
                }
            }
        });
        listenThread.setDaemon(true);
        listenThread.start();
    }

    private void parseLobbyData(String content) {
        String[] sections = content.split("\\|", -1);
        List<OnlinePlayer> players = new ArrayList<>();
        List<RoomInfo> rooms = new ArrayList<>();

        if (sections.length > 0 && !sections[0].isEmpty()) {
            String[] pEntries = sections[0].split(";");
            for (String pe : pEntries) {
                if (pe.isEmpty()) continue;
                String[] pParts = pe.split(",");
                if (pParts.length >= 3) {
                    try {
                        byte pid = Byte.parseByte(pParts[0]);
                        String pName = pParts[1];
                        String status = pParts[2];
                        players.add(new OnlinePlayer(pid, pName, status));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        if (sections.length > 1 && !sections[1].isEmpty()) {
            String[] rEntries = sections[1].split(";");
            for (String re : rEntries) {
                if (re.isEmpty()) continue;
                String[] rParts = re.split(",");
                if (rParts.length >= 6) {
                    try {
                        int rid = Integer.parseInt(rParts[0]);
                        String rName = rParts[1];
                        String hostName = rParts[2];
                        int cur = Integer.parseInt(rParts[3]);
                        int max = Integer.parseInt(rParts[4]);
                        String st = rParts[5];
                        rooms.add(new RoomInfo(rid, rName, hostName, cur, max, st));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        if (listener != null) {
            listener.onLobbyData(players, rooms);
        }
    }

    private void parseRoomData(String content) {
        String[] sections = content.split("\\|", -1);
        if (sections.length < 2) return;

        String[] hParts = sections[0].split(",");
        if (hParts.length < 4) return;

        int rId = Integer.parseInt(hParts[0]);
        String rName = hParts[1];
        byte hostId = Byte.parseByte(hParts[2]);
        String rStatus = hParts[3];

        List<RoomMember> members = new ArrayList<>();
        String hostName = "Unknown";

        if (!sections[1].isEmpty()) {
            String[] mEntries = sections[1].split(";");
            for (String me : mEntries) {
                if (me.isEmpty()) continue;
                String[] mParts = me.split(",");
                if (mParts.length >= 4) {
                    byte mid = Byte.parseByte(mParts[0]);
                    String mName = mParts[1];
                    boolean isReady = Boolean.parseBoolean(mParts[2]);
                    boolean isHost = Boolean.parseBoolean(mParts[3]);
                    members.add(new RoomMember(mid, mName, isReady, isHost));
                    if (mid == hostId) hostName = mName;
                }
            }
        }

        RoomInfo info = new RoomInfo(rId, rName, hostName, members.size(), 4, rStatus);
        if (listener != null) {
            listener.onRoomData(info, members);
        }
    }

    public void sendMatchmakingJoin() {
        sendRaw("MATCHMAKING_JOIN");
    }

    public void sendMatchmakingCancel() {
        sendRaw("MATCHMAKING_CANCEL");
    }

    public void sendCreateRoom(String roomName) {
        sendRaw("CREATE_ROOM:" + (roomName != null ? roomName.trim() : ""));
    }

    public void sendJoinRoom(int roomId) {
        sendRaw("JOIN_ROOM:" + roomId);
    }

    public void sendLeaveRoom() {
        sendRaw("LEAVE_ROOM");
    }

    public void sendToggleReady(boolean ready) {
        sendRaw("TOGGLE_READY:" + ready);
    }

    public void sendReady() {
        sendRaw("READY");
    }

    public void sendRematchReady() {
        sendRaw("REMATCH_READY");
    }

    public void sendInvite(byte targetPlayerId) {
        sendRaw("INVITE:" + targetPlayerId);
    }

    public void sendInviteResponse(int roomId, boolean accept) {
        sendRaw("INVITE_RESPONSE:" + roomId + "," + (accept ? "ACCEPT" : "DECLINE"));
    }

    public void sendRename(String newName) {
        sendRaw("RENAME:" + newName);
    }

    private final Object writeLock = new Object();

    private void sendRaw(String msg) {
        synchronized (writeLock) {
            try {
                if (out != null && isConnected) {
                    out.writeUTF(msg);
                    out.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void disconnect() {
        isConnected = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte getMyPlayerId() { return myPlayerId; }
    public boolean isConnected() { return isConnected; }
}
