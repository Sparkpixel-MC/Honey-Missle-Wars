# HoneyMissileWars（Paper 插件版）

将经典地图 **Honey Missile Wars 1.21.7**（原版数据包实现，作者 Chopper2112 / Supersette / kruthers，
[yeggs.org](https://www.yeggs.org/honey-missile-wars-2-0-0)）完整移植为 **Paper 1.21.10 插件**，
通过 **Advanced Slime Paper（ASP / SlimeWorld）** 实现单服多实例并行开局，并内置面向大型小游戏服务器的
队列 / 经济 / 统计 / 事件集成。使用 **Gradle** 构建。

> 数据包里"红石物理飞行器 + 结构方块 + 记分板状态机"的原版玩法全部保留：
> 33 种导弹（含社区导弹）、6 套导弹池、12 张程序化竞技场、蜂蜜盾、火球、手雷、
> 三层破墙判定、传送门破坏判负、大堂选队/设置室选项牌，全部在插件中 1:1 复刻。

---

## 1. 构建

```bash
cd HoneyMissileWars-Plugin
gradle build          # 需要 Gradle 8.10+ 与 JDK 21
# 产物: build/libs/HoneyMissileWars-1.0.0.jar
```

依赖仓库均在 `build.gradle.kts` 中声明（papermc / infernalsuite / extendedclip / jitpack）。
`file-loader` 会被 shade 进插件（ASP 文档要求：参考加载器不随服务器分发，需自行 shade）。

> **实现说明**：新版 Paper 的 API 中已不存在 Bukkit 结构 API
> （`org.bukkit.structure` / `org.bukkit.block.structure`）。本插件因此内置了一个
> 迷你 NBT 解析器（`NbtReader` + `StructureTemplate`），直接解析导弹的 `.nbt`
> 结构文件，并按**原版结构方块的旋转语义**（含 facing/axis/extended 等方块状态的旋转）
> 逐块粘贴——不依赖任何可能被 Paper 移除的结构 API，对 slime 虚拟世界也更稳。

## 2. 服务器部署

1. **安装 Paper 1.21.10**，并安装与该版本兼容的
   [Advanced Slime Paper](https://github.com/InfernalSuite/AdvancedSlimePaper)（4.0.0-SNAPSHOT 系）。
2. 将 `HoneyMissileWars-1.0.0.jar` 放入 `plugins/`，启动一次生成配置。
3. **地图不需要做任何修改**。把整个地图存档文件夹（含 `level.dat`、`region/`、`entities/` 等）
   原样上传到服务器**任意位置**——它不会被当作世界加载，datapacks 也不会生效
   （游戏逻辑已全部插件化，只读它的方块数据）。
4. 执行导入（把 anvil 存档读入 slime 格式模板，I/O 异步进行）：

   ```
   /hmwadmin import "/path/to/Honey Missile Wars 1.21.7"
   ```

   导入完成后模板以 `hmw_base.slime` 存放在 `plugins/HoneyMissileWars/slime_worlds/`。
5. 玩家通过 `/hmw join` 进入队列/游戏，或由前端系统调用 API 分配。

> 小建议（可选）：上传前在单人模式里把地图停在大堂视角再保存，模板会干净一点；
> 不做也没关系——对局开始时插件会原位重建竞技场。
> **内存提示**：每个对局实例都是模板的完整内存克隆（约 20–40MB/局）。
> 默认 `max-instances: 8`，请按 `实例数 × 地图大小` 预留堆内存。

## 3. 匹配队列（大型服务器推荐模式）

`queue.enabled: true` 时（默认），`/hmw join [arena]` 不再把玩家直接丢进地图大堂，而是：

1. 先尝试加入一个未满的等待中实例（有竞技场偏好就只匹配同图）；
2. 没有就进入**等待队列**并传送到配置的等待区（`queue.wait-zone`，默认主世界出生点；
   坐标可通过 `use-spawn: false` + x/y/z/yaw/pitch 自定义）；
3. 队列人数达到 `queue.min-players`（默认 8）后开始 `queue.countdown-seconds` 广播倒计时；
4. 倒计时结束：异步克隆一个全新 slime 实例 → 全员进入 → **自动随机分队** → 直接开始倒计时；
5. 玩家的竞技场偏好（`/hmw join honey`）会被带到开局，整批人自动开在 honey 图上；
   倒计时期间有人退出导致人数不足会自动取消并回到等待。

`queue.enabled: false` 则回到"地图玩法"模式：玩家直接进图的大堂，踩垫子选队、
在设置室调选项、按下开始按钮（人够 `game.min-players-to-start` 即可，单队也能开=原版单人模式）。

## 4. 游戏规则默认值（config.yml `game.options`）

原地图设置室的 10 余项规则全部进了配置文件，作为每个新实例的**默认值**
（对局内玩家仍可用大堂选项牌临时修改，与原地图一致）：

```yaml
game:
  options:
    item-rate: 2              # 物资速率: 3=Fast 2=Normal 1=Slow 0=Ultra Slow
    respawn-time: 2           # 重生时间: 0=立即 1=3秒 2=5秒 3=10秒
    jump-boost: 0             # 跳跃提升: 0=关 1..3=II/IV/VI
    speed-boost: 0            # 速度提升: 0=关 1..3=I/II/IV
    pickaxe: false            # 永久铁镐
    fall-damage: false        # 摔落伤害
    exploding-arrows: false   # TNT 箭
    elytra: false             # 3 秒鞘翅
    item-stacking: false      # true = Unlimited Items（取消同类道具上限）
    wall-missiles: false      # 允许在敌方墙体/传送门内放导弹
```

## 5. PvP

**已开启**，由三层规则构成：

1. 游戏世界的 Slime 属性 `PVP = true`（SlimePropertyMap，按 ASP 文档设置）；
2. 每局使用独立的计分板队伍，`allowFriendlyFire = false` —— 同队免伤，跨队正常互殴；
3. 仅在非对局阶段（大堂/倒计时/结算）全局禁伤，对局内不做任何伤害拦截。

## 6. Velocity / BungeeCord 回流

- `world.return-server` 填代理端的 lobby 服务器名。插件通过标准
  **BungeeCord plugin channel** 发送 `Connect` 消息——**Velocity 原生兼容该通道**
  （确认 `velocity.toml` 中 `bungee-plugin-channel-enabled = true`，默认开启）；
  BungeeCord 无需额外设置。
- `return-server` 留空则传送玩家到 `hub-world` 出生点并清空游戏物品。

## 7. 多竞技场多实例

- **12 张竞技场**（Classic/Cliff/Slope/Hill/Valley/Bridge/Island/Wall/Hardcore/Honey/Thick/Towers）
  由竞技场生成器在开局时于实例内原位重建——**不需要预烘焙 12 份模板**，一份 `hmw_base` 即可；
- 每个对局 = 一个独立命名的 slime 世界实例（`hmw_game_N`），互不干扰；
- `game.max-instances` 限制并发实例数；`max-players` 限制每局人数；
- `/hmw join <arena>` 指定偏好，队列与直连都遵循；
- `/hmw list`、`/hmwadmin list` 实时查看全部实例。

## 8. 游戏流程（与原地图一致）

- **大堂**：地图自带的大堂里，踩 红色/黄绿色/浅灰色 陶瓦垫 选队/离队；
  踩蓝色玻璃加速、黄色玻璃出粒子；两侧隧道传送到对方半场观景台。
- **设置室**：大堂两侧热区进入，右键 12 块选项牌切换规则。
- **开始**：按下大堂的云杉木按钮（再按取消）。倒计时期间插件原地重演原版生成器
  （每刻 3 列，快于原版每刻 1 列，玩家进场时竞技场已完整）。
- **对局**：开局补发 无限耐久弓（锋利9/火1/力量2）与队伍染色皮革装；
  按人数+速率周期性给全场发放同一件道具。右键导弹蛋在合法区域放置 → 结构粘贴 + 队伍重新着色。
  火球蛋=可被打靶定向的悬浮火球；鸡蛋=手雷（TNT 跟随 0.75s）；雪球=蜂蜜盾墙。
- **胜负**：三层染色玻璃墙逐层破防提示（守方挖己方玻璃/蜂蜜的当刻豁免，与原版一致）；
  己方巨型下界传送门平面被炸出空气洞 → 对方获胜；烟花庆祝后传送回大厅。
  掉出虚空 / 站进传送门 / 站在 y=0 屏障 = 死亡；重生倒计时按选项执行。

## 9. 面向大型服务器的集成

### 9.1 经济与经验（`rewards` 配置段）
- **Vault**（软依赖）：击杀、放置导弹、部署护盾、破墙（全队）、胜利、参与
  均按配置发放货币，消息可自定义。
- 同时/单独发放**原版经验**（`xp-per-coin` 系数）；未装 Vault 时自动降级为纯 XP。

### 9.2 玩家统计
- `plugins/HoneyMissileWars/stats.yml`：局数/胜场/击杀/死亡/导弹/护盾/破墙，异步读写。
- `/hmw stats [玩家]`、`/hmw top`；PlaceholderAPI（软依赖）：
  `%hmw_games% %hmw_wins% %hmw_kills% %hmw_deaths% %hmw_missiles% %hmw_shields%
  %hmw_breaches% %hmw_game% %hmw_team%`。

### 9.3 事件 API（供大厅/队列/任务系统监听）
```java
@EventHandler public void onEnd(GameEndEvent e) {
    Team winner = e.getWinner();          // null = 强制结束
    Game game = e.getGame();
}
```
`org.yeggs.hmw.api.event` 下提供：`GameStartEvent`、`GameEndEvent`、
`PlayerKillPlayerEvent`、`MissilePlaceEvent`、`WallBreachEvent`。

### 9.4 分配与回流
- 对局结束自动回程：配置 `world.return-server` 走 BungeeCord/Velocity 回流，
  否则传送至 `hub-world` 出生点并清空游戏物品。
- **断线重连**：对局中掉线 5 分钟内重进服务器自动归队参战（对应原地图 gameID 机制）。
- 空队/无人自动回收实例，服务器关闭时逐局卸载世界（遵循 ASP 常见问题文档：
  `unloadWorld(name, false)`，绝不在 onDisable 中带 save 卸载）。

## 10. 命令

| 命令 | 说明 |
|---|---|
| `/hmw join [arena]` | 加入/排队/创建对局 |
| `/hmw leave` | 退出当前对局或队列 |
| `/hmw list` | 查看所有实例 |
| `/hmw stats [player]` / `/hmw top` | 统计与排行 |
| `/hmw book` | 领取游戏手册 |
| `/hmwadmin import <folder>` | 导入 anvil 地图为 slime 模板 |
| `/hmwadmin templates` | 查看模板状态 |
| `/hmwadmin list` | 实例列表 |
| `/hmwadmin forcestart` / `end` | 强制开始/结束 |
| `/hmwadmin regenerate` | 同步重生成当前竞技场（调试用） |
| `/hmwadmin reload` | 重载配置 |

## 11. 与原地图的差异（有意为之）

| 原地图 | 插件版 | 原因 |
|---|---|---|
| 结构方块+红石块粘贴导弹 | 内置 NBT 解析器 + 原版旋转语义直接粘贴（旋转/偏移参数逐条照抄 kruthers 表） | slime 世界无 generated 目录；新版 Paper 已移除 Bukkit 结构 API |
| `chop:util/random` 线性同余随机（依赖缺失的 `const` 记分板，实际已损坏） | 插件 `Random` | 原版缺陷 |
| `use` 记分板"硬化盾"（原版即死代码） | 雪球 1 秒/落地自动展开 | 原版死代码 |
| 大堂展示架在切换导弹池时重贴 5 个模型 | 重命名悬浮名+刷新道具展示，模型保留模板状态 | 纯装饰性简化 |
| `/trigger endGame` 强制结束全场 | `/hmw leave` 仅个人退出，空队时才自动结束 | 服务器化更合理 |
| 管理员"Power Book"（34 个 trigger 动作） | `/hmwadmin` 命令替代 | 命令更适合运维 |

其余机制（破墙豁免、thick 场放置限制、epsilon y=60 活塞天花板、结构残渣鸡清理、
keepInventory 全套 gamerule、开局屏障墙、welcome 烟花等）均按原命令逐条移植。

## 12. 目录结构

```
HoneyMissileWars-Plugin/
├── build.gradle.kts / settings.gradle.kts / gradle.properties
└── src/main/
    ├── java/org/yeggs/hmw/
    │   ├── HoneyMissileWarsPlugin / HmwConfig / Messages / Fills
    │   ├── world/SlimeWorldService          ← ASP 导入/克隆/加载/卸载
    │   ├── arena/ArenaVariant + ArenaGenerator  ← 12 张图生成器（步进语义照抄）
    │   ├── missile/MissileType(33种参数表) + StructureService + NbtReader + StructureTemplate
    │   │        + StructureRotation(自研原版旋转) + MissileService + LobbyDisplayService
    │   ├── item/ItemFactory + MissileSet
    │   ├── game/Game(状态机) + GameManager + QueueService(匹配队列) + GameSettings
    │   │        + GameScoreboard + Team + PlayerSession
    │   ├── listener/…(大堂/导弹/投射物/方块/伤害/玩家 6 个监听器)
    │   ├── reward/RewardService + StatsService
    │   ├── integration/HmwApi + PlaceholderHook
    │   ├── api/event/…(5 个自定义事件)
    │   └── command/HmwCommand + HmwAdminCommand
    └── resources/
        ├── plugin.yml / config.yml
        └── structures/{minecraft,community}/*.nbt   ← 41 个导弹/盾结构（取自地图 generated 目录）
```

## 13. 致谢

- 地图与玩法：**Chopper2112、Supersette、kruthers**（社区导弹：IndigoLaser、Llew Vallis、Wasloigi & Pingu）
- Missiles Wars 原始玩法：Sethbling、Cubehamster（已授权重制）
- 世界多实例：[Advanced Slime Paper](https://github.com/InfernalSuite/AdvancedSlimePaper)
