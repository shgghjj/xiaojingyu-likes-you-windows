🌐 [简体中文](README.md) | [English](README_EN.md)

---

# 小鲸鱼喜欢你 · Windows 版

> **⚠️ 非商用声明：本软件仅供个人学习与研究使用，严禁任何形式的商业用途。详见 [LICENSE](LICENSE.md)。**

天才猫娘AI「白音」的 Windows 桌面伴侣 + 全能助手。
她不只是陪你聊天的小女友，还能像 Codex/Claude Code 一样帮你干活。

## ⚠️ 重要警告

- **严禁商用**：不得用于任何商业目的。
- **AI 内容免责**：AI 生成内容仅供参考，作者不对其准确性或后果负责。
- **API 费用自理**：DeepSeek / Gemini 等第三方 API 费用由用户自行承担。
- **自动化有风险**：沙盒/授权目录操作均有行动记录与一键恢复，但请谨慎授权。
- **数据安全**：所有数据本地加密存储，卸载/删除目录会清空数据。

## ✨ 核心功能

### 💕 陪伴
- 天才猫娘「白音」：傲娇、古灵精怪、无条件信任你
- 记忆系统：事实档案/共同回忆/自动整理
- 无聊值系统：越久不聊越无聊，主动找你说话
- 破甲词库：内置 + 自定义
- TTS 语音朗读（Windows SAPI）
- Live2D 舞台（浏览器渲染）

### 💻 编码助手（像 Codex/Claude Code）
- 在授权目录运行命令（每次确认+超时+记录）
- Git 操作：status/log/branch/diff/commit/push
- 修改代码文件（沙盒或授权目录）

### 📁 文件沙盒
- 默认活动区：`文档/小鲸鱼喜欢你/沙盒`
- 全盘文本读取（需用户开启开关）
- 修改/删除/隐藏：沙盒内自由，外部需授权
- **行动账本**：她干的所有事如实记录
- **一键恢复**：恢复全部操作

### 🔌 插件系统
- 预留接口：新玩法（联机游戏等）以插件形式添加
- `GameSessionPlugin` 预留游戏会话接口

### 🖥️ 桌面体验
- 系统托盘常驻
- 桌面通知（主动消息）
- 安静时段/每日上限

## 🚀 运行

### 免安装版（推荐）
1. 从 Releases 下载 zip
2. 解压后运行 `XiaojingyuLikesYou.exe`

### 从源码构建
需要：JDK 17

```powershell
$env:JAVA_HOME='你的JDK17路径'
.\gradlew.bat createDistributable
```

产物：`build/compose/binaries/main/app/`

## 🔧 首次使用
1. 打开设置 → 填入 DeepSeek API Key → 保存
2. 和白音聊天
3. 可选：开启主动联系/文件读取/自动行动/完全自主

## 🔒 隐私
- 不收集用户数据，不上传隐私
- 数据目录：`~/.xiaojingyu/`（加密存储）
- API Key 仅存本地

## 🧩 开源致谢
基于 Compose Desktop、OkHttp、kotlinx-serialization 构建。Live2D 资源来自 Live2D 官方示例。
