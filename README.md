# 分布式任务调度系统

## 项目概述

这是一个分布式任务调度系统，由server和client两个模块组成，支持任务的调度、执行和监控。

### 系统特点

- 分布式架构设计，支持多客户端注册
- 基于Netty的高性能通信
- 支持任务分组和定时执行
- 实时客户端状态监控
- 提供Web控制台查看客户端信息

## 系统架构

### 整体架构

系统由三个核心模块组成：

1. **scheduler-server**：作为任务调度中心，负责接收客户端注册、管理任务调度、监控任务状态
2. **scheduler-client**：作为任务执行节点，注册到server并执行分配的任务
3. **scheduler-console**：Web控制台，基于Vue 3 + TypeScript + Vite开发，提供可视化界面查看客户端信息和任务状态
4. **scheduler-test**：测试模块，用于系统功能测试

### 核心组件

#### Server模块
- **ClientManager**：管理客户端注册、心跳和状态
- **TaskScheduler**：任务调度器，根据cron表达式触发任务
- **TaskTrigger**：向客户端发送任务触发请求
- **TaskStatusMonitor**：监控任务执行状态
- **ServerConsoleController**：提供Web控制台接口

#### Client模块
- **ClientScheduler**：客户端调度器，负责与服务端通信
- **ClientHandler**：处理服务端消息，执行任务
- **TaskRegistry**：注册和管理本地任务执行器
- **TaskExecutor**：任务执行器接口，用户需要实现该接口来定义具体任务

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+
- Spring Boot 2.7.x
- Node.js 16+
- npm 8+

### 构建和启动

#### 服务端

```bash
# 进入服务端目录
cd scheduler-server

# 构建
mvn clean package -DskipTests

# 启动
java -jar target/scheduler-server.jar

```

#### 客户端

```bash
# 进入客户端目录
cd scheduler-client

# 构建
mvn clean package -DskipTests

# 启动
java -jar target/scheduler-client.jar

```

#### Web控制台

```bash
# 进入控制台目录
cd scheduler-console

# 安装依赖
npm install

# 开发环境启动
npm run dev

# 构建生产版本
npm run build

# 生产环境预览
npm run preview
```

## 使用方法

### 实现自定义任务

1. 在客户端应用中实现`TaskExecutor`接口：

```java
public class MyTask implements TaskExecutor {
    @Override
    public void execute(String taskId, String params) {
        // 任务执行逻辑
        System.out.println("执行任务: " + taskId + " ，参数: " + params);
    }

    @Override
    public String getTaskName() {
        return "MyTask";
    }

    @Override
    public String getTaskGroup() {
        return "MyGroup";
    }

    @Override
    public String getCronExpression() {
        return "0/30 * * * * ?"; // 每30秒执行一次
    }
}
```

2. 注册任务执行器：

```java
@Autowired
private ClientScheduler clientScheduler;

// 注册任务
try {
    clientScheduler.registerTask(new MyTask());
    System.out.println("任务注册成功");
} catch (Exception e) {
    e.printStackTrace();
}
```

### 访问控制台

启动Web控制台后，可以通过以下地址访问（默认端口为5173，具体以启动日志为准）：

```
http://localhost:5173
```

控制台功能包括：
- 显示所有已注册的客户端信息（客户端ID、应用名称、主机名、IP地址等）
- 实时监控客户端状态
- 查看任务执行历史记录
- 任务状态可视化展示

## API文档

### 服务端API

#### 获取所有客户端信息

```
GET /api/clients
```

返回所有已注册的客户端信息。

#### 获取系统状态

```
GET /api/status
```

返回系统状态信息，包括客户端数量和详细的客户端列表。

### 通信协议

系统使用自定义的消息格式进行通信，主要消息类型包括：

- CLIENT_REGISTER：客户端注册
- CLIENT_HEARTBEAT：客户端心跳
- CLIENT_UNREGISTER：客户端注销
- TASK_TRIGGER：任务触发
- TASK_STATUS_REPORT：任务状态报告
- TASK_RESULT_REPORT：任务结果报告

## 注意事项

1. 确保服务端和客户端的通信端口配置一致（默认为8888）
2. 客户端需要正确实现TaskExecutor接口
3. 服务端需要在8488端口启动，用于Web控制台访问
4. 客户端心跳超时时间为60秒，超过时间未收到心跳，服务端会将客户端标记为离线

## 故障排查

1. 客户端连接失败：检查网络连接和服务端是否正常启动
2. 任务未执行：检查cron表达式是否正确，任务执行器是否正确注册
3. 客户端离线：检查客户端心跳是否正常发送，网络连接是否稳定
