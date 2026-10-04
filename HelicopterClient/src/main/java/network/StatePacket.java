/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package network;

/**
 *
 * @author an013
 */import java.nio.ByteBuffer;

public class StatePacket {
    public int tick;           // Số thứ tự vòng lặp của Server (để Client đồng bộ)
    public byte playerCount;   // Số lượng người chơi hiện tại
    
    // Mảng chứa tọa độ X, Y, Góc, HP của 4 người chơi (Tạm dùng mảng 1 chiều cho đơn giản)
    public byte[] playerIds = new byte[4];
    public float[] playerXs = new float[4];
    public float[] playerYs = new float[4];
    public float[] playerAngles = new float[4];
    public byte[] playerHPs = new byte[4];

    // Encode thành mảng Byte (Server dùng)
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(73); // Tính toán cẩn thận số bytes cần thiết
        buffer.putInt(tick);
        buffer.put(playerCount);
        
        for (int i = 0; i < playerCount; i++) {
            buffer.put(playerIds[i]);
            buffer.putFloat(playerXs[i]);
            buffer.putFloat(playerYs[i]);
            buffer.putFloat(playerAngles[i]);
            buffer.put(playerHPs[i]);
        }
        return buffer.array();
    }

    // Decode từ mảng Byte (Client dùng)
    public static StatePacket fromBytes(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        StatePacket packet = new StatePacket();
        packet.tick = buffer.getInt();
        packet.playerCount = buffer.get();
        
        for (int i = 0; i < packet.playerCount; i++) {
            packet.playerIds[i] = buffer.get();
            packet.playerXs[i] = buffer.getFloat();
            packet.playerYs[i] = buffer.getFloat();
            packet.playerAngles[i] = buffer.getFloat();
            packet.playerHPs[i] = buffer.get();
        }
        return packet;
    }
}
