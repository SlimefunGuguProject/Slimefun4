# 修复背包快速打开导致物品复制

## Goal

修复 Slimefun 背包在快速/重复打开时产生多个 InventoryView（不同快照实例）从而导致物品可被复制（刷物品）的漏洞。修复后同一玩家在同一时刻最多存在一个"待加载"或"已打开"的背包界面，重复打开请求被直接拒绝。

## Background / Repro

用户提供的复现步骤（借助水影模组 AirPlace 功能发送快速右键数据包）：

1. 安装水影模组，开启 AirPlace 功能
2. 准备 1 个背包，命名，放入要刷的物品
3. 退出重进服务器
4. 看向天空，右键打开背包
5. 取出一件物品，关闭背包
6. 再打开背包，发现刚才取出的物品还在背包里，又能取出一次

调查确认的根因（已核实，非推测）：

- `BackpackListener.openBackpackInternal` 在异步加载背包期间不拦截重复的打开请求，`openingPlayers.add(...)` 未检查返回值，多个缓存未命中的加载可并行进行，为同一逻辑背包生成多个 `PlayerBackpack`/Inventory 实例（`ProfileDataController.getBackpackAsync` 的 check-then-load 不合并）。
- 玩家已有背包界面时，当前逻辑会主动关闭旧视图并重新打开，可能切换到旧快照实例，使已取出的物品"恢复"。

## Requirements

- R1: 异步加载背包期间，同一玩家的重复打开请求必须被拒绝（利用 `openingPlayers.add` 返回值做原子拦截）。
- R2: 玩家当前已有打开的背包视图时，新的打开请求必须被拒绝，不再执行"强制关闭并重新打开"。
- R3: `openingPlayers` 中的玩家状态必须在异步加载成功、返回 null、以及异常完成时都被释放（主线程完成回调统一清理），防止失败后玩家被永久锁定。
- R4: 保留既有行为：所有权校验、背包元数据校验、"背包已被其他玩家打开"保护、开关声音、关闭时保存（`onClose` → `saveBackpackInventory`）。
- R5: 修复范围限定在 `BackpackListener.java`；不改动 `ProfileDataController` / 缓存 / 存储层（避免扩大范围与兼容风险）。

## Constraints

- 最小正确修复：不做与本次漏洞无关的重构、格式化或兜底分支。
- 遵守仓库编码规范（Palantir 格式，Spotless 校验）。
- 工作区已有 13 个与本任务无关的脏路径，不得触碰或回退。

## Acceptance Criteria

- [ ] AC1: 同一玩家最多存在一个待加载或已打开的背包视图；重复 Use 数据包不会触发并行加载，也不会触发关闭重开。
- [ ] AC2: 按复现步骤操作（含 AirPlace 快速右键）：取出物品 → 关闭 → 再打开，物品不会恢复，无法复制。
- [ ] AC3: 正常关闭背包后再次打开功能正常（不破坏正常 UX）。
- [ ] AC4: 异步加载失败（异常/null）后，玩家仍可再次尝试打开背包（无永久锁定）。
- [ ] AC5: 同一背包仍不能被两个玩家同时打开（既有保护不回归）。
- [ ] AC6: JetBrains 项目构建通过；`./gradlew test` 与 `./gradlew spotlessCheck` 通过。

## Verification

- JetBrains 构建（仓库要求优先）→ `./gradlew test` → `./gradlew spotlessCheck`。
- 人工回归：按上述复现步骤在测试服务器验证（含 AirPlace 场景与普通开关场景）。

## Out of Scope

- 存储/缓存层的加载合并（in-flight load coalescing）重构。
- 其他与背包打开竞态无关的监听器或工具类改动。
