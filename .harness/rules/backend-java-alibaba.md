# 后端规则 — 阿里巴巴 Java 开发手册（强制/推荐条目精选）

> 适用范围：`backend/` Spring Boot 3 + JPA 工程。标注【强制】的为评审阻断项。

## 1. 命名规约
- 【强制】类名 UpperCamelCase；方法/变量 lowerCamelCase；常量全大写+下划线（`MAX_STOCK_COUNT`）。
- 【强制】抽象类以 `Abstract`/`Base` 开头；异常类以 `Exception` 结尾；测试类以被测类名+`Test`。
- 【强制】DO/实体与数据库表一一对应；对外传输用 DTO/VO，禁止 Controller 直接暴露实体的写接口语义混乱。
- 【强制】包名全小写、单数；类名不允许下划线与美元符号以外的特殊字符。
- 【推荐】布尔变量/POJO 布尔字段不加 `is` 前缀（避免序列化取不到值）。

## 2. 常量与魔法值
- 【强制】代码中禁止出现未定义常量的魔法值（字符串/数字直接量），状态码、类型值必须枚举或常量。
- 【强制】`long` 赋值使用大写 `L`：`Long id = 2L;`。
- 【推荐】常量按层次归位：跨应用共享放常量库，应用内共享放 `constant/` 包。

## 3. OOP
- 【强制】`equals` 方法常量或确定有值的对象在前：`"yes".equals(str)`。
- 【强制】所有 POJO 类（实体/DTO/VO）必须有 `toString`（Lombok `@Data`/`@ToString` 即可）。
- 【强制】包装类之间比较用 `equals`；注意 `Integer` 缓存 -128~127 的 == 陷阱。
- 【强制】构造方法内禁止写业务逻辑，初始化逻辑放 init 方法。
- 【推荐】POJO 类属性用包装类型（Integer/Long），局部变量用基本类型。

## 4. 集合
- 【强制】`Collections.singletonList()` / `Arrays.asList()` 返回值不可 `add/remove`。
- 【强制】`subList` 不能强转 `ArrayList`；遍历删除用 `Iterator` 或 `removeIf`。
- 【强制】`HashMap` 判空用 `isEmpty()`，不用 `size()==0`。
- 【推荐】集合初始化指定容量：`new HashMap<>(expectedSize / 0.75F + 1)`。

## 5. 并发
- 【强制】线程池禁止用 `Executors.newFixedThreadPool/newCachedThreadPool` 创建（OOM 风险），
  必须 `new ThreadPoolExecutor(...)` 显式指定核心/最大线程数与队列容量。
- 【强制】`SimpleDateFormat` 非线程安全，用 `DateTimeFormatter` 或 ThreadLocal。
- 【强制】线程必须通过线程池提交，禁止显式 `new Thread().start()`。

## 6. 控制语句
- 【强制】`if/else/for/while/do` 必须写大括号，即使单行。
- 【强制】三目运算符注意自动拆箱 NPE：`Boolean a = null; boolean b = a != null && a;`。
- 【推荐】方法圈复杂度 ≤10，嵌套层级 ≤3 层；卫语句提前返回。

## 7. 异常与日志
- 【强制】禁止空 catch 块吞异常；catch 后必须记录或抛出，日志带堆栈与上下文。
- 【强制】`finally` 块中禁止 `return`；禁止在 finally 中抛异常覆盖原异常。
- 【强制】不用 `System.out.println`；日志用 SLF4J 占位符：`log.info("save student id={}", id)`。
- 【强制】参数校验用 `@Valid` + `jakarta.validation` 注解；业务异常用统一异常处理器
  （`GlobalExceptionHandler`）转换为标准错误响应。
- 【推荐】对外部调用（HTTP/DB）的异常要分类处理并可重试/降级。

## 8. MySQL 与 ORM（JPA）
- 【强制】表名、字段名全小写+下划线：`student_number`、`create_time`。
- 【强制】字段禁止 `is_` 前缀；主键 `bigint`；表必备 `create_time`、`update_time`。
- 【强制】禁止 `SELECT *`（JPA 避免无关字段全量抓取）；列表查询必须分页。
- 【强制】禁止在 Repository 层拼接业务逻辑；复杂查询用 `@Query` 并参数绑定，杜绝字符串拼接 SQL。
- 【推荐】索引命名：主键 `pk_`、唯一 `uk_`、普通 `idx_`；varchar 代替 char。
- 【推荐】避免 N+1：关联查询用 fetch join / 实体图。

## 9. 工程分层
- 【强制】分层：`controller`（参数/鉴权/编排）→ `service`（业务）→ `repository`（持久化），
  禁止跨层调用（Controller 直接注入 Repository）。
- 【强制】Controller 方法返回统一结构；入参用 DTO + 校验注解，不用实体直接接收。
- 【推荐】配置项走 `application.properties`/`@ConfigurationProperties`，环境差异用 profile。
