## 启动方案
- 使用 Maven Wrapper 直接启动开发服务，端口 `6942`（`src/main/resources/application.yml:3`）。
- 备选：打包生成 Jar 后用 `java -jar` 运行。

## 环境准备
- 安装 JDK 8（项目 `pom.xml:30` 指定 `java.version=1.8`）。检查：`java -version`。
- 使用自带 Maven Wrapper（根目录存在 `mvnw`）。无需额外安装 Maven。检查：`./mvnw -v`。
- 可选：如需启用文字识别 TextIn，通过环境变量配置 `APP_ID`、`APP_SECRET`（`src/main/resources/application.yml:73-74`）。

## 使用 Maven Wrapper 启动
- 在项目根目录 `/Users/meizhiwen/dev/siyubao` 执行：
  - `./mvnw spring-boot:run`
- 关键入口：`src/main/java/cn/laobayou/siyubao/SiyubaoApplication.java:6-11`（`@SpringBootApplication`）。
- 启动成功日志通常包含：`Tomcat started on port(s): 6942` 与 `Started SiyubaoApplication`。

## 打包并以 Jar 运行
- 生成可运行 Jar（跳过测试）：
  - `./mvnw clean package -DskipTests`
- 运行 Jar（可显式指定端口）：
  - `java -jar target/siyubao-0.0.1-SNAPSHOT.jar --server.port=6942`

## 访问与验证
- 根端口：`http://localhost:6942`。
- H2 控制台：`http://localhost:6942/h2-console`
  - JDBC URL：`jdbc:h2:file:./data/siyubao`（`application.yml:31`）
  - 用户名：`sa`（`application.yml:33`）
  - 密码：留空（`application.yml:34`）
- 页面验证：访问 `http://localhost:6942/chat-preview`（由 `ChatPreviewController` 提供，`src/main/java/cn/laobayou/siyubao/controller/ChatPreviewController.java:18-26` 渲染 `templates/chat-preview.html`）。

## 可选配置
- 日志使用 `logback-local.xml`（`application.yml:61`）。需要本地日志格式或级别变更可按需调整该文件。
- 开发热更新：已引入 `spring-boot-devtools`（`pom.xml:42-47`）。修改模板或代码后自动重启或热刷（IDE 需触发编译）。
- 深度模型接口（如未安装本地服务可暂不使用）：`application.yml:64-67` 指向 `http://127.0.0.1:11434`；未运行也不影响基础功能。

## 常见问题
- 端口占用：若 `6942` 被占用，运行时追加 `--server.port=7000` 或在 `application.yml` 修改端口。
- 权限问题：首次运行会在 `./data` 目录创建 H2 文件库，确保有写权限。
- 依赖下载缓慢：国内网络可配置 Maven 镜像（如阿里云）到本机 Maven `settings.xml`，Wrapper 会继承系统 Maven 设置。

如确认上述方案，我将直接在本机为你启动并验证访问。