# 远程 MCP 与对话控制集成指南

本文档介绍 Web App 如何通过 `manager-api` 远程控制连接到服务器的 ESP32 设备，包括获取设备支持的工具列表、执行 MCP 指令以及发起远程对话。

## 1. 系统架构

远程控制链路如下：
**Web App (前端)** -> **manager-api (Java, 8002)** -> **xiaozhi-server (Python, 8003)** -> **ESP32 (WebSocket)**

所有的控制指令必须携带 `deviceId`（设备唯一标识），且该设备当前必须在线（已连接到 WebSocket 服务器）。

---

## 2. 获取设备支持的工具

在发送指令前，通常需要先了解该设备支持哪些 MCP 工具。

### 接口信息
- **URL**: `GET /device/mcp/tools/{deviceId}`
- **说明**: 返回该设备当前在线支持的所有 MCP 工具及其参数定义。

### 请求示例
```http
GET http://your-api-domain:8002/device/mcp/tools/ESP32_01
```

### 返回示例
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "tools": [
      {
        "type": "function",
        "function": {
          "name": "get_volume",
          "description": "获取设备当前的音量百分比",
          "parameters": {
            "type": "object",
            "properties": {},
            "required": []
          }
        }
      },
      {
        "type": "function",
        "function": {
          "name": "set_volume",
          "description": "设置设备音量",
          "parameters": {
            "type": "object",
            "properties": {
              "volume": { "type": "integer", "description": "0-100的音量值" }
            },
            "required": ["volume"]
          }
        }
      }
    ]
  }
}
```

---

## 3. 调用 MCP 工具（以查询音量为例）

通过工具调用接口，你可以指挥设备执行特定动作或获取状态。

### 接口信息
- **URL**: `POST /device/mcp/call`
- **说明**: 发送 JSON-RPC 指令到设备并等待返回值。

### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| `device_id` | String | 是 | 设备唯一标识 |
| `tool_name` | String | 是 | 工具名称（从 tools 列表获取） |
| `arguments` | Object | 否 | 工具执行所需的参数字典 |

### 请求示例（查询音量）
```json
POST http://your-api-domain:8002/device/mcp/call
Content-Type: application/json

{
  "device_id": "ESP32_01",
  "tool_name": "get_volume",
  "arguments": {}
}
```

### 响应示例
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "status": "success",
    "result": "当前音量为 75%"
  }
}
```

---

## 4. 远程对话控制

除了直接调用工具，你还可以远程发送文本给设备，触发设备的 LLM 思考并让其通过语音回复。

### 接口信息
- **URL**: `POST /device/mcp/chat`
- **说明**: 模拟用户说话，设备会进行语义理解、执行相关工具并进行语音播报。

### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| `device_id` | String | 是 | 设备唯一标识 |
| `text` | String | 是 | 想要对设备说的话 |

### 请求示例
```json
POST http://your-api-domain:8002/device/mcp/chat
Content-Type: application/json

{
  "device_id": "ESP32_01",
  "text": "帮我把音量调到 50%"
}
```

---

## 5. 注意事项

1. **超时机制**: MCP 工具调用是同步等待的。如果设备响应较慢（如复杂的硬件操作），接口可能会在 30 秒后超时。
2. **设备在线状态**: 如果设备掉线，接口将返回 `404 Device not online` 错误。
3. **参数校验**: 请严格按照 `tools` 列表中定义的 `parameters` 结构发送 `arguments`，否则设备端可能会执行失败。
4. **返回值格式**: `result` 通常为字符串。如果是视觉模型或特殊工具返回的复杂数据，可能需要根据业务逻辑进一步解析。
