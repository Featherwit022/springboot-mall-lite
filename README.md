# mall · 简易商城后端

基于 Spring Boot 3.5 + MyBatis-Plus 的单体电商后端示例项目，提供**登录鉴权**、**商品管理**与**下单（含库存扣减、幂等）**三条核心链路。项目刻意保持轻量，只保留分层结构、统一响应、全局异常、参数校验、日志切面、JWT 鉴权等工程骨架，不含前端页面，适合作为学习 SSM / Spring Boot 分层实践的基础模板，或作为新业务模块的起步脚手架。

鉴权部分刻意**不引入 Spring Security 的过滤器链**：只用 `spring-security-crypto` 拿 BCrypt，令牌签发用 JJWT，认证与授权落在 `HandlerInterceptor` 上。这样每个环节都是可见的普通 Java 代码，适合先把原理讲清楚，再迁移到框架。

> 一句话定位：一个能跑通「登录 → 查商品 → 下单 → 扣库存」的最小可用后端。

---

## 一、项目简介与用途

| 项目 | 说明 |
| --- | --- |
| 项目名 | mall（小卖部） |
| 类型 | RESTful 后端服务（无前端；JWT 登录 + 角色鉴权） |
| 端口 | 默认 `8080` |
| 适用场景 | 学习 Spring Boot 分层、MyBatis-Plus 用法与「无状态鉴权」落地；作为小型电商/进销存项目的后端骨架 |

项目解决的问题：演示一套"可维护"的后端写法——Controller 只做参数接收与结果包装，Service 承担事务与业务规则，Mapper 负责数据访问，异常统一在全局处理器中收敛，不向前端泄露堆栈。

---

## 二、主要功能特性

**鉴权模块**
- 登录签发 JWT（HS256）：`sub` 存 userId，claim 带 `username` / `role`，默认 2 小时过期
- 密码用 BCrypt（cost=10）加盐哈希入库；账号不存在与密码错误返回同一句提示，避免被枚举账号
- 认证由 `HandlerInterceptor` 完成，**只验签不查库**；解析出的身份放进 `UserContext`（ThreadLocal），一次请求内各层共享
- **默认拒绝**：`/api/**` 一律要求登录，公开接口必须显式标注 `@Public`（健康检查、商品浏览、登录）
- 角色鉴权：`@RequireRole(Role.ADMIN)` 控制商品写接口；未登录返回 `4010`，已登录但角色不足返回 `4030`，语义不混用
- 越权防护：下单的 `userId` 只从 token 取，`OrderCreateDTO` 不再接收 `userId`
- 日志脱敏：`WebLogAspect` 打印入参时把登录密码替换为 `***`
- 上下文必清理：拦截器 `afterCompletion` 中 `remove()`，避免 Tomcat 线程复用导致身份串号

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
- 统一响应体 `Result<T>`（`code` / `message` / `data` / `traceId`）与统一错误码 `ResultCode`（含 `4010` 未登录 / `4030` 无权限）
- 全局异常处理：业务异常、`@Valid` 校验失败、`ConstraintViolationException`、兜底异常分类处理
- 参数校验：`spring-boot-starter-validation`，DTO 上声明规则，分页参数限制 `size ≤ 100`
- 请求日志切面：记录 Controller 入参（敏感字段脱敏）、耗时、异常；`@CostTime` 注解可标记任意方法统计耗时
- MyBatis-Plus 分页插件、驼峰映射、主键自增、SQL 控制台打印
- Jackson 统一时间格式 `yyyy-MM-dd HH:mm:ss`
- 链路追踪：`TraceIdFilter` 为每个请求生成（或透传上游的）`traceId`，写入 MDC、响应头 `X-Trace-Id` 与 `Result.traceId`
- 多环境配置：`local` / `dev` / `prod` 三套 profile，数据库凭据与 JWT 密钥均通过环境变量注入

---

## 三、技术栈与环境依赖

| 分类 | 选型 | 版本 |
| --- | --- | --- |
| 语言 | Java | 17（`pom.xml` 中 `java.version`） |
| 框架 | Spring Boot（web / aop / validation） | 3.5.16 |
| ORM | MyBatis-Plus | 3.5.17（`mybatis-plus-spring-boot3-starter` + `mybatis-plus-jsqlparser`） |
| 数据库 | MySQL | 8.0+（5.7 需将驱动固定为 8.0.33） |
| 驱动 | mysql-connector-j | 随 Boot 版本管理（9.7.0） |
| 构建 | Maven | 3.6.3+（建议 3.9.x） |
| 鉴权 | spring-security-crypto（仅 BCrypt，不含过滤器链） | 随 Boot 版本管理（6.5.11） |
| 令牌 | JJWT（api / impl / jackson） | 0.13.0 |
| 其他 | Lombok **未使用**（实体手写 getter/setter） | — |

环境要求：
- JDK 17 及以上（项目按 17 编译）
- Maven 3.6.3+
- 可访问的 MySQL 实例，库名建议 `mall`
- 首次构建需要能访问 Maven 仓库（拉取 `jjwt`、`mybatis-plus` 与 `spring-security-crypto`），之后可离线编译

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

-- 用户表：password_hash 只存 BCrypt 哈希（60 字符定长），绝不存明文
CREATE TABLE sys_user (
  id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
  username      VARCHAR(50)  NOT NULL,
  password_hash CHAR(60)     NOT NULL                COMMENT 'BCrypt 哈希，$2a$ 开头',
  nickname      VARCHAR(50)  NOT NULL DEFAULT '',
  role          VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT 'USER / ADMIN',
  status        TINYINT      NOT NULL DEFAULT 1      COMMENT '1=正常 0=禁用',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
  user_id        BIGINT         NOT NULL,          -- 取自 token 的 sub，不再由前端传
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

### 2. 初始化登录账号

`sys_user` 里刻意不预置账号——把固定哈希写进仓库，等于给所有人一个已知密码。生成方式：跑一次测试工具，把输出粘进 INSERT。

```bash
mvn -q -Dtest=PasswordHashPrinterTest test
```

输出形如（每次运行都不同，因为盐是随机的；用**最后一次**的输出）：

```
盐生效: true, 校验通过: true
admin / admin123 -> $2a$10$...
tom   / tom123   -> $2a$10$...
```

```sql
INSERT INTO sys_user (username, password_hash, nickname, role) VALUES
  ('admin', '$2a$10$把上面 admin 那行粘到这里', '管理员', 'ADMIN'),
  ('tom',   '$2a$10$把上面 tom 那行粘到这里',   '小明',   'USER');
```

> `password_hash` 是 `CHAR(60)`：BCrypt 输出恒为 60 字符，写成长度不符会直接报错，比静默写坏好排查。

### 3. 配置数据源

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

### 4. 编译与启动

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

启动成功后验证（`/api/ping` 是 `@Public`，不带 token 也应 200）：

```bash
curl http://localhost:8080/api/ping
# {"status":"ok","app":"mall"}
```

### 5. 关键配置项

| 配置 | 位置 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `server.port` | application.yml | `8080` | 服务端口 |
| `spring.profiles.active` | application.yml | `local` | 激活的环境 |
| `mall.name` | application.yml | `小卖部` | 应用名（绑定到 `MallProperties`） |
| `mall.order.pay-timeout` | application.yml | `15m` | 支付超时，绑定为 `Duration`（预留，当前未参与业务） |
| `mall.order.max-items` | application.yml | `20` | 单笔订单最大明细数，校验范围 1–100 |
| `mall.auth.secret` | application.yml | 开发默认值 | JWT 签名密钥，**至少 32 字节**（HS256 要求 256 bit），不足启动即失败 |
| `mall.auth.ttl` | application.yml | `2h` | 令牌有效期，绑定为 `Duration` |
| `mall.auth.issuer` | application.yml | `mall` | 签发方，验签时强校验，防止别处签的 token 混入 |
| `mybatis-plus.configuration.log-impl` | application.yml | StdOut | 控制台打印 SQL（仅建议开发期开启） |

> 生产环境必须用环境变量覆盖密钥：`export MALL_JWT_SECRET=$(openssl rand -base64 48)`。
> `application-prod.yml` 里写的是 `${MALL_JWT_SECRET}`（无默认值），漏配会直接启动失败，而不是悄悄用开发密钥。
> 轮换密钥会让所有已签发 token 立即失效，这是预期行为，需配合前端重新登录。

---

## 五、基本使用与示例

### 统一响应结构

所有接口返回统一包装，`code = 0` 表示成功；`traceId` 用于把前端报错与后端日志对上：

```json
{ "code": 0, "message": "成功", "data": {}, "traceId": "3f1c8a5e7b2d4a90" }
```

响应头里同时带 `X-Trace-Id`，上游已带该头时原样复用。

| code | 含义 |
| --- | --- |
| 0 | 成功 |
| 4000 | 参数不合法 |
| 4010 | 未登录：没带 `Authorization`、签名不符、已过期、账号已删除 |
| 4030 | 无权限：身份有效但角色不足（如普通用户调商品写接口） |
| 4004 | 资源不存在（如商品 id 不存在） |
| 4100 | 业务处理失败（库存不足、状态冲突等） |
| 5000 | 系统繁忙（兜底，不返回内部细节） |

> `4010` 与 `4030` 必须分开消费：前端拿到 `4010` 才跳登录页，拿到 `4030` 应提示「无权限」而不是把用户踢出去重登。

### 鉴权约定

- 除标注「公开」的接口外，所有 `/api/**` 都必须带请求头：`Authorization: Bearer <token>`
- token 由 `POST /api/auth/login` 返回，默认 2 小时有效；过期后重新登录，本项目没有 refresh token
- 新增接口**默认需要登录**；确需公开时在方法上加 `@Public`（并在代码注释里写明理由）
- 只允许管理员调用的接口加 `@RequireRole(Role.ADMIN)`
- 当前用户身份统一用 `UserContext.require()` 获取，**不要**从请求参数里接收 `userId`

### 接口一览

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/ping` | 公开 | 健康检查 |
| POST | `/api/auth/login` | 公开 | 登录，返回 token 与用户信息 |
| GET | `/api/auth/me` | 登录 | 当前登录用户信息（前端刷新页面时校验登录态） |
| POST | `/api/auth/logout` | 登录 | 登出；无状态 token 由客户端删除，服务端仅返回成功 |
| GET | `/api/products` | 公开 | 商品分页查询（`page`、`size`、`categoryId`、`keyword`） |
| GET | `/api/products/{id}` | 公开 | 商品详情 |
| POST | `/api/products` | ADMIN | 新增商品 |
| PUT | `/api/products/{id}` | ADMIN | 更新商品 |
| PATCH | `/api/products/{id}/off-shelf` | ADMIN | 下架商品 |
| POST | `/api/orders` | 登录 | 创建订单（支持幂等，`userId` 取自 token） |

### 示例：登录并取得 token

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "user": { "id": 1, "username": "admin", "nickname": "管理员", "role": "ADMIN" }
  }
}
```

失败时统一返回 `4010` + `用户名或密码错误`（不区分「账号不存在」和「密码错误」）；账号被禁用返回 `4030`。

后续请求带上 token：

```bash
TOKEN=<上一步返回的 token>
curl -s http://localhost:8080/api/auth/me -H "Authorization: Bearer $TOKEN"
```

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

### 示例：新增商品（管理员）

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"雪碧 330ml","categoryId":1,"price":3.50,"stock":50}'
```

- 没带 token 返回 `4010`；带的是普通用户 token 返回 `4030`；这两种都在进入 Controller 之前就被拦截，不会触达业务代码
- 校验失败时返回 `code = 4000`，`message` 汇总所有字段错误，例如：`price: 价格必须大于 0; stock: 库存不能为负`。新增（POST）与更新（PUT）两个接口都会校验

### 示例：创建订单（幂等）

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
        "idempotentKey": "order-20261102-001",
        "items": [
          { "productId": 1, "quantity": 2 },
          { "productId": 2, "quantity": 1 }
        ]
      }'
```

```json
{ "code": 0, "message": "成功", "data": "202611021219268790083" }
```

> 请求体里不再有 `userId`。身份来自 token（`sub`），即使手工塞一个 `userId` 也会被忽略——DTO 里没有这个字段。

行为说明：
- 同一用户 + 相同 `idempotentKey` 重复请求，直接返回首次生成的订单号，不会重复扣库存
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
    │   │   ├── CostTime.java             # 方法耗时统计注解
    │   │   ├── Public.java               # 免登录白名单（必须 RUNTIME 保留）
    │   │   └── RequireRole.java          # 角色要求
    │   ├── common/
    │   │   ├── Result.java               # 统一响应体
    │   │   ├── ResultCode.java           # 全局错误码枚举（4010 / 4030 分开）
    │   │   ├── BusinessException.java    # 业务异常
    │   │   └── GlobalExceptionHandler.java  # 全局异常收敛
    │   ├── config/
    │   │   ├── MallProperties.java       # mall.* 配置绑定（构造器绑定 + 校验）
    │   │   ├── AuthProperties.java       # mall.auth.* 配置绑定（密钥 / 有效期 / issuer）
    │   │   ├── PasswordConfig.java       # BCryptPasswordEncoder Bean
    │   │   ├── WebMvcConfig.java         # 注册鉴权拦截器（只覆盖 /api/**）
    │   │   ├── MybatisPlusConfig.java    # 分页插件
    │   │   ├── JacksonConfig.java        # 时间序列化格式
    │   │   ├── TraceIdFilter.java        # traceId 生成/透传（写入 MDC 与响应头）
    │   │   └── WebLogAspect.java         # 请求日志 / 耗时切面（登录入参脱敏）
    │   ├── controller/
    │   │   ├── PingController.java
    │   │   ├── AuthController.java       # 登录 / 当前用户 / 登出
    │   │   ├── ProductController.java
    │   │   └── OrderController.java
    │   ├── service/
    │   │   ├── AuthService.java          # 校验密码、签发 token
    │   │   ├── ProductService.java
    │   │   ├── OrderService.java
    │   │   └── impl/
    │   │       ├── ProductServiceImpl.java
    │   │       └── OrderServiceImpl.java
    │   ├── mapper/
    │   │   ├── ProductMapper.java        # 继承 BaseMapper + 自定义 SQL
    │   │   ├── OrderMapper.java
    │   │   ├── OrderItemMapper.java
    │   │   └── SysUserMapper.java
    │   ├── security/                     # 鉴权基础设施
    │   │   ├── JwtService.java           # 签发 / 验签（HS256，不查库）
    │   │   ├── LoginUser.java            # 已认证身份（userId / username / role）
    │   │   ├── Role.java                 # USER / ADMIN
    │   │   ├── UserContext.java          # ThreadLocal 请求上下文
    │   │   └── AuthInterceptor.java      # 认证 → 角色校验
    │   ├── entity/                       # 数据库实体（sys_user / product / orders / order_item）
    │   ├── dto/                          # 入参对象（含校验注解，登录用 LoginDTO）
    │   └── vo/                           # 出参视图对象（ProductVO / UserVO / LoginVO）
    └── resources
        ├── application.yml               # 公共配置
        ├── application-local.yml.example # 本地配置模板（复制为 application-local.yml）
        ├── application-dev.yml           # 开发环境：SQL 日志
        ├── application-prod.yml          # 生产环境：环境变量注入 + 更大连接池
        └── mapper/ProductMapper.xml      # 自定义 SQL（联表分页、扣库存、行锁查询）
```

分层约定：`Controller` 不写业务逻辑 → `Service` 管事务与规则 → `Mapper` 只管取数；数据库字段 `snake_case` 与实体 `camelCase` 由 MyBatis-Plus 自动映射。

鉴权约定：`AuthInterceptor` 只负责「你是谁 + 你能不能调这个接口」，**数据归属**（只能看自己的订单）仍要写进 Service 的查询条件里；`Service` 不读 `UserContext`，身份一律由 `Controller` 显式传入，保持可单测。

---

## 七、贡献与许可

### 参与贡献

1. Fork 本仓库并从 `main` 切出特性分支：`git checkout -b feature/your-feature`
2. 遵循现有代码风格：构造器注入、DTO 承担校验、业务异常抛 `BusinessException`、Controller 统一返回 `Result`
3. 新增接口默认即为「需要登录」；确需公开请加 `@Public` 并写明理由，管理员接口加 `@RequireRole(Role.ADMIN)`
4. 新增 SQL 优先放 `resources/mapper/*.xml`，简单单表操作直接使用 `BaseMapper`
5. 提交前确认 `mvn clean package` 通过；涉及接口变更请同步更新本 README 的接口表与错误码表
6. 提交 PR 时说明动机、改动点与验证方式

### 注意事项（提交前自查）

- **不要提交** `application-local.yml`、`.env` 或任何明文口令；生产凭据只通过 `DB_URL` / `DB_USER` / `DB_PASSWORD` 注入
- **不要提交** JWT 密钥；`application-prod.yml` 用 `${MALL_JWT_SECRET}` 且不留默认值，漏配应当在启动时报错
- 涉及金额的计算一律使用 `BigDecimal`，禁止 `double`
- 新增接口请补充参数校验注解，避免脏数据落到库中
- 新增接收敏感字段（密码、手机号、身份证）的 DTO 时，同步在 `WebLogAspect.safeArgs` 里加脱敏分支，否则会明文进日志

### 已知限制

- 无刷新令牌：token 过期（默认 2h）只能重新登录
- 无法强制下线：无状态 JWT 签发后在有效期内一直可用；账号被禁用只对 `/api/auth/me` 立即生效，其他接口要等 token 过期。要立刻踢人需加 `token_version` 字段或 Redis 黑名单（按 `jti` 存）
- 登录接口无限流：未做失败次数统计与锁定，公网部署前应补按 IP + 用户名的计数
- 角色只有 `USER` / `ADMIN` 两档，且 `ADMIN` 隐含 `USER` 能力；需要细粒度权限时应拆成 `user` / `role` / `permission` 三张表
- 无支付、退款、订单状态流转（`orders.status` 目前恒为 `0`）
- 错误统一返回 **HTTP 200**，靠 body 里的 `code` 区分，前端不能依赖 HTTP 状态码判断成败（对接标准网关时应改成 401/403 真实状态码）
- traceId 已写入响应头与 `Result.traceId`，但控制台日志格式未包含它；要在日志里看到，需在 `application-dev.yml` 增加 `logging.pattern.console` 并使用 `%X{traceId:-}`
- 未提供接口文档（Swagger）；测试共 12 个用例，覆盖下单回滚与幂等、分页校验、4010/4030 分支、登录接口放行与 traceId 透传，尚未覆盖「真实登录成功后再用 token 调业务接口」的端到端链路

### 许可证

本项目当前**未附带 LICENSE 文件**，默认保留所有权利。若需开源发布，建议补充 `LICENSE`（如 MIT 或 Apache-2.0）并在本节说明，以便他人明确使用与分发条件。
