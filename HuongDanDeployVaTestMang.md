# HƯỚNG DẪN TRIỂN KHAI MÁY CHỦ VPS VÀ QUY TRÌNH TEST GAME QUA MẠNG THẬT

> **Tài liệu hướng dẫn kỹ thuật** dành cho dự án **Helicopter Combat (Altitude Attack)**.
> Giúp triển khai Server lên Cloud VPS để người chơi tải game về là chơi ngay mà không cần cấu hình IP rườm rà.

---

## MỤC LỤC
1. [Kiến Trúc Mạng & Cơ Chế Kết Nối](#1-kiến-trúc-mạng--cơ-chế-kết-nối)
2. [Cơ Chế "Auto-Connect Không Cần Nhập IP"](#2-cơ-chế-auto-connect-không-cần-nhập-ip)
3. [Quy Trình Triển Khai Server Lên Cloud VPS (Linux)](#3-quy-trình-triển-khai-server-lên-cloud-vps-linux)
4. [Quy Trình Đóng Gói Client "Chạy Là Chơi" (Portable Package)](#4-quy-trình-đóng-gói-client-chạy-là-chơi-portable-package)
5. [Kịch Bản Test Thực Tế Trên Nhiều Máy Tính](#5-kịch-bản-test-thực-tế-trên-nhiều-máy-tính)
6. [Xử Lý Sự Cố Thường Gặp (Troubleshooting)](#6-xử-lý-sự-cố-thường-gặp-troubleshooting)

---

## 1. KIẾN TRÚC MẠNG & CƠ CHẾ KẾT NỐI

Game sử dụng mô hình kết hợp **Hybrid TCP + UDP**:

```
                       ┌───────────────────────────────┐
                       │     CLOUD VPS SERVER          │
                       │     (Public IP cố định)       │
                       │                               │
                       │  - TCP Server : Cổng 8888     │
                       │  - UDP Server : Cổng 9999     │
                       └───────────────▲───────────────┘
                                       │
                ┌──────────────────────┴──────────────────────┐
                │                                             │
      TCP 8888  │ Sảnh, Phòng, Ghép trận            TCP 8888  │ Sảnh, Phòng, Ghép trận
      UDP 9999  │ Đạn, Vị trí, Máu, Điểm            UDP 9999  │ Đạn, Vị trí, Máu, Điểm
                │                                             │
   ┌────────────▼──────────────┐                ┌─────────────▼─────────────┐
   │    CLIENT 1 (Máy bạn A)   │                │    CLIENT 2 (Máy bạn B)   │
   │  - Wi-Fi nhà riêng / 4G   │                │  - Wi-Fi quán cafe / KTX  │
   │  - Chỉ cần nhập Tên Pilot │                │  - Chỉ cần nhập Tên Pilot │
   └───────────────────────────┘                └───────────────────────────┘
```

* **Giao thức TCP (Port 8888)**:
  * Đảm bảo truyền tin cậy 100% (không rơi rớt gói tin).
  * Đảm nhiệm: Đăng nhập vào sảnh, danh sách pilot online, tạo phòng, vào phòng, gửi lời mời đấu, toggle trạng thái sẵn sàng (Ready), đếm ngược trận đấu.
* **Giao thức UDP (Port 9999)**:
  * Tốc độ cực cao, độ trễ thấp (Low-latency), tối ưu cho vòng lặp game 60 FPS.
  * Đảm nhiệm: Gửi phím bấm điều khiển phi thuyền (`InputPacket`) và phát sóng trạng thái trận đấu theo thời gian thực (`StatePacket`: vị trí phi thuyền, góc xoay, đạn, máu, khiên, buff item, thiên thạch).

---

## 2. CƠ CHẾ "AUTO-CONNECT KHÔNG CẦN NHẬP IP"

Để người chơi không phải gõ địa chỉ IP thủ công:
1. Client sẽ đọc cấu hình từ file `config.properties` nằm cùng thư mục với file game.
2. Nếu file không tồn tại, Client tự động dùng IP mặc định cấu hình sẵn trong mã nguồn.

### Cấu trúc file `config.properties`:
```properties
# Địa chỉ IP công khai hoặc tên miền của máy chủ VPS
server.host=103.123.45.67

# Cổng dịch vụ mạng
server.port.tcp=8888
server.port.udp=9999
```

👉 **Lợi ích**: Khi đổi sang VPS mới hoặc khi cần test localhost ở nhà, bạn chỉ cần mở file `config.properties` sửa lại IP trong 2 giây mà **không cần biên dịch lại mã nguồn**.

---

## 3. QUY TRÌNH TRIỂN KHAI SERVER LÊN CLOUD VPS (LINUX)

### Bước 3.1: Chuẩn bị VPS
* Hệ điều hành khuyên dùng: **Ubuntu 22.04 LTS** hoặc **Ubuntu 24.04 LTS** / **Debian 12**.
* Cấu hình tối thiểu: 1 vCPU, 1GB RAM (rất rẻ, từ 30.000đ – 50.000đ/tháng hoặc dùng Free Tier của Oracle Cloud / Google Cloud / AWS).

### Bước 3.2: Cài đặt Java Runtime trên VPS
Mở Terminal của VPS (thông qua SSH) và chạy lệnh:
```bash
sudo apt update
sudo apt install -y openjdk-21-jre-headless
```
Kiểm tra cài đặt thành công:
```bash
java -version
# Kết quả hiển thị openjdk version "21.x.x" là thành công!
```

### Bước 3.3: Mở cổng Tường lửa (Firewall)
1. **Trên hệ điều hành VPS (UFW)**:
   ```bash
   sudo ufw allow 8888/tcp
   sudo ufw allow 9999/udp
   sudo ufw reload
   ```
2. **Trên bảng điều khiển nhà cung cấp Cloud (AWS, Oracle, GCP, DigitalOcean, v.v.)**:
   * Truy cập mục **Security Groups** (hoặc **Firewall / Inbound Rules**).
   * Thêm 2 quy tắc Inbound:
     * **Port 8888 / Protocol TCP / Source: 0.0.0.0/0 (Anywhere)**.
     * **Port 9999 / Protocol UDP / Source: 0.0.0.0/0 (Anywhere)**.

### Bước 3.4: Đẩy file `HelicopterServer.jar` lên VPS
Sử dụng công cụ **WinSCP** hoặc **FileZilla** kết nối vào VPS (qua giao thức SFTP/SSH) và tải file `HelicopterServer.jar` vào thư mục `/home/ubuntu/helicopter-server/`.

Hoặc dùng lệnh `scp` từ máy của bạn:
```powershell
scp HelicopterServer.jar root@<IP_VPS>:/root/
```

### Bước 3.5: Khởi chạy Server chạy ngầm 24/7
Có 2 cách chạy phổ biến:

#### Cách 1: Dùng lệnh `nohup` (Nhanh và đơn giản nhất)
```bash
cd /root
nohup java -jar HelicopterServer.jar > server.log 2>&1 &
```
* Xem log trực tiếp khi server hoạt động:
  ```bash
  tail -f server.log
  ```
* Dừng server khi cần:
  ```bash
  pkill -f HelicopterServer.jar
  ```

#### Cách 2: Dùng `screen` (Xem bảng điều khiển server trực quan)
```bash
# Cài đặt screen nếu chưa có
sudo apt install -y screen

# Mở session mới cho server
screen -S gameserver

# Khởi chạy server
java -jar HelicopterServer.jar

# Bấm Ctrl + A rồi ấn phím D để thoát ra ngoài mà server vẫn chạy ngầm
# Khi muốn quay lại xem màn hình server:
screen -r gameserver
```

---

## 4. QUY TRÌNH ĐÓNG GÓI CLIENT "CHẠY LÀ CHƠI" (PORTABLE PACKAGE)

Để gửi cho bạn bè mà họ chỉ cần click đúp là vào game:

### Cấu trúc thư mục đóng gói:
```
HelicopterClient_Release/
├── assets/
│   ├── Font/             (Kenney Future.ttf, Kenney Future Narrow.ttf)
│   ├── images/           (Phi thuyền, thiên thạch, đạn, khiên, buff item)
│   ├── PNG/              (11 Icon Kenney sử dụng trong sảnh & phòng)
│   └── sounds/           (BGM Darkwave.wav, âm thanh súng, nổ, mời bạn)
├── config.properties     (Điền sẵn IP của VPS)
├── HelicopterClient.jar  (File thực thi Java)
└── PlayGame.bat          (Click đúp chuột để chạy game)
```

### Nội dung file `PlayGame.bat`:
```bat
@echo off
title Helicopter Combat - Sci-Fi Multiplayer Arena
cd /d "%~dp0"

echo Dang kiem tra moi truong Java...
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [LOI] May tinh cua ban chua cai dat Java!
    echo Vui long tai va cai dat Java 21 tai: https://adoptium.net/
    pause
    exit /b
)

echo Dang khoi dong Helicopter Combat...
start javaw -jar HelicopterClient.jar
exit
```

---

## 5. KỊCH BẢN TEST THỰC TẾ TRÊN NHIỀU MÁY TÍNH

Khi Server đã chạy trên VPS và Client đã điền IP của VPS:

| Bước | Hành động | Kết quả mong đợi |
| :--- | :--- | :--- |
| **1. Khởi động Client** | 2-4 máy tính khác nhau mở `PlayGame.bat`. | Cửa sổ game bật lên với nền vũ trụ sao rơi 60 FPS, nhạc nền sảnh phát mượt mà. |
| **2. Đăng nhập** | Mỗi người nhập một tên Pilot riêng (Ví dụ: `AlphaPilot`, `BravoOne`) và bấm **ENTER LOBBY**. | Kết nối TCP thành công đến VPS, chuyển sang màn hình Dashboard. Cột **ONLINE PILOTS** tự động hiển thị đầy đủ tên các pilot đang online. |
| **3. Mời & Vào phòng** | Pilot 1 bấm **CREATE ROOM**. Pilot 2 chọn phòng bấm **JOIN ROOM** (hoặc Pilot 1 bấm **INVITE** Pilot 2). | Cả 2 pilot cùng hiển thị trong 4 slot phi thuyền của phòng. Thẻ phòng hiện đúng màu và tên của từng người. |
| **4. Sẵn sàng** | Cả 2 pilot bấm nút **READY**. | Nút đổi sang **NOT READY** (màu cam), nhãn đổi sang **READY** (xanh lá). Server tự đếm ngược 5 giây kèm âm thanh beep. |
| **5. Vào trận đấu (UDP)** | Hết đếm ngược, chuyển vào màn hình thi đấu `GamePanel`. | Gói tin UDP truyền liên tục: phi thuyền di chuyển mượt mà, xoay hướng nòng súng, bắn đạn đỏ/xanh/vàng, nhặt vật phẩm hồi máu/khiên. |
| **6. Kết thúc trận** | Hết thời gian hoặc còn 1 người sống sót. | Bảng tổng kết vinh danh người chiến thắng hiện lên, có thể bấm nút **Chơi lại (Rematch)** hoặc **Về sảnh (Lobby)**. |

---

## 6. XỬ LÝ SỰ CỐ THƯỜNG GẶP (TROUBLESHOOTING)

### Lỗi 1: Bấm "ENTER LOBBY" báo "[x] Failed to connect to server"
* **Nguyên nhân**:
  1. Chưa mở cổng 8888 trên Tường lửa của VPS (ufw hoặc Security Group).
  2. IP trong `config.properties` bị gõ sai.
  3. Server chưa chạy trên VPS.
* **Cách khắc phục**:
  * Kiểm tra server trên VPS đang chạy không: `ps aux | grep java`.
  * Kiểm tra port đang lắng nghe: `netstat -tulnp | grep 8888`.
  * Thử `telnet <IP_VPS> 8888` từ máy tính để xem cổng có thông không.

### Lỗi 2: Vào được sảnh và phòng, nhưng vào trận thì phi thuyền đứng im, không bắn được
* **Nguyên nhân**: Cổng TCP 8888 đã thông, nhưng cổng **UDP 9999** đang bị nhà cung cấp VPS chặn!
* **Cách khắc phục**:
  * Vào trang quản trị Cloud VPS (AWS/Oracle/Google Cloud) kiểm tra Inbound Rule, đảm bảo đã mở cổng **9999** với giao thức **UDP** (không phải TCP).
  * Chạy lệnh `sudo ufw allow 9999/udp` trên terminal VPS.

### Lỗi 3: Mở game lên không có tiếng nhạc hoặc chữ bị lỗi font
* **Nguyên nhân**: Thư mục `assets/` không nằm cùng cấp với file `HelicopterClient.jar`.
* **Cách khắc phục**: Đảm bảo cấu trúc thư mục phân phối giữ nguyên thư mục con `assets/` đi kèm file `.jar`.

---
*Tài liệu được khởi tạo và lưu trữ cho dự án Helicopter Combat - Lập trình mạng Socket TCP/UDP.*
