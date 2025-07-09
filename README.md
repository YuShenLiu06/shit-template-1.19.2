# ShitMod - Minecraft Fabric Mod

![Minecraft Fabric](https://img.shields.io/badge/Minecraft-1.19.2-brightgreen?style=flat-square)
![Fabric API](https://img.shields.io/badge/Fabric_API-Required-blue?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)

ShitMod 是一个为 Minecraft 1.19.2 Fabric 模组加载器设计的实用模组，添加了丰富的榜单系统和趣味功能，包括在线时间统计、玩家排行榜、白糖食用效果和特殊命令。

## 主要功能

### 🏆 榜单系统
- **实时榜单**：显示挖掘数量、在线时间和死亡次数
- **榜单轮播**：榜单每30秒自动切换
- **个性化控制**：
  - `/mylb` - 切换个人榜单显示
  - `/lb global on/off` - 管理员控制全局榜单开关
  - `/lb player <玩家> on/off` - 管理员控制特定玩家榜单
- **玩家数据管理**：
  - `/lb info <玩家>` - 查看玩家榜单信息
  - `/lb info <玩家> reset` - 重置玩家榜单数据

### ⏱️ 在线时间统计
- 精确记录玩家在线时间（精确到秒）
- 自动保存数据到文件
- `/lb info` 显示格式化的在线时间（xxhxxmin）
- 每分钟自动更新显示

### 🍬 白糖食用效果
- 白糖现在可食用
- 食用后获得特殊效果：
  - 漂浮效果（5秒）
  - 虚弱效果（15秒）
  - 反胃效果（15秒）
- 完整的食用动画和音效

### 💩 特殊命令
- `/shit` - 产生腐肉并消耗饱食度
  - 随机产生8-13个腐肉
  - 每次使用消耗3-5点饱食度
  - 饱食度不足时触发负面效果
- `/shit forbid <玩家>` - 禁止玩家使用/shit命令
- `/shit allow <玩家>` - 允许玩家使用/shit命令

## 安装指南

### 前置要求
- Minecraft 1.19.2
- [Fabric Loader](https://fabricmc.net/use/)
- [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)

### 安装步骤
1. 下载最新版本的 ShitMod
2. 将模组文件放入 Minecraft 的 `mods` 文件夹
3. 启动 Minecraft 并选择 Fabric 加载器
4. 创建或加入世界享受模组功能

## 管理员命令

| 命令 | 描述 | 权限等级 |
|------|------|----------|
| `/lb global on/off` | 开启/关闭全局榜单 | 2 |
| `/lb player <玩家> on/off` | 开启/关闭玩家榜单 | 2 |
| `/lb next` | 切换到下一个榜单 | 2 |
| `/lb refresh` | 刷新所有榜单 | 2 |
| `/lb info <玩家>` | 查看玩家榜单信息 | 2 |
| `/lb info <玩家> reset` | 重置玩家榜单数据 | 2 |
| `/shit forbid <玩家>` | 禁止玩家使用/shit命令 | 2 |
| `/shit allow <玩家>` | 允许玩家使用/shit命令 | 2 |

## 玩家命令

| 命令 | 描述 | 权限等级 |
|------|------|----------|
| `/mylb` | 切换个人榜单显示 | 0 |
| `/shit` | 执行特殊效果（消耗饱食度） | 0 |

## 配置文件
模组会在服务器根目录生成配置文件：
- `shitmod_playtime.json` - 存储玩家在线时间数据
- `shitmod_scoreboard.json` - 存储榜单配置

## 开发指南

### 构建环境
```bash
git clone https://github.com/yourusername/ShitMod.git
cd ShitMod
./gradlew build
```

### 依赖项
- Java 17+
- Gradle 7+
- Fabric Loom

### 贡献代码
1. Fork 本项目
2. 创建新分支 (`git checkout -b feature/your-feature`)
3. 提交更改 (`git commit -am 'Add some feature'`)
4. 推送分支 (`git push origin feature/your-feature`)
5. 创建 Pull Request

## 技术支持
- 问题报告：请提交到 [Issues](https://github.com/yourusername/ShitMod/issues)
- 功能建议：欢迎在 Discussions 中提出

## 许可证
本项目采用 [MIT 许可证](LICENSE)

---

**注意**：本模组仍在开发中，功能可能会有所变动。请定期查看更新日志以获取最新信息。

享受游戏！🎮
