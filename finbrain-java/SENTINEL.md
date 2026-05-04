# Sentinel 限流熔断配置指南

## 概述

本项目已集成阿里巴巴 Sentinel，实现企业级的限流、熔断降级功能。

## 功能特性

- ✅ **流量控制**：QPS限流、线程数限流
- ✅ **熔断降级**：慢调用比例、异常比例、异常数熔断
- ✅ **实时监控**：通过 Sentinel Dashboard 查看实时流量数据
- ✅ **动态规则**：支持动态修改限流规则

## 快速开始

### 1. 启动 Sentinel Dashboard

```bash
# 下载 Sentinel Dashboard
wget https://github.com/alibaba/Sentinel/releases/download/1.8.6/sentinel-dashboard-1.8.6.jar

# 启动 Dashboard
java -Dserver.port=8080 -Dcsp.sentinel.dashboard.server=localhost:8080 -Dproject.name=sentinel-dashboard -jar sentinel-dashboard-1.8.6.jar
```

访问：http://localhost:8080
默认账号：sentinel / sentinel

### 2. 配置应用连接

在 `application.yml` 中配置：

```yaml
sentinel:
  transport:
    dashboard: localhost:8080  # Sentinel Dashboard地址
    port: 8719                 # 与Dashboard通信的端口
  eager: true                  # 应用启动时立即初始化
```

### 3. 启动应用

```bash
mvn spring-boot:run
```

应用启动后，会在 Sentinel Dashboard 中自动注册。

## 限流规则配置

### 代码配置

在 `SentinelConfig.java` 中已预配置以下限流规则：

| 资源名 | QPS阈值 | 说明 |
|--------|---------|------|
| auth:login | 5 | 登录接口限流 |
| auth:register | 3 | 注册接口限流 |
| ai:chat | 20 | AI对话限流 |
| order:create | 10 | 订单创建限流 |
| product:search | 60 | 产品搜索限流 |
| product:detail | 100 | 产品详情限流 |
| data:import | 5 | 数据导入限流 |
| global | 1000 | 全局限流 |

### 动态配置

通过 Sentinel Dashboard 可以动态修改规则：

1. 访问 Dashboard：http://localhost:8080
2. 选择应用：finbrain-java
3. 点击"流控规则"
4. 点击"新增流控规则"

## 熔断降级配置

已配置以下熔断规则：

### AI对话熔断策略
- **类型**：慢调用比例
- **慢调用阈值**：5000ms
- **比例阈值**：50%
- **最小请求数**：10
- **熔断时长**：30秒

### 订单创建熔断策略
- **类型**：异常比例
- **异常比例阈值**：30%
- **最小请求数**：10
- **熔断时长**：30秒

## 使用示例

### 在Controller中使用

```java
@PostMapping("/login")
@SentinelResource(
    value = "auth:login",
    blockHandler = "loginBlockHandler",
    blockHandlerClass = SentinelBlockHandler.class
)
public Result<Map<String, Object>> login(@RequestBody LoginDTO dto) {
    return Result.success(authService.login(dto));
}
```

### 自定义BlockHandler

```java
public class SentinelBlockHandler {
    public static Result<Map<String, Object>> loginBlockHandler(BlockException e) {
        return Result.error(429, "登录请求过于频繁，请稍后再试");
    }
}
```

## 监控指标

### 实时监控

通过 Dashboard 可以查看：
- 实时QPS
- 响应时间
- 异常数
- 通过/拒绝请求数

### Prometheus集成

Sentinel 指标已集成到 Prometheus：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus
```

访问：http://localhost:8080/actuator/prometheus

## 最佳实践

### 1. 限流阈值设置

- **登录接口**：5 QPS/IP - 防止暴力破解
- **AI接口**：20 QPS/用户 - 控制成本
- **订单接口**：10 QPS/用户 - 防止刷单
- **查询接口**：60-100 QPS/用户 - 防止爬虫

### 2. 熔断策略选择

- **慢调用比例**：适用于响应时间敏感的接口
- **异常比例**：适用于稳定性要求高的接口
- **异常数**：适用于关键业务接口

### 3. 降级处理

- 提供友好的错误提示
- 记录限流日志
- 返回HTTP 429状态码

## 生产环境配置

### 环境变量

```bash
# Sentinel Dashboard地址
export SENTINEL_DASHBOARD=your-dashboard-host:8080

# Sentinel通信端口
export SENTINEL_PORT=8719
```

### Docker部署

```yaml
services:
  finbrain-java:
    environment:
      - SENTINEL_DASHBOARD=sentinel-dashboard:8080
      - SENTINEL_PORT=8719
```

## 故障排查

### 1. Dashboard连接失败

检查：
- Dashboard是否启动
- 网络是否通畅
- 端口是否正确

### 2. 限流不生效

检查：
- @SentinelResource注解是否正确
- blockHandler方法签名是否正确
- 资源名是否匹配

### 3. 规则不持久化

Sentinel默认规则存储在内存中，重启后失效。
解决方案：
- 使用Nacos配置中心
- 使用Apollo配置中心
- 实现自定义规则存储

## 参考资料

- [Sentinel官方文档](https://sentinelguard.io/zh-cn/)
- [Spring Cloud Alibaba Sentinel](https://spring-cloud-alibaba-group.github.io/)
- [Sentinel Dashboard](https://github.com/alibaba/Sentinel/tree/master/sentinel-dashboard)
