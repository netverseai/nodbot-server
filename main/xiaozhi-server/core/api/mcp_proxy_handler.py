import json
from aiohttp import web
from core.handle.mcpHandle import call_mcp_tool

TAG = __name__

class McpProxyHandler:
    def __init__(self, server):
        self.server = server # WebSocketServer 实例

    async def handle_call(self, request):
        """
        处理 MCP 调用请求
        POST /api/mcp/call
        Body: { "device_id": "...", "tool_name": "...", "arguments": {} }
        """
        try:
            data = await request.json()
            device_id = data.get("device_id")
            tool_name = data.get("tool_name")
            arguments = data.get("arguments", {})

            if not device_id or not tool_name:
                return web.json_response({"error": "Missing device_id or tool_name"}, status=400)

            # 查找在线设备
            handler = self.server.get_connection_by_device_id(device_id)
            if not handler:
                return web.json_response({"error": f"Device {device_id} not online"}, status=404)

            if not hasattr(handler, 'mcp_client') or handler.mcp_client is None:
                return web.json_response({"error": f"Device {device_id} MCP client not initialized"}, status=404)

            # 调用 MCP 工具
            # call_mcp_tool(conn, mcp_client, tool_name, args, timeout)
            result = await call_mcp_tool(handler, handler.mcp_client, tool_name, arguments)
            return web.json_response({"status": "success", "result": result})

        except Exception as e:
            return web.json_response({"error": str(e)}, status=500)

    async def handle_get_tools(self, request):
        """
        获取指定在线设备支持的工具列表
        GET /api/mcp/tools?device_id=...
        """
        device_id = request.query.get("device_id")
        if not device_id:
            return web.json_response({"error": "Missing device_id"}, status=400)

        handler = self.server.get_connection_by_device_id(device_id)
        if not handler or not hasattr(handler, 'mcp_client') or handler.mcp_client is None:
            return web.json_response({"error": "Device offline or MCP not ready"}, status=404)

        tools = handler.mcp_client.get_available_tools()
        return web.json_response({"tools": tools})

    async def handle_chat(self, request):
        """
        处理远程对话请求
        POST /api/mcp/chat
        Body: { "device_id": "...", "text": "..." }
        """
        try:
            data = await request.json()
            device_id = data.get("device_id")
            text = data.get("text")

            if not device_id or not text:
                return web.json_response({"error": "Missing device_id or text"}, status=400)

            handler = self.server.get_connection_by_device_id(device_id)
            if not handler:
                return web.json_response({"error": f"Device {device_id} not online"}, status=404)

            # 调用对话逻辑，在线程池中运行
            self.server.logger.bind(tag=TAG).info(f"Remote chat for {device_id}: {text}")
            handler.executor.submit(handler.chat, text)

            return web.json_response({"status": "success", "message": "Chat command initiated"})

        except Exception as e:
            return web.json_response({"error": str(e)}, status=500)
