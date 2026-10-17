package network;

import java.nio.ByteBuffer;

public class StatePacket {
    public int tick;
    public byte playerCount;
    public short remainingSeconds; // Thời gian còn lại của trận đấu (giây)

    public byte[] playerIds = new byte[4];
    public float[] playerXs = new float[4];
    public float[] playerYs = new float[4];
    public float[] playerAngles = new float[4];
    public byte[] playerHPs = new byte[4];
    public byte[] playerScores = new byte[4];
    public byte[] playerShieldTypes = new byte[4]; // 0=Không có khiên, 1=Đồng, 2=Bạc, 3=Vàng
    public byte[] playerShieldHits = new byte[4];  // Số lượt đỡ đạn còn lại (0..3)
    public byte[] playerWeaponBuffs = new byte[4]; // 0=None, 1=Red, 2=Green, 3=Blue
    public byte[] playerBuffRemainingSeconds = new byte[4]; // Thời gian buff còn lại (0..15s)

    public byte bulletCount;
    public float[] bulletXs = new float[40];
    public float[] bulletYs = new float[40];
    public float[] bulletAngles = new float[40];
    public byte[] bulletTypes = new byte[40]; // 0: Laser đỏ thường, 1: Cầu đỏ 3 tia, 2: Cầu xanh mẹ, 3: Cầu xanh con, 4: Cầu xanh dương

    public byte shieldItemCount;
    public byte[] shieldItemTypes = new byte[10];
    public float[] shieldItemXs = new float[10];
    public float[] shieldItemYs = new float[10];

    public byte pillItemCount;
    public byte[] pillItemTypes = new byte[10];
    public float[] pillItemXs = new float[10];
    public float[] pillItemYs = new float[10];

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.putInt(tick);
        buffer.put(playerCount);
        buffer.putShort(remainingSeconds);

        for (int i = 0; i < playerCount; i++) {
            buffer.put(playerIds[i]);
            buffer.putFloat(playerXs[i]);
            buffer.putFloat(playerYs[i]);
            buffer.putFloat(playerAngles[i]);
            buffer.put(playerHPs[i]);
            buffer.put(playerScores[i]);
            buffer.put(playerShieldTypes[i]);
            buffer.put(playerShieldHits[i]);
            buffer.put(playerWeaponBuffs[i]);
            buffer.put(playerBuffRemainingSeconds[i]);
        }

        buffer.put(bulletCount);
        for (int i = 0; i < bulletCount; i++) {
            buffer.putFloat(bulletXs[i]);
            buffer.putFloat(bulletYs[i]);
            buffer.putFloat(bulletAngles[i]);
            buffer.put(bulletTypes[i]);
        }

        buffer.put(shieldItemCount);
        for (int i = 0; i < shieldItemCount; i++) {
            buffer.put(shieldItemTypes[i]);
            buffer.putFloat(shieldItemXs[i]);
            buffer.putFloat(shieldItemYs[i]);
        }

        buffer.put(pillItemCount);
        for (int i = 0; i < pillItemCount; i++) {
            buffer.put(pillItemTypes[i]);
            buffer.putFloat(pillItemXs[i]);
            buffer.putFloat(pillItemYs[i]);
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
        packet.remainingSeconds = buffer.getShort();

        for (int i = 0; i < packet.playerCount; i++) {
            packet.playerIds[i] = buffer.get();
            packet.playerXs[i] = buffer.getFloat();
            packet.playerYs[i] = buffer.getFloat();
            packet.playerAngles[i] = buffer.getFloat();
            packet.playerHPs[i] = buffer.get();
            packet.playerScores[i] = buffer.get();
            packet.playerShieldTypes[i] = buffer.get();
            packet.playerShieldHits[i] = buffer.get();
            if (buffer.hasRemaining()) {
                packet.playerWeaponBuffs[i] = buffer.get();
            }
            if (buffer.hasRemaining()) {
                packet.playerBuffRemainingSeconds[i] = buffer.get();
            }
        }

        packet.bulletCount = buffer.get();
        for (int i = 0; i < packet.bulletCount; i++) {
            packet.bulletXs[i] = buffer.getFloat();
            packet.bulletYs[i] = buffer.getFloat();
            packet.bulletAngles[i] = buffer.getFloat();
            if (buffer.hasRemaining()) {
                packet.bulletTypes[i] = buffer.get();
            }
        }

        if (buffer.hasRemaining()) {
            packet.shieldItemCount = buffer.get();
            for (int i = 0; i < packet.shieldItemCount; i++) {
                packet.shieldItemTypes[i] = buffer.get();
                packet.shieldItemXs[i] = buffer.getFloat();
                packet.shieldItemYs[i] = buffer.getFloat();
            }
        }

        if (buffer.hasRemaining()) {
            packet.pillItemCount = buffer.get();
            for (int i = 0; i < packet.pillItemCount; i++) {
                packet.pillItemTypes[i] = buffer.get();
                packet.pillItemXs[i] = buffer.getFloat();
                packet.pillItemYs[i] = buffer.getFloat();
            }
        }

        return packet;
    }
}
