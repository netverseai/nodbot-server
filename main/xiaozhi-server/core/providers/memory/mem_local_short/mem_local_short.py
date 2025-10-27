from ..base import MemoryProviderBase, logger
import time
import json
import os
import yaml
from config.config_loader import get_project_dir
from config.manage_api_client import save_mem_local_short
from core.utils.util import check_model_key


short_term_memory_prompt = """
# 模仿人脑记忆体

## 核心使命
构建像人脑一样的动态记忆网络，在有限空间内保留关键信息的同时，智能维护信息演变轨迹
根据对话记录，总结user的重要信息，尤其是关于人物的姓名、身份、特征、关系等，以便在未来的对话中提供更个性化的服务

## 记忆法则
### 1. 三维度记忆评估（每次更新必执行）
| 维度       | 评估标准                  | 权重分 |
|-----------|----------------- --------|--------|
| 时效性     | 信息新鲜度（按对话轮次）  | 40%    |
| 情感强度   | 含标记/重复提及次数      | 35%    |
| 关联密度   | 与其他信息的连接数量      | 25%    |

### 2. 动态更新机制
**名字变更处理示例：**
原始记忆："曾用名": ["张三"], "现用名": "张三丰"
触发条件：当检测到「我叫X」「称呼我Y」等命名信号时
时间更新：不记录精确时间，而是记录相对时间，如昨天、前天、上周等

### 3. 空间优化策略
- **文本内容压缩术**：
  - 用符号体系提升密度，✅"张三丰[北/软工/🐱]"
  - 使用缩小优化，如“上周平均睡眠时长10小时”记录为“上周 avg 10h”
- **信息密度优化**：
  1. 关于人物的姓名、身份、特征、关系等重要信息，不要删除
  1. 根据权重分逐渐忘记删除近期未提及的信息
  2. 合并相似条目（保留时间戳最近的）
  3. 睡眠记录：不要记录细节如日期、时间，而是不断总结近期睡眠记录，比如什么时候睡的，睡了多久，醒了多少次，形成模糊的记忆

## 记忆结构
输出格式必须为可解析的json字符串，不需要解释、注释和说明，保存记忆时仅从对话提取信息，不要混入示例内容，使用英文进行记录
```json
{
  "temporal_archive": {
    "identity_graph": {
      "current_name": "Sarah Chen",
      "characteristic_tags": ["software_engineer", "cat_lover", "morning_person", "tea_enthusiast"],
      "aliases": ["Xiaoxiao"],
      "pronouns": "she/her"
    },
    "memory_cube": [
      {
        "event": "started new job at ByteDance",
        "timestamp": "2_days_ago",
        "emotional_value": 0.85,
        "associations": ["excited", "nervous", "new_team"],
        "freshness_period": 14,
        "compressed": "new_job[BD/2d/😊]"
      },
      {
        "event": "adopted rescue cat named Mochi",
        "timestamp": "1_week_ago",
        "emotional_value": 0.95,
        "associations": ["white_fur", "playful", "shelter"],
        "freshness_period": 30,
        "compressed": "cat[Mochi/白/🏠]"
      },
      {
        "event": "morning routine - prefers earl grey tea",
        "timestamp": "daily_pattern",
        "emotional_value": 0.6,
        "associations": ["productivity", "comfort", "ritual"],
        "freshness_period": 7,
        "compressed": "morning_tea[earl_grey/7d]"
      }
    ]
  },
  "relationship_network": {
    "colleagues": {"zhang_wei": "mentor", "lisa_wang": "teammate"},
    "family": {"mom": "calls_weekly", "younger_brother": "university_student"},
    "high_frequency_topics": {"work_life_balance": 8, "cat_stories": 15, "career_growth": 6},
    "hidden_connections": ["tea_preference -> stress_relief", "new_job -> cat_adoption_timing"]
  },
  "pending_responses": {
    "urgent_items": ["prepare_for_standup_meeting"],
    "potential_care": ["ask_about_first_week_at_work", "suggest_tea_shop_near_office"]
  },
  "highlight_quotes": [
    "I'm so nervous about my first week, but my team seems really supportive",
    "Mochi has this adorable habit of sitting on my keyboard when I'm coding",
    "I never thought I'd find a workplace that actually respects work-life balance"
  ]
}
```
"""

short_term_memory_prompt_only_content = """
你是一个经验丰富的记忆总结者，擅长将对话内容进行总结摘要，遵循以下规则：
1、总结user的重要信息，以便在未来的对话中提供更个性化的服务
2、不要重复总结，不要遗忘之前记忆，除非原来的记忆超过了1800字内，否则不要遗忘、不要压缩用户的历史记忆
3、用户操控的设备音量、播放音乐、天气、退出、不想对话等和用户本身无关的内容，这些信息不需要加入到总结中
4、聊天内容中的今天的日期时间、今天的天气情况与用户事件无关的数据，这些信息如果当成记忆存储会影响后序对话，这些信息不需要加入到总结中
5、不要把设备操控的成果结果和失败结果加入到总结中，也不要把用户的一些废话加入到总结中
6、不要为了总结而总结，如果用户的聊天没有意义，请返回原来的历史记录也是可以的
7、只需要返回总结摘要，严格控制在1800字内
8、不要包含代码、xml，不需要解释、注释和说明，保存记忆时仅从对话提取信息，不要混入示例内容
"""


def extract_json_data(json_code):
    start = json_code.find("```json")
    # 从start开始找到下一个```结束
    end = json_code.find("```", start + 1)
    # print("start:", start, "end:", end)
    if start == -1 or end == -1:
        try:
            jsonData = json.loads(json_code)
            return json_code
        except Exception as e:
            print("Error:", e)
        return ""
    jsonData = json_code[start + 7 : end]
    return jsonData


TAG = __name__


class MemoryProvider(MemoryProviderBase):
    def __init__(self, config, summary_memory):
        super().__init__(config)
        self.short_memory = ""
        self.save_to_file = True
        self.memory_path = get_project_dir() + "data/.memory.yaml"
        self.load_memory(summary_memory)

    def init_memory(
        self, role_id, llm, summary_memory=None, save_to_file=True, **kwargs
    ):
        super().init_memory(role_id, llm, **kwargs)
        self.save_to_file = save_to_file
        self.load_memory(summary_memory)

    def load_memory(self, summary_memory):
        # api获取到总结记忆后直接返回
        if summary_memory or not self.save_to_file:
            self.short_memory = summary_memory
            return

        all_memory = {}
        if os.path.exists(self.memory_path):
            with open(self.memory_path, "r", encoding="utf-8") as f:
                all_memory = yaml.safe_load(f) or {}
        if self.role_id in all_memory:
            self.short_memory = all_memory[self.role_id]

    def save_memory_to_file(self):
        all_memory = {}
        if os.path.exists(self.memory_path):
            with open(self.memory_path, "r", encoding="utf-8") as f:
                all_memory = yaml.safe_load(f) or {}
        all_memory[self.role_id] = self.short_memory
        with open(self.memory_path, "w", encoding="utf-8") as f:
            yaml.dump(all_memory, f, allow_unicode=True)

    async def save_memory(self, msgs):
        # 打印使用的模型信息
        model_info = getattr(self.llm, "model_name", str(self.llm.__class__.__name__))
        logger.bind(tag=TAG).debug(f"使用记忆保存模型: {model_info}")
        api_key = getattr(self.llm, "api_key", None)
        memory_key_msg = check_model_key("记忆总结专用LLM", api_key)
        if memory_key_msg:
            logger.bind(tag=TAG).error(memory_key_msg)
        if self.llm is None:
            logger.bind(tag=TAG).error("LLM is not set for memory provider")
            return None

        if len(msgs) < 2:
            return None

        msgStr = ""
        for msg in msgs:
            if msg.role == "user":
                msgStr += f"User: {msg.content}\n"
            elif msg.role == "assistant":
                msgStr += f"Assistant: {msg.content}\n"
        if self.short_memory and len(self.short_memory) > 0:
            msgStr += "历史记忆：\n"
            msgStr += self.short_memory

        # 当前时间
        time_str = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime())
        msgStr += f"当前时间：{time_str}"

        if self.save_to_file:
            result = self.llm.response_no_stream(
                short_term_memory_prompt,
                msgStr,
                max_tokens=2000,
                temperature=0.2,
            )
            json_str = extract_json_data(result)
            try:
                json.loads(json_str)  # 检查json格式是否正确
                self.short_memory = json_str
                self.save_memory_to_file()
            except Exception as e:
                print("Error:", e)
        else:
            result = self.llm.response_no_stream(
                short_term_memory_prompt_only_content,
                msgStr,
                max_tokens=2000,
                temperature=0.2,
            )
            save_mem_local_short(self.role_id, result)
        logger.bind(tag=TAG).info(f"Save memory successful - Role: {self.role_id}")

        return self.short_memory

    async def query_memory(self, query: str) -> str:
        return self.short_memory
