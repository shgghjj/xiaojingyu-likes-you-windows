🌐 [English](README_EN.md) | [简体中文](README.md)

---

# Xiaojingyu Likes You - Windows Edition

> **⚠️ Non-commercial notice: This software is for personal learning and research only. Any commercial use is strictly prohibited. See [LICENSE.md](LICENSE.md).**

The Windows desktop companion and all-round assistant for the genius catgirl AI **"Baiyin" (Albyra)**.
She is not just a girlfriend who chats with you - she can also get work done for you, just like Codex / Claude Code.

## ⚠️ Important Warnings

- **No commercial use**: This software must not be used for any commercial purpose.
- **AI content disclaimer**: AI-generated content is for reference only; the author is not responsible for its accuracy or any consequences.
- **API costs are on you**: Fees from third-party APIs such as DeepSeek / Gemini are paid by the user.
- **Automation has risks**: All sandbox / authorized-directory operations are recorded and support one-click rollback, but please grant permissions carefully.
- **Data security**: All data is stored locally and encrypted; uninstalling / deleting the directory will wipe the data.

## ✨ Core Features

### 💕 Companionship
- Genius catgirl "Baiyin": tsundere, quirky, trusts you unconditionally
- Memory system: fact archives / shared memories / auto organization
- Boredom system: the longer you do not talk, the more bored she gets, and she texts you first
- Jailbreak word library: built-in + custom
- TTS voice reading (Windows SAPI)
- Live2D stage (rendered in browser)

### 💻 Coding Assistant (like Codex / Claude Code)
- Run commands in authorized directories (each time with confirmation + timeout + logging)
- Git operations: status / log / branch / diff / commit / push
- Modify code files (sandbox or authorized directories)

### 📁 File Sandbox
- Default activity area: `Documents/Xiaojingyu Likes You/Sandbox`
- Full-disk text reading (requires the user to toggle it on)
- Modify / delete / hide: free inside the sandbox, requires authorization outside
- **Action ledger**: everything she does is recorded faithfully
- **One-click rollback**: restore all operations

### 🔌 Plugin System
- Reserved interfaces: new gameplay (online games, etc.) added as plugins
- `GameSessionPlugin` reserves the game-session interface

### 🖥️ Desktop Experience
- Resident in the system tray
- Desktop notifications (proactive messages)
- Quiet hours / daily limits

## 🚀 Running

### No-Install Version (Recommended)
1. Download the zip from Releases
2. Extract and run `XiaojingyuLikesYou.exe`

### Build from Source
Requires: JDK 17

```powershell
$env:JAVA_HOME='path-to-your-JDK17'
.\gradlew.bat createDistributable
```

Output: `build/compose/binaries/main/app/`

## 🔧 First Use
1. Open Settings -> fill in the DeepSeek API Key -> save
2. Chat with Baiyin
3. Optional: enable proactive contact / file reading / automatic actions / full autonomy

## 🔒 Privacy
- No user data collection, no private information uploads
- Data directory: `~/.xiaojingyu/` (encrypted storage)
- API Key is stored locally only

## 🧩 Open-Source Credits
Built on Compose Desktop, OkHttp, kotlinx-serialization. Live2D assets come from the official Live2D samples.
