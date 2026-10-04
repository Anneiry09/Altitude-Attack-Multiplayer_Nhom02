# CẤU TRÚC CÂY THƯ MỤC PROJECT GAME

Trả lời câu hỏi của bạn: **ĐÚNG VẬY!** Bạn phải copy 2 file `InputPacket.java` và `StatePacket.java` vào CẢ 2 PROJECT. 
Lý do: Server cần file đó để *đóng gói* tọa độ gửi đi, còn Client cần chính file đó để *giải mã* tọa độ nhận về. Chúng là chiếc "cầu nối" bắt buộc phải giống hệt nhau ở cả 2 bên.

Dưới đây là cấu trúc cây thư mục chuẩn (Package Structure) cho NetBeans mà bạn nên tạo để dễ quản lý code.

---

## 1. PROJECT: Helicopter_Server
Project này không cần giao diện, chỉ có logic và mạng.

```text
Helicopter_Server/
│
├── src/
│   ├── main/
│   │   └── ServerMain.java         # Chứa hàm main() chạy Server
│   │
│   ├── network/                    # Package chứa code mạng
│   │   ├── TCPServer.java          # Mở cổng TCP đón người chơi vào Lobby
│   │   ├── UDPServer.java          # Mở cổng UDP nhận Input và Gửi State
│   │   ├── InputPacket.java        # <--- File dùng chung (Copy vào đây)
│   │   └── StatePacket.java        # <--- File dùng chung (Copy vào đây)
│   │
│   └── game/                       # Package chứa Logic Game
│       ├── GameEngine.java         # Vòng lặp Game Loop 20Hz (Update tọa độ)
│       ├── Player.java             # Lưu thông tin (X, Y, HP) của 1 người chơi
│       └── Bullet.java             # Lưu thông tin (X, Y) của 1 viên đạn
│
└── build/                          # (Thư mục ẩn của NetBeans sinh ra)
```

---

## 2. PROJECT: Helicopter_Client
Project này có giao diện kéo thả, đồ họa vẽ tay và hình ảnh/âm thanh.

```text
Helicopter_Client/
│
├── src/
│   ├── main/
│   │   └── ClientMain.java         # Chứa hàm main() khởi động Client
│   │
│   ├── network/                    # Package chứa code mạng
│   │   ├── TCPClient.java          # Kết nối TCP vào Lobby
│   │   ├── UDPClient.java          # Gửi Input và Nhận State qua UDP
│   │   ├── InputPacket.java        # <--- File dùng chung (Copy vào đây giống hệt Server)
│   │   └── StatePacket.java        # <--- File dùng chung (Copy vào đây giống hệt Server)
│   │
│   ├── gui/                        # Package chứa Giao diện KÉO THẢ (Lobby)
│   │   └── LobbyFrame.java         # (JFrame Form) Màn hình đăng nhập & Sảnh chờ
│   │
│   └── game/                       # Package chứa Giao diện VẼ TAY (In-game)
│       ├── GamePanel.java          # (JPanel) Vòng lặp 60 FPS, vẽ máy bay, bắt phím/chuột
│       └── SpriteRenderer.java     # Class hỗ trợ load ảnh, xoay ảnh máy bay
│
├── assets/                         # Thư mục BẠN TỰ TẠO (Ngang hàng với src)
│   ├── images/
│   │   ├── heli.png                # Ảnh trực thăng
│   │   ├── bullet.png              # Ảnh viên đạn
│   │   └── bg.png                  # Ảnh nền hang động
│   │
│   └── sounds/
│       └── shoot.wav               # Tiếng bắn súng
│
└── build/                          # (Thư mục ẩn của NetBeans sinh ra)
```

---

### Hướng dẫn cách tạo trong NetBeans:
1. Mở NetBeans, chuột phải vào chữ `Source Packages` của Project -> Chọn `New` -> `Java Package...`
2. Tạo lần lượt các package như mình liệt kê ở trên (Ví dụ: `network`, `game`, `gui`).
3. Chuột phải vào package tương ứng -> Chọn `New` -> `Java Class...` để tạo các file `.java`.

Sau khi phân chia rõ ràng thế này, nếu bạn phụ trách code Mạng, bạn chỉ cần mở package `network` ra làm việc. Bạn phụ trách đồ họa chỉ cần vào package `game` và `gui` làm việc. Code sẽ cực kỳ gọn gàng và không bị rối mắt!
