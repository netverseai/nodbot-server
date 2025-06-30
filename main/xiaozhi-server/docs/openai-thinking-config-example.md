# OpenAI Thinking 配置示例

## 概述
在OpenAI类型的LLM配置中，可以通过添加`thinking`参数来控制是否使用深度思考能力。

## 配置示例

### 1. 禁用深度思考（默认）
```yaml
LLM:
  OpenAILLM:
    type: openai
    model_name: gpt-4
    api_key: your-api-key
    base_url: https://api.openai.com/v1
    thinking:
      type: "disabled"  # 不使用深度思考能力
```

### 2. 启用深度思考
```yaml
LLM:
  OpenAILLM:
    type: openai
    model_name: gpt-4
    api_key: your-api-key
    base_url: https://api.openai.com/v1
    thinking:
      type: "enabled"  # 使用深度思考能力
```

### 3. 自动判断是否使用深度思考
```yaml
LLM:
  OpenAILLM:
    type: openai
    model_name: gpt-4
    api_key: your-api-key
    base_url: https://api.openai.com/v1
    thinking:
      type: "auto"  # 模型自行判断是否使用深度思考能力
```

## 支持的thinking类型

- `disabled`: 不使用深度思考能力（默认）
- `enabled`: 使用深度思考能力
- `auto`: 模型自行判断是否使用深度思考能力

## 动态控制

除了在配置文件中设置，还可以在调用时动态控制：

```python
# 在response方法调用时传入thinking参数
response = llm_provider.response(
    session_id="session_123",
    dialogue=messages,
    thinking="enabled"  # 动态启用深度思考
)
```

## 注意事项

1. thinking功能需要模型支持，不是所有模型都支持此功能
2. 使用深度思考会增加响应时间
3. 建议在使用深度思考时设置较大的超时时间（推荐30分钟以上）
4. 如果配置了无效的thinking类型，系统会自动使用"disabled"并记录警告日志 