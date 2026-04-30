# FinBrain 金融智能顾问

一个基于企业级前后端分离架构的金融智能顾问系统，集成AI智能对话功能。

## 项目架构

```
FinBrain/
├── finbrain-java/          # Java SpringBoot 后端
├── finbrain-vue/           # Vue3 前端
├── finbrain-ai/            # Python FastAPI AI服务
├── init.sql                # 数据库初始化脚本
├── docker-compose.yml      # Docker编排配置
└── .env.example            # 环境变量示例
```

## 技术栈

### 后端 (Java)
- SpringBoot 3.2.x + Spring Security 6 + JWT
- MyBatis-Plus 3.5.x + MySQL 8.0 + Redis 7
- Knife4j 接口文档

### 前端 (Vue3)
- Vue3 + Vite + Pinia + Vue Router
- Element Plus + ECharts

### AI服务 (Python)
- FastAPI + LangChain + LangGraph
- 阿里云百炼 (qwen-max)
- Milvus 向量数据库

## 快速开始

### 1. 环境准备

确保已安装:
- Docker & Docker Compose
- Node.js 18+ (开发环境)
- JDK 17+ (开发环境)
- Python 3.11+ (开发环境)

### 2. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env 文件，填入必要的配置
```

**重要**: 必须配置 `DASHSCOPE_API_KEY`，可从阿里云百炼平台获取。

### 3. 启动服务

```bash
# 构建并启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### 4. 访问应用

- 前端页面: http://localhost
- 后端API文档: http://localhost:8080/doc.html
- AI服务健康检查: http://localhost:8001/health

### 5. 默认账号

- 用户名: `testuser`
- 密码: `123456`

## 开发指南

### 后端开发

```bash
cd finbrain-java
mvn spring-boot:run
```

### 前端开发

```bash
cd finbrain-vue
npm install
npm run dev
```

### AI服务开发

```bash
cd finbrain-ai
pip install -r requirements.txt
python main.py
```

## 功能模块

1. **用户认证**: 登录/注册/JWT认证
2. **理财产品**: 产品列表/搜索/详情
3. **AI智能顾问**: 多轮对话/意图识别/工具调用
4. **资产管理**: 资产总览/收益统计
5. **订单管理**: 下单/撤单/订单查询
6. **风险测评**: 问卷测评/等级评估

## API接口

### 认证接口
- POST `/auth/login` - 登录
- POST `/auth/register` - 注册
- POST `/auth/logout` - 退出登录

### 产品接口
- GET `/product/list` - 产品列表
- POST `/product/search` - 产品搜索
- GET `/product/{id}` - 产品详情

### AI工具接口
- POST `/ai-tools/search-products` - 搜索产品
- POST `/ai-tools/calculate-income` - 计算收益
- GET `/ai-tools/user-asset/{userId}` - 用户资产
- POST `/ai-tools/create-order` - 创建订单
- GET `/ai-tools/orders/{userId}` - 订单列表
- POST `/ai-tools/risk-assessment` - 风险测评

## 部署说明

### 阿里云百炼配置

1. 访问 [阿里云百炼平台](https://bailian.console.aliyun.com/)
2. 开通服务并获取 API Key
3. 将 API Key 配置到 `.env` 文件的 `DASHSCOPE_API_KEY`

### 生产环境部署

1. 修改 `.env` 文件中的敏感配置
2. 配置 HTTPS 证书
3. 调整 JVM 参数和数据库连接池
4. 配置日志收集和监控

## 许可证

MIT License
