# 校园教室预约管理系统

校园教室预约管理系统面向在校学生和教务管理员，提供教室查询、时段预约、审批、取消、占用率展示和数据看板等能力。

本仓库采用前后端一体化结构：同一个 Spring Boot 应用同时提供 Thymeleaf 页面和 `/api/v1` REST API。MySQL 保存业务数据，Nginx 负责统一入口，摄像头推理在教室本地设备完成。

## 读者指南

### 如果你是部署者

建议按以下顺序阅读：

1. [系统架构](#系统架构)
2. [部署前准备](#部署前准备)
3. [环境变量](#环境变量)
4. [Docker-部署](#docker-部署)
5. [日常运维](#日常运维)

部署者负责准备服务器、配置环境变量、初始化数据库、启动容器、检查运行状态并保护密钥。

### 如果你是项目参与者

建议按以下顺序阅读：

- 先阅读[接口契约](docs/api-v1.md)，确认字段、路径、状态和错误码。
- 根据[项目目录](#项目目录)定位前端页面、后端模块、数据库脚本或部署文件。
- 摄像头成员同时阅读[摄像头方案说明](docs/camera-solution-guidance.md)。
- 修改跨端接口前，先更新接口契约，再修改前后端或摄像头客户端。
- 阅读[前后端改动说明](docs/frontend-backend-changes.md)了解当前结构与设计取舍。

## 系统架构

```text
浏览器
  |
  v
Nginx :80/:443
  |
  v
Spring Boot :8080
  ├── Thymeleaf 页面
  ├── /api/v1 用户与管理员 API
  └── /api/v1/ai 设备配置与上报 API
  |
  v
MySQL 8

教室摄像头客户端
  └── 本地推理 → 只上传人数与占用率数字
```

### 组件职责

| 组件 | 职责 |
|---|---|
| Spring Boot | 鉴权、教室、预约、审批、看板、设备上报、页面路由 |
| Thymeleaf | 服务端页面框架，页面数据由 JavaScript 调用 REST API |
| MySQL | 用户、教室、预约、占用率历史和设备绑定 |
| Nginx | 外部统一入口，反向代理 Spring Boot |
| Docker Compose | 管理 app、db、nginx 三个服务 |
| 摄像头客户端 | 在教室本地运行模型，按接口上报人数和占用率 |

## 项目目录

```text
classroom-booking-integrated/
├── Dockerfile                       # 多阶段应用镜像
├── compose.yml                      # app、MySQL、Nginx 编排
├── pom.xml                          # Maven 与依赖配置
├── mvnw / mvnw.cmd                  # Maven Wrapper
├── .env.example                     # 部署环境变量模板
│
├── docs/
│   ├── api-v1.md                    # 前后端与设备端接口契约
│   ├── db-schema.sql                # MySQL 建表脚本
│   ├── seed-dev.sql                 # 初始教室数据
│   ├── frontend-backend-changes.md  # 架构与改动说明
│   ├── camera-solution-guidance.md  # 摄像头方案与接口约束
│   └── test-report.md               # 项目验证记录
│
├── infra/
│   ├── maven/settings.xml           # Docker 构建 Maven 镜像
│   └── nginx/default.conf           # Nginx 反向代理配置
│
└── src/
    ├── main/
    │   ├── java/com/example/classroombooking/
    │   │   ├── common/              # 响应、异常、鉴权辅助、时间与 JSON
    │   │   ├── config/              # 管理员、设备初始化
    │   │   ├── controller/          # 页面与 REST API
    │   │   ├── dto/                 # 请求参数模型
    │   │   ├── entity/              # 数据库实体
    │   │   ├── mapper/              # MyBatis-Plus Mapper
    │   │   ├── service/             # 业务规则
    │   │   └── utils/               # JWT、定时任务
    │   └── resources/
    │       ├── application.yml      # 应用配置
    │       ├── static/css           # 页面样式
    │       ├── static/js            # 页面交互
    │       └── templates            # Thymeleaf 页面
    └── test/                        # 自动化验证代码与测试配置
```

## 技术栈

- Java 25
- Spring Boot 4.1
- Spring MVC + Thymeleaf
- MyBatis-Plus 3.5
- MySQL 8
- JWT（jjwt）
- BCrypt
- Docker Compose
- Nginx

## 部署前准备

部署机器需要：

- 能够运行 Docker Engine。
- 已安装 Docker Compose Plugin。
- 可以拉取 MySQL、Nginx 和 Java 基础镜像。
- 已开放对外的 HTTP 端口；启用 HTTPS 时还需要域名与 443 端口。

如果部署环境访问 Docker Hub 较慢，可以配置可用的 registry mirror。若构建阶段访问 Maven Central 较慢，可在 `infra/maven/settings.xml` 中维护构建镜像地址。

不要将摄像头视频流发送到业务服务器。摄像头在教室本地完成推理，服务器只处理数字上报。

## 环境变量

从模板创建本地配置：

```bash
cp .env.example .env
```

`.env` 只保留在部署机器，权限建议设置为：

```bash
chmod 600 .env
```

### 必填变量

| 变量 | 用途 |
|---|---|
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码，仅由数据库容器使用 |
| `MYSQL_APP_USER` | 应用数据库账号 |
| `MYSQL_APP_PASSWORD` | 应用数据库密码 |
| `JWT_SECRET` | JWT 签名密钥，至少 32 个字符 |
| `APP_ADMIN_USERNAME` | 首次启动时创建的管理员账号 |
| `APP_ADMIN_PASSWORD` | 首次启动时创建的管理员密码 |

### 可选变量

| 变量 | 默认值 | 用途 |
|---|---|---|
| `APP_ADMIN_NAME` | `系统管理员` | 管理员姓名 |
| `APP_ADMIN_DEPARTMENT` | `教务处` | 管理员部门 |
| `SERVER_PORT` | `8080` | Spring Boot 内部端口 |
| `OCCUPANCY_STALE_AFTER_SECONDS` | `300` | 占用率过期时间 |
| `CAMERA_REPORT_INTERVAL_SECONDS` | `30` | 建议摄像头上报间隔 |

### 摄像头启用变量

| 变量 | 用途 |
|---|---|
| `CAMERA_DEVICE_ID` | 设备唯一标识 |
| `CAMERA_DEVICE_TOKEN` | 设备密钥 |
| `CAMERA_CLASSROOM_ID` | 设备绑定的教室 ID |

三项均为空时不会创建设备，AI 上报接口也不会接受任何设备。

## Docker 部署

### 首次部署

```bash
cp .env.example .env
# 编辑 .env，填写数据库、JWT 和管理员配置
docker compose up -d --build
```

容器启动后，通过服务器 IP、域名或现有反向代理访问：

```text
http://<服务器地址>/
```

### 构建架构

在 x86_64 服务器上直接执行 `docker compose up -d --build` 即可构建对应架构的镜像。

如果在本机 Apple Silicon 构建后要推送至 x86_64 服务器，应显式指定平台：

```bash
docker buildx build --platform linux/amd64 \
  -t <镜像仓库>/classroom-booking-integrated:<版本号> .
docker push <镜像仓库>/classroom-booking-integrated:<版本号>
```

生产环境也可以将 `compose.yml` 的 `build` 替换为固定 `image`，由 CI 或镜像仓库负责构建。

### 数据库初始化

全新 MySQL 数据卷首次启动时执行：

```text
docs/db-schema.sql
docs/seed-dev.sql
```

注意：

- 初始化脚本只在空数据卷首次启动时执行。
- 已有数据库不会自动重新执行初始化脚本。
- 正式环境的结构变更应通过独立迁移脚本完成。
- SQL 使用 `utf8mb4`，不要删除初始化脚本中的 `SET NAMES utf8mb4`。
- 不要通过删除生产数据卷来处理结构升级。

### HTTPS

当前 Compose 默认提供 HTTP。正式开放前应配置域名和 TLS，并确保管理员密码、JWT 和预约数据不通过无加密公网链路传输。

## 本地开发

### 启动数据库

创建 `classroom_booking` 数据库，并依次执行：

```text
docs/db-schema.sql
docs/seed-dev.sql
```

### 启动应用

设置数据库、JWT 和管理员环境变量后执行：

```bash
./mvnw spring-boot:run
```

默认访问：

```text
http://localhost:8080/
```

当 8080 端口被占用时：

```bash
SERVER_PORT=18080 ./mvnw spring-boot:run
```

## 开发协作约定

### 页面与接口

- 页面路由只负责返回视图。
- 业务数据统一通过 `/api/v1` 获取。
- 所有接口字段、状态和错误码以[接口契约](docs/api-v1.md)为准。
- 不得重新引入 Mock 控制器到生产应用。
- 不得用用户名文本判断管理员角色。

### 时间与数据

- 可写时间使用带时区的 ISO 8601 格式。
- 校区业务时区为 `Asia/Shanghai`。
- 教室容量使用 `capacity`。
- 占用率计算使用座位总数 `seatCount`。
- 同一教室同一时段只允许一条已审批通过预约。

### 摄像头

- 正式模型使用 YOLO 人员或人头检测。
- 不使用 SFace、人脸 embedding 或身份匹配。
- 不保存、不上传原始画面。
- 设备通过 `X-Device-Id` 和 `X-Device-Token` 鉴权。
- 上报路径为 `/api/v1/ai/occupancy/report`。
- 字段和重试规则见[摄像头方案说明](docs/camera-solution-guidance.md)。

### 配置与密钥

- 不提交 `.env`、数据库密码、JWT 密钥或设备 Token。
- 不在日志中输出 Token。
- 新增部署配置时同步更新 `.env.example`。
- 修改接口、数据库字段或状态时同步更新文档。

## 日常运维

### 查看服务

```bash
docker compose ps
```

### 查看应用日志

```bash
docker compose logs -f app
```

### 重启应用

```bash
docker compose restart app
```

### 更新代码并重新部署

```bash
git pull
docker compose up -d --build
```

### 停止服务

```bash
docker compose down
```

不要使用 `docker compose down -v` 处理普通重启或升级，该命令会删除 MySQL 数据卷。

## 参考文档

- [接口契约](docs/api-v1.md)
- [前后端改动说明](docs/frontend-backend-changes.md)
- [摄像头方案说明](docs/camera-solution-guidance.md)
- [项目验证记录](docs/test-report.md)
