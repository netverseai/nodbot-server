import time
import jwt
from config.logger import setup_logging
from collections import defaultdict
from threading import Lock

TAG = __name__
logger = setup_logging()

class AuthenticationError(Exception):
    """认证异常基类"""
    pass

class AuthMiddleware:
    """
    企业级认证中间件
    支持：
    1. JWT (HS256) 鉴权，验证 sub 与 device-id 匹配
    2. 设备白名单校验
    3. 基于 IP 的滑窗速率限制 (防止握手泛洪攻击)
    """
    def __init__(self, config):
        self.config = config
        self.auth_config = config.get("server", {}).get("auth", {})
        # 从配置中获取 secret，默认为固定值以确保 Docker 环境启动稳定性
        self.secret = config.get("server", {}).get("secret", "xiaozhi_default_secret_2025")

        # 速率限制相关 (滑窗计数器)
        self.rate_limit_lock = Lock()
        self.connection_history = defaultdict(list)
        # 企业级安全配置：每 10 秒允许最多 5 次连接尝试
        self.max_requests_per_window = 5
        self.window_seconds = 10

    async def authenticate(self, headers, client_ip=None):
        """
        验证连接请求并执行频率检查
        :param headers: WebSocket 请求头
        :param client_ip: 客户端 IP 地址
        """
        # 1. 频率限制检查 (防御性编程)
        if client_ip:
            self._check_rate_limit(client_ip)

        # 2. 检查是否启用认证
        if not self.auth_config.get("enabled", False):
            return True

        device_id = headers.get("device-id", "")
        auth_header = headers.get("authorization", "")

        # 3. 检查设备白名单 (高优先级)
        allowed_devices = self.auth_config.get("allowed_devices", [])
        if allowed_devices and device_id in allowed_devices:
            return True

        # 4. JWT 验证逻辑
        if not auth_header.startswith("Bearer "):
            raise AuthenticationError("Missing Authorization header (Bearer token required)")

        token = auth_header.split(" ")[1]
        try:
            # 企业级实践：严格验证 sub (Subject) 声明，确保 Token 是发给该设备的
            payload = jwt.decode(token, self.secret, algorithms=["HS256"])
            if payload.get("sub") != device_id:
                logger.bind(tag=TAG).warning(f"Token sub mismatch: {payload.get('sub')} != {device_id}")
                raise AuthenticationError("Token device mismatch")
        except jwt.ExpiredSignatureError:
            raise AuthenticationError("Token signature has expired")
        except jwt.InvalidTokenError as e:
            raise AuthenticationError(f"Invalid token: {str(e)}")

        return True

    def _check_rate_limit(self, ip):
        """
        防止握手洪水攻击 (Rate Limiting)
        使用线程安全的滑窗算法
        """
        with self.rate_limit_lock:
            now = time.time()
            attempts = self.connection_history[ip]
            # 清理超出时间窗口的旧记录
            attempts = [t for t in attempts if now - t < self.window_seconds]

            if len(attempts) >= self.max_requests_per_window:
                logger.bind(tag=TAG).warning(f"Rate limit exceeded for IP: {ip} ({len(attempts)} attempts in {self.window_seconds}s)")
                raise AuthenticationError("Too many connection attempts, please try later")

            attempts.append(now)
            self.connection_history[ip] = attempts
