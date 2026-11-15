# 校园教室预约管理系统测试报告

## 1. 文档信息

| 项目 | 内容 |
|---|---|
| 系统名称 | 校园教室预约管理系统（一体化版） |
| 测试日期 | 2026-11-14 至 2026-11-15 |
| 测试基线 | Git `main`，提交 `af7f723` |
| 接口依据 | [API 契约](./api-v1.md) |
| 需求依据 | `课程项目/需求分析.doc` |
| 测试对象 | Spring Boot 页面、REST API、MySQL、Nginx、Docker Compose、AI 设备上报接口 |
| 结论 | 预约核心业务与部署链路通过；真实摄像头模型和完整浏览器 E2E 尚未执行 |

> 本报告只记录实际执行过的检查。未执行的测试会明确标记，不使用“预计通过”代替测试结果。

---

## 2. 测试环境

### 2.1 本地开发环境

| 项目 | 版本/配置 |
|---|---|
| 操作系统 | macOS |
| Java | OpenJDK 27.0.1 |
| Spring Boot | 4.1.2 |
| MySQL | 8.4 |
| Docker | 29.9 |
| Docker Compose | 5.x |
| Node.js | 24.x |
| 应用本地端口 | 18080 |
| MySQL 测试端口 | 3307 |

### 2.2 云服务器环境

| 项目 | 配置 |
|---|---|
| 操作系统 | Ubuntu 24.04.5 LTS |
| CPU | 4 核 |
| 内存 | 3.6 GiB，另有约 1.9 GiB swap |
| 磁盘 | 69 GB |
| Docker | Docker CE 29.9.3 |
| Docker Compose | v5.7.1 |
| 对外服务 | Nginx HTTP 80 |
| 容器 | `app`、`db`、`nginx` |

服务器地址、密码和密钥不写入本测试报告。

---

## 3. 测试范围

本次测试覆盖：

- 用户注册、登录、JWT 鉴权与角色限制。
- 教室列表、详情、新增、编辑和软删除。
- 预约创建、多个待审批申请、审批、取消与冲突。
- 并发审批一致性。
- 审批人、驳回理由和状态转换。
- 管理员全部预约与自动/人工释放。
- 数据看板预约次数统计和人流量历史。
- AI 设备鉴权、占用率上报、幂等与乱序处理。
- MySQL 初始化、UTF-8 中文数据。
- 本地 Docker 全栈启动。
- 云服务器安装 Docker、构建镜像、部署和公网访问。

未覆盖：

- 真实 YuNet、SFace 或 YOLO 模型精度。
- 真实摄像头取帧、遮挡、逆光和多人场景。
- 逐页点击、表单提交的完整浏览器自动化 E2E。
- HTTPS/TLS、域名和证书。
- 大规模并发、压力测试和长时间稳定性测试。
- 安全渗透测试。

---

## 4. 自动化测试

### 4.1 执行命令

```bash
./mvnw test
```

2026-11-15 重新执行结果：通过。

测试类：

```text
src/test/java/com/example/classroombooking/ApiIntegrationTests.java
```

测试数据库使用 H2 MySQL 模式，启动时执行：

```text
src/test/resources/schema-test.sql
```

### 4.2 自动化用例

| 编号 | 测试方法 | 测试内容 | 预期 | 结果 |
|---|---|---|---|---|
| AT-01 | `authenticationAndCameraReportWork` | 无 Token 访问教室接口 | HTTP 401，`errorCode=TOKEN_INVALID_OR_EXPIRED` | 通过 |
| AT-02 | `authenticationAndCameraReportWork` | 有效设备凭据上报 42 人、96 个座位 | 成功写入，占用率为 43.75% | 通过 |
| AT-03 | `authenticationAndCameraReportWork` | 上报后读取教室列表 | `occupancyFresh=true`，占用率 43.75% | 通过 |
| AT-04 | `pendingBookingsCanCoexistButOnlyOneCanBeApproved` | 两个用户提交重叠时段预约 | 两条均可进入 `PENDING` | 通过 |
| AT-05 | `pendingBookingsCanCoexistButOnlyOneCanBeApproved` | 审批通过第一条 | 第一条变为 `APPROVED`，审批人 ID 非空 | 通过 |
| AT-06 | `pendingBookingsCanCoexistButOnlyOneCanBeApproved` | 再审批通过第二条 | HTTP 409，`BOOKING_CONFLICT_APPROVED` | 通过 |
| AT-07 | `pendingBookingsCanCoexistButOnlyOneCanBeApproved` | 第一条已通过预约取消 | 状态变为 `CANCELLED` | 通过 |
| AT-08 | `pendingBookingsCanCoexistButOnlyOneCanBeApproved` | 第一条取消后再审批第二条 | 第二条可以变为 `APPROVED` | 通过 |
| AT-09 | `pendingBookingsCanCoexistButOnlyOneCanBeApproved` | 查询本人预约 | 返回 `CANCELLED`，教室号为 A101 | 通过 |
| AT-10 | `publicRegistrationCannotCreateAdmin` | 注册请求中伪造 `role=ADMIN` | 后端仍创建 `STUDENT` | 通过 |

### 4.3 自动化覆盖限制

- 测试数据库为 H2，不能完全替代 MySQL 的行锁、字符集和外键行为。
- H2 自动化测试未执行真正同时到达服务器的并发请求。
- 真实并发审批通过 MySQL 手工测试补充验证。

---

## 5. 真实 MySQL 接口联调

### 5.1 环境

- MySQL 8.4 运行于 Docker。
- Spring Boot 连接真实 MySQL，不使用 H2。
- 通过环境变量初始化管理员和摄像头设备。

### 5.2 手工用例

| 编号 | 场景 | 实际结果 | 结论 |
|---|---|---|---|
| MY-01 | 管理员首次启动自动创建 | 管理员账号创建成功 | 通过 |
| MY-02 | 学生注册并登录 | 返回 JWT，角色为 `STUDENT` | 通过 |
| MY-03 | 请求体伪造管理员角色 | 实际角色仍为 `STUDENT` | 通过 |
| MY-04 | 无 Token 请求受保护接口 | HTTP 401 | 通过 |
| MY-05 | 管理员新增教室 | 返回 HTTP 201 和教室 ID | 通过 |
| MY-06 | 管理员编辑教室容量和座位数 | 修改成功并返回新值 | 通过 |
| MY-07 | 存在未来预约时停用教室 | HTTP 409，`CLASSROOM_HAS_ACTIVE_BOOKINGS` | 通过 |
| MY-08 | AI 设备获取配置 | 返回 `classroomId`、`seatCount`、上报间隔 | 通过 |
| MY-09 | 摄像头上报 42 人、96 个座位 | 计算占用率 43.75% | 通过 |
| MY-10 | 重复提交相同设备序号 | 不重复写记录，返回原记录 | 通过 |
| MY-11 | 创建重叠时段的两个待审批预约 | 两条均创建成功，第二条提示 `conflictWithPending=true` | 通过 |
| MY-12 | 审批通过第一条预约 | 审批人 ID 写入数据库 | 通过 |
| MY-13 | 审批第二条与已通过预约冲突的申请 | HTTP 409 | 通过 |
| MY-14 | 取消已通过且未开始的预约 | 状态变为 `CANCELLED` | 通过 |
| MY-15 | 取消后审批第二条 | 第二条成功变为 `APPROVED` | 通过 |
| MY-16 | 查询本人预约 | `roomNo`、`rejectReason`、完整状态正常返回 | 通过 |
| MY-17 | 管理员查询全部预约 | 可分页并显示审批人和状态 | 通过 |
| MY-18 | 看板总览 | 返回各状态预约数量 | 通过 |
| MY-19 | 热门教室统计 | 使用 `bookingCount` 按预约次数统计 | 通过 |
| MY-20 | 人流量历史 | 返回时间桶、平均人数、峰值人数和平均占用率 | 通过 |

### 5.3 缺陷与修复

| 编号 | 发现问题 | 修复 |
|---|---|---|
| DF-01 | `bookings/my` 默认 `status=ALL` 被错误判为非法状态 | 统一将 `ALL` 解释为不过滤状态 |
| DF-02 | Spring Boot 4 不再兼容旧 Jackson 2 时间序列化配置 | 使用新的 Jackson 3 自定义序列化器，输出 `+08:00` |
| DF-03 | 审批接口缺少审批人写入 | 从 JWT 获取管理员 ID 并保存到 `approver_id` |
| DF-04 | 取消授权依赖前端状态 | 后端统一校验本人、状态和开始时间 |

---

## 6. 并发审批测试

### 6.1 测试方法

在同一教室、同一时段创建两条 `PENDING` 预约，然后并行发送两个审批通过请求。

### 6.2 实际结果

```text
预约 A 审批结果：HTTP 200
预约 B 审批结果：HTTP 409
```

数据库最终只有一条预约为 `APPROVED`。

### 6.3 结论

教室行锁和审批前二次冲突检查生效，满足“同一教室同一时段不能存在两条已审批通过预约”的核心要求。

---

## 7. 前端静态与页面路由测试

### 7.1 JavaScript 语法

执行：

```bash
for f in src/main/resources/static/js/*.js; do node --check "$f"; done
```

结果：全部通过。

### 7.2 页面路由

以下路由均返回 HTTP 200：

| 页面 | 路径 |
|---|---|
| 登录 | `/login` |
| 注册 | `/register` |
| 教室列表 | `/classrooms` |
| 教室详情 | `/classrooms/1` |
| 预约表单 | `/booking` |
| 我的预约 | `/my-bookings` |
| 管理员教室管理 | `/admin/classrooms` |
| 管理员待审批 | `/admin/pending` |
| 管理员全部预约 | `/admin/bookings` |
| 管理员看板 | `/admin/dashboard` |

根路径 `/` 返回 302，跳转登录页。

### 7.3 限制

已检查页面路由、脚本语法和接口返回，但没有使用浏览器自动化逐项点击所有按钮。完整 E2E 交互仍属于待补充测试。

---

## 8. Docker 测试

### 8.1 镜像构建

执行：

```bash
docker compose build app
docker compose up -d
```

结果：通过。

### 8.2 本地全栈验证

| 编号 | 检查 | 结果 |
|---|---|---|
| DK-01 | `app` 容器健康 | 通过 |
| DK-02 | `db` 容器健康 | 通过 |
| DK-03 | `nginx` 启动并映射 80 端口 | 通过 |
| DK-04 | `/api/v1/health` 返回 HTTP 200 | 通过 |
| DK-05 | 健康接口显示数据库 `UP` | 通过 |
| DK-06 | 管理员登录 | 通过 |
| DK-07 | 页面路由通过 Nginx 访问 | 通过 |

测试结束后已关闭容器并删除临时数据卷。

---

## 9. 云服务器部署测试

| 编号 | 检查 | 实际结果 | 结论 |
|---|---|---|---|
| CL-01 | Ubuntu 24.04 环境检查 | 4 核、3.6 GiB 内存、69 GB 磁盘 | 通过 |
| CL-02 | Docker 和 Compose 安装 | Docker 29.9.3、Compose 5.7.1 | 通过 |
| CL-03 | Docker 开机启动 | `docker.service` 为 enabled/active | 通过 |
| CL-04 | Docker Hub 加速 | 配置腾讯云镜像后成功拉取镜像 | 通过 |
| CL-05 | Maven 依赖镜像 | 配置腾讯云 Maven 镜像后构建成功 | 通过 |
| CL-06 | 三容器启动 | app、db、nginx 均运行 | 通过 |
| CL-07 | 应用和数据库健康 | 两者均 healthy | 通过 |
| CL-08 | 公网健康接口 | HTTP 200，数据库 UP | 通过 |
| CL-09 | 公网页面访问 | 登录、注册、教室和看板路由 200 | 通过 |
| CL-10 | 公网管理员登录 | 返回 ADMIN | 通过 |
| CL-11 | MySQL 建表 | 五张业务表存在 | 通过 |
| CL-12 | 服务器 Git 工作区 | `main` 与远程一致，工作区干净 | 通过 |

### 9.1 部署阶段发现的问题

| 编号 | 问题 | 处理 |
|---|---|---|
| DP-01 | 服务器直连 Docker Hub 超时 | 配置腾讯云 registry mirror |
| DP-02 | Maven 首次依赖解析过慢 | 在 Docker 构建中加入腾讯云 Maven settings |
| DP-03 | SQL 挂载文件权限为 700，MySQL 容器无法读取 | 规范源码和部署文件权限为 644，脚本 `mvnw` 为 755 |
| DP-04 | 初始化 SQL 中文被按错误字符集写入 | 建表和种子脚本加入 `SET NAMES utf8mb4` |

---

## 10. 中文教室数据测试

部署后曾发现楼栋和设备显示为乱码，根因是 MySQL 初始化会话字符集不正确。

修复后验证：

- A101、A102、B201、B202、C305、D401 六间教室正常显示。
- 楼栋名称和中文设备名称通过 API 返回正常 UTF-8。
- 原 B202 关联的一条待审批预约已平滑迁移到新 B202。
- 新数据库初始化脚本已加入 `SET NAMES utf8mb4`。

---

## 11. 安全管理测试

| 编号 | 检查 | 结果 |
|---|---|---|
| SC-01 | 公开注册不能创建管理员 | 通过 |
| SC-02 | AI 接口需要设备 Token | 通过 |
| SC-03 | 无 Token 用户接口返回 401 | 通过 |
| SC-04 | 管理员 Token 来自环境变量配置 | 通过 |
| SC-05 | `.env` 文件权限为 600 | 通过 |
| SC-06 | GitHub 未包含 `.env` | 通过 |
| SC-07 | 修改管理员密码后新密码成功、旧密码失败 | 通过 |

尚未执行：

- HTTPS/TLS 证书测试。
- 暴力破解防护、限流和账号锁定。
- XSS/CSRF 渗透测试。
- 密钥轮换流程测试。

---

## 12. 总体结论

当前版本的核心预约业务、角色权限、管理员功能、数据看板、数据库部署和 Docker 上线链路均已通过实际验证。

项目可以作为预约核心系统的可运行版本进入下一阶段。摄像头模型部分仍是接口占位，不能把模型精度视为已验收。正式开放使用前，应优先补充：

1. YOLO 人头/人员检测客户端和真实摄像头测试。
2. 域名与 HTTPS。
3. 浏览器端完整 E2E 测试。
4. 压力和长时间稳定性测试。
