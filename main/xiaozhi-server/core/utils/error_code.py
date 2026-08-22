# -*- coding: utf-8 -*-
"""
国际化错误码模块。

与 manager-api 的错误码规范保持一致，统一使用
"ERROR[CODE] KEY_NAME" 的标准格式，避免在设备端语音
(TTS) 提示中出现硬编码中文。

错误码分区：
  11xxx - 设备模块（激活码/绑定）
  18xxx - 参数 / OTA 配置
""" 

# 设备模块 11xxx
BIND_CODE_FORMAT_INVALID = 11005
DEVICE_BIND_REQUIRED = 11006

# 参数 / OTA 配置 18xxx
DEVICE_VERSION_NOT_FOUND = 18012
OUTPUT_LIMIT_REACHED = 18013

# 错误码到国际化文案的映射
ERROR_MESSAGES = {
    BIND_CODE_FORMAT_INVALID: "ERROR[11005] BIND_CODE_FORMAT_INVALID",
    DEVICE_BIND_REQUIRED: "ERROR[11006] DEVICE_BIND_REQUIRED ({code})",
    DEVICE_VERSION_NOT_FOUND: "ERROR[18012] DEVICE_VERSION_NOT_FOUND",
    OUTPUT_LIMIT_REACHED: "ERROR[18013] OUTPUT_LIMIT_REACHED",
}


def get_error_message(code: int) -> str:
    """根据错误码返回 ERROR[CODE] KEY_NAME 格式的国际化文案。"""
    return ERROR_MESSAGES.get(code, f"ERROR[{code}] UNKNOWN")