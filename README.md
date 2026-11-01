# mall · 简易商城后端

基于 Spring Boot 2.3 + MyBatis-Plus 的单体电商后端示例项目，提供**商品管理**与**下单（含库存扣减、幂等）**两条核心业务链路。项目刻意保持轻量，去掉了登录鉴权和前端页面，只保留分层结构、统一响应、全局异常、参数校验、日志切面等工程骨架，适合作为学习 SSM / Spring Boot 分层实践的基础模板，或作为新业务模块的起步脚手架。

> 一句话定位：一个能跑通「查商品 → 下单 → 扣库存」的最小可用后端。

---

## 一、项目简介与用途

| 项目 | 说明 |
| --- | --- |
| 项目名 | mall（小卖部） |
| 类型 | RESTful 后端服务（无前端、无鉴权） |
| 端口 | 默认 `8080` |
| 适用场景 | 学习 Spring Boot 分层与 MyBatis-Plus 用法；作为小型电商/进销存项目的后端骨架 |

项目解决的问题：演示一套"可维护"的后端写法——Controller 只做参数接收与结果包装，Service 承担事务与业务规则，Mapper 负责数据访问，异常统一在全局处理器中收敛，不向前端泄露堆栈。

---

## 二、主要功能特性

**商品模块**
- 分页查询上架商品，支持按分类过滤、按名称关键字模糊搜索（关联 `category` 表返回分类名）
- 商品详情查询（返回 `ProductVO`，含 `categoryName`；id 不存在返回 `4004`）
- 新增商品（名称、分类、价格、库存校验，默认上架）
- 全量更新商品信息
- 下架商品（逻辑删除：`status = 0`，不物理删除数据）

**订单模块**
- 创建订单：校验商品是否存在/上架 → `SELECT ... FOR UPDATE` 行锁 → 条件更新扣减库存（`stock >= qty` 才扣，防超卖）→ 写订单主表与明细表，全程单事务
- 金额一律由后端按数据库价格计算（`BigDecimal`），不信任前端传入金额
- 订单明细保存商品名与价格**快照**，后续改价不影响历史订单
- 幂等下单：基于 `(user_id, idempotent_key)` 唯一索引，重复提交返回同一订单号（含并发下 `DuplicateKeyException` 的兜底查询）
- 订单号生成：`yyyyMMddHHmmssSSS` + 4 位随机数

**工程能力**
- 统一响应体 `Result<T>`（`code` / `message` / `data`）与统一错误码 `ResultCode`
- 全局异常处理：业务异常、`@Valid` 校验失败、`ConstraintViolationException`、兜底异常分类处理
- 参数校验：`spring-boot-starter-validation`，DTO 上声明规则，分页参数限制 `size ≤ 100`
- 请求日志切面：记录 Controller 入参、耗时、异常；`@CostTime` 注解可标记任意方法统计耗时
- MyBatis-Plus 分页插件、驼峰映射、主键自增、SQL 控制台打印
- Jackson 统一时间格式 `yyyy-MM-dd HH:mm:ss`
- 多环境配置：`local` / `dev` / `prod` 三套 profile，凭据通过环境变量注入

---

## 三、技术栈与环境依赖

| 分类 | 选型 | 版本 |
| --- | --- | --- |
| 语言 | Java | 11（`pom.xml` 中 `java.version`） |
| 框架 | Spring Boot（web / aop / validation） | 2.3.12.RELEASE |
| ORM | MyBatis-Plus | 3.4.3.4 |
| 数据库 | MySQL | 5.7 / 8.0 |
| 驱动 | mysql-connector-java | 随 Boot 版本管理（8.0.x） |
| 构建 | Maven | 3.6+ |
| 其他 | Lombok **未使用**（实体手写 getter/setter） | — |

环境要求：
- JDK 11 及以上（项目按 11 编译，17 亦可运行）
- Maven 3.6+
- 可访问的 MySQL 实例，库名建议 `mall`

---

## 四、安装与配置

### 1. 克隆并准备数据库

```bash
git clone <your-repo-url> mall
cd mall
```

仓库未内置 SQL 初始化脚本，请按下述结构建库建表（字段与实体一一对应，缺表会导致启动后查询报错）：

```sql
CREATE DATABASE IF NOT EXISTS mall DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE mall;

CREATE TABLE category (
  id   BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(60) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  name        VARCHAR(120)   NOT NULL,
  category_id BIGINT         NOT NULL,
  price       DECIMAL(10, 2) NOT NULL,
  stock       INT            NOT NULL DEFAULT 0,
  status      TINYINT        NOT NULL DEFAULT 1 COMMENT '1=上架 0=下架',
  create_time DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE orders (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no       VARCHAR(32)    NOT NULL,
  user_id        BIGINT         NOT NULL,
  total_amount   DECIMAL(12, 2) NOT NULL,
  status         TINYINT        NOT NULL DEFAULT 0 COMMENT '0=待支付',
  create_time    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  idempotent_key VARCHAR(64)    DEFAULT NULL,
  UNIQUE KEY uk_order_no (order_no),
  UNIQUE KEY uk_user_idem (user_id, idempotent_key)   -- 幂等下单依赖此唯一索引
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_item (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id     BIGINT         NOT NULL,
  product_id   BIGINT         NOT NULL,
  product_name VARCHAR(120)   NOT NULL,
  price        DECIMAL(10, 2) NOT NULL,
  quantity     INT            NOT NULL,
  KEY idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 可选：一条初始数据，便于验证接口
INSERT INTO category (id, name) VALUES (1, '饮料');
INSERT INTO product (name, category_id, price, stock, status)
VALUES ('可乐 330ml', 1, 3.50, 100, 1);
```

> 说明：`uk_user_idem` 是幂等能力的实现基础，缺失时重复提交会生成多张订单。

### 2. 配置数据源

项目默认激活 `local` profile，而 `application-local.yml` 已被 `.gitignore` 排除（避免凭据入库）。首次运行请复制模板并填入自己的连接信息：

```bash
cp src/main/resources/application-local.yml.example \
   src/main/resources/application-local.yml
```

```yaml
# src/main/resources/application-local.yml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/mall?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: your_username
    password: your_password
```

参数要点：
- `useSSL=false` + `allowPublicKeyRetrieval=true`：本地连接 MySQL 8 的常用组合
- `serverTimezone=Asia/Shanghai`：避免时间字段偏移
- 生产环境（`prod` profile）不写明文，改用环境变量 `DB_URL` / `DB_USER` / `DB_PASSWORD`

### 3. 编译与启动

```bash
# 打包
mvn clean package -DskipTests

# 方式一：Maven 直接运行
mvn spring-boot:run

# 方式二：运行 jar
java -jar target/mall-1.0-SNAPSHOT.jar

# 方式三：指定 profile（默认 local）
java -jar target/mall-1.0-SNAPSHOT.jar --spring.profiles.active=dev
```

启动成功后验证：

```bash
curl http://localhost:8080/api/ping
# {"status":"ok","app":"mall"}
```

### 4. 关键配置项

| 配置 | 位置 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `server.port` | application.yml | `8080` | 服务端口 |
| `spring.profiles.active` | application.yml | `local` | 激活的环境 |
| `mall.name` | application.yml | `小卖部` | 应用名（绑定到 `MallProperties`） |
| `mall.order.pay-timeout` | application.yml | `15m` | 支付超时，绑定为 `Duration`（预留，当前未参与业务） |
| `mall.order.max-items` | application.yml | `20` | 单笔订单最大明细数，校验范围 1–100 |
| `mybatis-plus.configuration.log-impl` | application.yml | StdOut | 控制台打印 SQL（仅建议开发期开启） |

---

## 五、基本使用与示例

### 统一响应结构

所有接口返回统一包装，`code = 0` 表示成功：

```json
{ "code": 0, "message": "成功", "data": {} }
```

| code | 含义 |
| --- | --- |
| 0 | 成功 |
| 4000 | 参数不合法 |
| 4010 | 未登录（预留） |
| 4004 | 资源不存在（如商品 id 不存在） |
| 4100 | 业务处理失败（库存不足、状态冲突等） |
| 5000 | 系统繁忙（兜底，不返回内部细节） |

### 接口一览

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/ping` | 健康检查 |
| GET | `/api/products` | 商品分页查询（`page`、`size`、`categoryId`、`keyword`） |
| GET | `/api/products/{id}` | 商品详情 |
| POST | `/api/products` | 新增商品 |
| PUT | `/api/products/{id}` | 更新商品 |
| PATCH | `/api/products/{id}/off-shelf` | 下架商品 |
| POST | `/api/orders` | 创建订单（支持幂等） |

### 示例：查询商品列表

```bash
curl "http://localhost:8080/api/products?page=1&size=10&categoryId=1&keyword=可乐"
```

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "records": [
      { "id": 1, "name": "可乐 330ml", "price": 3.50, "stock": 100, "categoryName": "饮料" }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

### 示例：新增商品

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"雪碧 330ml","categoryId":1,"price":3.50,"stock":50}'
```

校验失败时返回 `code = 4000`，`message` 汇总所有字段错误，例如：`price: 价格必须大于 0; stock: 库存不能为负`。新增（POST）与更新（PUT）两个接口都会校验。

### 示例：创建订单（幂等）

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
        "userId": 1001,
        "idempotentKey": "order-1001-20261031-001",
        "items": [
          { "productId": 1, "quantity": 2 },
          { "productId": 2, "quantity": 1 }
        ]
      }'
```

```json
{ "code": 0, "message": "成功", "data": "202610312300123456789" }
```

行为说明：
- 相同 `userId + idempotentKey` 重复请求，直接返回首次生成的订单号，不会重复扣库存
- `idempotentKey` 传空串或纯空白等同于不传：服务端会归一化成 `NULL` 入库（`NULL` 不参与唯一索引校验），因此不会出现"同一用户第二单撞唯一索引"
- 商品不存在/已下架返回 `4100`：`商品不存在或已下架: 3`
- 库存不足返回 `4100`：`库存不足: 可乐 330ml`，整个事务回滚
- 明细数超过 `mall.order.max-items` 返回 `4100`：`单笔最多 20 件`
- 订单号（`uk_order_no`）极小概率撞车时返回 `4100`：`订单号生成冲突，请重试`，不会返回空单号

---

## 六、目录结构

```
mall
├── pom.xml
├── README.md
├── .gitignore
└── src/main
    ├── java/com/example/mall
    │   ├── MallApplication.java          # 启动类：@MapperScan、@ConfigurationPropertiesScan
    │   ├── annotation/
    │   │   └── CostTime.java             # 方法耗时统计注解
    │   ├── common/
    │   │   ├── Result.java               # 统一响应体
    │   │   ├── ResultCode.java           # 全局错误码枚举
    │   │   ├── BusinessException.java    # 业务异常
    │   │   └── GlobalExceptionHandler.java  # 全局异常收敛
    │   ├── config/
    │   │   ├── MallProperties.java       # mall.* 配置绑定（构造器绑定 + 校验）
    │   │   ├── MybatisPlusConfig.java    # 分页插件
    │   │   ├── JacksonConfig.java        # 时间序列化格式
    │   │   └── WebLogAspect.java         # 请求日志 / 耗时切面
    │   ├── controller/
    │   │   ├── PingController.java
    │   │   ├── ProductController.java
    │   │   └── OrderController.java
    │   ├── service/
    │   │   ├── ProductService.java
    │   │   ├── OrderService.java
    │   │   └── impl/
    │   │       ├── ProductServiceImpl.java
    │   │       └── OrderServiceImpl.java
    │   ├── mapper/
    │   │   ├── ProductMapper.java        # 继承 BaseMapper + 自定义 SQL
    │   │   ├── OrderMapper.java
    │   │   └── OrderItemMapper.java
    │   ├── entity/                       # 数据库实体（product / orders / order_item）
    │   ├── dto/                          # 入参对象（含校验注解）
    │   └── vo/ProductVO.java             # 出参视图对象
    └── resources
        ├── application.yml               # 公共配置
        ├── application-local.yml.example # 本地配置模板（复制为 application-local.yml）
        ├── application-dev.yml           # 开发环境：SQL 日志
        ├── application-prod.yml          # 生产环境：环境变量注入 + 更大连接池
        └── mapper/ProductMapper.xml      # 自定义 SQL（联表分页、扣库存、行锁查询）
```

分层约定：`Controller` 不写业务逻辑 → `Service` 管事务与规则 → `Mapper` 只管取数；数据库字段 `snake_case` 与实体 `camelCase` 由 MyBatis-Plus 自动映射。

---

## 七、贡献与许可

### 参与贡献

1. Fork 本仓库并从 `main` 切出特性分支：`git checkout -b feature/your-feature`
2. 遵循现有代码风格：构造器注入、DTO 承担校验、业务异常抛 `BusinessException`、Controller 统一返回 `Result`
3. 新增 SQL 优先放 `resources/mapper/*.xml`，简单单表操作直接使用 `BaseMapper`
4. 提交前确认 `mvn clean package` 通过；涉及接口变更请同步更新本 README 的接口表
5. 提交 PR 时说明动机、改动点与验证方式

### 注意事项（提交前自查）

- **不要提交** `application-local.yml`、`.env` 或任何明文口令；生产凭据只通过 `DB_URL` / `DB_USER` / `DB_PASSWORD` 注入
- 涉及金额的计算一律使用 `BigDecimal`，禁止 `double`
- 新增接口请补充参数校验注解，避免脏数据落到库中

### 已知限制

- 无登录鉴权与用户体系，`userId` 由调用方直接传入
- 无支付、退款、订单状态流转（`orders.status` 目前恒为 `0`）
- 错误统一返回 **HTTP 200**，靠 body 里的 `code` 区分，前端不能依赖 HTTP 状态码判断成败
- 未接入 traceId：`Result.traceId` 字段保留但恒为 `null`（缺一个往 MDC 写值的 Filter）
- 未提供接口文档（Swagger），测试仅覆盖下单回滚、幂等与两个分页校验用例

### 许可证

本项目当前**未附带 LICENSE 文件**，默认保留所有权利。若需开源发布，建议补充 `LICENSE`（如 MIT 或 Apache-2.0）并在本节说明，以便他人明确使用与分发条件。
