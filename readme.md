# SiyuBao 项目

SiyuBao 是一个完整的桌面应用系统，包含用户端和管理端两个主要模块，提供聊天预览、线路管理、会员中心等功能。

## 项目结构

```
siyubao/
├── user-app/         # 用户端桌面应用
│   ├── frontend/     # Vue 3 前端代码
│   ├── src/          # Spring Boot 后端代码
│   └── README.md     # 用户端说明文档
├── admin-app/        # 管理端后台系统
│   ├── frontend/     # Vue 3 前端代码
│   ├── src/          # Spring Boot 后端代码
│   └── README.md     # 管理端说明文档
└── README.md         # 项目总说明文档
```

## 模块说明

### 1. user-app (用户端桌面应用)

**技术栈**：
- 后端: Spring Boot 2.7.18, Java 17+
- 前端: Vue 3, Vite, Tailwind CSS
- 桌面集成: JCEF (Java Chromium Embedded Framework)
- 数据库: SQLite

**核心功能**：
- **聊天预览**: 支持抖音、视频号、小红书三个平台的聊天界面预览
- **线路管理**: 支持新增、编辑、删除线路，上传和清除线路头像
- **会员中心**: 显示用户账号信息、会员套餐信息和到期时间
- **历史记录**: 查询和管理聊天历史记录
- **系统设置**: 应用配置和个人设置
- **使用教程**: 应用使用指南
- **联系客服**: 客服联系方式和反馈
- **软件声明**: 软件使用条款和声明

**运行方式**：
- 直接运行 Java 入口 `DesktopApp.java`
- 或使用打包后的可执行文件

### 2. admin-app (管理端后台系统)

**技术栈**：
- 后端: Spring Boot 2.7.18, Java 17+
- 前端: Vue 3, Vite, Tailwind CSS
- 数据库: SQLite

**核心功能**：
- **用户管理**: 用户账号管理和权限控制
- **会员管理**: 会员套餐设置和订阅管理
- **设备管理**: 设备注册和管理
- **订单管理**: 支付订单管理和查询
- **公告管理**: 系统公告发布和管理
- **客服管理**: 客服信息设置和反馈处理
- **二维码管理**: 支付二维码管理
- **统计分析**: 用户使用统计和数据分析

**运行方式**：
- 运行 `SiyubaoAdminApplication.java`
- 访问 `http://localhost:6943` 登录管理后台

## 安装说明

### 环境要求
- JDK 17 或更高版本
- Maven 3.6 或更高版本
- Node.js 16 或更高版本 (用于前端构建)

### 构建步骤

#### 步骤 1: 构建用户端前端
```bash
cd user-app/frontend
npm install
npm run build
```

#### 步骤 2: 构建管理端前端
```bash
cd ../../admin-app/frontend
npm install
npm run build
```

#### 步骤 3: 构建后端和打包
```bash
cd ../..
mvn clean package -pl user-app,admin-app
```

## 运行配置

### 配置文件
- **用户端**: `user-app/src/main/resources/application.yml`
- **管理端**: `admin-app/src/main/resources/application.yml`

### 主要配置项
- `admin.base-url`: 管理端 API 基础 URL (用户端配置)
- `server.port`: 应用服务端口
- `spring.datasource`: 数据库配置

## 开发指南

### 前端开发
```bash
# 用户端前端
cd user-app/frontend
npm run dev

# 管理端前端
cd admin-app/frontend
npm run dev
```

### 后端开发
使用 IDE 导入项目，直接运行对应的主类：
- **用户端**: `user-app/src/main/java/cn/laobayou/siyubao/DesktopApp.java`
- **管理端**: `admin-app/src/main/java/cn/laobayou/siyubao/SiyubaoAdminApplication.java`

## 常见问题

### 1. 应用启动失败
- 检查 JDK 版本是否符合要求
- 检查端口是否被占用
- 检查数据库文件权限

### 2. 聊天预览生成失败
- 检查网络连接
- 检查会员套餐是否有效
- 检查今日生成次数是否已用完

### 3. 管理端登录失败
- 检查用户名和密码是否正确
- 检查网络连接
- 检查用户账号是否被禁用

## 许可证

MIT License

## 联系方式

- 客服邮箱: support@siyubao.com
- 官方网站: https://siyubao.com

---

**注意**: 本项目仅供个人学习和娱乐使用，请勿用于商业用途。