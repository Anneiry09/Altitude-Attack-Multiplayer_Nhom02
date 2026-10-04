# KẾ HOẠCH CHI TIẾT PHÁT TRIỂN GAME "TRỰC THĂNG ĐỐI KHÁNG"
**Ngôn ngữ:** Java | **Môi trường:** Apache NetBeans | **Nhân sự:** 3 thành viên | **Thời gian:** 6 Tuần

*Gợi ý phân công:*
*   **Thành viên 1 (Dev Server):** Xử lý Game Loop, Vật lý, Va chạm.
*   **Thành viên 2 (Dev Network):** Xử lý TCP/UDP Socket, Encode/Decode gói tin.
*   **Thành viên 3 (Dev Client):** Xử lý giao diện (Swing), Bắt sự kiện bàn phím/chuột, Render đồ hoạ.

---

## 📌 GIAI ĐOẠN 1: Thiết kế kiến trúc & Khởi tạo dự án (Tuần 1)
**Mục tiêu:** Khởi tạo base code, cấu hình Git và định nghĩa chuẩn giao tiếp.

- [ ] **1.1. Cấu hình môi trường (Cả nhóm):**
  - Khởi tạo 2 project NetBeans: `Helicopter_Server` và `Helicopter_Client`.
  - Tạo Git Repository chung, cấu hình file `.gitignore` để bỏ qua các thư mục `build/`, `nbproject/private/` của NetBeans.
- [ ] **1.2. Định nghĩa Cấu trúc gói tin - Packet Struct (Dev Network):**
  - Tạo class `PacketUtils.java` sử dụng `ByteBuffer`.
  - Thiết kế byte-layout cho `InputPacket` (Client gửi): `[SeqNum(4B) | PlayerID(1B) | X_Dir(1B) | Y_Dir(1B) | MouseAngle(4B) | IsShooting(1B)]`.
  - Thiết kế byte-layout cho `GameStatePacket` (Server gửi): `[Tick(4B) | PlayerCount(1B) | [ID, X, Y, Angle, HP, Score] * N | BulletCount(2B) | [X, Y] * M]`.
- [ ] **1.3. Giao diện Client cơ sở (Dev Client):**
  - Tạo `MainFrame.java` (thừa kế `JFrame`).
  - Tạo `GamePanel.java` (thừa kế `JPanel`), chuẩn bị sẵn phương thức `paintComponent(Graphics g)`.
  - Thiết lập vòng lặp vẽ (Render Loop) bằng `javax.swing.Timer` (khoảng 60 FPS / 16ms).

## 📌 GIAI ĐOẠN 2: Xây dựng TCP Lobby & Game Loop (Tuần 2)
**Mục tiêu:** Kết nối người chơi vào phòng chờ và khởi tạo luồng chạy game trên Server.

- [ ] **2.1. Lập trình TCP Server (Dev Network):**
  - Viết class `TCPServer.java` sử dụng `ServerSocket` chạy ở port 8888.
  - Viết class `ClientHandler.java` (kế thừa `Thread`) để duy trì kết nối TCP cho mỗi client vào phòng.
- [ ] **2.2. Giao diện Phòng chờ - Lobby (Dev Client):**
  - Dựng Form nhập IP Server và Tên người chơi.
  - Hiển thị danh sách các người chơi đang có trong phòng (nhận data từ TCP Server).
  - Thêm nút "Sẵn sàng" và đồng hồ đếm ngược 5 giây khi tất cả đã sẵn sàng.
- [ ] **2.3. Xây dựng Server Game Loop (Dev Server):**
  - Tạo class `GameEngine.java` implement `Runnable`.
  - Viết vòng lặp `while(isRunning)` với `Thread.sleep(50)` để ép tickrate ở mức 20Hz.
  - Tách hàm: `processInputs()`, `updatePhysics()`, `checkCollisions()`, `broadcastState()`.
- [ ] **2.4. Quản lý trạng thái kết nối (Dev Network):**
  - Xử lý Exception khi một Socket bị đứt đột ngột ở sảnh chờ, báo cho các Client khác cập nhật lại danh sách sảnh.

## 📌 GIAI ĐOẠN 3: Giao tiếp UDP & Di chuyển cơ bản (Tuần 3)
**Mục tiêu:** Truyền nhận dữ liệu thời gian thực và điều khiển được vật thể trên màn hình.

- [ ] **3.1. Thiết lập UDP Sockets (Dev Network):**
  - Khởi tạo `DatagramSocket` ở port 9999 trên Server (`UDPServer.java`).
  - Khởi tạo luồng lắng nghe gói tin UDP liên tục trên Client (`UDPListenerThread.java`).
- [ ] **3.2. Thu thập Input từ Client (Dev Client):**
  - Gắn `KeyListener` (W, A, S, D) và `MouseMotionListener` (Lấy toạ độ X, Y của chuột) vào `GamePanel`.
  - Tính góc (Angle) giữa toạ độ máy bay và toạ độ chuột sử dụng `Math.atan2()`.
  - Đóng gói dữ liệu gọi hàm gửi UDP (mỗi 50ms).
- [ ] **3.3. Xử lý di chuyển trên Server (Dev Server):**
  - Trong `GameEngine.java`, lấy hàng đợi Input vừa nhận.
  - Cập nhật biến `x`, `y` của từng máy bay dựa trên hướng di chuyển và vận tốc cơ bản.
  - Gọi hàm Serialize đóng gói toàn bộ `x, y` gửi ngược về toàn bộ Clients.
- [ ] **3.4. Vẽ vật thể cơ sở (Dev Client):**
  - Đọc `GameStatePacket` từ Server, cập nhật tọa độ.
  - Vẽ các ô vuông (đại diện máy bay) lên `GamePanel` để test độ trễ mạng.

## 📌 GIAI ĐOẠN 4: Hoàn thiện Logic Game & Đồ hoạ (Tuần 4)
**Mục tiêu:** Áp dụng vật lý, bắn đạn, va chạm và hình ảnh thật.

- [ ] **4.1. Áp dụng Vật lý và Bắn súng (Dev Server):**
  - Thêm quán tính: Máy bay không dừng hẳn ngay mà giảm tốc từ từ khi thả phím.
  - Logic đạn: Khi nhận cờ `isShooting = true`, tạo Object `Bullet` có vận tốc V theo hướng `Angle` (`vX = V * cos(Angle), vY = V * sin(Angle)`).
  - Thêm biến `lastShotTime` để giới hạn tốc độ bắn (Cooldown súng 0.2s/viên).
- [ ] **4.2. Xử lý Va chạm & Trạng thái (Dev Server):**
  - Dùng `Rectangle.intersects()` hoặc công thức khoảng cách 2 điểm để xét va chạm giữa Đạn và Máy bay.
  - Viết logic: Trừ HP -> Nếu HP = 0 -> Tăng Score cho người bắn -> Chuyển máy bay chết sang trạng thái Respawn (đếm ngược 3s).
- [ ] **4.3. Đồ hoạ & Sprite (Dev Client):**
  - Load ảnh trực thăng, ảnh đạn, ảnh nền hang động bằng `ImageIO.read()`.
  - Sử dụng `Graphics2D.rotate(angle, centerX, centerY)` để vẽ máy bay xoay đúng hướng chuột.
- [ ] **4.4. Kỹ thuật giảm giật/lag (Cả nhóm):**
  - **Dev Client:** Viết thuật toán *Interpolation* (Nội suy). Lưu trạng thái State n-1 và State n, render vị trí ở giữa 2 state để mượt hóa chuyển động.
  - **Dev Network:** Thêm cơ chế Heartbeat, Server đếm timer nếu 5 giây không nhận UDP của Client A -> Xoá Client A khỏi mảng.

## 📌 GIAI ĐOẠN 5: Kiểm thử (Testing) & Debugging (Tuần 5)
**Mục tiêu:** Chơi thử nghiệm đa máy tính và xử lý các lỗi mạng phát sinh.

- [ ] **5.1. Kiểm thử LAN (Cả nhóm):**
  - Máy Dev Server phát Wifi hotspot hoặc dùng mạng LAN nội bộ.
  - 2 Dev còn lại nhập IP LAN của Server để kết nối. Test chức năng tạo sảnh, đếm ngược và vào game.
- [ ] **5.2. Kiểm thử Tải (Stress Test):**
  - Cả 3 người cùng giữ chặt phím bắn để tạo ra số lượng đạn tối đa.
  - Quan sát FPS của Client và TPS (Ticks Per Second) của Server xem có bị rớt không.
- [ ] **5.3. Kiểm thử Ngoại lệ mạng:**
  - Đang chơi, 1 máy tắt Wifi đột ngột -> Server phải phát hiện và xoá người chơi đó khỏi Game State.
  - Rút phích cắm Server -> Client phải hiện thông báo "Mất kết nối tới Server" và quay về màn hình chính.
- [ ] **5.4. Theo dõi gói tin (Wireshark):**
  - Mở Wireshark lắng nghe port 9999. Verify rằng kích thước gói tin đúng như thiết kế ở Tuần 1, không gửi dữ liệu rác.

## 📌 GIAI ĐOẠN 6: Đóng gói, Viết Báo Cáo & Chuẩn bị Demo (Tuần 6)
**Mục tiêu:** Hoàn thiện sản phẩm để nộp cho Giảng viên.

- [ ] **6.1. Âm thanh & UI phụ (Dev Client):**
  - Dùng `Clip` trong `javax.sound.sampled` để phát file `.wav` tiếng súng nổ, nhạc nền.
  - Thiết kế màn hình Kết thúc ván (Victory/Defeat) hiển thị bảng xếp hạng điểm số.
- [ ] **6.2. Viết Báo Cáo (Cả nhóm):**
  - Vẽ Sơ đồ luồng hoạt động (Sequence Diagram) giải thích TCP Lobby.
  - Kẻ bảng mô tả chi tiết Byte Layout của các gói tin UDP.
  - Chụp màn hình Wireshark đưa vào báo cáo để chứng minh việc tối ưu mạng.
  - Giải thích thuật toán Interpolation và Fixed Tickrate đã code như thế nào.
- [ ] **6.3. Quay Video Demo:**
  - Dùng OBS Studio quay một trận đấu khoảng 2-3 phút có mặt 3 người chơi cùng lúc (để backup hôm bảo vệ nếu có sự cố mạng).
- [ ] **6.4. Nộp Bài:**
  - Dọn dẹp code, xoá các `System.out.println` dùng để debug.
  - Export project NetBeans thành `.zip` và nộp lên hệ thống.
