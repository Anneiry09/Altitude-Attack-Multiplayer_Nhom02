# SỔ TAY "XÓA MÙ" GAME NETWORKING CHO NGƯỜI MỚI BẮT ĐẦU

Làm game mạng (Multiplayer Game) hoàn toàn khác với làm các ứng dụng quản lý (như Quản lý thư viện, Quản lý sinh viên) mà bạn từng học. Thay vì đợi người dùng bấm nút rồi mới xử lý dữ liệu, Game là một **thế giới luôn vận động liên tục**.

Dưới đây là 7 khái niệm cốt lõi bạn PHẢI hiểu trước khi viết dòng code đầu tiên. Hãy coi đây là "kim chỉ nam" để không bị lạc lối.

---

## 1. Game Loop (Vòng lặp Game) là gì?
Ở các app Java bình thường, code đứng im chờ bạn bấm nút (Event-Driven). Nhưng trong Game, thế giới không bao giờ dừng lại (đạn vẫn bay, thời gian vẫn trôi dù bạn không bấm gì).
Vì thế, trái tim của mọi game là một vòng lặp vô tận: `while (isRunning) { ... }`.
Trong vòng lặp này sẽ làm đúng 3 việc lặp đi lặp lại:
1. **Lấy Input:** Người chơi đang bấm phím gì? Chuột ở đâu?
2. **Update (Cập nhật Logic):** Tính toán vật lý, di chuyển máy bay, đạn bay, xét va chạm.
3. **Render (Vẽ):** Xóa màn hình cũ, vẽ hình ảnh mới lên tọa độ vừa cập nhật.

## 2. Tickrate (Server) vs FPS (Client)
- **FPS (Frames Per Second - Client):** Là số lần màn hình Client vẽ lại hình ảnh mỗi giây. Thường là 60 FPS (mỗi 16ms vẽ 1 lần) để mắt người thấy mượt.
- **Tickrate (Server):** Là số lần Server chạy vòng lặp Game Loop để tính toán logic mỗi giây. Với game của bạn, Server chỉ nên chạy **20 Tick/giây (20Hz)** (nghĩa là `Thread.sleep(50)`). 
-> *Tại sao?* Vì tính toán mạng tốn tài nguyên. Không cần gửi dữ liệu mạng 60 lần/giây, 20 lần là quá đủ để đồng bộ. Client sẽ tự làm mượt khoảng trống đó (đọc mục 6).

## 3. Server "Độc tài" (Authoritative Server)
Quy tắc sống còn: **Không bao giờ tin tưởng Client (Người chơi).**
- **Sai:** Client gửi gói tin: *"Tôi vừa bắn chết máy bay B"*. (Nếu code thế này, hacker sẽ gửi giả gói tin để thắng ngay lập tức).
- **Đúng:** Client gửi gói tin: *"Tôi vừa bấm nút cách (Space) lúc chuột đang chỉ ở góc 45 độ"*. Server sẽ nhận lệnh đó, tự tính đường đạn bay. Nếu đạn trúng, Server mới báo về: *"Máy bay B đã bị nổ"*.
👉 Client chỉ là cái **Tivi** để hiển thị và là cái **Tay cầm** để bấm. Mọi não bộ nằm ở Server.

## 4. Game State (Trạng thái Game) là gì?
Game State thực chất chỉ là một "bức ảnh chụp nhanh" (snapshot) chứa toàn bộ các CON SỐ của phòng chơi tại một phần ngàn giây. 
Ví dụ: `[Máy bay A đang ở X=100, Y=200, Góc=45, HP=100], [Đạn 1 đang ở X=150, Y=250]`.
Nhiệm vụ của Server là: Tính toán ra bức ảnh này -> Gửi cho toàn bộ Client -> Client nhìn số liệu vẽ ra hình.

## 5. UDP và Việc mất gói tin (Packet Loss)
Tại sao game lại dùng UDP dù nó có thể bị mất dữ liệu giữa đường?
- Hãy tưởng tượng bạn gửi tọa độ máy bay liên tục 20 lần/giây. 
- Nếu gói tin thứ 3 bị rớt mạng, bạn CÓ CẦN gửi lại gói tin thứ 3 không? **KHÔNG!**
- Vì gói tin thứ 4 (chứa tọa độ mới nhất) đã tới nơi rồi. Việc gửi lại tọa độ cũ trong quá khứ là vô nghĩa trong game. Đó là lý do UDP sinh ra cho game: Nhanh, nhẹ, rớt thì bỏ luôn, chờ cập nhật sau.

## 6. Interpolation (Nội suy) - Phép thuật che giấu độ trễ
Như đã nói, Server chỉ gửi tọa độ 20 lần/giây (cách nhau 50ms). Nhưng màn hình Client vẽ 60 lần/giây (cách nhau 16ms).
Nếu Client cứ nhận được tọa độ nào mới vẽ tọa độ đó, máy bay sẽ bị "dịch chuyển tức thời" (teleport/giật) mỗi 50ms.
**Cách giải quyết (Interpolation):**
Client sẽ lưu lại tọa độ CŨ (của 50ms trước) và tọa độ MỚI (vừa nhận). Sau đó, nó tự "trượt" máy bay một cách mượt mà từ điểm CŨ sang điểm MỚI trên màn hình. Mắt người sẽ thấy chuyển động trơn tru.

## 7. Trục Tọa Độ 2D trong Java (Quan trọng)
Trong Toán học, gốc tọa độ (0,0) nằm ở DƯỚI CÙNG BÊN TRÁI, trục Y hướng lên trên.
**Nhưng trong Java (và đa số Engine Game):**
- Gốc tọa độ (0,0) nằm ở **GÓC TRÊN CÙNG BÊN TRÁI** của màn hình.
- Trục X hướng sang phải.
- **Trục Y hướng XUỐNG DƯỚI.**
👉 Khi lập trình vật lý, muốn máy bay bay LÊN TRÊN, bạn phải TRỪ Y (Y = Y - 10). Muốn máy bay bay XUỐNG DƯỚI, bạn phải CỘNG Y (Y = Y + 10). Hãy ghi nhớ để không code nhầm hướng bay của đạn!

## 8. Tại sao phải gói dữ liệu bằng Byte (ByteBuffer)
Nhiều bạn mới sẽ dùng `String` để truyền qua mạng. Ví dụ: gửi chuỗi `"X:100;Y:200;Shoot:true"`.
- **Nhược điểm:** Phải dùng hàm `split(";")` rất tốn CPU để cắt chuỗi. Chuỗi text dài tốn băng thông mạng.
- **Giải pháp (Dùng Byte):** Mọi thứ biến thành mảng Byte. Tọa độ X là số nguyên (int = 4 byte). Góc quay (float = 4 byte). 
Máy tính đọc mảng Byte (10101010) cực kỳ nhanh (bằng 1/1000 thời gian xử lý String). Đó là lý do `java.nio.ByteBuffer` là người bạn thân thiết của bạn trong đồ án này.
