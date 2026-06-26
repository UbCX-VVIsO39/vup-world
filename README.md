# VUP 出道局 - 30 天出道模拟器

Spring Boot 3 + MyBatis 的本地 VUP 30 天模拟器。玩家在 30 天里选择每日行动、直播标题、事件处理和下播补救，系统生成日报、业务日志和最终结局复盘。

## 当前口径

- 当前目标是单机买断版体验，不是公网 SaaS。
- 默认目标是一局 30 天。
- 路线口径固定为 7 个基础路线、11 个玩家路线身份、9 个正式结局 key。
- 11 个玩家路线身份验收需要跑到第 30 天 `ENDING_READY`。
- 下播链路、日报、业务日志、路线证据链和结局复盘都属于核心玩法。
- 7 天短局只用于快速试玩，不作为正式质量目标。
- 账号、支付、云存档、公开部署、安全网关都不是当前优先事项。

基础路线用于计分骨架，玩家路线身份用于路线卡、事件和验收报告，正式结局 key 用于 Day 30 结局复盘。

## 运行环境

- JDK 21
- Maven 3.9.x
- Node.js
- 浏览器

正式单机游玩优先使用 `singleplayer` profile。它使用 H2 文件库落盘，不依赖本机 MySQL 服务，存档默认在 `tmp\singleplayer-save\vupworld.mv.db`，并启用同步写入以提高本地存档可靠性。

```bat
scripts\run-singleplayer.cmd -Port 18087 -Refresh
```

看到 `Tomcat started on port 18087` 后，打开：

```bat
http://localhost:18087/
```

开发验证和临时人工验收仍可使用 `manual` profile。它使用 H2 内存库，关闭服务后不会保留存档。

```bat
scripts\run-latest-manual.cmd -Port 18087 -Refresh
```

修改 Java 代码或静态资源后，建议带 `-Refresh` 重新编译。

## 朋友试玩

完整试玩：

```bat
scripts\run-singleplayer.cmd -Port 18087 -Refresh
```

快速短局试玩：

```bat
scripts\run-friend-short.cmd
```

短局只用于观察理解成本；正式验收仍以 30 天 / 11 玩家路线身份为准。

## 验证命令

基础验证：

```bat
scripts\verify.cmd
```

`verify.cmd` 不会自动启动 manual 服务。要跑真实可玩探针，先启动：

```bat
scripts\run-latest-manual.cmd -Port 18087 -Refresh
```

再运行：

```bat
scripts\verify.cmd
```

发布前或交付前还应单独跑一次单机存档完整闭环：

```bat
scripts\probe-singleplayer-save-flow.cmd
```

快速可玩闭环：

```bat
scripts\probe-play.cmd
```

单机存档完整闭环：

```bat
scripts\probe-singleplayer-save-flow.cmd
```

该探针会启动 singleplayer profile，覆盖快速开始、保存、服务重启后继续、导出槽 1、导入槽 2、继续导入档、跑到 30 天结局、读取结局复盘和重开到 Day 1。

30 天 / 11 玩家路线身份验收：

```bat
scripts\acceptance-routes.cmd
```

质量报告：

```bat
node scripts\quality-readiness-check.mjs
```

发布候选总验收：

```bat
scripts\release-candidate-check.cmd
```

该命令会顺序刷新 `verify.cmd`、单机存档探针、11 玩家路线身份验收、质量报告和浏览器 UI 证据。日常小改不必每次都跑完整候选验收，可以继续使用上面的局部门禁。

数值模拟：

```bat
node scripts\balance-sim.mjs --base-url http://localhost:18087 --runs 5 --concurrency 2
```

## Profile 简表

| Profile | 用途 | 数据库 | 说明 |
| --- | --- | --- | --- |
| `singleplayer` | 正式单机游玩 | H2 文件库 | 推荐玩家入口，默认存档在 `tmp\singleplayer-save\` |
| `manual` | 开发验证、临时人工验收 | H2 内存库 | 重启后不保留存档 |
| `test` | 自动化测试 | H2 内存库 | `mvn test` 使用 |
| `mysql` | 本机 MySQL 验证 | MySQL | 需要手动建库导入 |
| `prod` | 正式化运行起点 | 外部数据库 | 不代表当前版本可直接公开发布 |

## 仓库内容

公开仓库只保留游戏本体、验证脚本和运行需要的资源：

- `pom.xml`
- `src\`
- `scripts\`
- `图库\`
- `README.md`
- `.gitignore`

`target\`、`tmp\`、`logs\`、`docs\`、本地代理目录和美术生成过程文件不是源码交付必需项，不随公开仓库提交。
