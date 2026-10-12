package game;

import network.ClientHandler;
import network.UDPServer;
import network.TCPServer;
import java.util.concurrent.CopyOnWriteArrayList;

public class Room {
    public static final int MAX_PLAYERS = 4;
    public enum Status { WAITING, COUNTDOWN, PLAYING }

    private int id;
    private String name;
    private byte hostId;
    private Status status = Status.WAITING;
    private CopyOnWriteArrayList<ClientHandler> members = new CopyOnWriteArrayList<>();
    private UDPServer udpServer;
    private TCPServer tcpServer;
    private GameEngine engine;
    private Thread countdownThread;
    private Thread engineThread;

    public Room(int id, String name, byte hostId, UDPServer udpServer, TCPServer tcpServer) {
        this.id = id;
        this.name = name;
        this.hostId = hostId;
        this.udpServer = udpServer;
        this.tcpServer = tcpServer;
    }

    public synchronized boolean addMember(ClientHandler client) {
        if (members.size() >= MAX_PLAYERS) return false;
        if (status == Status.PLAYING) return false;

        members.add(client);
        client.setCurrentRoom(this);
        client.setStatus(ClientHandler.PlayerStatus.IN_ROOM);
        client.setReady(false);

        if (members.size() == 1) {
            this.hostId = client.getPlayerId();
        }

        broadcastRoomData();
        tcpServer.broadcastLobbyData();
        return true;
    }

    public synchronized void removeMember(ClientHandler client) {
        boolean removed = members.remove(client);
        if (!removed) return;

        client.setCurrentRoom(null);
        client.setStatus(ClientHandler.PlayerStatus.LOBBY);
        client.setReady(false);
        client.sendMessage("LEFT_ROOM");

        if (status == Status.COUNTDOWN) {
            cancelCountdown();
        }

        if (engine != null) {
            engine.removePlayer(client.getPlayerId());
        }

        if (members.isEmpty()) {
            if (engine != null) engine.stop();
            tcpServer.getRoomManager().removeRoom(id);
        } else {
            if (client.getPlayerId() == hostId) {
                hostId = members.get(0).getPlayerId();
            }
            broadcastRoomData();
        }

        tcpServer.broadcastLobbyData();
    }

    public synchronized void toggleReady(ClientHandler client, boolean ready) {
        if (status == Status.PLAYING) return;
        client.setReady(ready);
        broadcastRoomData();
        checkAllReady();
    }

    public synchronized void checkAllReady() {
        if (members.size() < 2) {
            if (status == Status.COUNTDOWN) {
                cancelCountdown();
            }
            return;
        }

        boolean allReady = true;
        for (ClientHandler m : members) {
            if (!m.isReady()) {
                allReady = false;
                break;
            }
        }

        if (allReady && status == Status.WAITING) {
            startCountdown(5); // 5 giây đếm ngược theo yêu cầu
        } else if (!allReady && status == Status.COUNTDOWN) {
            cancelCountdown();
        }
    }

    public synchronized void startCountdown(int seconds) {
        if (countdownThread != null) countdownThread.interrupt();
        status = Status.COUNTDOWN;
        broadcastRoomData();
        tcpServer.broadcastLobbyData();

        countdownThread = new Thread(() -> {
            try {
                for (int i = seconds; i > 0; i--) {
                    broadcast("COUNTDOWN:" + i);
                    System.out.println("[Room #" + id + "] Countdown: " + i);
                    Thread.sleep(1000);
                }
                startGame();
            } catch (InterruptedException e) {
                System.out.println("[Room #" + id + "] Countdown interrupted.");
                broadcast("CANCEL_COUNTDOWN");
            } finally {
                countdownThread = null;
            }
        });
        countdownThread.start();
    }

    public synchronized void cancelCountdown() {
        if (countdownThread != null) {
            countdownThread.interrupt();
            countdownThread = null;
        }
        if (status == Status.COUNTDOWN) {
            status = Status.WAITING;
            broadcast("CANCEL_COUNTDOWN");
            broadcastRoomData();
            tcpServer.broadcastLobbyData();
        }
    }

    public synchronized void startGame() {
        status = Status.PLAYING;
        for (ClientHandler m : members) {
            m.setStatus(ClientHandler.PlayerStatus.IN_GAME);
            m.setReady(false);
        }
        broadcastRoomData();
        tcpServer.broadcastLobbyData();

        if (engine != null) engine.stop();
        engine = new GameEngine(udpServer);
        for (ClientHandler m : members) {
            engine.addPlayer(m.getPlayerId());
        }
        engine.resetMatch();

        broadcast("START_GAME");
        engineThread = new Thread(engine);
        engineThread.start();
        System.out.println("[Room #" + id + "] Game started with " + members.size() + " players.");
    }

    public synchronized void handleRematchReady(ClientHandler client) {
        client.setReady(true);
        boolean allReady = true;
        for (ClientHandler m : members) {
            if (!m.isReady()) {
                allReady = false;
                break;
            }
        }

        if (allReady && status == Status.PLAYING) {
            countdownThread = new Thread(() -> {
                try {
                    for (int i = 3; i > 0; i--) {
                        broadcast("COUNTDOWN:" + i);
                        Thread.sleep(1000);
                    }
                    for (ClientHandler m : members) {
                        m.setReady(false);
                        engine.addPlayer(m.getPlayerId());
                    }
                    engine.resetMatch();
                    broadcast("START_GAME");
                    System.out.println("[Room #" + id + "] Rematch started.");
                } catch (InterruptedException e) {
                    broadcast("CANCEL_COUNTDOWN");
                } finally {
                    countdownThread = null;
                }
            });
            countdownThread.start();
        }
    }

    public void broadcast(String message) {
        for (ClientHandler m : members) {
            m.sendMessage(message);
        }
    }

    public void broadcastRoomData() {
        broadcast(toDataString());
    }

    public String toDataString() {
        // Format: ROOM_DATA:id,name,hostId,status|m1Id,m1Name,m1IsReady,m1IsHost;...
        String safeName = (name != null ? name : "Room #" + id)
                          .replace(",", " ").replace(";", " ").replace("|", " ");
        StringBuilder sb = new StringBuilder("ROOM_DATA:");
        sb.append(id).append(",").append(safeName).append(",").append(hostId).append(",").append(status.name()).append("|");
        for (int i = 0; i < members.size(); i++) {
            ClientHandler m = members.get(i);
            boolean isHost = (m.getPlayerId() == hostId);
            String safePName = (m.getPlayerName() != null ? m.getPlayerName() : "Player #" + m.getPlayerId())
                               .replace(",", " ").replace(";", " ").replace("|", " ");
            sb.append(m.getPlayerId()).append(",")
              .append(safePName).append(",")
              .append(m.isReady()).append(",")
              .append(isHost);
            if (i < members.size() - 1) sb.append(";");
        }
        return sb.toString();
    }

    public boolean hasMember(byte playerId) {
        for (ClientHandler m : members) {
            if (m.getPlayerId() == playerId) return true;
        }
        return false;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public byte getHostId() { return hostId; }
    public Status getStatus() { return status; }
    public CopyOnWriteArrayList<ClientHandler> getMembers() { return members; }
    public GameEngine getEngine() { return engine; }
    public String getHostName() {
        for (ClientHandler m : members) {
            if (m.getPlayerId() == hostId) {
                return m.getPlayerName() != null ? m.getPlayerName() : "Player #" + hostId;
            }
        }
        return "Unknown";
    }
}
