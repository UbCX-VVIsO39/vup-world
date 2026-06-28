# VUP 出道局 — 游戏性增强与 UI 重构方案

> 基于项目代码结构调研，针对游戏性短板、体验闭环、UI 布局、性能优化给出可落地的实现方案。
> 代码定位以主源码 `d:\Develop\Code\SSM\src` 为准。

---

## 目录

- [模块一：P0 游戏性功能补全](#模块一p0-游戏性功能补全)
  - [1.1 礼物影响直播收益](#11-礼物影响直播收益)
  - [1.2 弹幕影响直播质量](#12-弹幕影响直播质量)
  - [1.3 每日行动点系统](#13-每日行动点系统)
  - [1.4 对手进度可视化威胁](#14-对手进度可视化威胁)
- [模块二：P1 体验闭环](#模块二p1-体验闭环)
  - [2.1 每日中途存档](#21-每日中途存档)
  - [2.2 风险预警面板](#22-风险预警面板)
  - [2.3 结局图鉴与解锁驱动](#23-结局图鉴与解锁驱动)
  - [2.4 难度选择](#24-难度选择)
  - [2.5 快速回放/复盘](#25-快速回放复盘)
- [模块三：P2 锦上添花](#模块三p2-锦上添花)
  - [3.1 周目继承深化](#31-周目继承深化)
  - [3.2 粉丝来信回信影响 NPC 羁绊](#32-粉丝来信回信影响-npc-羁绊)
  - [3.3 排行榜接真实数据](#33-排行榜接真实数据)
  - [3.4 成就解锁实质奖励](#34-成就解锁实质奖励)
- [模块四：前端 UI 布局重构](#模块四前端-ui-布局重构)
  - [4.1 顶栏瘦身](#41-顶栏瘦身)
  - [4.2 左侧栏状态聚合](#42-左侧栏状态聚合)
  - [4.3 中央主决策区单卡片化](#43-中央主决策区单卡片化)
  - [4.4 右侧栏上下文情报](#44-右侧栏上下文情报)
  - [4.5 平台 Tab 改全屏抽屉](#45-平台-tab-改全屏抽屉)
  - [4.6 总览 Tab 改独立页](#46-总览-tab-改独立页)
  - [4.7 直播间浮层按需出现](#47-直播间浮层按需出现)
- [模块五：性能优化](#模块五性能优化)
  - [5.1 render() 拆分按区渲染](#51-render-拆分按区渲染)
  - [5.2 状态 diff 渲染](#52-状态-diff-渲染)
  - [5.3 panel-bundle.js 拆包](#53-panel-bundlejs-拆包)
- [附录：关键代码定位索引](#附录关键代码定位索引)

---

## 关键架构提示（实现前必读）

1. **直播标题结算不走 RewardCalculator**：`TitleService.settleStreamTitle`（`service\TitleService.java:670`）是手写 setter 链，不经 `RewardDelta`/`applyToVup`。新增 modifier 需直接在此方法内叠加，并在平行方法 `titlePreview`（行 338-488）同步加一段。
2. **RewardDelta record 不可变**：新增字段必须同步改 `empty()`/`applyFanCap()`/`applyNewPlayerProtection()`/`merge()`/`recomputeFanChange()` 5 处（`dto\RewardDelta.java`）。
3. **Vup 实体加字段链路**：`model\Vup.java` + `schema.sql` + `db\migration\V1__init_schema.sql`（已上线则新增 `V3__add_xxx.sql`）+ `VupMapper` SQL + 前端 `state`/`previousStats`。
4. **render 函数跨文件分散**：`render()` 编排器在 `js\panel-bundle.js:3102`，但被调用的子函数大半在 `app.js`（如 `renderActions` 在 app.js:2963、`renderEnding` 在 app.js:7186）。
5. **直播间互动层在 gameContainer 之外**：`#liveRightSidebar`/`#giftPanelOverlay`/`#giftFloatContainer` 都是 body 直接子节点，靠 fixed 定位浮于游戏界面之上，可独立处理。
6. **礼物/弹幕无持久累计**：仅 `giftComboCount` 瞬时态（bili-ui.js:81），3 秒清零；无跨会话累计。

---

# 模块一：P0 游戏性功能补全

## 1.1 礼物影响直播收益

### 现状
[bili-ui.js](file:///d:\Develop\Code\SSM\src\main\resources\static\bili-ui.js) 的 `sendGift()`（行 144）只触发动画，礼物 10 种（行 83-94）的 `price` 字段仅用于显示，不扣余额、不影响结算。直播间 UI 是"装饰壳"。

### 目标
玩家在直播阶段（NEED_TITLE / ACTION_RESOLVED）打赏礼物，累积为"直播热度加成"，在 `chooseTitle` 结算时折算为 `coin` + `watchHeat` 加成，让直播间 UI 真正连上游戏数值。

### 改动清单

**后端**
| 文件 | 改动 |
|---|---|
| [model/Vup.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\model\Vup.java) | 无需改（coin 已有） |
| [model/DaySession.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\model\DaySession.java) | 加字段 `int giftCount`、`int giftCoinValue`（本日累积礼物数与折算币值） |
| [db/migration/V3__add_gift_columns.sql](file:///d:\Develop\Code\SSM\src\main\resources\db\migration)（新建） | `ALTER TABLE day_session ADD COLUMN gift_count INT NOT NULL DEFAULT 0; ALTER TABLE day_session ADD COLUMN gift_coin_value INT NOT NULL DEFAULT 0;` |
| [mapper/DaySessionMapper.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\mapper) | `updateAfterAction` 的 SQL 加 `gift_count=#{giftCount}, gift_coin_value=#{giftCoinValue}`；新增 `incrementGift(vupId, day, count, coinValue)` |
| [dto/ActionDtos.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\dto\ActionDtos.java) | 新增 `record SendGiftRequest(String giftId, int qty, String idempotencyKey){}`；新增 `record GiftAccumulateDTO(int giftCount, int giftCoinValue, int coinBalance){}` |
| [web/StreamController.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\web\StreamController.java) | 新增 `POST /api/stream/gift`（入参 SendGiftRequest → GiftAccumulateDTO）。校验 phase∈{NEED_TITLE, ACTION_RESOLVED}，按 GIFTS 表的 price×qty 累加 giftCoinValue |
| [service/TitleService.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\service\TitleService.java) | 在 `settleStreamTitle`（行 670）的 `fortuneModifier` 块（行 903-920）之后、`return`（行 922）之前插入 `giftBonus` modifier：`int giftHeatBonus = session.getGiftCount() > 0 ? Math.min(20, session.getGiftCoinValue() / 500) : 0; vup.setWatchHeat(vup.getWatchHeat() + giftHeatBonus); vup.setCoin(vup.getCoin() + session.getGiftCoinValue());` 并在 evidenceRef 加 `"giftBonus": giftHeatBonus` |
| [service/infra/BalanceConfig.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\service\infra\BalanceConfig.java) | 加 `int giftHeatBonusCap(){return 20;}`、`int giftHeatBonusPerCoin(){return 500;}` |

**前端**
| 文件 | 改动 |
|---|---|
| [app.js](file:///d:\Develop\Code\SSM\src\main\resources\static\app.js) | `state`（行 124）加 `giftAccum: {count:0, coinValue:0}`；`previousStats`（行 183）加 `giftCoinValue` |
| [bili-ui.js](file:///d:\Develop\Code\SSM\src\main\resources\static\bili-ui.js) | 改造 `sendGift()`（行 144）：除现有动画外，调用 `apiPost('/api/stream/gift', {giftId, qty, idempotencyKey})`，返回值写入 `state.giftAccum`，刷新 `#giftAccumBadge` 显示 |
| [js/panel-bundle.js](file:///d:\Develop\Code\SSM\src\main\resources\static\js\panel-bundle.js) | `renderHeader`（行 3438）加礼物累积徽章；`renderTitles`（app.js:4444）在标题预览里展示 `+N 热度（来自礼物）` |
| index.html | 顶栏加 `<span id="giftAccumBadge" class="hidden">🎁 ×0</span>` |

### 实现步骤
1. 建 `V3__add_gift_columns.sql` 迁移脚本（Flyway 自动执行）
2. 改 DaySession 实体 + DaySessionMapper SQL
3. 加 StreamController `/api/stream/gift` 端点 + SendGiftRequest/GiftAccumulateDTO
4. 改 TitleService.settleStreamTitle 插入 giftBonus modifier（同步改 titlePreview）
5. 前端 bili-ui.js sendGift 调用新端点
6. 前端 state 加字段 + 渲染徽章

### 工作量：中（后端 0.5 天 + 前端 0.5 天）

---

## 1.2 弹幕影响直播质量

### 现状
`sendDanmaku()`（bili-ui.js:62）只生成飞行弹幕动画，无后端交互，无累积。

### 目标
直播阶段弹幕密度/情绪作为 `watchHeat`/`reputation` 修正器。玩家发送弹幕提升热度，但若发送负面弹幕（预设选项）会降口碑。

### 改动清单

**后端**
| 文件 | 改动 |
|---|---|
| [model/DaySession.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\model\DaySession.java) | 加字段 `int danmakuCount`、`int danmakuHeat` |
| V3 迁移脚本 | 追加 `ALTER TABLE day_session ADD COLUMN danmaku_count INT NOT NULL DEFAULT 0; ALTER TABLE day_session ADD COLUMN danmaku_heat INT NOT NULL DEFAULT 0;` |
| DaySessionMapper | `updateAfterAction` 加两字段；新增 `incrementDanmaku(vupId, day, heatDelta)` |
| [dto/ActionDtos.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\dto\ActionDtos.java) | 新增 `record SendDanmakuRequest(String text, String mood, String idempotencyKey){}`（mood∈{positive, neutral, negative}）；`record DanmakuAccumulateDTO(int count, int heat){}` |
| StreamController | 新增 `POST /api/stream/danmaku`（入参 SendDanmakuRequest → DanmakuAccumulateDTO）。positive: heat+2；neutral: heat+1；negative: heat+1 但 reputation-1（记到 pending） |
| TitleService.settleStreamTitle | 在 giftBonus 之后插入 `danmakuHeatBonus = Math.min(15, session.getDanmakuHeat())`；若 negative 弹幕累计>3 则 reputation 额外 -2 |
| BalanceConfig | 加 `int danmakuHeatBonusCap(){return 15;}`、`int danmakuNegativeReputationPenalty(){return 2;}` |

**前端**
| 文件 | 改动 |
|---|---|
| [bili-ui.js](file:///d:\Develop\Code\SSM\src\main\resources\static\bili-ui.js) | 改造 `sendDanmaku()`（行 62）：调用 `apiPost('/api/stream/danmaku', {text, mood, idempotencyKey})`。在输入框旁加 3 个心情切换按钮（😊正面/😐中性/😡负面），默认 positive |
| app.js | `state` 加 `danmakuAccum: {count:0, heat:0}`；`spawnDanmakuOverlayMessage`（行 3624）按 mood 着色（positive 绿/neutral 白/negative 红） |
| panel-bundle.js | `renderHeader` 加弹幕热度指示条 |

### 实现步骤
1. V3 迁移脚本加 danmaku 字段
2. DaySession + Mapper 改造
3. StreamController `/api/stream/danmaku` 端点
4. TitleService 插入 danmakuHeatBonus modifier（同步 titlePreview）
5. 前端 bili-ui.js 改造 + 心情切换 UI

### 工作量：中（0.5 天 + 0.5 天）

---

## 1.3 每日行动点系统

### 现状
每天仅 1 个主行动，30 天 = 30 次决策，节奏偏慢、决策点稀疏。

### 目标
引入"行动点"（AP）：主行动耗 2 点，场外/NPC 互动/粉丝群各耗 1 点，初始 4 点/天。大幅增加每日决策密度。

### 改动清单

**后端**
| 文件 | 改动 |
|---|---|
| [domain/ActionType.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\domain\ActionType.java) | 加方法 `int actionPointCost()`：STREAM_PLAN/PUBLISH_VIDEO/PUBLISH_CLIP=2，其余主行动=2，场外=1 |
| [model/DaySession.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\model\DaySession.java) | 加字段 `int actionPoints`（当日剩余）、`int maxActionPoints` |
| V3 迁移脚本 | `ALTER TABLE day_session ADD COLUMN action_points INT NOT NULL DEFAULT 4; ALTER TABLE day_session ADD COLUMN max_action_points INT NOT NULL DEFAULT 4;` |
| DaySessionMapper | `updateAfterAction` 加两字段 |
| [service/ActionService.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\service\ActionService.java) | `doSubmitAction`（约行 200）校验 `session.getActionPoints() >= ActionType.valueOf(actionType).actionPointCost()`，不足抛 GameException("行动点不足")；提交后 `session.setActionPoints(session.getActionPoints() - cost)` |
| [service/core/DayService.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\service\core\DayService.java) | `doNextDay` 创建新 DaySession 时 `actionPoints = maxActionPoints = balanceConfig.dailyActionPoints()` |
| BalanceConfig | 加 `int dailyActionPoints(){return 4;}`（可配 `game.run.daily-action-points`） |
| [domain/DayPhaseStateMachine.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\domain) | 调整：`READY` 状态下若 actionPoints≥2 可继续行动；actionPoints<2 时只允许 REST/SKIP 进 OFF_STREAM |
| ActionDtos.java | `ActionOptionDTO` 加 `int actionPointCost` 字段，前端展示 |

**前端**
| 文件 | 改动 |
|---|---|
| app.js | `state` 加 `actionPoints`；`previousStats` 加 `actionPoints` |
| [js/panel-bundle.js](file:///d:\Develop\Code\SSM\src\main\resources\static\js\panel-bundle.js) | `renderHeader`（行 3438）加行动点显示 `⚡×3/4`；`renderActions`（app.js:2963）每张行动卡片标注 `⚡2`，AP 不足的卡片置灰 |
| index.html | 顶栏加 `<span id="actionPointBadge">⚡ ×4</span>` |
| 动画 | `animateStatChanges` 加 actionPoints 变化反馈（消耗时 `-2` 浮动数字） |

### 实现步骤
1. V3 迁移脚本加 action_points 字段
2. ActionType 加 actionPointCost() 方法
3. DaySession + Mapper + DayService 初始化
4. ActionService 校验与扣减逻辑
5. DayPhaseStateMachine 放宽 READY 多次行动
6. 前端 AP 显示 + 卡片置灰

### 工作量：中偏大（后端 1 天 + 前端 0.5 天）
> ⚠️ 这是游戏性改动最大的方案，需重新平衡数值（30 天 × 多行动会使粉丝增长过快，需调 BalanceConfig 的 fanCap 阶段上限）

---

## 1.4 对手进度可视化威胁

### 现状
[RivalProgressService](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\service\progression) 存在但前端弱，玩家感受不到竞争压力。

### 目标
平台 Tab 展示"7 日流量竞争进度条"，对手粉丝增长逼近，制造紧迫感；被超越时触发负面事件。

### 改动清单

**后端**
| 文件 | 改动 |
|---|---|
| [service/progression/RivalProgressService.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\service\progression) | `progress()` 返回值增加 `List<RivalDTO>` 含 `name/route/fans/growthRate/threatLevel`；每日推进时按路线相关性增长对手粉丝；若对手粉丝超过玩家×1.2 触发 `RivalOvertakeEvent` |
| [dto/NpcDtos.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\dto\NpcDtos.java) | 新增 `record RivalDTO(String name, String route, int fans, int growthRate, String threatLevel){}`；`RivalProgressDTO` 加 `List<RivalDTO> rivals`、`boolean overtaken` |
| [web/RivalController.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\web) | `GET /api/rivals` 返回扩展后的 RivalProgressDTO |
| EventService | 新增 `RivalOvertakeEvent` 事件类型（NEED_EVENT_CHOICE），选项：硬刚（消耗 AP+口碑风险）/ 借势联动（需 NPC 羁绊）/ 放任（掉粉） |

**前端**
| 文件 | 改动 |
|---|---|
| app.js | `state` 加 `rivals` |
| [js/panel-bundle.js](file:///d:\Develop\Code\SSM\src\main\resources\static\js\panel-bundle.js) | 新增 `renderRivalThreat()` 函数，在平台 Tab 渲染水平进度条对比（玩家 vs 最强对手），威胁高时红色脉冲 |
| refreshAfterWrite | 加 `state.rivals = await api('/api/rivals')` |
| [css/animations.css](file:///d:\Develop\Code\SSM\src\main\resources\static\css\animations.css) | 加 `@keyframes threatPulse` 红色脉冲 |

### 实现步骤
1. RivalProgressService 扩展对手数据 + 每日增长
2. RivalDTO + RivalController 端点
3. RivalOvertakeEvent 事件
4. 前端 renderRivalThreat 进度条

### 工作量：中（1 天）

---

## 模块二：P1 体验闭环

## 2.1 每日中途存档

### 现状
只有日报后收尾才落库，玩家中途退出只能弃局。day_session 已有 `locked` 字段但未用。

### 目标
支持"暂停存档"——任意阶段可保存当前 day_session 状态，退出后可恢复。

### 改动清单

**后端**
| 文件 | 改动 |
|---|---|
| [service/core/GameService.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\service\core\GameService.java) | 新增 `pauseSave(userId, slot)`：把当前 vup + day_session 状态序列化到 `manual_save_snapshot` 表（已存在），phase 也存入；`resumeSave(userId, slot)` 反序列化恢复 phase |
| [model/ManualSaveSnapshot.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\model\ManualSaveSnapshot.java) | 确认有 `phase` 字段，无则加 |
| [web/GameController.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\web) | 新增 `POST /api/save-slots/{n}/pause-save`、`POST /api/save-slots/{n}/resume` |
| DaySessionMapper | 新增 `updatePhase(vupId, day, phase)` 用于恢复 |

**前端**
| 文件 | 改动 |
|---|---|
| [js/panel-bundle.js](file:///d:\Develop\Code\SSM\src\main\resources\static\js\panel-bundle.js) | 顶栏加"⏸ 暂停存档"按钮，调用 `/pause-save`；存档槽卡片显示"暂停中（Day N · 阶段）"标识 |
| [app.js](file:///d:\Develop\Code\SSM\src\main\resources\static\app.js) | 新增 `pauseSave()` async 函数 |

### 实现步骤
1. 确认 manual_save_snapshot 表结构，补 phase 字段（V3 迁移）
2. GameService 实现 pauseSave/resumeSave
3. GameController 两个端点
4. 前端暂停按钮 + 槽位标识

### 工作量：小（0.5 天）

---

## 2.2 风险预警面板

### 现状
旧账/债务/运营压力概念多，反馈滞后，新手莫名其妙崩盘。

### 目标
右侧栏聚合"危机倒计时"：即将到期旧账、运营压力值、粉丝不满度，红色高亮。

### 改动清单

**后端**
| 文件 | 改动 |
|---|---|
| [service/risk/DebtService.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\service\risk) | 新增 `crisisAlerts(vupId)` 返回 `List<CrisisAlertDTO>`：扫描 OPEN 状态 risk_debt，按 dueDay-day 排序 |
| [service/operating/OperatingPressureService.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\service\operating) | 暴露当前压力值与阈值 |
| [dto/RiskToolDtos.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\dto\RiskToolDtos.java) | 新增 `record CrisisAlertDTO(String type, String title, int daysLeft, String severity, String description){}` |
| [web/RiskToolController.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\web) | 新增 `GET /api/risk-tool/alerts` |

**前端**
| 文件 | 改动 |
|---|---|
| app.js | `state` 加 `crisisAlerts: []` |
| [js/panel-bundle.js](file:///d:\Develop\Code\SSM\src\main\resources\static\js\panel-bundle.js) | 新增 `renderCrisisAlerts()` 渲染危机列表，daysLeft≤2 红色脉冲 |
| refreshPhaseData | 加 `state.crisisAlerts = await api('/api/risk-tool/alerts')` |
| index.html | 右侧栏加 `<div id="crisisAlertPanel"></div>` |

### 实现步骤
1. DebtService.crisisAlerts 扫描到期债务
2. CrisisAlertDTO + RiskToolController 端点
3. 前端 renderCrisisAlerts + 红色高亮

### 工作量：小（0.5 天）

---

## 2.3 结局图鉴与解锁驱动

### 现状
game_unlock 表已有，但重开时是否展示"已解锁 X/9"未知，缺重玩钩子。

### 目标
重开入口展示结局图鉴（已解锁/未解锁灰显）+ 推荐下一目标路线。

### 改动清单

**后端**
| 文件 | 改动 |
|---|---|
| [service/ending/EndingAtlasService.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\service\ending) | 确认 `restartTarget`/`nextRestartTargetType` 已返回推荐；新增 `atlasOverview(userId)` 返回 9 结局的解锁状态 + 推荐目标 |
| [dto/EndingDtos.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\dto\EndingDtos.java) | 新增 `record EndingAtlasDTO(List<EndingAtlasItem> items, String recommendedNext, int unlockedCount, int totalCount){}`；`record EndingAtlasItem(String endingType, String title, boolean unlocked, String hint){}` |
| [web/EndingController.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\web\EndingController.java) | 新增 `GET /api/ending/atlas` |

**前端**
| 文件 | 改动 |
|---|---|
| app.js | `state` 加 `endingAtlas` |
| [js/panel-bundle.js](file:///d:\Develop\Code\SSM\src\main\resources\static\js\panel-bundle.js) | 新增 `renderEndingAtlas()` 在重开入口展示 3×3 结局网格，未解锁灰显+提示文字，推荐目标高亮 |
| [css/bili-theme.css](file:///d:\Develop\Code\SSM\src\main\resources\static\css\bili-theme.css) | 加 `.ending-atlas-item.locked` 灰度滤镜 |

### 实现步骤
1. EndingAtlasService.atlasOverview 聚合 game_unlock
2. EndingAtlasDTO + EndingController 端点
3. 前端 renderEndingAtlas 网格

### 工作量：小（0.5 天）

---

## 2.4 难度选择

### 现状
无难度选择，新手门槛高。

### 目标
开局选 简单/标准/硬核，影响旧账频率、对手强度、初始资源、AP 上限。

### 改动清单

**后端**
| 文件 | 改动 |
|---|---|
| [service/infra/BalanceConfig.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\service\infra\BalanceConfig.java) | 加 `enum Difficulty{EASY,STANDARD,HARD}`；构造器注入 `@Value("${game.run.difficulty:STANDARD}")`；所有数值方法加难度系数分支（如 `dailyActionPoints()` EASY=5/STANDARD=4/HARD=3；`titleBackfireDebtDelay()` EASY+2/HARD-2） |
| [model/Vup.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\model\Vup.java) | 加字段 `String difficulty` |
| V3 迁移脚本 | `ALTER TABLE vup ADD COLUMN difficulty VARCHAR(16) NOT NULL DEFAULT 'STANDARD';` |
| [dto/GameDtos.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\dto\GameDtos.java) | `CreateVupRequest` 加 `String difficulty`；`QuickStartRequest` 加 `String difficulty` |
| [service/core/VupService.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\service\core\VupService.java) | `createOrResumeVup` 按难度设 BalanceConfig（或 BalanceConfig 改为按 vup.difficulty 读取） |

**前端**
| 文件 | 改动 |
|---|---|
| [js/panel-bundle.js](file:///d:\Develop\Code\SSM\src\main\resources\static\js\panel-bundle.js) | 开局入口加 3 个难度卡片（简单🌿/标准⚖️/硬核🔥），各显示差异说明 |
| [app.js](file:///d:\Develop\Code\SSM\src\main\resources\static\app.js) | `quickStartGuest` 传 difficulty 参数 |

### 实现步骤
1. BalanceConfig 加 Difficulty 枚举 + 系数分支
2. Vup 加 difficulty 字段 + V3 迁移
3. DTO 加 difficulty + VupService 按难度初始化
4. 前端难度选择卡片

### 工作量：中（1 天）
> ⚠️ 难度系数需全表调平，建议先只调 AP/旧账延迟/初始资源三项

---

## 2.5 快速回放/复盘

### 现状
business_log 存了完整明细（乘数/上限/钳制/权重/RNG），但结局后无回放。

### 目标
结局后展示"本局时间线回放"——30 天关键决策点 + 数值曲线。

### 改动清单

**后端**
| 文件 | 改动 |
|---|---|
| [web/ReportsController.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\web) | 新增 `GET /api/reports/timeline?vupId=` 返回按天聚合的 BusinessLog 摘要（day/action/fanChange/routeScore/关键事件） |
| [dto/ReportDtos.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\dto\ReportDtos.java) | 新增 `record TimelineDTO(List<TimelineDayDTO> days){}`；`record TimelineDayDTO(int day, String action, int fanChange, int routeScore, String highlight){}` |
| BusinessLogMapper | 新增 `findTimelineByVupId(vupId)` 按天聚合查询 |

**前端**
| 文件 | 改动 |
|---|---|
| [js/panel-bundle.js](file:///d:\Develop\Code\SSM\src\main\resources\static\js\panel-bundle.js) | 新增 `renderTimelineReplay()` 用 Chart.js（已引入）画粉丝增长曲线 + 关键决策点标注 |
| [app.js](file:///d:\Develop\Code\SSM\src\main\resources\static\app.js) | 结局后 `state.timeline = await api('/api/reports/timeline')` |
| index.html | 结局全屏加"📜 查看回放"按钮 |

### 实现步骤
1. BusinessLogMapper.findTimelineByVupId 聚合查询
2. TimelineDTO + ReportsController 端点
3. 前端 Chart.js 曲线 + 决策点

### 工作量：中（1 天）

---

# 模块三：P2 锦上添花

## 3.1 周目继承深化

### 目标
二周目解锁新标题池/新事件/隐藏路线，让重玩有质变。

### 改动清单
**后端**
| 文件 | 改动 |
|---|---|
| [service/ending/RebirthService.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\service\ending) | `restart` 时按 previousEndingType 解锁对应"继承内容"：歌势结局→解锁新歌类标题池；黑红结局→解锁高风险高回报标题 |
| [service/content/TitleService.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\service\content) | 标题候选生成时，若 `vup.previousEndingId` 匹配，从 game_content 加载 `category=INHERITED_TITLE` 的扩展池 |
| [db/migration/V3__add_gift_columns.sql](file:///d:\Develop\Code\SSM\src\main\resources\db\migration) | 无需改表，内容走 game_content 的 EAV 模式新增 `INHERITED_TITLE` category |
| V2 种子或新迁移 | 插入 ~20 条 `INHERITED_TITLE` 内容，sub_key 标记来源结局类型 |

### 实现步骤
1. 设计 9 结局各自的继承内容（标题/事件）
2. game_content 加 INHERITED_TITLE 数据
3. TitleService 按 previousEndingId 扩展候选池
4. RebirthService 标记继承状态

### 工作量：中（1 天）

---

## 3.2 粉丝来信回信影响 NPC 羁绊

### 目标
粉丝来信回信（`/api/fan-letter/reply`）影响对应 NPC 羁绊，建立"粉丝-NPC"联动。

### 改动清单
**后端**
| 文件 | 改动 |
|---|---|
| [service/fan/FanLetterService.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\service\fan) | `reply` 方法内，按信件 `npcBinding` 字段调用 `npcRelationshipService.interact` 增加亲密度 +1~3 |
| [dto/FanLetterDtos.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\dto\FanLetterDtos.java) | `FanLetterDTO` 加 `String npcBinding`（信件关联的 NPC） |
| game_content | FAN_LETTER 内容加 `npcBinding` 字段到 content_json |

### 实现步骤
1. FanLetter 内容补 npcBinding 字段
2. FanLetterService.reply 联动 NpcRelationshipService
3. 前端来信列表展示关联 NPC 标签

### 工作量：小（0.5 天）

---

## 3.3 排行榜接真实数据

### 目标
右侧排行榜（粉丝榜/互动榜/弹幕榜）从 mock 改为真实 NPC 数据。

### 改动清单
**后端**
| 文件 | 改动 |
|---|---|
| [service/progression/RivalProgressService.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\service\progression) | 新增 `leaderboard(userId)` 返回含玩家 + 8 NPC 的粉丝/互动/弹幕三榜 |
| [dto/NpcDtos.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\dto\NpcDtos.java) | 新增 `record LeaderboardDTO(List<LeaderboardItem> fans, List<LeaderboardItem> gifts, List<LeaderboardItem> danmaku){}` |
| [web/RivalController.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\web) | 新增 `GET /api/leaderboard`（若已有则扩展返回三榜） |

**前端**
| 文件 | 改动 |
|---|---|
| [bili-ui.js](file:///d:\Develop\Code\SSM\src\main\resources\static\bili-ui.js) | `updateSidebarContent()`（行 242）填充三榜真实数据，移除 mock |

### 实现步骤
1. RivalProgressService.leaderboard 聚合 NPC 数据
2. LeaderboardDTO + 端点
3. 前端 bili-ui.js 填充三榜

### 工作量：小（0.5 天）

---

## 3.4 成就解锁实质奖励

### 目标
成就解锁给实质奖励（灵感/金币/初始 buff），而非仅展示。

### 改动清单
**后端**
| 文件 | 改动 |
|---|---|
| [service/progression/AchievementService.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\service\progression) | `unlock` 时按成就 `rewardType` 发放：INSPIRATION→vup.inspiration+5；COIN→vup.coin+500；BUFF→写 tutorialFlagsJson 标记开局 buff |
| [dto/AchievementDtos.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\dto) | `AchievementDTO` 加 `String rewardType`、`int rewardValue` |
| game_content | ACHIEVEMENT 内容的 content_json 加 rewardType/rewardValue |
| RebirthService | `restart` 时检查已解锁成就，应用开局 buff |

### 实现步骤
1. 30 条成就内容补 reward 字段
2. AchievementService.unlock 发放奖励
3. RebirthService 应用开局 buff

### 工作量：小（0.5 天）

---

# 模块四：前端 UI 布局重构

> 目标布局（详见前述对话）：
> ```
> ┌─────────────────────────────────────────────────┐
> │ 顶栏：天数·阶段徽章·5 项核心数值·AP·暂停         │
> ├──────────┬──────────────────────┬───────────────┤
> │ 左栏     │  中央主决策区        │  右栏情报     │
> │ VUP状态  │  (阶段驱动单卡片)    │  危机倒计时   │
> │ 雷达图   │  9行动网格/标题/事件 │  对手威胁     │
> │ 粉丝饼图 │  /日报/结局          │  运势·combo   │
> │ 路线分   │  + 阶段进度条        │  教练建议     │
> ├──────────┴──────────────────────┴───────────────┤
> │ 直播间浮层(仅直播阶段出现)                       │
> └─────────────────────────────────────────────────┘
> ```

## 4.1 顶栏瘦身

### 现状
顶栏（[index.html:112-167](file:///d:\Develop\Code\SSM\src\main\resources\static\index.html)）塞了天数/阶段/进度条/快捷属性/直播间信息/BGM/主播模式/菜单/帮助/设置/用户，过载。

### 改动清单
| 文件 | 改动 |
|---|---|
| [index.html](file:///d:\Develop\Code\SSM\src\main\resources\static\index.html) | header-right 移除 `#liveInfoCompact`(144)、`#bgmToggle`/`#bgmVolume`(150-151)、`#streamerModeToggle`(153)，收进 `#settingsPanel`；保留 `#dayBadge`/`#phaseText`/`#quickStats`/`#actionPointBadge`/`#giftAccumBadge`/`#pauseSaveBtn`/齿轮/`#userInfo` |
| [js/panel-bundle.js](file:///d:\Develop\Code\SSM\src\main\resources\static\js\panel-bundle.js) | `renderHeader`（行 3438）精简为 5 项核心数值 chips（粉丝/口碑/热度/体力/金币）+ AP + 礼物徽章 |
| [css/layout.css](file:///d:\Develop\Code\SSM\src\main\resources\static\css\layout.css) | `.game-header.bili-style`（行 2871）调整 padding，header-right 用 `gap:8px` |

### 步骤
1. index.html 移元素到 settingsPanel
2. renderHeader 精简 chips
3. CSS 调间距

### 工作量：小（0.5 天）

---

## 4.2 左侧栏状态聚合

### 目标
左侧栏去重（顶栏已显示的 5 项不重复），放 6 维雷达图 + 粉丝结构饼图 + 路线分迷你条。

### 改动清单
| 文件 | 改动 |
|---|---|
| [index.html](file:///d:\Develop\Code\SSM\src\main\resources\static\index.html) | `.main-panel-left`（行 213）内 `#statsSection` 改为三个子区：`#radarChart`、`#fanPieChart`、`#routeScoreMini` |
| [js/panel-bundle.js](file:///d:\Develop\Code\SSM\src\main\resources\static\js\panel-bundle.js) | `renderStats`（行 4175）改为用 Chart.js（已引入 `vendor/chartjs`）画雷达图（6 维能力）+ 饼图（4 类粉丝）+ 路线分条；移除与顶栏重复的数值 |
| [app.js](file:///d:\Develop\Code\SSM\src\main\resources\static\app.js) | 新增 `initStatCharts()` 在 hydrate 后初始化两个 Chart.js 实例，render 时 `update()` 而非重建 |

### 步骤
1. index.html 左栏结构改造
2. renderStats 接入 Chart.js
3. initStatCharts 初始化 + update

### 工作量：中（1 天）

---

## 4.3 中央主决策区单卡片化

### 目标（核心 UX 改动）
根据 DayPhase 只显示一个主交互卡片，玩家永远知道"现在该干嘛"。

### 改动清单
| 文件 | 改动 |
|---|---|
| [index.html](file:///d:\Develop\Code\SSM\src\main\resources\static\index.html) | `.main-panel-right`（行 227）内 `.game-panels`（239）改为单一 `#mainDecisionCard` 容器，移除 `#actionPanel`/`#offStreamPanel`/`#endingPanel`/`#secondaryPanels` 的并列，改为按 phase 切换内容 |
| [js/panel-bundle.js](file:///d:\Develop\Code\SSM\src\main\resources\static\js\panel-bundle.js) | 新增 `renderMainDecisionCard()` 统一调度：phase=READY→渲染 `renderActions` 内容到主卡片；NEED_TITLE→渲染 `renderTitles`；NEED_EVENT_CHOICE→`renderEvent`；OFF_STREAM_READY→`renderOffStreamPanel`；REPORT_READY→`renderReport`+下一天按钮；ENDING_READY→`renderEnding`。在 `render()` 编排器（行 3102）中替换原 5 个分散调用 |
| [app.js](file:///d:\Develop\Code\SSM\src\main\resources\static\app.js) | `renderActions`(2963)/`renderTitles`(4444)/`renderEvent`(4650)/`renderOffStreamPanel`(3286)/`renderEnding`(7186) 改为返回 HTML 字符串而非直接 setHtml，供 renderMainDecisionCard 注入 |
| index.html | 主卡片下方加横向 4 步进度条 `#phaseStepper`（行动→标题→事件→日报），高亮当前步 |

### 步骤
1. index.html 改主决策区结构 + 进度条
2. renderMainDecisionCard 调度器
3. 5 个 render* 改返回字符串
4. render() 编排器替换调用

### 工作量：中偏大（1.5 天）
> ⚠️ 这是 UX 提升最大的改动，但涉及 5 个核心 render 函数改造，需谨慎

---

## 4.4 右侧栏上下文情报

### 目标
右侧栏聚合危机倒计时 + 对手威胁 + 运势 + combo 提示 + 教练建议，默认可折叠。

### 改动清单
| 文件 | 改动 |
|---|---|
| [index.html](file:///d:\Develop\Code\SSM\src\main\resources\static\index.html) | `.play-shell`（layout.css:305）当前是 3 列 grid `minmax(210px,240px) minmax(460px,1fr) minmax(300px,340px)`，保持；`.main-panel-right` 顶部改为 `#contextSidebar` 容器，内含 `#crisisAlertPanel`/`#rivalThreatPanel`/`#fortunePanel`/`#comboHintPanel`/`#coachAdvicePanel` |
| [js/panel-bundle.js](file:///d:\Develop\Code\SSM\src\main\resources\static\js\panel-bundle.js) | `renderCoach`(3790) 内容合并为 1 条建议注入 `#coachAdvicePanel`；新增 `renderContextSidebar()` 调度 5 个子面板；`renderInsightDigest`(app.js:8150) 内容拆分到各子面板 |
| [css/components.css](file:///d:\Develop\Code\SSM\src\main\resources\static\css\components.css) | 加 `.context-sidebar-item` 折叠样式 |

### 步骤
1. index.html 右栏结构改造
2. renderContextSidebar 调度
3. renderCoach 精简为 1 条
4. CSS 折叠样式

### 工作量：中（1 天）

---

## 4.5 平台 Tab 改全屏抽屉

### 目标
平台 Tab 不再和"今日"平级，改为顶栏 🌐 图标点开的全屏抽屉。

### 改动清单
| 文件 | 改动 |
|---|---|
| [index.html](file:///d:\Develop\Code\SSM\src\main\resources\static\index.html) | `#mainTabBar`(193) 移除平台 tab 按钮；顶栏加 `<button id="platformDrawerBtn">🌐</button>`；`#tabPlatform`(261) 移到 body 下改为 `#platformDrawer` 全屏抽屉 |
| [js/panel-bundle.js](file:///d:\Develop\Code\SSM\src\main\resources\static\js\panel-bundle.js) | `syncTabAvailability`(3105) 移除平台 tab 逻辑；新增 `togglePlatformDrawer()` 开关抽屉 |
| [css/layout.css](file:///d:\Develop\Code\SSM\src\main\resources\static\css\layout.css) | 加 `.platform-drawer` fixed 全屏 + 滑入动画 |

### 步骤
1. index.html 移平台 tab + 加抽屉容器
2. togglePlatformDrawer 逻辑
3. CSS 全屏抽屉

### 工作量：小（0.5 天）

---

## 4.6 总览 Tab 改独立页

### 目标
总览 Tab（图鉴/成就/术语/历史）改为顶栏 📊 图标的独立页，不抢"今日"注意力。

### 改动清单
| 文件 | 改动 |
|---|---|
| [index.html](file:///d:\Develop\Code\SSM\src\main\resources\static\index.html) | `#mainTabBar` 移除总览 tab；顶栏加 `<button id="infoHubBtn">📊</button>`；`#tabInfoHub`(274) 移到 body 下改为 `#infoHubPage` 全屏页 |
| [js/panel-bundle.js](file:///d:\Develop\Code\SSM\src\main\resources\static\js\panel-bundle.js) | `syncGameplayInfoTab`(3108) 改造；新增 `toggleInfoHubPage()` |
| mainTabBar | 只保留"🎮 今日"单 tab（或移除 tab 栏，今日即默认） |

### 步骤
1. index.html 移总览 tab + 加独立页
2. toggleInfoHubPage 逻辑
3. tab 栏精简

### 工作量：小（0.5 天）

---

## 4.7 直播间浮层按需出现

### 目标
非直播阶段底栏隐藏；直播阶段（NEED_TITLE/ACTION_RESOLVED）全屏浮现直播间，礼物/弹幕可真实影响结算（依赖 1.1/1.2）。

### 改动清单
| 文件 | 改动 |
|---|---|
| [index.html](file:///d:\Develop\Code\SSM\src\main\resources\static\index.html) | `#liveRoomBottomBar`(334) 加 class `hidden` 默认隐藏 |
| [js/panel-bundle.js](file:///d:\Develop\Code\SSM\src\main\resources\static\js\panel-bundle.js) | `syncLiveRoomStage`(3111) 按 phase 控制：phase∈{NEED_TITLE,ACTION_RESOLVED,NEED_INTERACTION_CHOICE}→显示底栏+右侧栏+礼物面板；否则隐藏 |
| [bili-ui.js](file:///d:\Develop\Code\SSM\src\main\resources\static\bili-ui.js) | 礼物/弹幕面板仅在直播浮层显示时启用 |
| [css/layout.css](file:///d:\Develop\Code\SSM\src\main\resources\static\css\layout.css) | `#liveRoomBottomBar.hidden`/`#liveRightSidebar.hidden` `display:none` |

### 步骤
1. index.html 默认隐藏
2. syncLiveRoomStage 按 phase 切换
3. CSS hidden 类

### 工作量：小（0.5 天）

---

# 模块五：性能优化

## 5.1 render() 拆分按区渲染

### 现状
`render()`（[panel-bundle.js:3102](file:///d:\Develop\Code\SSM\src\main\resources\static\js\panel-bundle.js)）每次调用 49 个子函数全量重绘。写操作后 `refreshAfterWrite()` 触发全量 render。

### 目标
拆为按区渲染：`renderTopBar()`/`renderMainCard()`/`renderSidebar()`/`renderLiveLayer()`，写操作后只重渲染受影响区域。

### 改动清单
| 文件 | 改动 |
|---|---|
| [js/panel-bundle.js](file:///d:\Develop\Code\SSM\src\main\resources\static\js\panel-bundle.js) | `render()`(3102) 改为调度器，按区分组调用；新增 `renderTopBar()`(合并 renderHeader/renderStatus)、`renderMainCard()`(合并 5 个决策 render)、`renderSidebar()`(合并 renderCoach/renderInsightDigest/renderCrisisAlerts 等)、`renderLiveLayer()`(合并 syncLiveRoomStage/renderDanmaku) |
| [app.js](file:///d:\Develop\Code\SSM\src\main\resources\static\app.js) | `refreshAfterWrite()`(3043) 改为按需调用：vup/session 变化→`renderTopBar()+renderMainCard()`；phase 变化→`renderMainCard()+renderLiveLayer()`；新增 `state.lastRenderedPhase` 记录上次渲染阶段，phase 未变则跳过 LiveLayer |

### 步骤
1. render() 拆为 4 个分区函数
2. refreshAfterWrite 按数据变化选择重渲染区域
3. 加 lastRenderedPhase 跳过逻辑

### 工作量：中（1 天）

---

## 5.2 状态 diff 渲染

### 现状
state.js 已有 `previousState`，但 render 仍全量 innerHTML 替换。

### 目标
扩展 state.js 的 subscribe 机制，只对变化字段做 DOM patch。

### 改动清单
| 文件 | 改动 |
|---|---|
| [js/state.js](file:///d:\Develop\Code\SSM\src\main\resources\static\js\state.js) | `patchState` 增强：对比 previousState，生成 `changedKeys` 集合传给 listeners；新增 `subscribeKey(key, cb)` 细粒度订阅 |
| [js/dom.js](file:///d:\Develop\Code\SSM\src\main\resources\static\js\dom.js) | 新增 `patchText(id, newText)` 只在文本变化时更新 textContent，避免重排 |
| [js/panel-bundle.js](file:///d:\Develop\Code\SSM\src\main\resources\static\js\panel-bundle.js) | `renderHeaderStatChip` 改用 `subscribeKey('vup', ...)` 只更新变化的 chip |

### 步骤
1. state.js 加 changedKeys + subscribeKey
2. dom.js 加 patchText
3. renderHeaderStatChip 改订阅式

### 工作量：中（1 天）

---

## 5.3 panel-bundle.js 拆包

### 现状
panel-bundle.js 单文件 257KB，含 hydrate/api/render + 全部 render* 函数，难维护。

### 目标
按区域拆成多个按需加载模块。

### 改动清单
| 文件 | 改动 |
|---|---|
| 新建 `js/render/main-card.js` | 抽离 renderMainDecisionCard + 5 个决策 render |
| 新建 `js/render/sidebar.js` | 抽离 renderContextSidebar + renderCoach + renderCrisisAlerts |
| 新建 `js/render/ending.js` | 抽离 renderEnding + renderEndingAtlas + renderTimelineReplay |
| 新建 `js/render/platform.js` | 抽离 renderPlatform + renderNPC + renderRivalThreat |
| 保留 `js/panel-bundle.js` | 只留 hydrate/api/render 编排器 + renderHeader + renderStats |
| [index.html](file:///d:\Develop\Code\SSM\src\main\resources\static\index.html) | script 加载改为 ES module import 链 |
| [app.js](file:///d:\Develop\Code\SSM\src\main\resources\static\app.js) | 动态注入逻辑改为按需 import() |

### 步骤
1. 按 4 个区域拆分函数到新文件
2. panel-bundle.js 保留核心，import 子模块
3. app.js 改动态 import
4. index.html 调整加载

### 工作量：中偏大（1.5 天）
> ⚠️ 拆包需处理双轨架构（IIFE vs ESM）的桥接，建议在 5.1/5.2 完成后进行

---

# 附录：关键代码定位索引

## 后端关键方法行号

| 方法 | 文件 | 行号 |
|---|---|---|
| `TitleService.settleStreamTitle` | [service/TitleService.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\service\TitleService.java) | 670 |
| `TitleService.titlePreview`（平行预览） | 同上 | 338-488 |
| modifier 叠加链 | 同上 | 719(actionFatigue)→765(audiencePressure)→805(stageMomentum)→827(combo)→852(stageObjective)→873(platformTrend)→903(fortune)→922(return) |
| `RewardCalculator.applyToVup` | [service/risk/RewardCalculator.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\service\risk\RewardCalculator.java) | 63 |
| `ActionService.doSubmitAction` | [service/ActionService.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\service\ActionService.java) | ~200 |
| `DayService.doNextDay` | [service/core/DayService.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\service\core\DayService.java) | — |
| `DayFlowService.finishSessionAfterReport` | [service/core/DayFlowService.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\service\core\DayFlowService.java) | — |

## 前端关键函数行号

| 函数 | 文件 | 行号 |
|---|---|---|
| `render()` 编排器 | [js/panel-bundle.js](file:///d:\Develop\Code\SSM\src\main\resources\static\js\panel-bundle.js) | 3102 |
| `refreshAfterWrite()` | 同上 | 3034 |
| `renderHeader` | 同上 | 3438 |
| `renderStats` | 同上 | 4175 |
| `renderCoach` | 同上 | 3790 |
| `renderDemo` | 同上 | 2171 |
| `state` 对象 | [app.js](file:///d:\Develop\Code\SSM\src\main\resources\static\app.js) | 124 |
| `previousStats` | 同上 | 183 |
| `SFX` 对象 | 同上 | 212 |
| `BGM` 对象 | 同上 | 272 |
| `ANIM` 对象 | 同上 | 814 |
| `renderActions` | 同上 | 2963 |
| `renderTitles` | 同上 | 4444 |
| `renderEvent` | 同上 | 4650 |
| `renderOffStreamPanel` | 同上 | 3286 |
| `renderEnding` | 同上 | 7186 |
| `renderRouteGallery` | 同上 | 8920 |
| `renderEndingForecast` | 同上 | 9424 |
| `renderInsightDigest` | 同上 | 8150 |
| `spawnDanmakuOverlayMessage` | 同上 | 3624 |
| `sendGift` | [bili-ui.js](file:///d:\Develop\Code\SSM\src\main\resources\static\bili-ui.js) | 144 |
| `sendDanmaku` | 同上 | 62 |
| GIFTS 数据 | 同上 | 83-94 |
| `updateSidebarContent` | 同上 | 242 |

## 数据库表结构

| 表 | 文件 | 行号 |
|---|---|---|
| day_session | [V1__init_schema.sql](file:///d:\Develop\Code\SSM\src\main\resources\db\migration\V1__init_schema.sql) | 88-118 |
| vup | 同上 | — |
| business_log | 同上 | — |

## 关键枚举

| 枚举 | 文件 | 值 |
|---|---|---|
| DayPhase | [domain/DayPhase.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\domain\DayPhase.java) | READY/NEED_TITLE/ACTION_RESOLVED/OFF_STREAM_READY/OFF_STREAM_RESOLVED/NEED_INTERACTION_CHOICE/NEED_EVENT_CHOICE/REPORT_READY/ENDING_READY |
| ActionType | [domain/ActionType.java](file:///d:\Develop\Code\SSM\src\main\java\com\example\vupworld\domain\ActionType.java) | TRAIN_SONG/TRAIN_DANCE/TRAIN_TALK/STREAM_PLAN/PUBLISH_VIDEO/PUBLISH_CLIP/FAN_GROUP_MAINTAIN/NPC_INTERACT/REST |

---

## 实施优先级建议

| 优先级 | 方案 | 理由 |
|---|---|---|
| 🔴 第一批 | 1.1 礼物 + 1.2 弹幕 + 4.7 直播间浮层 | 让"装饰壳"变"真玩法"，ROI 最高 |
| 🔴 第一批 | 4.3 中央主决策区 | UX 体感最明显 |
| 🟡 第二批 | 2.2 风险预警 + 1.4 对手威胁 | 解决新手痛点 |
| 🟡 第二批 | 2.3 结局图鉴 + 2.1 中途存档 | 完善体验闭环 |
| 🟢 第三批 | 1.3 行动点 + 2.4 难度 | 游戏性深化（需重新平衡数值） |
| 🟢 第三批 | 4.1/4.2/4.4/4.5/4.6 布局细调 | 配合主决策区 |
| ⚪ 第四批 | 5.1/5.2/5.3 性能 + 3.x 锦上添花 | 优化与扩展 |

---

*文档完*

