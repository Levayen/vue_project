# Spec: identity — 账号、认证与角色权限

> 模块 id：`identity` ｜ 依赖：无（基础模块）｜ 能力地图：SPEC-exam-system-map.md

## Objective

现有学生管理系统没有任何认证模块，所有接口裸奔。本模块为考试系统（及后续功能）提供：

- 账号登录认证（用户名 + 密码）
- 三类角色：`ADMIN`（管理员）、`TEACHER`（教师）、`STUDENT`（学生）
- 学生账号与现有 `student` 表关联；教师/管理员为独立账号
- 基于 token 的无状态鉴权，接口按角色授权

**用户故事**
- 作为管理员，我能创建教师/管理员账号，以便教师使用系统。
- 作为教师，我能用账号登录并管理题库、试卷、考试、查看成绩。
- 作为学生，我能用学号作为账号登录，参加我所选课程的考试。
- 作为任意用户，未登录访问受保护接口时得到 401，权限不足时得到 403。

## Tech Stack

- 后端：Spring Boot 3.2.5 / JPA / MySQL（沿用现有）
- 新增依赖（pom.xml，需在变更单说明）：
  - `org.springframework.security:spring-security-crypto`（仅用 BCryptPasswordEncoder，不引入整套 security 过滤链）
  - `io.jsonwebtoken:jjwt-api/impl/jackson:0.12.5`（JWT 签发与校验）
- 前端：Vue3 + Element Plus + vue-router（沿用），axios 拦截器附加 token

## Commands

```powershell
# 后端（工程内置 Maven）
cd backend; ..\tools\maven\apache-maven-3.9.16\bin\mvn.cmd -q test          # 跑测试
cd backend; ..\tools\maven\apache-maven-3.9.16\bin\mvn.cmd spring-boot:run # 启动
# 前端
cd frontend; npx vue-tsc --noEmit   # 类型门禁
cd frontend; npm run dev            # 启动
```

## Project Structure（本模块新增）

```
backend/src/main/java/com/example/app/
├─ entity/SysUser.java            # 账号实体（表 sys_user）
├─ repository/SysUserRepository.java
├─ service/AuthService.java       # 登录、注册、账号管理
├─ controller/AuthController.java # /api/auth/**
├─ controller/SysUserController.java # /api/users/**（管理员管理账号）
├─ security/
│  ├─ JwtUtil.java                # JWT 签发/解析/校验
│  ├─ AuthInterceptor.java        # 校验 token，注入当前用户
│  └─ UserContext.java            # ThreadLocal 当前用户
└─ config/WebConfig.java          # 注册拦截器、放行 /api/auth/login
frontend/src/
├─ api/auth.ts                    # 登录/账号管理 API + 类型
├─ stores/auth.ts                 # 当前用户、token、角色（reactive 本地存储）
├─ views/Login.vue
├─ router/index.ts                # 路由 meta.roles + 全局守卫
└─ api/axios.ts                   # 请求头带 token；401 跳登录
```

## 数据模型

**sys_user**（新表，字段小写下划线）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK auto | |
| username | varchar(64) unique not null | 学生=学号；教师/管理员=自定义登录名 |
| password_hash | varchar(100) not null | BCrypt 哈希，禁止明文 |
| real_name | varchar(64) | 显示名 |
| role | varchar(16) not null | ADMIN / TEACHER / STUDENT |
| student_id | bigint null | role=STUDENT 时关联 student.id，外键 |
| enabled | tinyint default 1 | 账号是否启用 |
| create_time / update_time | datetime | 审计字段 |

约束：`student_id` 唯一（一个学生最多一个账号）；role=STUDENT 时 student_id 必填。

**JWT 约定**：claims 含 `uid`、`username`、`role`；有效期 8 小时；密钥放
`application.properties`（`app.jwt.secret`），不入库不硬编码。

## API 契约

| 方法 | 路径 | 角色 | 说明 |
|---|---|---|---|
| POST | /api/auth/login | 公开 | body `{username, password}` → `{token, user:{id,username,realName,role,studentId}}`；失败 401 |
| GET | /api/auth/me | 登录 | 当前用户信息 |
| POST | /api/auth/logout | 登录 | 前端丢弃 token（JWT 无状态，服务端不处理） |
| GET | /api/users | ADMIN | 账号列表（分页/按角色筛选） |
| POST | /api/users | ADMIN | 创建账号；学生账号可传 `studentId` 自动以学号为用户名 |
| PUT | /api/users/{id} | ADMIN | 重置密码 / 启用停用 |
| DELETE | /api/users/{id} | ADMIN | 删除账号（不可删自己） |

错误响应统一结构（扩展现有 GlobalExceptionHandler）：
`{ "code": 401, "message": "用户名或密码错误" }`

## Code Style（沿用阿里规约与现有代码）

```java
// Controller 只做参数与编排，业务在 Service；入参用 DTO + @Valid
@PostMapping("/login")
public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
    return authService.login(req.username(), req.password())
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));
}
// 密码：BCrypt；禁止日志打印密码/token
```

```ts
// 前端：路由守卫按 meta.roles
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.token) return '/login'
  if (to.meta.roles && !to.meta.roles.includes(auth.role)) return '/403'
})
```

## Testing Strategy

- 后端单测（JUnit5，`src/test/java`）：
  - `AuthServiceTest`：正确/错误密码、停用账号、学生账号与 student 关联、重复用户名
  - `JwtUtilTest`：签发→解析 round-trip、过期/篡改 token 校验失败
  - `AuthInterceptorTest`（MockMvc）：无 token 401、角色不足 403、学生不能访问 /api/users
- 前端：`vue-tsc --noEmit` 零错误；手测登录/登出/越权访问跳转
- 覆盖率重点：AuthService 与拦截器授权分支

## Boundaries

- **Always**：密码 BCrypt 哈希；接口默认需登录，公开接口显式放行；每个受保护接口声明所需角色
- **Ask first**：引入 security 之外的新依赖；改动现有 student 表结构
- **Never**：明文存储/日志输出密码或 token；在前端存密码；学生账号越权访问教师/管理接口

## Success Criteria

- [ ] 管理员可创建教师账号与学生账号（学生账号可按学号批量/单个开通）
- [ ] 三类角色均可登录，前端按角色显示不同菜单
- [ ] 未登录访问受保护接口返回 401；角色不符返回 403
- [ ] 后端 AuthService/JWT/拦截器单测全部通过
- [ ] 密码不以任何明文形式出现在数据库或日志中

## Open Questions

1. 学生账号开通方式：管理员手动开通，还是学生首次用学号 + 预设初始密码登录？（建议：管理员开通，初始密码=学号，首次登录强制改密——强制改密是否本期做？）
2. 是否需要"忘记密码"流程？（建议本期不做，管理员重置即可）
