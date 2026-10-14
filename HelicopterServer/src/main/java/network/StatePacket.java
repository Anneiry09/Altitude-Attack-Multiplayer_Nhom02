package network;

import java.nio.ByteBuffer;

public class StatePacket {
    public int tick;
    public byte playerCount;
    public short remainingTime; // Thời gian còn lại của ván đấu (giây)

    public byte[] playerIds = new byte[4];
    public float[] playerXs = new float[4];
    public float[] playerYs = new float[4];
    public float[] playerAngles = new float[4];
    public byte[] playerHPs = new byte[4];
    public byte[] playerScores = new byte[4];

    public byte bulletCount;
    public float[] bulletXs = new float[30];
    public float[] bulletYs = new float[30];
    public float[] bulletAngles = new float[30];

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(512);
        buffer.putInt(tick);
        buffer.put(playerCount);
        buffer.putShort(remainingTime);

        for (int i = 0; i < playerCount; i++) {
            buffer.put(playerIds[i]);
            buffer.putFloat(playerXs[i]);
            buffer.putFloat(playerYs[i]);
            buffer.putFloat(playerAngles[i]);
            buffer.put(playerHPs[i]);
            buffer.put(playerScores[i]);
        }

        buffer.put(bulletCount);
        for (int i = 0; i < bulletCount; i++) {
            buffer.putFloat(bulletXs[i]);
            buffer.putFloat(bulletYs[i]);
            buffer.putFloat(bulletAngles[i]);
        }

        byte[] result = new byte[buffer.position()];
        System.arraycopy(buffer.array(), 0, result, 0, buffer.position());
        return result;
    }

    public static StatePacket fromBytes(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        StatePacket packet = new StatePacket();
        packet.tick = buffer.getInt();
        packet.playerCount = buffer.get();
        packet.remainingTime = buffer.getShort();

        for (int i = 0; i < packet.playerCount; i++) {
            packet.playerIds[i] = buffer.get();
            packet.playerXs[i] = buffer.getFloat();
            packet.playerYs[i] = buffer.getFloat();
            packet.playerAngles[i] = buffer.getFloat();
            packet.playerHPs[i] = buffer.get();
            packet.playerScores[i] = buffer.get();
        }

        packet.bulletCount = buffer.get();
        for (int i = 0; i < packet.bulletCount; i++) {
            packet.bulletXs[i] = buffer.getFloat();
            packet.bulletYs[i] = buffer.getFloat();
            packet.bulletAngles[i] = buffer.getFloat();
        }

        return packet;
    }
}
