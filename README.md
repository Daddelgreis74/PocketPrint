# PocketPrint 🖨️

> **The modern, privacy-first, 100% offline Android companion app for portable Bluetooth thermal printers (Denver MBP-32BK, WalkPrint, and compatible 58mm / 203 DPI ESC/POS models).**

---

## ✨ Features

- 🛒 **Smart Shopping List:** Add items via typing or **voice recognition (Speech-to-Text)**, automatically split items into single checklist entries, and print clear tear-off receipt checklists with `[ ]` / `[✓]`.
- 📌 **Notes & Memos:** Clean note cards with tags (`📌 Notiz`, `💡 Idee`, `⏰ Termin`, `⚠️ Wichtig`), date/timestamp, and formatted borders.
- 📷 **Photos & Graphics:** Advanced dithering engine (**Floyd-Steinberg**, **Bayer 4x4 Matrix (Retro)**, **Threshold**), plus live Brightness and Contrast controls.
- 📝 **Free Text & Templates:** Multi-line text formatting (Bold, Double size, Align Left/Center/Right), quick insertion for checkboxes, divider rules, and timestamps.
- 🏁 **QR- & Barcodes:** Live rendering and printing of QR-Codes (URLs, WiFi, text) and Code-128 Barcodes.
- 🎨 **Material 3 & Signature Themes:**
  - `📜 Nordic Paper` (Warm, organic paper texture & typography)
  - `✨ Aurora Glass` (Midnight dark, glowing cyber-cyan glassmorphism)
  - `☀️ Light`, `🌙 Dark`, and `⚙️ System` dynamic colors.
- 🔄 **In-App GitHub Auto-Updater:** Seamless updates directly from GitHub Releases without needing the Play Store.
- 🔒 **100% Offline & Privacy-Friendly:** Zero analytics, no account requirements, zero cloud dependencies. Works completely via local Bluetooth SPP.

---

## 📱 Hardware Compatibility

- **Native Support:** Denver MBP-32BK
- **Compatible Devices:** WalkPrint, YHK-MBP-32, PeriPage / Paperang compatible 57mm / 58mm Bluetooth Classic thermal printers.
- **Specifications:** 384 dots width (203 DPI), 8 lines tear-off feed margin, ESC/POS raster protocol.

---

## 🚀 Installation & Download

Download the latest APK directly from the [Releases](https://github.com/Daddelgreis74/PocketPrint/releases) page or build it from source.

```bash
# Clone the repository
git clone https://github.com/Daddelgreis74/PocketPrint.git
cd PocketPrint

# Build the Debug or Release APK
./gradlew assembleDebug
```

---

## 📄 License

Open-source under the Apache License 2.0. Built with Jetpack Compose & Material 3.
