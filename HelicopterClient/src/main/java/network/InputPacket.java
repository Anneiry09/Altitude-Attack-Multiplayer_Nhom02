/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package network;

/**
 *
 * @author an013
 */
import java.nio.ByteBuffer;

public class InputPacket {
    public int sequenceNumber; // Chống gửi nhầm gói cũ
    public byte playerId;      // ID của người chơi
    public byte moveX;         // Di chuyển X (-1: Trái, 0: Đứng im, 1: Phải)
    public byte moveY;         // Di chuyển Y (-1: Lên, 0: Đứng im, 1: Xuống)
    public float mouseAngle;   // Góc xoay của chuột để bắn
    public boolean isShooting; // Có đang bấm chuột bắn không (1 hoặc 0)

    // Hàm đóng gói (Encode) thành Byte array để gửi UDP
    public byte[] toBytes() {
        // Cấp phát 12 bytes (int=4, byte=1, float=4, boolean quy về byte=1)
        ByteBuffer buffer = ByteBuffer.allocate(12);
        buffer.putInt(sequenceNumber);
        buffer.put(playerId);
        buffer.put(moveX);
        buffer.put(moveY);
        buffer.putFloat(mouseAngle);
        buffer.put((byte) (isShooting ? 1 : 0));
        return buffer.array();
    }

    // Hàm giải mã (Decode) từ Byte array khi Server nhận được
    public static InputPacket fromBytes(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        InputPacket packet = new InputPacket();
        packet.sequenceNumber = buffer.getInt();
        packet.playerId = buffer.get();
        packet.moveX = buffer.get();
        packet.moveY = buffer.get();
        packet.mouseAngle = buffer.getFloat();
        packet.isShooting = buffer.get() == 1;
        return packet;
    }
}