# 校园教室预约管理系统接口契约

版本：v1.1  
状态：前后端、摄像头端共同遵守的唯一接口基线  
需求依据：`课程项目/需求分析.doc`  
适用部署：Spring Boot 一体化页面与应用接口，MySQL 持久化；摄像头在教室本地推理

> 本文取代原 `api-v1.md`。实现与本文不一致时，以本文为准。  
> 若需求后续变化，应先修改本文和对应测试用例，再修改前后端代码。

---

## 1. 总体约定

### 1.1 基础地址

所有业务 API 的基础路径为：

```text
/api/v1
```

推荐由同一个 Spring Boot 应用同时提供页面和 API：

```text
浏览器 -> Nginx/HTTPS -> Spring Boot(页面 + /api/v1) -> MySQL
摄像头客户端 -> HTTPS -> /api/v1/ai/*
```

一体化部署时页面和 API 同源，不需要 CORS。前后端分开部署时，必须明确前端域名、后端域名和跨域白名单，不能使用生产环境 `*` 允许来源。

### 1.2 时间格式

所有可写时间字段一律使用带时区的 ISO 8601 格式：

```text
yyyy-MM-dd'T'HH:mm:ssXXX
```

示例：

```text
2026-11-14T10:00:00+08:00
```

用户页面的 `datetime-local` 输入值不带时区，前端提交前必须补上系统约定时区 `+08:00`。后端不得猜测服务器本地时区。

### 1.3 JSON 约定

- 请求与响应均为 `application/json; charset=UTF-8`。
- Java/JavaScript 字段统一使用小驼峰命名。
- 数据库下划线字段由后端映射为小驼峰，不能把 `reject_reason` 直接暴露给前端。
- 所有 ID 使用 JSON 数字，但前端不得对其做位运算。
- 金额不在本期范围；占用率使用两位小数。
- 未经明确声明的字段，前端不得依赖。

### 1.4 统一响应

成功响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": "2026-11-14T19:30:00+08:00"
}
```

失败响应：

```json
{
  "code": 409,
  "errorCode": "BOOKING_CONFLICT_APPROVED",
  "message": "该教室在所选时段已有审批通过的预约",
  "data": null,
  "timestamp": "2026-11-14T19:30:00+08:00"
}
```

规则：

- HTTP 状态码与响应体 `code` 保持一致。
- 错误响应必须包含稳定的字符串 `errorCode`；成功响应可以省略该字段。
- 前端用 `errorCode` 做逻辑分支，用 `message` 做用户提示，不能依赖中文提示文本判断错误类型。
- 无数据成功时 `data` 可以为 `null`。
- 错误响应不得把 Java 异常栈、SQL、数据库口令或 Token 返回给客户端。
- 前端根据 HTTP 状态或响应体 `code` 都可以判断失败，但必须优先处理 401。

常用状态码：

| HTTP | code | 含义 |
|---:|---:|---|
| 200 | 200 | 查询、更新、审批成功 |
| 201 | 201 | 新增教室成功 |
| 400 | 400 | 参数格式错误、字段缺失或业务输入非法 |
| 401 | 401 | 未登录、Token 缺失或过期 |
| 403 | 403 | 当前角色无权限 |
| 404 | 404 | 资源不存在 |
| 409 | 409 | 时间冲突、状态冲突、并发冲突 |
| 422 | 422 | 数据可解析但不符合业务约束 |
| 500 | 500 | 未预期服务器错误 |

核心业务错误码：

| errorCode | HTTP | 场景 |
|---|---:|---|
| `VALIDATION_ERROR` | 400 | 请求字段缺失、格式错误 |
| `INVALID_CREDENTIALS` | 401 | 账号或密码错误 |
| `TOKEN_INVALID_OR_EXPIRED` | 401 | Token 无效或过期 |
| `DEVICE_UNAUTHORIZED` | 401 | AI 设备凭据错误 |
| `FORBIDDEN` | 403 | 角色无权限 |
| `RESOURCE_NOT_FOUND` | 404 | 资源不存在 |
| `USERNAME_EXISTS` | 409 | 账号已存在 |
| `BOOKING_CONFLICT_APPROVED` | 409 | 与已通过预约冲突 |
| `BOOKING_STATE_CONFLICT` | 409 | 当前状态不允许该操作 |
| `CLASSROOM_HAS_ACTIVE_BOOKINGS` | 409 | 有未结束预约，不能停用教室 |
| `INVALID_BOOKING_TIME` | 422 | 时间范围非法 |
| `ROLE_NOT_ALLOWED` | 422 | 公开注册试图创建非学生角色 |
| `REJECT_REASON_REQUIRED` | 422 | 驳回时缺少理由 |
| `OCCUPANCY_RATE_MISMATCH` | 422 | 客户端占用率与后端重算不一致 |
| `OCCUPANCY_OUT_OF_ORDER` | 422 | 上报序号或采集时间非法 |
| `INTERNAL_ERROR` | 500 | 未预期服务器错误 |

### 1.5 分页

列表接口统一支持：

```text
page=1&size=20
```

- `page` 从 1 开始，默认 1。
- `size` 默认 20，最大 100。

分页响应：

```json
{
  "items": [],
  "page": 1,
  "size": 20,
  "total": 0
}
```

---

## 2. 鉴权与角色

### 2.1 用户鉴权

除注册、登录和 AI 设备接口外，用户接口必须携带：

```http
Authorization: Bearer <JWT>
```

JWT 至少包含：

```json
{
  "sub": "1",
  "role": "STUDENT",
  "iat": 1763100000,
  "exp": 1763107200
}
```

规则：

- 公开注册接口只能创建 `STUDENT`，忽略或拒绝请求中的管理员角色。
- 管理员账号由部署初始化脚本或环境变量创建，不开放管理员自助注册。
- Token 失效时后端必须返回 HTTP 401 和统一 JSON 错误体，不能返回 500。
- 用户被禁用后，已有 Token 也不得继续执行业务操作。

### 2.2 AI 设备鉴权

AI 上报接口不使用用户 JWT，使用设备凭据：

```http
X-Device-Id: cam-A101-01
X-Device-Token: <设备密钥>
```

规则：

- 设备密钥只允许出现在摄像头端环境变量中，不得写入前端、日志或 Git。
- 设备必须绑定唯一 `classroomId`。
- 请求体中的 `deviceId`、`classroomId` 必须与设备绑定关系一致。
- 未携带设备凭据的上报返回 401。
- 原始画面、视频、人脸特征和身份信息不得上传。

### 2.3 权限矩阵

| 功能 | STUDENT | ADMIN | AI 设备 |
|---|:---:|:---:|:---:|
| 注册、登录 | 允许 | 允许 | 不适用 |
| 查看启用教室、当前占用率 | 允许 | 允许 | 通过配置接口 |
| 提交预约 | 允许 | 可选允许 | 禁止 |
| 查看本人预约、取消本人预约 | 允许 | 仅本人记录 | 禁止 |
| 教室新增、编辑、停用/删除 | 禁止 | 允许 | 禁止 |
| 查看待审批和全部预约 | 禁止 | 允许 | 禁止 |
| 审批、人工释放异常预约 | 禁止 | 允许 | 禁止 |
| 数据看板 | 禁止 | 允许 | 禁止 |
| 上报占用率 | 禁止 | 禁止 | 允许 |

---

## 3. 枚举与状态机

### 3.1 用户角色 `role`

```text
STUDENT
ADMIN
```

### 3.2 教室状态 `status`

```text
ACTIVE
INACTIVE
```

- 学生只能查询 `ACTIVE`。
- 管理员可以查询两种状态。
- “删除教室”采用软删除，即设置为 `INACTIVE`，不得删除历史预约和占用率记录。

### 3.3 预约状态 `status`

```text
PENDING
APPROVED
REJECTED
CANCELLED
RELEASED
```

允许的状态转换：

```text
PENDING -> APPROVED
PENDING -> REJECTED
PENDING -> CANCELLED      学生取消，且尚未开始
PENDING -> RELEASED       到达结束时间仍未处理，系统自动释放
APPROVED -> CANCELLED     学生取消，且尚未开始
APPROVED -> RELEASED      已结束，或管理员人工释放
```

禁止任意跳转。管理员接口只允许提交审批决定 `APPROVE` 或 `REJECT`，不能直接提交任意 `status`。

### 3.4 占用率来源 `source`

```text
REAL
SIMULATED
```

- `REAL`：摄像头真实推理结果。
- `SIMULATED`：开发联调或演示数据，必须在页面标识。

### 3.5 占用率新鲜度

- 摄像头建议每 30 秒上报一次。
- 后端默认超过 300 秒未收到新数据，将 `occupancyFresh` 设为 `false`。
- `occupancyFresh=false` 时，前端显示“暂无实时数据”，同时显示最后更新时间，不得把旧百分比继续当作当前值展示。

---

## 4. 核心数据模型

### 4.1 User

```json
{
  "id": 1,
  "username": "2024141460169",
  "name": "陈同学",
  "role": "STUDENT",
  "department": "计算机学院",
  "status": "ACTIVE"
}
```

`passwordHash` 永远不能出现在响应中。

### 4.2 Classroom

```json
{
  "id": 1,
  "roomNo": "A101",
  "building": "一教",
  "capacity": 100,
  "seatCount": 96,
  "equipment": "投影仪,空调",
  "status": "ACTIVE",
  "currentHeadCount": 42,
  "occupancyRate": 43.75,
  "occupancyUpdatedAt": "2026-11-14T19:30:00+08:00",
  "occupancyFresh": true,
  "createdAt": "2026-11-01T09:00:00+08:00",
  "updatedAt": "2026-11-14T19:30:00+08:00"
}
```

字段说明：

| 字段 | 类型 | 规则 |
|---|---|---|
| `roomNo` | string | 必填，全局唯一，1～50 字符 |
| `building` | string | 必填，1～100 字符 |
| `capacity` | integer | 必填，大于 0，表示教室最大容纳人数 |
| `seatCount` | integer | 必填，大于 0，表示用于占用率计算的座位总数 |
| `equipment` | string | 可空，英文逗号分隔的设备标签 |
| `currentHeadCount` | integer/null | 没有上报时为 0 或 null |
| `occupancyRate` | number/null | 后端计算，两位小数；可以超过 100 |
| `occupancyFresh` | boolean | 后端根据最后上报时间计算 |

如果某教室没有独立座位总数，初始化时 `seatCount = capacity`，但必须显式写入，不能在代码中永久混用。

### 4.3 Booking

```json
{
  "id": 10,
  "userId": 1,
  "userName": "陈同学",
  "classroomId": 1,
  "roomNo": "A101",
  "building": "一教",
  "startTime": "2026-11-15T10:00:00+08:00",
  "endTime": "2026-11-15T12:00:00+08:00",
  "purpose": "小组研讨",
  "expectedAttendees": 8,
  "status": "PENDING",
  "approverId": null,
  "approverName": null,
  "reviewedAt": null,
  "rejectReason": null,
  "cancelledAt": null,
  "releasedAt": null,
  "releaseReason": null,
  "createdAt": "2026-11-14T19:30:00+08:00",
  "updatedAt": "2026-11-14T19:30:00+08:00"
}
```

前端必须使用 `rejectReason`，不得使用不存在的 `reason` 字段。

### 4.4 OccupancyRecord

```json
{
  "id": 1001,
  "classroomId": 1,
  "headCount": 42,
  "seatCount": 96,
  "occupancyRate": 43.75,
  "collectedAt": "2026-11-14T19:30:00+08:00",
  "source": "REAL",
  "deviceId": "cam-A101-01"
}
```

---

## 5. 认证接口

### 5.1 学生注册

```http
POST /api/v1/auth/register
```

请求：

```json
{
  "username": "2024141460169",
  "password": "至少8位的密码",
  "name": "陈同学",
  "department": "计算机学院"
}
```

约束：

- `username` 必填、唯一、1～50 字符。
- `password` 必填，至少 8 位，服务端使用 BCrypt 保存。
- `name` 必填，1～50 字符。
- `department` 必填，1～100 字符。
- 服务端固定创建 `STUDENT`。

成功：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "2024141460169"
  }
}
```

常见失败：

- `409 USERNAME_EXISTS`：账号已注册。
- `422 ROLE_NOT_ALLOWED`：请求试图注册非学生角色。

### 5.2 登录

```http
POST /api/v1/auth/login
```

请求：

```json
{
  "username": "2024141460169",
  "password": "用户密码"
}
```

成功：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "jwt-token",
    "expiresAt": "2026-11-15T19:30:00+08:00",
    "user": {
      "id": 1,
      "username": "2024141460169",
      "name": "陈同学",
      "role": "STUDENT",
      "department": "计算机学院",
      "status": "ACTIVE"
    }
  }
}
```

所有“账号不存在、密码错误”统一返回 401 和同一提示，避免泄露账号是否存在。

### 5.3 获取当前用户

```http
GET /api/v1/auth/me
Authorization: Bearer <JWT>
```

成功时返回最新 `User`，用于页面刷新后校验 Token 和角色。

---

## 6. 教室接口

### 6.1 获取启用教室列表

```http
GET /api/v1/classrooms?building=一教
Authorization: Bearer <JWT>
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "roomNo": "A101",
      "building": "一教",
      "capacity": 100,
      "seatCount": 96,
      "equipment": "投影仪,空调",
      "status": "ACTIVE",
      "currentHeadCount": 42,
      "occupancyRate": 43.75,
      "occupancyUpdatedAt": "2026-11-14T19:30:00+08:00",
      "occupancyFresh": true
    }
  ]
}
```

学生端只返回启用教室。管理员管理列表可另行传 `status=ALL`，该参数仅 ADMIN 有效。

### 6.2 教室详情

```http
GET /api/v1/classrooms/{id}
Authorization: Bearer <JWT>
```

返回单个 Classroom，至少包含当前人数、占用率、最后采集时间和新鲜度。该接口对应需求中的“教室详情页”。

### 6.3 新增教室

```http
POST /api/v1/classrooms
Authorization: Bearer <ADMIN JWT>
Content-Type: application/json
```

请求：

```json
{
  "roomNo": "A101",
  "building": "一教",
  "capacity": 100,
  "seatCount": 96,
  "equipment": "投影仪,空调",
  "status": "ACTIVE"
}
```

成功使用 HTTP 201，返回新建 Classroom。

### 6.4 编辑教室

```http
PUT /api/v1/classrooms/{id}
Authorization: Bearer <ADMIN JWT>
```

请求字段与新增一致。`roomNo` 修改后仍需保持全局唯一。

### 6.5 停用/删除教室

```http
DELETE /api/v1/classrooms/{id}
Authorization: Bearer <ADMIN JWT>
```

该操作是软删除，将状态改为 `INACTIVE`。

若存在尚未结束的 `PENDING` 或 `APPROVED` 预约，返回：

```json
{
  "code": 409,
  "errorCode": "CLASSROOM_HAS_ACTIVE_BOOKINGS",
  "message": "该教室存在未结束预约，请先处理后再停用",
  "data": {
    "unfinishedBookingCount": 2
  },
  "timestamp": "2026-11-14T19:30:00+08:00"
}
```

历史预约和占用率记录必须保留。

---

## 7. 预约接口

### 7.1 提交预约

```http
POST /api/v1/bookings
Authorization: Bearer <JWT>
```

请求：

```json
{
  "classroomId": 1,
  "startTime": "2026-11-15T10:00:00+08:00",
  "endTime": "2026-11-15T12:00:00+08:00",
  "purpose": "小组研讨",
  "expectedAttendees": 8
}
```

校验：

- 教室存在且为 `ACTIVE`。
- 开始时间早于结束时间，不能相等。
- 开始时间必须晚于当前时间。
- `purpose` 必填，1～255 字符。
- `1 <= expectedAttendees <= classroom.capacity`。
- 与同一教室已有 `APPROVED` 预约不得重叠。
- 允许同一时段存在多个 `PENDING`，以便管理员比较后选择一个审批。
- 提交时如与已有 `PENDING` 重叠，允许创建，但返回 `conflictWithPending=true`，管理端应显示冲突提示。

并发正确性要求：

- 创建和审批都必须在事务内先锁定对应 `classroom` 行，再执行冲突查询。
- 不得只依赖“查询后再插入”，因为两个并发请求可能同时查到无冲突。
- 审批通过时必须再次检查已有 `APPROVED`，不能只依赖提交时的检查。

成功：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "bookingId": 10,
    "status": "PENDING",
    "conflictWithPending": false
  }
}
```

冲突失败示例：

```json
{
  "code": 409,
  "errorCode": "BOOKING_CONFLICT_APPROVED",
  "message": "该教室在所选时段已有审批通过的预约",
  "data": null,
  "timestamp": "2026-11-14T19:30:00+08:00"
}
```

### 7.2 获取我的预约

```http
GET /api/v1/bookings/my?page=1&size=20&status=ALL
Authorization: Bearer <JWT>
```

分页返回 Booking，包含 `roomNo`、`rejectReason` 和所有状态字段。

### 7.3 取消预约

```http
PUT /api/v1/bookings/{id}/cancel
Authorization: Bearer <JWT>
```

规则：

- 只能取消本人预约。
- 只有 `PENDING` 和 `APPROVED` 可以取消。
- `startTime <= now` 后不能取消。
- 成功后状态为 `CANCELLED`，写入 `cancelledAt`。

前端取消按钮显示条件必须与上述规则一致，不得只对 `PENDING` 显示。

### 7.4 管理员获取待审批预约

```http
GET /api/v1/bookings/admin/pending?page=1&size=20
Authorization: Bearer <ADMIN JWT>
```

返回分页 Booking。列表必须包含：

- 申请人和教室；
- 开始、结束时间；
- 用途和预计人数；
- 是否与已有待审批或已通过预约冲突。

建议增加：

```json
{
  "conflictWithApproved": false,
  "conflictingBookingIds": [8]
}
```

### 7.5 管理员获取全部预约

```http
GET /api/v1/bookings/admin?status=ALL&classroomId=1&from=...&to=...&page=1&size=20
Authorization: Bearer <ADMIN JWT>
```

用于需求中的“查看全部预约记录”和查询自动释放记录。可用的特殊筛选：

```text
status=ALL
status=RELEASED
releaseReason=AUTO_EXPIRED
releaseReason=ADMIN_MANUAL
```

### 7.6 审批预约

```http
PUT /api/v1/bookings/admin/{id}/audit
Authorization: Bearer <ADMIN JWT>
```

通过：

```json
{
  "decision": "APPROVE"
}
```

驳回：

```json
{
  "decision": "REJECT",
  "reason": "该时段已有课程安排"
}
```

规则：

- 只能审批 `PENDING`。
- `decision` 只能是 `APPROVE` 或 `REJECT`。
- `REJECT` 时 `reason` 必填，1～255 字符。
- 审批通过前必须锁定教室并再次检查 `APPROVED` 冲突。
- 审批人来自 JWT，后端必须写入 `approverId`，不得由前端传入。
- 审批时间写入 `reviewedAt`。

成功返回最新 Booking。

### 7.7 管理员人工释放

```http
PUT /api/v1/bookings/admin/{id}/release
Authorization: Bearer <ADMIN JWT>
```

请求：

```json
{
  "reason": "管理员处理后释放"
}
```

规则：

- 只能释放 `PENDING` 或 `APPROVED`。
- 写入 `releasedAt`、`releaseReason`。
- 状态改为 `RELEASED`。

### 7.8 自动释放

后端定时任务每 5 分钟执行一次：

- 已超过 `endTime` 的 `PENDING`：改为 `RELEASED`，`releaseReason=AUTO_EXPIRED`。
- 已超过 `endTime` 的 `APPROVED`：改为 `RELEASED`，`releaseReason=AUTO_ENDED`。
- 已取消、已驳回的记录不得再次修改。
- 任务必须幂等，并记录执行结果。

---

## 8. 数据看板接口

### 8.1 总览

```http
GET /api/v1/admin/dashboard/summary?from=2026-11-01T00:00:00%2B08:00&to=2026-11-30T23:59:59%2B08:00
Authorization: Bearer <ADMIN JWT>
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalBookings": 36,
    "pendingBookings": 4,
    "approvedBookings": 20,
    "releasedBookings": 8,
    "cancelledBookings": 2,
    "rejectedBookings": 2,
    "topClassrooms": [
      {
        "classroomId": 1,
        "roomNo": "A101",
        "bookingCount": 12
      }
    ]
  }
}
```

`topClassrooms` 必须按区间内的预约次数排序，字段名固定为 `bookingCount`。不得用“当前占用率最高的教室”冒充预约频次。

### 8.2 各教室预约频次

```http
GET /api/v1/admin/dashboard/booking-frequency?from=...&to=...
Authorization: Bearer <ADMIN JWT>
```

响应：

```json
{
  "classroomId": 1,
  "roomNo": "A101",
  "bookingCount": 12,
  "approvedCount": 8,
  "releasedCount": 3
}
```

### 8.3 人流量历史

```http
GET /api/v1/admin/dashboard/occupancy-series?classroomId=1&from=...&to=...&bucketMinutes=60
Authorization: Bearer <ADMIN JWT>
```

响应：

```json
{
  "classroomId": 1,
  "roomNo": "A101",
  "bucketMinutes": 60,
  "points": [
    {
      "bucketStart": "2026-11-14T09:00:00+08:00",
      "avgHeadCount": 25.4,
      "maxHeadCount": 36,
      "avgOccupancyRate": 26.46
    }
  ]
}
```

---

## 9. AI 摄像头接口

### 9.1 设备获取配置

```http
GET /api/v1/ai/config
X-Device-Id: cam-A101-01
X-Device-Token: <设备密钥>
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "classroomId": 1,
    "roomNo": "A101",
    "seatCount": 96,
    "reportIntervalSeconds": 30,
    "staleAfterSeconds": 300,
    "serverTime": "2026-11-14T19:30:00+08:00"
  }
}
```

摄像头启动时必须先获取配置，不得再硬编码 `SEAT_TOTAL=40`。

### 9.2 上报占用率

```http
POST /api/v1/ai/occupancy/report
X-Device-Id: cam-A101-01
X-Device-Token: <设备密钥>
Content-Type: application/json
```

请求：

```json
{
  "classroomId": 1,
  "deviceId": "cam-A101-01",
  "headCount": 42,
  "occupancyRate": 43.75,
  "collectedAt": "2026-11-14T19:30:00+08:00",
  "source": "REAL",
  "sequence": 1058
}
```

字段规则：

- `classroomId` 和设备绑定关系必须一致。
- `headCount` 是大于等于 0 的整数。
- `occupancyRate = headCount / seatCount * 100`，保留两位小数。
- 后端以自身教室 `seatCount` 重新计算；客户端值与计算值误差超过 `0.05` 时返回 422。
- `collectedAt` 必须是推理完成时间，不能使用接收时间代替。
- `source` 只能是 `REAL` 或 `SIMULATED`。
- `sequence` 为设备内单调递增整数，用于幂等和乱序判断。

成功：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "recordId": 1001,
    "currentUpdated": true,
    "serverTime": "2026-11-14T19:30:01+08:00"
  }
}
```

后端行为：

- 每次合法上报写入 `occupancy_record`。
- `(deviceId, sequence)` 必须唯一；重复上报返回第一次成功结果，不重复写入。
- 如果 `collectedAt` 早于该教室最后采集时间：
  - 可以保留历史记录；
  - 不得覆盖教室当前人数和当前占用率；
  - 响应 `currentUpdated=false`。
- 上报事务内同时更新 `classroom.current_head_count`、`occupancy_rate`、`occupancy_updated_at`。
- 后端只保存数字和采集元数据，不保存图片、视频、人脸特征或身份信息。

### 9.3 摄像头客户端接口行为

摄像头客户端必须满足：

- 在教室本地完成视频读取和模型推理。
- 按固定间隔采样，不要求每一帧都上报。
- 首次启动先调用配置接口。
- 从配置读取 `classroomId`、`seatCount` 和上报间隔。
- 上报失败时采用有限次数指数退避；只缓存尚未发送的数字结果。
- 摄像头离线时停止上报，不能伪造 0 人作为真实数据。
- `source=SIMULATED` 只能由开发环境设备凭据上报，并在页面明确展示。
- 模型实现可以替换，但不得改变本文上报协议。

模型验收说明：

- 正式需求要求 YOLO 人头/人员检测，且只统计人数。
- YuNet 只能检测可见人脸，不能覆盖背对摄像头、低头、遮挡或侧脸场景，通常会造成漏计。
- SFace 属于人脸特征提取和识别，违反“不识别身份”的隐私要求，正式版本禁止启用。
- 如果保留 YuNet 作为临时开发演示，必须使用同一 AI 上报协议，并明确标记为“可见人脸近似计数”，不能作为需求验收通过依据。

---

## 10. 健康检查

### 10.1 应用健康

```http
GET /api/v1/health
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "status": "UP",
    "database": "UP",
    "time": "2026-11-14T19:30:00+08:00"
  }
}
```

该接口只能暴露运行状态，不得返回数据库地址、版本详情、环境变量或密钥。

---

## 11. 前端实现约束

### 11.1 页面与接口对应

| 页面 | 路径 | API |
|---|---|---|
| 登录 | `/login` | `POST /auth/login` |
| 注册 | `/register` | `POST /auth/register` |
| 教室列表 | `/classrooms` | `GET /classrooms` |
| 教室详情 | `/classrooms/{id}` | `GET /classrooms/{id}` |
| 提交预约 | `/booking` | `GET /classrooms`、`POST /bookings` |
| 我的预约 | `/my-bookings` | `GET /bookings/my`、`PUT /bookings/{id}/cancel` |
| 管理员教室管理 | `/admin/classrooms` | 教室 CRUD |
| 管理员待审批 | `/admin/pending` | `GET /bookings/admin/pending`、审批 |
| 管理员全部预约 | `/admin/bookings` | `GET /bookings/admin`、人工释放 |
| 管理员看板 | `/admin/dashboard` | 看板接口 |

### 11.2 状态文字

前端必须完整映射：

```javascript
{
  PENDING: "待审批",
  APPROVED: "已通过",
  REJECTED: "已驳回",
  CANCELLED: "已取消",
  RELEASED: "已释放"
}
```

未知状态显示“未知状态”，不能显示 `undefined`。

### 11.3 取消按钮

只有同时满足以下条件才显示：

```javascript
["PENDING", "APPROVED"].includes(booking.status)
  && new Date(booking.startTime) > new Date()
```

### 11.4 占用率显示

- `occupancyFresh=true`：显示百分比和进度条。
- `occupancyFresh=false`：显示“暂无实时数据”，并显示最后更新时间。
- `occupancyRate > 100`：显示“超过座位数”，进度条最大宽度限制为 100%，同时保留数值。
- 页面应每 30～60 秒重新请求，或通过 WebSocket 推送；只加载一次不算实时。

### 11.5 安全渲染

- 不得将 `userName`、`roomNo`、`equipment`、`purpose` 和驳回理由直接拼入 `innerHTML`。
- 优先使用 `textContent` 和 DOM API。
- 必须使用字符串转义时，应统一封装，不得在各页面自行拼接。

### 11.6 时间转换

前端把 `datetime-local` 的：

```text
2026-11-15T10:00
```

提交为：

```text
2026-11-15T10:00:00+08:00
```

不能继续使用没有时区的 `"2026-11-15 10:00:00"`。

---

## 12. 数据库一致性要求

以下约束必须由 SQL 迁移和业务层共同保证：

1. `user.username` 唯一。
2. `classroom.room_no` 唯一。
3. `booking.user_id`、`classroom_id`、`approver_id` 建立外键或由数据库触发器等效保证。
4. `occupancy_record.classroom_id` 建立外键。
5. `occupancy_record(device_id, sequence)` 唯一。
6. `booking.status`、用户角色、教室状态、上报来源使用受控枚举或受控字典。
7. 预约创建和审批使用教室行锁，防止并发重复审批。
8. `approver_id` 必须真实写入，不能长期为空。
9. `capacity` 与 `seat_count` 均必须大于 0。
10. 删除教室采用软删除，保留历史预约和占用率。

MySQL 没有原生的时间区间排他约束，因此预约重叠主要依赖事务内教室行锁和统一服务入口，不能只依靠普通索引。

---

## 13. 前后端联调验收

至少覆盖以下场景：

1. 普通注册用户的 `role` 始终为 `STUDENT`，无法越权注册管理员。
2. 未登录、过期 Token、禁用用户分别返回 401/403，并能正确跳转。
3. 学生只能查看自己的预约，不能取消他人预约。
4. 同一教室可存在多个待审批申请，但不能审批通过两个重叠预约。
5. 两个并发提交或并发审批请求经过行锁后，最多只有一个成功通过冲突预约。
6. 驳回原因必填，审批人 ID 非空。
7. 已通过且尚未开始的预约可以取消，前端按钮逻辑一致。
8. `REJECTED`、`CANCELLED`、`RELEASED` 在页面均能正确显示。
9. 摄像头第一次上报后教室列表更新；超过 300 秒后显示“暂无实时数据”。
10. 重复 `sequence` 不重复写记录；乱序上报不覆盖最新当前值。
11. 设备 Token 错误时无法伪造占用率。
12. 看板“热门教室”按预约次数展示，字段为 `bookingCount`。
13. 人流量历史按时间桶聚合，不能只返回当前占用率。
14. 摄像头离线时，登录、教室查询、预约和审批仍可工作。
