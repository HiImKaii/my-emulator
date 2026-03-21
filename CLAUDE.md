# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a monorepo with two projects:

- **`freej2me/`** — J2ME emulator core (Java + C/C++ native). Runs MIDlet `.jar` games on desktop. Also supports WIPI/SKVM/KTF/LGT Korean game formats. **Note: `freej2me/` is its own git repository** (nested `.git/`), not a submodule of the parent.
- **`j2me-launcher/`** — Desktop launcher UI (Java 17 + JavaFX). Browser for managing and launching games via FreeJ2ME or WIE.

## Building

**FreeJ2ME** (from `freej2me/`):
```bash
cd freej2me/
ant
```
Outputs: `build/freej2me.jar` (AWT), `build/freej2me-lr.jar` (Libretro), `build/freej2me-sdl.jar` (SDL2).

**j2me-launcher** (from `j2me-launcher/`):
```bash
cd j2me-launcher/
mvn clean package -DskipTests
# Output: target/j2me-launcher-1.0.0.jar
```

**Native libretro core** (Linux, from `freej2me/src/libretro/`):
```bash
make
# Output: freej2me_libretro.so → RetroArch cores/
#         freej2me-lr.jar     → RetroArch system/
```

**Native SDL2 frontend** (Linux, from `freej2me/src/sdl2/`):
```bash
make
# Requires: SDL2, FreeImage, pthread
# Output: sdl_interface binary → /usr/local/bin/sdl_interface
```

## Running

**FreeJ2ME AWT** (standalone):
```bash
java -jar freej2me/build/freej2me.jar                              # interactive (file picker)
java -jar freej2me/build/freej2me.jar 'file:///path/to/game.jar' 240 320 2  # CLI
# Args: [file URL] [width] [height] [scale]
```

**j2me-launcher** (from `j2me-launcher/`):
```bash
./run.sh
```
The launcher expects `freej2me/build/freej2me.jar` at the path configured in its settings. It also supports WIE for Korean game formats (configured separately in launcher settings).

## Architecture

### freej2me — Three Frontends

`FreeJ2ME.java` — AWT desktop window with keyboard/mouse input, canvas rendering, window scaling, screenshot.
`Libretro.java` — Binary stdin/stdout protocol paired with a C `.so`/`.dll` core.
`Anbu.java` — Spawns external `sdl_interface` C++ binary, communicates via `Process` pipes.

### freej2me — Core Platform Layer (`org.recompile.mobile`)

`Mobile.java` — Static facade holding `MobilePlatform` singleton and keycode constants.
`MobilePlatform.java` — Abstract platform interface that each frontend implements (AWT, libretro, SDL2).
`MIDletLoader.java` — Custom `URLClassLoader` that loads game JARs. **Critical:** uses ASM bytecode instrumentation to intercept `Class.getResourceAsStream()` and redirect to the MIDlet's resources. Preloads all `javax.microedition.*` classes into itself to prevent `IllegalAccessError` across classloader boundaries. **Do not modify classloader logic without deep understanding of Java delegation.**
`PlatformImage.java` — Wraps `BufferedImage` as J2ME `Image`.
`PlatformGraphics.java` — Wraps `Graphics2D` as J2ME `Graphics`.
`PlatformFont.java` — J2ME `Font` backed by `java.awt.Font`.
`PlatformPlayer.java` — J2ME `Player` backed by Java `Clip`/`SourceDataLine`.

### j2me-launcher — Key Services

`GameScannerService` — Async scan of games folder, detects game type from JAR manifest attributes (`MIDlet-Name`→MIDLET, `WIPI-Version`→WIPI, `SKVM-Version`→SKVM, etc.).
`GameLauncherService` — Spawns subprocesses. FreeJ2ME uses `java -jar freej2me.jar 'file://...' width height scale` with JVM flags `--add-opens java.base/*=ALL-UNNAMED`. Also handles WIE subprocess spawning.
`GameCacheService` — Maintains `~/.cache/j2me-launcher/games.json` with scanned game metadata.
`ConfigService` — Manages `~/.config/j2me-launcher/config.json` application settings.

### j2me-launcher — Key Models

`GameInfo.java` — Game metadata (name, path, type, icon, last played).
`GameSettings.java` — Per-game settings: resolution, FPS cap, audio toggle.
`AppConfig.java` — Global launcher settings (games folder, emulator paths).

### Config & Data Locations

Per-game FreeJ2ME config: `./config/<gamename>/game.conf` (plain text key-value). Example:
```
width=240
height=320
fps=0
rotate=off
phone=Nokia
sound=on
```
**The `phone` setting controls arrow key behavior:** `Standard` (arrow keys = 2/4/6/8 numpad), `Nokia` (arrow keys = directional NOKIA_* codes), `Siemens`, `Motorola`.

RMS save data: stored in the working directory FreeJ2ME is run from.
Launcher config: `~/.config/j2me-launcher/config.json`
Launcher game cache: `~/.cache/j2me-launcher/games.json`
Launcher recent list: `~/.cache/j2me-launcher/recent.json`

### Keybindings (current active mappings)

See `freej2me/KEYMAP.md` for the full tables. Summary:

| Key | Function |
|-----|----------|
| F1 | Left softkey (menu/OK) |
| F2 | Right softkey (back/cancel) |
| Esc | freej2me in-game Options menu |
| Arrow keys | Navigation (directional) — sends NOKIA_* keycodes when `phone=Nokia` |
| Enter | Fire key (NOKIA_FIRE) |
| Backspace | Right softkey (back) |
| Q / W | Left / Right softkeys (same as F1/F2) |
| E / R | `*` / `#` |
| Ctrl+C | Screenshot (AWT frontend) |
| +/- | Window scale (AWT frontend) |
| Number row 0–9 | Phone numpad 0–9 (KEY_NUM0–KEY_NUM9) |
| Numpad 0–9 | Phone numpad 0–9 |
