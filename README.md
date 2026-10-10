# HDC Assistant

> HarmonyOS 版 ADB Idea —— 在 DevEco Studio 里一键执行 HDC 设备操作。
>
> *Bring the famous ADB Idea workflow to HarmonyOS development.*

通过 Tools 菜单对鸿蒙设备/模拟器执行常见的 `hdc` 操作，自动识别安装包、bundleName、EntryAbility 和 hdc 路径，自带 HDC 控制台输出。

## ✨ 功能特性

| 操作 | 说明 | 底层命令 |
|---|---|---|
| **Install App** | 自动寻找构建产物里最新 `.hap`，找不到弹窗手选 | `hdc install -r <hap>` |
| Uninstall App | 卸载应用 | `hdc uninstall <bundle>` |
| Kill App | 强杀应用进程 | `hdc shell aa force-stop` |
| Start / Restart App | 启动 / 重启 | `hdc shell aa start -b -a` |
| Clear App Data | 清理应用数据，可带重启 | `hdc shell bm clean -d` |
| Start with Debugger | 调试模式启动（`-D`，等待 hdc 调试器 attach） | `aa start -D` |
| Enable / Disable Wi-Fi | 开关 Wi-Fi | `ifconfig wlan0 up/down` |
| Enable / Disable Mobile Data | 开关移动数据，自动从 ifconfig 识别 rmnet/ccmni 等网卡 | `ifconfig <iface> up/down` |
| **Get Device UDID** | 获取设备 UDID 并自动复制到剪贴板 | `hdc shell bm get --udid` |

- 🔍 **Operations Popup**：`Ctrl+Alt+Shift+A` 呼出可搜索操作列表，输入即过滤
- 🧠 **自动识别**：bundleName / EntryAbility（解析 `app.json5`、`module.json5`，支持无 `mainElement` 时回退 `abilities[0]`）
- 📦 **安装包自动定位**：扫描 `<module>/build/*/outputs/**` 下最新修改的 `.hap`
- 🖥️ **HDC 控制台**：底部工具窗口实时输出每条命令和执行结果
- 🔧 **hdc 路径自动检测**：DevEco 自带 SDK → `%LOCALAPPDATA%\Huawei\Sdk` → 手动指定

## 🛠️ 环境要求

- DevEco Studio 4.x / 5.x / 6.x（平台 223+）
- 已通过 `hdc` 连接的设备或模拟器（`hdc list targets` 可见）

## 📥 安装

### 方式一：插件市场（推荐）

Settings → Plugins → Marketplace → 搜索 **HDC Assistant** → Install。
（插件正在提交 JetBrains 官方仓库审核，审核通过前请用方式二。）

### 方式二：本地安装

1. 下载 [HdcAssistant-1.1.0.zip](dist/HdcAssistant-1.1.0.zip)
2. DevEco Studio → Settings → Plugins → ⚙ → **Install Plugin from Disk...** 选择该 zip
3. 重启 IDE

## 🚀 使用

1. 用数据线或 Wi-Fi 连接设备，确认 `hdc list targets` 能看到设备
2. 打开 HarmonyOS 工程，通过以下任一入口执行操作：
   - **Tools → HDC Assistant** 菜单
   - `Ctrl+Alt+Shift+A` 呼出 Operations Popup，输入过滤（如 `install`、`udid`）回车执行
3. 底部 **HDC 工具窗口**查看命令输出；Get UDID 的结果会自动复制到剪贴板
4. 如需覆盖 hdc 路径或 bundleName：**Tools → HDC Assistant → HDC Settings...**

## 🏗️ 从源码构建

依赖：Python 3.x + DevEco Studio（自带 JBR 和平台 jar）。

```bash
python build.py
```

产出：

- `out/HdcAssistant.jar` —— 插件 jar（本地磁盘安装用）
- `dist/HdcAssistant-1.1.0.zip` —— 分发包（`lib/` 标准布局，上传插件市场用）

### 冒烟测试

验证命令构建、安装包定位、bundleName 识别、hdc 路径搜索等核心逻辑：

```bash
javac -encoding UTF-8 -cp out/classes -d out/smoke smoketest/Smoke.java
java -cp out/smoke;out/classes Smoke        # 末尾输出 ALL PASS 即通过
```

## 📂 项目结构

```
HdcIdea/
├── build.py                    # 构建脚本：javac 编译 → 打 jar → 打 zip
├── resources/META-INF/plugin.xml   # 插件清单（动作注册、兼容版本）
├── src/com/harmony/hdc/
│   ├── HdcCommands.java        # 纯逻辑命令构建器（不依赖 IDE，可单测）
│   ├── HdcRunner.java          # 后台执行命令序列，输出到控制台，UDID 复制剪贴板
│   ├── AbstractHdcAction.java  # Action 基类：hdc 定位 → hap 解析 → bundle 识别 → 执行
│   ├── HdcActions.java         # 菜单 Action（每个 Operation 一个嵌套类）
│   ├── HdcOperationsPopupAction.java  # Ctrl+Alt+Shift+A 可搜索弹窗
│   ├── HapLocator.java         # 构建产物里找最新 .hap（纯逻辑）
│   ├── ProjectModel.java       # 解析 app.json5 / module.json5 识别 bundleName / ability
│   ├── HdcPathLocator.java     # hdc 可执行文件定位（IDE 自带 SDK / 用户 SDK / 手动指定）
│   ├── HdcConsole.java         # HDC 工具窗口控制台面板
│   ├── HdcSettings.java        # 项目级设置（PropertiesComponent 持久化）
│   ├── HdcSettingsAction.java / HdcSettingsDialog.java  # 设置入口与对话框
│   └── HdcToolWindowFactory.java # 底部 HDC 工具窗口
├── smoketest/Smoke.java        # 脱离 IDE 的冒烟测试
└── dist/                       # 构建产物
```

设计分层：**命令构建（纯逻辑，可单测）→ 进程执行（HdcRunner）→ UI（Action/弹窗/控制台）**，与 ADB Idea 同款套路。

## 📋 版本历史

### 1.1.0

- 新增 **Install App**：`hdc install -r` 安装最新 `.hap`，自动识别 + 手选兜底
- 新增 **Get Device UDID**：`hdc shell bm get --udid`，结果自动复制剪贴板

### 1.0.0

- 初始版本：uninstall / kill / start / restart / clear data / 调试启动 / Wi-Fi / 移动数据开关

## 📄 License

MIT（LICENSE 文件随仓库提供）

## 🙋 致谢

灵感来自经典的 [ADB Idea](https://github.com/pbreault/adb-idea) 插件，把同样的工作流带给 HarmonyOS 开发者。
