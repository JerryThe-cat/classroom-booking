# 校园教室预约管理系统（一体化版）

本目录是前后端一体化的 Spring Boot 工程：同一个应用提供 Thymeleaf 页面和 `/api/v1` REST API。数据库结构、接口契约和 Docker 部署文件均在本目录内。

原目录 `课程项目/后端/classroom-booking` 和 `课程项目/前端工程模板` 未被修改。

## 已实现

- 学生注册、登录、JWT 鉴权和禁用用户校验。
- 管理员由部署环境变量初始化，不开放管理员自助注册。
- 教室列表、详情、新增、编辑、软删除。
- 教室实时占用率、数据新鲜度、无实时数据提示和30秒自动刷新。
- 预约提交、至少8位密码、用途、人数和时间校验。
- 同一时段允许多个待审批申请；审批通过前使用教室行锁再次校验冲突。
- 审批人 ID、审批时间、驳回理由、取消、人工释放和自动释放。
- 管理员全部预约、状态筛选、待审批列表。
- 预约次数统计、热门教室和人流量历史时间桶。
- AI 设备凭据、设备配置、占用率上报、去重和乱序保护。
- 统一 JSON 响应、HTTP 状态码和业务错误码。
- 10个页面和响应式基础样式。
- H2 自动化接口测试和 MySQL 8.4 实际联调。

摄像头客户端本身尚未放入工程。后端已经保留 `/api/v1/ai/config` 和 `/api/v1/ai/occupancy/report` 接口，等摄像头组确定模型后可直接接入。

## 技术栈

- Java 25 / Spring Boot 4.1
- Spring MVC + Thymeleaf
- MyBatis-Plus 3.5
- MySQL 8
- JWT（jjwt）
- Docker Compose + Nginx

## Docker 部署

首次使用：

```bash
cp .env.example .env
```

修改 `.env` 中的数据库、JWT、管理员和摄像头设备凭据，然后执行：

```bash
docker compose up -d --build
```

默认通过：

```text
http://服务器IP/
```

访问系统。数据库和 Spring Boot 不直接映射到宿主机公网端口。

如果本机是 Apple Silicon、云服务器是常见的 x86_64，要避免直接把本机生成的 arm64 镜像推送过去。可以在云服务器执行上面的构建命令，或者显式构建 amd64 镜像：

```bash
docker buildx build --platform linux/amd64 \
  -t your-registry/classroom-booking-integrated:1.0.0 .
docker push your-registry/classroom-booking-integrated:1.0.0
```

此时生产服务器也可以把 `compose.yml` 中的 `build` 改为 `image: your-registry/classroom-booking-integrated:1.0.0`。

首次启动时 MySQL 会执行：

```text
docs/db-schema.sql
docs/seed-dev.sql
```

`docs/seed-dev.sql` 包含三个演示教室。已有 MySQL 数据卷不会重复执行初始化 SQL，正式环境应使用独立迁移流程，不能依赖修改数据卷内文件。

### 环境变量

至少必须设置：

```text
MYSQL_ROOT_PASSWORD
MYSQL_APP_PASSWORD
JWT_SECRET
APP_ADMIN_PASSWORD
```

摄像头接入后设置：

```text
CAMERA_DEVICE_ID
CAMERA_DEVICE_TOKEN
CAMERA_CLASSROOM_ID
```

不设置摄像头变量时，AI 接口不会接受任何设备。

## 本地运行

先创建 MySQL 数据库并执行：

```text
docs/db-schema.sql
docs/seed-dev.sql
```

然后设置数据库和管理员环境变量，使用：

```bash
./mvnw spring-boot:run
```

常用环境变量：

```text
SERVER_PORT
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
APP_ADMIN_USERNAME
APP_ADMIN_PASSWORD
APP_ADMIN_NAME
APP_ADMIN_DEPARTMENT
```

如果宿主机 8080 端口已占用，可设置：

```bash
SERVER_PORT=18080 ./mvnw spring-boot:run
```

## 摄像头接入约定

摄像头端必须：

1. 调用 `GET /api/v1/ai/config` 获取教室和座位数。
2. 在教室本地读取视频并推理。
3. 按配置间隔调用 `POST /api/v1/ai/occupancy/report`。
4. 请求头携带 `X-Device-Id` 和 `X-Device-Token`。
5. 只上传人数、占用率、采集时间和序号。
6. 不上传视频、图片、人脸特征或身份信息。

正式方案应使用 YOLO 人员/人头检测。YuNet 会产生背对摄像头漏计，SFace 涉及人脸识别，均不应作为正式验收实现。

接口字段、错误码、状态机和请求示例见：

```text
docs/api-v1.md
```

## 测试

自动化接口测试：

```bash
./mvnw test
```

编译：

```bash
./mvnw package
```

检查 Compose 配置：

```bash
cp .env.example .env
docker compose config
```

## 项目文档

- [接口契约](docs/api-v1.md)
- [测试报告](docs/test-report.md)
- [前后端改动说明](docs/frontend-backend-changes.md)
- [摄像头方案说明](docs/camera-solution-guidance.md)

## 部署资源建议

4核4G云服务器适合当前业务应用和 MySQL，前提是摄像头在教室本地运行。建议：

- Spring Boot JVM 使用 `-XX:MaxRAMPercentage=70`。
- MySQL InnoDB buffer pool 约 384MB。
- 配置 1～2GB swap。
- 磁盘至少 20～40GB。
- 只开放 80/443，MySQL 和 8080 保持在 Docker 内网。
- 6M 公网带宽只传输页面和 JSON，不上传视频流。
