# 学生考试系统（NEXUS STUDENT SYS）

基于 Spring Boot 3 + Vue 3 + Element Plus + MySQL 的三角色在线考试系统，支持题库管理、组卷、在线考试、自动阅卷、成绩统计与错题本。

## 功能模块

| 模块 | 能力 |
|---|---|
| identity | 三角色账号（管理员 / 教师 / 学生）、JWT 登录、权限拦截 |
| question-bank | 单选 / 多选 / 判断 / 填空四类客观题，按课程归类，答案解析 |
| exam-paper | 手动选题组卷 + 按题型/难度/数量规则随机抽题，总分校验 |
| exam-session | 发布考试（时间窗口、时长、次数限制、开卷/闭卷）、倒计时、到时自动交卷、答案防抖暂存、防作弊事件记录 |
| grading | 客观题自动判分（多选全对得分，错选/漏选 0 分），按 BEST/LAST 规则聚合有效成绩 |
| results | 学生查成绩 / 考后回顾（含解析）/ 错题本；教师成绩列表与统计（均分、及格率、分数段） |

## 技术栈

- 后端：Spring Boot 3.2.5、Spring Data JPA、MySQL 8、JWT（jjwt 0.12）、BCrypt
- 前端：Vue 3.4（`<script setup>` + TypeScript）、Vue Router 4、Element Plus 2.6、Axios、Vite 5
- 工具：Maven、Node.js ≥ 18、Swagger UI（OpenAPI 3）

## 环境要求

- JDK 21（开发时也兼容 JDK 26）
- Maven 3.9（项目自带 Maven Wrapper `mvnw`）
- MySQL 8.x
- Node.js ≥ 18

## 快速开始

### 1. 准备数据库

创建 MySQL 数据库：

```sql
CREATE DATABASE studentdb DEFAULT CHARACTER SET utf8mb4;
```

默认连接配置（`backend/src/main/resources/application.properties`）：

```
url:      jdbc:mysql://localhost:3306/studentdb
username: root
password: 123456
```

> 首次启动时 JPA 会自动建表（`spring.jpa.hibernate.ddl-auto=update`），并由 `DataInitializer` 写入种子账号。

### 2. 启动后端

```bash
cd backend
./mvnw spring-boot:run
```

后端默认运行在 `http://localhost:8080`。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，开发环境通过 Vite 代理将 `/api` 转发到后端 8080 端口。

## 演示账号

系统首次启动时自动创建：

| 角色 | 用户名 | 密码 |
|---|---|---|
| 管理员 | `admin` | `admin123` |
| 教师 | `teacher` | `teacher123` |
| 学生 | 学号（如 `20230101`） | `student123` |

> 学生账号由 `DataInitializer` 为每个已存在的 `student` 记录自动开通，用户名即学号。

## 接口文档（Swagger UI）

后端启动后访问：

```
http://localhost:8080/swagger-ui/index.html
```

- 所有 `/api/**` 接口均已收录（共 37 个路径）
- 调用受保护接口：点击页面右上角 **Authorize**，填入登录接口返回的 `token`（不含 `Bearer ` 前缀），即可在 Swagger UI 内直接调试

## 测试

### 后端单测

```bash
cd backend
./mvnw test
```

当前基线：**106 个单测全绿**，覆盖判分规则、成绩聚合、组卷、考试会话、统计指标等核心逻辑。

### 前端类型检查与构建

```bash
cd frontend
npx vue-tsc --noEmit   # 类型检查
npm run build          # 生产构建（含类型检查）
```

## 目录结构

```
vue_project/
├── backend/                       # Spring Boot 后端
│   ├── src/main/java/com/example/app/
│   │   ├── controller/            # REST 控制器（13 个）
│   │   ├── service/               # 业务逻辑（含 AnswerMatcher 纯判分）
│   │   ├── entity/                # JPA 实体
│   │   ├── repository/            # Spring Data JPA 仓库
│   │   ├── dto/                   # 接口 DTO（Java record）
│   │   ├── security/              # JWT、拦截器、角色注解
│   │   └── config/                # WebMVC、OpenAPI、数据初始化
│   └── src/test/java/             # 单元测试
├── frontend/                      # Vue 3 前端
│   └── src/
│       ├── api/                   # Axios 封装与各模块接口
│       ├── views/                 # 页面（teacher/ 教师端，student/ 学生端）
│       ├── components/            # 公共组件（侧边栏等）
│       ├── stores/                # 认证状态
│       └── router/                # 路由与角色权限
└── SPEC-*.md                      # 各模块规格说明
```

## 核心设计要点

- **判分纯函数**：`AnswerMatcher` 为无副作用静态方法，独立于 JPA/Spring，便于高覆盖单测；多选题集合完全匹配才得分。
- **事务一致性**：交卷判分（落库 `AttemptAnswer` 明细 + `ExamRecord` 聚合）在同一事务内完成。
- **服务端计时**：考试倒计时完全基于服务端 `deadline`，前端仅展示；交卷/暂存接口强制超时校验。
- **闭卷脱敏**：闭卷发卷时服务端不下发 `answer_snapshot` 与解析，交卷后回顾页才开放。
- **数据隔离**：学生接口通过 `ResultService.currentStudentId()` 强制仅返回本人数据；教师接口按课程归属校验。
