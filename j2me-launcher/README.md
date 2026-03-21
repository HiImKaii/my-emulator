# J2ME Launcher

Desktop launcher cho J2ME games trên Fedora Linux, hỗ trợ **FreeJ2ME** (MIDlet games) và **WIE** (Korean WIPI/SKVM/KTF/LGT games).

## Tính năng

- **Game Library** - Quét thư mục chứa .jar/.jad files
- **Auto-detect** - Tự động nhận diện loại game (MIDlet, WIPI, SKVM, KTF, LGT)
- **Per-game Settings** - Resolution, FPS cap, audio riêng cho từng game
- **Recent Games** - 10 game gần đây
- **Search** - Tìm kiếm theo tên
- **Dark Theme** - Giao diện tối

## Cài đặt

### Yêu cầu
- Java 17+
- JavaFX 21
- Maven (để build)

### Build

```bash
cd /home/quan/Desktop/emulator/j2me-launcher
mvn clean package -DskipTests
```

### Chạy

```bash
./run.sh
```

## Cấu hình

Lần đầu chạy, ứng dụng tạo config tại:
- `~/.config/j2me-launcher/config.json` - App settings
- `~/.cache/j2me-launcher/games.json` - Game metadata cache
- `~/.cache/j2me-launcher/recent.json` - Recent games

### Settings
1. Click **Settings** trong sidebar
2. Điền đường dẫn:
   - **Games Folder** - Thư mục chứa game
   - **FreeJ2ME Path** - Đường dẫn tới freej2me.jar
   - **WIE Path** - Đường dẫn tới wie binary
3. Click **Save Settings**

## Cách sử dụng

| Chức năng | Mô tả |
|-----------|--------|
| Click 1 lần | Xem/chỉnh settings game |
| Double click | Khởi động game |
| Library | Xem tất cả game |
| Recent | 10 game gần chơi |
| Settings | Cấu hình đường dẫn |

## Project Structure

```
j2me-launcher/
├── src/main/java/com/j2me/launcher/
│   ├── Main.java              # Entry point
│   ├── controller/           # UI Controller
│   ├── model/                 # Data models
│   └── service/               # Business logic
├── src/main/resources/
│   ├── fxml/                  # UI layout
│   └── css/                   # Styles
├── pom.xml                   # Maven config
├── run.sh                    # Run script
└── README.md
```

## Troubleshooting

### "JavaFX not found"
```bash
export JAVAFX_HOME=~/.m2/repository/org/openjfx
./run.sh
```

### Game không khởi động
- Kiểm tra đường dẫn FreeJ2ME/WIE trong Settings
- Xem log tại `~/.cache/j2me-launcher/`

## License

MIT License
