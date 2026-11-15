# 前后端改动说明

## 1. 改版目标

本次改版把原先分开的“前端模板 + 后端接口”整理为一个可部署的一体化工程，并按照[接口契约](./api-v1.md)补齐课程需求中的主要功能。

改版重点：

- 消除 Mock 数据和虚假登录逻辑。
- 页面与 API 同源部署，去掉双进程端口和 CORS 冲突。
- 补齐管理员教室管理、全部预约和人流量历史。
- 统一前后端字段名、时间格式、状态枚举和错误处理。
- 增加预约并发保护、审批人记录和数据新鲜度。
- 为摄像头端保留稳定的设备配置和上报接口。
- 提供 Docker Compose 云服务器部署。

原工程：

```text
课程项目/前端工程模板
课程项目/后端/classroom-booking
```

新工程：

```text
课程项目/classroom-booking-integrated
```

原工程保持不变，新工程可独立运行和部署。

---

## 2. 架构改动

### 2.1 原架构问题

- 前端使用 `MockApiController`，预约和登录不会写真实数据库。
- 前端用“用户名包含 admin”判断角色，不符合真实权限模型。
- 前端和后端都计划使用 8080 端口。
- 前端模板没有管理员教室管理、全部预约等页面。
- 看板字段与后端实体不一致。
- 摄像头脚本不调用后端 HTTP 接口。

### 2.2 新架构

```text
浏览器
  |
  v
Nginx :80
  |
  v
Spring Boot :8080
  ├── Thymeleaf 页面
  ├── /api/v1 REST API
  └── AI 设备接口占位
  |
  v
MySQL 8
```

摄像头客户端作为独立的教室边缘程序，只向 `/api/v1/ai/*` 上报数字，不上传视频。

---

## 3. 后端改动

### 3.1 统一响应与异常

新增或修改：

```text
common/Result.java
common/ApiException.java
common/GlobalExceptionHandler.java
common/PageResult.java
```

变化：

- 响应统一为 `code/message/data/timestamp`。
- 错误响应增加稳定 `errorCode`。
- HTTP 状态码与响应体 `code` 保持一致。
- 参数校验、未登录、权限不足、冲突和系统错误统一处理。
- 不再把 Java 异常信息直接返回给用户。

### 3.2 JWT 鉴权

修改：

```text
utils/JwtUtils.java
utils/JwtInterceptor.java
common/WebConfig.java
```

变化：

- Token 缺失或过期返回 HTTP 401，而不是 500。
- 每次请求重新核对用户是否存在且状态为 `ACTIVE`。
- 当前用户 ID 和角色由后端 JWT 获取，不信任请求体。
- 管理员由部署环境变量初始化，公开注册固定为 `STUDENT`。
- JWT 密钥从环境变量读取。

### 3.3 教室模块

新增/修改：

```text
controller/ClassroomController.java
service/ClassroomService.java
service/ClassroomServiceImpl.java
entity/Classroom.java
dto/ClassroomUpsertDTO.java
```

支持：

- 启用教室列表。
- 单间教室详情。
- 管理员新增、编辑和软删除。
- 教室编号唯一性校验。
- `capacity` 最大容纳人数与 `seatCount` 座位总数分离。
- 当前人数、占用率和最后采集时间。
- `occupancyFresh` 数据新鲜度。
- 有未结束预约时禁止停用教室。

### 3.4 预约模块

新增/修改：

```text
controller/BookingController.java
service/BookingService.java
service/BookingServiceImpl.java
mapper/BookingMapper.java
dto/BookingCreateDTO.java
dto/AuditDecisionDTO.java
dto/ReleaseDTO.java
```

支持：

- 提交预约、查询本人预约、取消预约。
- 管理员待审批列表、全部预约和状态筛选。
- 审批通过、驳回、人工释放。
- 自动释放过期预约。
- 同一时段可以有多个待审批申请。
- 最终只允许一条重叠的 `APPROVED`。

并发措施：

- 创建和审批事务内锁定教室行。
- 审批通过前再次检查已通过预约冲突。
- 实测两个并发审批结果为 HTTP 200 和 409。

状态：

```text
PENDING
APPROVED
REJECTED
CANCELLED
RELEASED
```

### 3.5 审批数据

原先审批接口没有记录审批人。现在：

- `approverId` 从管理员 JWT 获取。
- `reviewedAt` 记录审批时间。
- 驳回必须填写 `rejectReason`。
- 人工释放必须填写 `releaseReason`。

### 3.6 数据看板

新增/修改：

```text
controller/DashboardController.java
service/DashboardService.java
service/DashboardServiceImpl.java
```

支持：

- 总预约数、各状态数量。
- 按区间统计各教室预约次数。
- 热门教室字段统一为 `bookingCount`。
- 人流量历史按时间桶聚合，返回平均人数、峰值人数和平均占用率。

原先“热门教室”错误地按当前占用率排序且没有 `count` 字段，现已修正。

### 3.7 AI 接口占位

新增/修改：

```text
controller/AiController.java
service/OccupancyService.java
service/OccupancyServiceImpl.java
entity/AiDevice.java
dto/OccupancyReportDTO.java
```

支持：

- 设备凭据鉴权。
- 获取教室、座位数和上报间隔。
- 占用率上报。
- `(deviceId, sequence)` 幂等。
- 乱序上报只写历史，不覆盖最新当前值。
- 按 `seatCount` 重算占用率。
- 上报时间和当前人数更新。

摄像头客户端尚未加入工程，当前为接口占位。

### 3.8 时间格式

所有可写时间统一为带时区的 ISO 8601：

```text
2026-11-15T10:00:00+08:00
```

后端响应中的 `LocalDateTime` 也通过 Jackson 3 序列化器加上 `+08:00`。

---

## 4. 前端改动

### 4.1 请求封装

修改：

```text
static/js/request.js
```

变化：

- 自动从 `localStorage` 注入 Bearer Token。
- 同时接受 HTTP 200、201 等 2xx 响应。
- 401 自动清除本地登录状态并跳回登录页。
- 统一定义状态文字和样式。
- 提供 HTML 转义与日期格式化。
- 增加页面角色检查。
- 增加 DELETE 请求方法。

### 4.2 页面清单

| 页面 | 文件 | 主要功能 |
|---|---|---|
| 登录 | `login.html` | 真实后端登录、按角色跳转 |
| 注册 | `register.html` | 学生账号注册 |
| 教室列表 | `classrooms.html` | 中文楼栋、设备、占用率与30秒刷新 |
| 教室详情 | `classroom-detail.html` | 当前人数、占用率、新鲜度和预约入口 |
| 提交预约 | `booking.html` | 教室、时间、用途和人数 |
| 我的预约 | `my-bookings.html` | 全部状态、驳回原因、取消 |
| 教室管理 | `admin-classrooms.html` | 新增、编辑、停用 |
| 待审批 | `admin-pending.html` | 冲突提示、通过和驳回 |
| 全部预约 | `admin-bookings.html` | 状态筛选和人工释放 |
| 数据看板 | `admin-dashboard.html` | 状态统计、预约频次和流量历史 |

### 4.3 页面行为修复

- 删除 Mock 登录。
- 删除“用户名含 admin 即管理员”的逻辑。
- 时间从 `datetime-local` 转换为 `+08:00` ISO 格式。
- 取消按钮条件改为 `PENDING` 或 `APPROVED` 且尚未开始。
- 完整映射 `CANCELLED` 和 `RELEASED`，不再显示 `undefined`。
- 驳回原因读取 `rejectReason`，不再读取不存在的 `reason`。
- 看板读取 `bookingCount`，不再把当前占用率当预约次数。
- 无实时占用率时显示“暂无实时数据”，不继续显示旧百分比。
- 用户可控字段进入页面前进行转义。

---

## 5. 数据库改动

修改：

```text
docs/db-schema.sql
docs/seed-dev.sql
```

主要变化：

- 用户和教室状态改为业务字符串。
- 教室增加 `seat_count`。
- 教室增加 `current_head_count`、`current_occupancy_rate`、`occupancy_updated_at`。
- 预约增加 `reviewed_at`。
- 占用率历史增加 `seat_count`、`device_id`、`sequence_no`。
- 增加 `ai_device` 表。
- 增加用户、教室、审批人、设备等外键。
- `(device_id, sequence_no)` 唯一约束。
- SQL 初始化显式设置 `SET NAMES utf8mb4`。

演示教室：

```text
A101 / 一教 / 100人 / 96座
A102 / 一教 / 80人 / 76座
B201 / 二教 / 60人 / 60座
B202 / 二教 / 48人 / 48座
C305 / 三教 / 150人 / 144座
D401 / 四教 / 120人 / 112座
```

---

## 6. 配置与安全改动

### 6.1 环境变量

新增：

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
APP_ADMIN_USERNAME
APP_ADMIN_PASSWORD
APP_ADMIN_NAME
APP_ADMIN_DEPARTMENT
CAMERA_DEVICE_ID
CAMERA_DEVICE_TOKEN
CAMERA_CLASSROOM_ID
```

密码和密钥不写入 Git。

### 6.2 数据库密码

原先数据库口令直接写在源码配置中，现已改为环境变量。

### 6.3 AI 设备鉴权

AI 接口不再公开接受任意 JSON，必须携带：

```text
X-Device-Id
X-Device-Token
```

---

## 7. Docker 与部署改动

新增：

```text
Dockerfile
compose.yml
infra/nginx/default.conf
infra/maven/settings.xml
.env.example
.dockerignore
```

部署特点：

- 多阶段构建。
- 运行镜像使用 Java 25 JRE。
- 应用以非 root 用户运行。
- MySQL 数据使用命名卷。
- 只映射 Nginx 80/443。
- MySQL 和 Spring Boot 不暴露公网端口。
- Spring Boot、MySQL、Nginx 自动重启。
- 支持腾讯云 Docker/Maven 镜像加速。
- Nginx 反向代理页面和 API。

---

## 8. 兼容性说明

- API 基础路径保持 `/api/v1`。
- 登录和注册字段保持 `username/password/name/department`。
- 预约字段保持 `classroomId/startTime/endTime/purpose/expectedAttendees`。
- 时间值由无时区字符串改为带时区 ISO 8601。
- 审批请求由直接传 `status` 改为传 `decision=APPROVE|REJECT`。
- 教室接口由只读扩展为管理员 CRUD。
- 摄像头客户端必须使用新字段 `occupancyRate/collectedAt/sequence`。

前后端或摄像头端如与上述不一致，应以[接口契约](./api-v1.md)为准。

---

## 9. 当前仍待完成

1. 接入真实 YOLO 摄像头客户端。
2. 配置域名和 HTTPS。
3. 增加完整浏览器 E2E。
4. 增加自动释放任务的独立测试。
5. 增加压力测试和长时间稳定性测试。

详见：

- [测试报告](./test-report.md)
- [摄像头方案说明](./camera-solution-guidance.md)
