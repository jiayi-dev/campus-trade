# goods_id=13 商品状态异常修复报告

- 修复时间：2026-11-01 13:58 ~ 14:00（Asia/Shanghai）
- 数据库：`campus_trade`（本机 MySQL，`hostname=jjy`、`port=3306`、`datadir=D:\mysql-8.0.22-winx64\data\`，`CURRENT_USER=root@localhost`）
- 数据库身份结论：本地开发库，非生产库（连接串来自被 `.gitignore` 忽略的 `application-local.yml`）
- 影响范围：仅 `goods.goods_id = 13` 的 `status` 单字段，影响 1 行
- 本轮未创建演示账号、未修改其他商品、未执行任何 `v1.2-*.sql`、未改动代码/配置/前端/图片、未做 Docker 与 Git 操作

---

## 一、异常原因及发现方式

**发现方式**：公开演示环境数据库审计时，对 `goods.status` 做分组统计：

```sql
SELECT status, COUNT(*) FROM goods GROUP BY status;
```

统计结果为 `status=0` 共 13 条、`status=1` 共 48 条、`status=NULL` 共 1 条。进一步定位该 NULL 记录即 `goods_id=13`，全库仅此一条。

**异常原因推断**：

- `goods_id=13` 的商品名为「测试默认状态商品」，描述为「测试choose默认值」，`create_time=2026-09-11 17:36:12`，是早期验证「新建商品 status 默认值」的测试数据。
- 该行同时缺失 `condition_level`、`campus`、`location`，说明是绕过了正常发布流程（未走完整发布页表单校验）写入的。
- 最可能的成因：测试时直接用工具/SQL 向 `goods` 表插入一行并显式写入了 `status = NULL`，而非依赖列默认值 `DEFAULT '1'`。MySQL 允许显式 NULL 覆盖 `DEFAULT`，因此产生了这条脏数据。
- 代码侧并不存在能把 `status` 写成 NULL 的正常写入路径（详见第三节），所以这不是业务 Bug，而是历史测试数据问题。

---

## 二、状态字段合法值及业务含义

`goods.status` 只有 **0 / 1** 两个合法值，不存在第三种「已售出」状态：

| 值 | 含义 | 判定依据 |
|----|------|----------|
| `1` | 在售（公开列表可见，可下单） | `schema.sql` 默认值 `DEFAULT '1'`；列表 SQL 硬过滤 `g.status = 1` |
| `0` | 下架（列表不可见；若同时存在订单，详情页展示为「已售出」） | `GoodsService.updateStatus` 仅接受 0/1 |
| `NULL` | **非法值**，任何业务分支都未定义其语义 | 无 |

「已售出」不是独立状态，而是派生状态：`status = 0` 且 `orderMapper.countByGoodsId(goodsId) > 0`，见 `GoodsService.findById`：

```java
goods.setSold(goods.getStatus() != null
        && goods.getStatus() != 1
        && orderMapper.countByGoodsId(goodsId) > 0);
```

其他相关状态（非本字段，仅作区分）：`orders.order_status` 为 0 待交易 / 1 待买家确认 / 2 已完成 / 3 已取消。

---

## 三、修复目标值判定证据

### 3.1 应修复为 `1`（在售）

| 证据类型 | 具体证据 |
|----------|----------|
| 建表定义 | `sql/schema.sql:133` → `` `status` int DEFAULT '1' COMMENT '商品状态' ``，schema 层默认值就是 1 |
| 线上表结构 | `SHOW CREATE TABLE goods` → `status int DEFAULT '1'`，无 CHECK / ENUM 约束，允许 NULL 但业务默认 1 |
| 新增逻辑 | `GoodsService.insert`：`if (goods.getStatus() == null) { goods.setStatus(1); }` —— 注释「新商品默认在售，不信任前端传入的状态」 |
| 编辑逻辑 | `GoodsService.update`：`goods.setStatus(null);` 后调用 Mapper，编辑不写 status（`GoodsMapper.xml` 动态 SET 已排除 status） |
| 状态流转 | `GoodsService.updateStatus` 只接受 0/1，且上架前会检查订单数，说明「在售」是唯一可恢复的正向状态 |
| 关联数据 | `orders` = 0、`favorite` = 0、`evaluation` = 0，从未参与交易，不存在「已售出」或「被下架」的业务事实 |
| 数据特征 | 商品名/描述为默认值测试数据，全套业务字段为空，符合「刚插入、尚未上架」的测试场景 |

### 3.2 为什么不是 `0`（下架 / 已售出）

- `status=0` 在本项目有明确语义：主动下架，或下单后自动下架。
- 下架的唯一入口是 `PUT /goods/{id}/status`（`GoodsController`），且 `updateStatus` 要求调用者为商品所有者并记录操作；该商品无任何订单、收藏、评价，也没有下架痕迹。
- 「已售出」判定要求存在订单：`orderMapper.countByGoodsId(13) = 0`，不满足。
- `data.sql` 中的种子数据约定也是「下单即 status=0」，本商品没下过单。

### 3.3 为什么不是其他值

- 代码中不存在 2、3 等状态值；`updateStatus` 对 `targetStatus != 0 && targetStatus != 1` 直接抛「不支持的商品状态」。
- 保留 `NULL` 不可行：列表 SQL 硬过滤 `g.status = 1`，该商品在商品中心、首页、专题集市、分类页全部不可见，属于事实上的「数据黑洞」。

**结论：修复目标值 = `1`（在售）。**

---

## 四、修复前记录（已隐去无关个人信息；goods 表本身不含手机号/邮箱）

| 字段 | 修复前值 |
|------|----------|
| goods_id | 13 |
| user_id | 2（卖家，user.status=1） |
| category_id | 1 |
| goods_name | 测试默认状态商品 |
| description | 测试choose默认值 |
| price | 88.00 |
| **status** | **NULL（异常）** |
| view_count | 0 |
| favorite_count | 0 |
| condition_level | NULL |
| campus | NULL |
| location | NULL |
| create_time | 2026-09-11 17:36:12 |

关联记录（修复前）：

| 关联表 | 记录数 |
|--------|--------|
| orders | 0 |
| favorite | 0 |
| evaluation（经 orders 关联） | 0 |
| goods_image | 1（image_id=28，外链图片 URL） |

---

## 五、执行的 SQL

执行前预检（确认命中行数为 1）：

```sql
SELECT COUNT(*) FROM goods WHERE goods_id = 13 AND status IS NULL;   -- 返回 1
```

正式修复（单条、带条件保护、只改 status）：

```sql
UPDATE goods SET status = 1 WHERE goods_id = 13 AND status IS NULL;
```

- 实际主键字段名为 `goods_id`（非 `id`），故条件写作 `goods_id = 13`。
- 主键 + `status IS NULL` 双重条件，物理上最多命中 1 行。
- 实际影响行数（`SELECT ROW_COUNT()`）：**1**。
- 未执行任何整库/整表脚本，未改动其他字段与其他商品。

---

## 六、备份

| 项 | 内容 |
|----|------|
| 备份文件 | `.local-backups/goods_id_13_before_fix_20261101_135821.sql`（位于两个仓库之外的本机目录） |
| 文件大小 | 2484 字节 |
| 备份时间 | 2026-11-01 13:58:21（Asia/Shanghai） |
| 备份方式 | `mysqldump --no-create-info --no-create-db --complete-insert --where="goods_id=13" campus_trade goods` |
| 备份内容 | 一条 `INSERT INTO goods (...) VALUES (13,2,1,'测试默认状态商品','测试choose默认值',88.00,NULL,0,0,NULL,NULL,NULL,'2026-09-11 17:36:12');`，保留了原始 `status = NULL` |
| 位置说明 | 放在两个仓库之外的 `.local-backups` 目录，不污染 Git 工作区，不入库 |
| 敏感性 | 不含数据库账号密码；goods 表无手机号/邮箱字段 |

配套留存文件（同目录）：

- `goods_status_snapshot_before_20261101_135821.txt` — 修复前全表商品快照
- `goods_status_snapshot_after_20261101_140000.txt` — 修复后全表商品快照
- `pre_fix_metrics_20261101_135821.txt` — 修复前校验指标
- `post_fix_metrics_20261101_140000.txt` — 修复后校验指标

---

## 七、修复结果与验证

### 7.1 修复前后对比（仅 status 变化）

| 字段 | 修复前 | 修复后 | 是否变化 |
|------|--------|--------|----------|
| status | NULL | 1 | ✅ 仅此项变化 |
| goods_name | 测试默认状态商品 | 测试默认状态商品 | 否 |
| description | 测试choose默认值 | 测试choose默认值 | 否 |
| price | 88.00 | 88.00 | 否 |
| user_id / category_id | 2 / 1 | 2 / 1 | 否 |
| view_count / favorite_count | 0 / 0 | 0 / 0 | 否 |
| condition_level / campus / location | NULL / NULL / NULL | NULL / NULL / NULL | 否 |
| create_time | 2026-09-11 17:36:12 | 2026-09-11 17:36:12 | 否 |

### 7.2 校验项结果

| 校验项 | 预期 | 实际 | 结果 |
|--------|------|------|------|
| goods 13 状态为合法值 | status=1 | 1 | ✅ |
| 全库无 NULL 状态 | 0 条 | 0 条 | ✅ |
| 商品总数不变 | 62 | 62 | ✅ |
| 状态分布 | 1: 48→49，0: 13，NULL: 1→0 | 49 / 13 / 0 | ✅ |
| 其他商品未被修改（CRC32 指纹） | 148895341194 | 148895341194 | ✅ |
| goods 13 关联订单 | 0 | 0 | ✅ |
| goods 13 关联收藏 | 0 | 0 | ✅ |
| goods 13 关联评价 | 0 | 0 | ✅ |
| goods 13 关联图片 | 1（image_id=28） | 1（image_id=28，URL 一致） | ✅ |
| 全表前后快照 diff | 仅 goods_id=13 的 status 一行 | 仅该一行 | ✅ |

快照 diff 实际输出只有两行（同一行记录的前后状态），不存在其他差异。

---

## 八、其他待处理异常（仅记录，本轮未修复）

1. **`goods` 表缺少状态约束**：`status int DEFAULT '1'`，无 `NOT NULL`、无 CHECK 约束，因此仍可能再次写入 NULL。建议后续由开发侧评估是否改为 `NOT NULL DEFAULT 1`（需单独评审，涉及存量数据与兼容性，本轮不动）。
2. **大量商品缺失 `condition_level` / `campus` / `location`**：id=1~10、13、14、15、20、22、67、68 等测试与早期种子数据为空，不影响状态逻辑，但会影响筛选和展示完整性，是否补数据需另行决定。
3. **商品 13 为测试数据**：名称为「测试默认状态商品」，若准备公开演示环境，建议按既定演示数据方案统一处理（本轮禁止修改，未动）。
4. **商品 13 主图为外链**：`goods_image` 中该图指向外部生成服务 URL，未落地到本地上传目录，换环境/断网时存在失效风险（属图片治理范畴，本轮不动）。

---

## 九、边界说明

- 未创建/修改/删除任何演示账号与演示数据。
- 未执行 `sql/v1.2-*.sql` 任何脚本。
- 未修改任何业务代码、配置文件、前端文件、图片文件。
- 未执行 Docker 启停或部署操作。
- 未执行 `git add` / `commit` / `push`。
- 报告中不含数据库密码、API Key、真实手机号、邮箱或密码哈希。
