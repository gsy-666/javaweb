# JavaWeb 报修系统（三端）

本项目为 JavaWeb 课程验收项目，实现“一个系统三种角色端”的报修业务闭环：**用户端（报修）/ 工人端（接单处理）/ 管理端（管理与态势）**。  
技术栈：**Spring Boot 3（Java 17）+ MyBatis-Plus + Redis + Flyway + Vue 3 + Vite + Ant Design Vue**。

## 一、项目亮点（验收要点）

- **三端一体**：同一个 Vue SPA，根据角色展示不同端（管理/工人/用户），路由基于 `meta.roles` 权限控制
- **登录鉴权完整**：前端请求自动附带 `Authorization: Bearer <token>`；后端 Redis 保存 token 会话并在过滤器中解析用户
- **数据库迁移规范**：使用 Flyway 管理建表与数据变更脚本（可复现）
- **态势页面（Situation）**：跨角色态势页，支持统一查看业务态势与相关指标/状态

## 二、运行环境

建议环境 ：

- JDK 17
- Maven 3.8+
- Node.js 18+（含 npm）
- MySQL 8.x
- Redis 6.x+

## 三、项目结构

- `backend/`：后端 Spring Boot API（默认端口 `8082`）
- `web/`：前端 Vue3 SPA（一个应用，按角色呈现三端）
- `backend/src/main/resources/db/migration/`：Flyway 迁移脚本
- `web/src/router.ts`：前端路由与角色权限
- `web/src/api.ts`：Axios 封装（baseURL + token + 401/403 自动登出）

## 四、快速启动（验收版）

> 你可以选择 **Docker 一键启动** 或 **本地启动**。

### 方式 A：Docker 一键启动（推荐验收）

> 仓库根目录提供了 `docker-compose.yml`。

1）启动

```bash
docker compose up -d
```

2）查看服务状态

```bash
docker compose ps
```

3）停止

```bash
docker compose down
```

> 若你的 compose 文件同时包含前后端与数据库/Redis，则此方式最适合现场验收。

### 方式 B：本地启动（MySQL + Redis）

#### 1）准备数据库与 Redis

1. 启动 MySQL、Redis
2. 创建数据库（示例）

```sql
CREATE DATABASE `repair` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

3. 修改后端配置：`backend/src/main/resources/application.yml`

把下面信息改成你自己的：

- `spring.datasource.url/username/password`
- `spring.data.redis.host/port/password`（如有）

> Flyway 默认启用：首次启动会自动执行 `db/migration/` 下的脚本完成建表/初始化（以你实际脚本为准）。

#### 2）启动后端（backend）

```bash
cd backend
mvn -DskipTests spring-boot:run
```

后端默认地址：`http://localhost:8082`

#### 3）启动前端（web）

```bash
cd web
npm install
npm run dev
```

前端默认会把 API 指向 `http(s)://<host>:8082`。若验收环境端口/地址不同，可用环境变量指定：

- `VITE_API_BASE_URL=http://localhost:8082`

## 五、验收演示流程（建议老师按此走）

### 0）确认服务已启动

- 后端：8082 端口可访问
- 前端：Vite dev server 正常打开页面

### 1）登录演示（三角色）

请在此处填写验收用账号（务必保证可登录）：

- 管理员：admin / （请填写）
- 工人：worker1 / （请填写）
- 用户：user1 / （请填写）

> 登录后不同角色进入不同首页；无权限页面会被路由守卫拦截并跳转。

### 2）用户端（报修端）演示

- 新建报修单（填写标题/描述/地点等，按页面字段）
- 查看报修单列表与详情
- 查看状态流转（新建→处理中→已完成 等）

### 3）工人端演示

- 查看待处理/已接单任务列表
- 接单/处理/完成（按系统按钮与流程）
- 状态变化能回到用户端看到对应变化

### 4）管理端演示

- 管理员查看报修单全局情况
- 查看/管理用户、工人、报修单（按已有功能）
- 打开“态势（Situation）”页面展示全局态势

## 六、鉴权与权限说明

- 前端：`web/src/api.ts` 会自动在请求头加 `Authorization: Bearer <token>`
- 后端：Redis 存 token session，并在过滤器中解析当前用户上下文  
  入口位置：`backend/src/main/java/com/example/repair/common/security/UserContextFilter.java`
- 前端权限路由：`web/src/router.ts` 使用 `meta.roles` 控制角色可访问页面

## 七、常用命令

### 前端

```bash
cd web && npm run dev
cd web && npm run build
cd web && npm run preview
```

### 后端（未配置高德key）

```bash
cd backend && mvn spring-boot:run
cd backend && mvn -DskipTests spring-boot:run
cd backend && mvn test
```

## 八、常见问题（验收现场）

1. **前端请求后端跨域/连不上**  
   确认后端端口是 `8082`，或设置 `VITE_API_BASE_URL` 指向实际后端地址。

2. **数据库没表/没数据**  
   确认 `application.yml` 数据库配置正确；确认 Flyway 启用且 `db/migration/` 脚本存在。

3. **Redis 连接失败导致登录异常**  
   检查 Redis 地址/密码配置，确认 Redis 服务已启动。
