# Altitude Attack: Helicopter Battle Multiplayer (2D Real-time Network Game)

> **Học phần:** Lập trình mạng máy tính  
> **Lớp:** 23DTHE4 | **Mã nhóm:** 02  
> **Khoa:** Công nghệ Thông tin - Trường Đại học Công nghệ TP.HCM (HUTECH)

---

## 1. Giới Thiệu Đề Tài

**Altitude Attack (Trực Thăng Đối Kháng)** là trò chơi hành động chiến đấu nhiều người chơi theo thời gian thực (Real-time Multiplayer) hoạt động theo mô hình **Client - Server**. Trò chơi được thiết kế nhằm mô phỏng và ứng dụng chuyên sâu các giao thức ở tầng vận chuyển (Transport Layer) và tầng ứng dụng (Application Layer).

### Điểm nhấn công nghệ:
* **Mô hình mạng Lai (Hybrid TCP & UDP Architecture):**
  * **TCP (Cổng 8888):** Đảm bảo tính tin cậy tuyệt đối cho các luồng dữ liệu điều khiển: Xác thực tên duy nhất, Tạo/Vào phòng chờ (Lobby), Sẵn sàng (Ready), Chat phòng và Đồng bộ kết quả/bảng điểm cuối trận.
  * **UDP (Cổng 9999):** Tối ưu hóa độ trễ cực thấp cho luồng dữ liệu thời gian thực: Đồng bộ tọa độ, hướng quay, trạng thái bắn và HP ở tần suất cố định **60 Tick/giây (60 FPS)**.
* **Giao thức Nhị phân Tự định nghĩa (Custom Binary Protocol):**
  * Mã hóa trực tiếp bằng `java.nio.ByteBuffer`, gói tin điều khiển (`InputPacket`) được nén xuống chỉ **12 bytes**, loại bỏ hoàn toàn chi phí băng thông của JSON/Text.
  * Gói tin trạng thái (`StatePacket`) nén linh hoạt theo số lượng người chơi, kiểm soát buffer chính xác, không tồn đọng byte rác (đã xác thực qua Wireshark).
* **Kiến trúc Server-Authoritative:**
  * Toàn bộ logic vật lý (quán tính, gia tốc, ma sát), phát hiện va chạm đạn (AABB Collision) và tính máu/hồi sinh được xử lý độc lập trên Server tại 60 Tick/giây để chống gian lận (Anti-cheat).
* **Đồng bộ hóa mượt mà (Client-Side Prediction & Interpolation):**
  * Phía Client phản hồi tức thì với thao tác phím của người chơi và áp dụng nội suy chuyển động để hiển thị mượt mà các máy bay đối thủ bất chấp biến động độ trễ mạng.
* **Môi trường Triển khai Đa dạng:**
  * Kiểm thử thành công trên mạng nội bộ LAN, mạng Mesh VPN (Tailscale) giữa 3 máy tính vật lý và đóng gói `HelicopterServer.jar` chạy Headless (0% GUI, tiêu thụ <50MB RAM) trên Linux Cloud VPS.

---

## 2. Bảng Ma Trận Đóng Góp Của Các Thành Viên (Theo Mục 6 Quy Trình)

| STT | MSSV | Họ và Tên | Công việc cụ thể đã thực hiện | Nhánh / Merge Requests | Tỉ lệ đóng góp |
| :---: | :---: | :---: | :--- | :---: | :---: |
| 1 | **2380602652** | **Nguyễn Đăng An**<br/>*(Trưởng nhóm)* | • Thiết kế kiến trúc mạng Hybrid TCP (Port 8888) & UDP (Port 9999)<br/>• Định nghĩa giao thức nhị phân (`InputPacket` 12B, `StatePacket`)<br/>• Xây dựng Server Game Engine 60 FPS (Vật lý, va chạm, anti-cheat)<br/>• Tối ưu Concurrency (`ConcurrentLinkedQueue`, `volatile`) & Chống trùng tên<br/>• Đóng gói Headless Cloud Server | `main`<br/>`feature/2380602652-issue-1-core-network` | **34%** |
| 2 | **2380601682** | **Nguyễn Hữu Phong** | • Thiết kế giao diện Client Swing (Giao diện Starfield & Coral Orange UI)<br/>• Xây dựng hệ thống Chatbox trong phòng chờ (Waiting Room Chat)<br/>• Xây dựng module cài đặt âm thanh `SettingsDialog` (BGM / SFX Slider)<br/>• Tích hợp Font chữ hệ thống không lỗi tiếng Việt và xử lý icon UI | `phong`<br/>`feature/2380601682-issue-3-lobby-ui` | **33%** |
| 3 | **2380602317** | **Phan Hữu Trí** | • Xây dựng luồng logic trạng thái phòng chờ (Ready, Countdown 5s)<br/>• Xây dựng kịch bản kiểm thử kết nối đa thiết bị qua Tailscale Mesh VPN<br/>• Thực hiện đo đạc, bắt và phân tích gói tin mạng bằng Wireshark (Mục 5.4)<br/>• Xây dựng các test script kiểm thử logic game và chịu tải mạng | `Tri`<br/>`feature/2380602317-issue-6-network-testing` | **33%** |

---

## 3. Cấu Trúc Mã Nguồn

```text
a:/LTM/
├── HelicopterServer/               # Mã nguồn Máy chủ (Headless Server)
│   └── src/main/java/
│       ├── network/                # TCPServer, UDPServer, ClientHandler, Protocol
│       ├── game/                   # GameEngine, Helicopter, Bullet, Physics
│       └── MainServer.java         # Điểm khởi chạy Server (chạy được cả GUI/Headless)
│
├── HelicopterClient/               # Mã nguồn Máy khách (Game Client)
│   └── src/main/java/
│       ├── network/                # TCPClient, UDPClient, InputPacket, StatePacket
│       ├── gui/                    # LoginFrame, LobbyFrame, SettingsDialog, Starfield
│       ├── game/                   # GamePanel, GameCanvas, SoundManager, Assets
│       └── MainClient.java         # Điểm khởi chạy ứng dụng Client
│
└── README.md                       # Tài liệu tổng quan và ma trận đóng góp
```

---

## 4. Hướng Dẫn Biên Dịch & Chạy Ứng Dụng

### Yêu cầu hệ thống:
* Java Development Kit (JDK): **Java 17** hoặc **Java 21**.
* Apache Maven hoặc IDE (NetBeans / IntelliJ IDEA / Eclipse).

### 4.1. Khởi chạy Server (Máy chủ):
1. Mở terminal tại thư mục `HelicopterServer`.
2. Biên dịch và chạy:
   ```bash
   mvn clean compile exec:java -Dexec.mainClass="MainServer"
   ```
   *(Hoặc chạy file JAR Headless trên Cloud Linux: `java -jar HelicopterServer.jar -Djava.awt.headless=true`)*
3. Server sẽ tự động mở cổng TCP `8888` và UDP `9999`.

### 4.2. Khởi chạy Client (Người chơi):
1. Mở terminal tại thư mục `HelicopterClient`.
2. Biên dịch và chạy:
   ```bash
   mvn clean compile exec:java -Dexec.mainClass="MainClient"
   ```
3. Nhập Tên người chơi và Địa chỉ IP Server (Ví dụ: `localhost` hoặc IP Tailscale/Public IP) -> Bấm **Tham gia**.
4. Vào sảnh chờ, tạo hoặc chọn phòng -> Bấm **Sẵn sàng** để đếm ngược vào trận.

---

## 5. Hướng Dẫn Điều Khiển Trong Trận Đấu

| Thao tác | Phím bấm / Chuột | Chức năng |
| :--- | :---: | :--- |
| **Di chuyển** | `W` / `A` / `S` / `D` hoặc `Phím mũi tên` | Tiến / Lùi / Đảo chiều (vật lý quán tính trôi dạt) |
| **Định hướng bắn** | `Con trỏ chuột (Mouse Cursor)` | Xoay nòng súng và thân trực thăng 360 độ |
| **Khai hỏa** | `Chuột trái (Left Click)` hoặc `Phím Space` | Bắn đạn (áp dụng cooldown súng chống spam) |
| **Radar / Minimap** | Góc phải màn hình | Theo dõi vị trí tương đối của bản thân và đối thủ |
| **Bảng điểm (HUD)** | Góc trên màn hình | Xem lượng máu (HP), thời gian đếm ngược (60s) và Kill/Death |
