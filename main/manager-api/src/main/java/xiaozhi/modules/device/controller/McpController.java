package xiaozhi.modules.device.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import xiaozhi.common.utils.Result;
import java.util.Map;

@Tag(name = "设备MCP控制")
@AllArgsConstructor
@RestController
@RequestMapping("/device/mcp")
public class McpController {

    private final RestTemplate restTemplate = new RestTemplate();
    
    // 生产环境建议从配置文件读取 xiaozhi-server 的地址
    private final String XIAOZHI_SERVER_URL = "http://127.0.0.1:8003";

    @GetMapping("/tools/{deviceId}")
    @Operation(summary = "获取设备在线支持的工具列表")
    public Result<Object> getTools(@PathVariable String deviceId) {
        String url = XIAOZHI_SERVER_URL + "/api/mcp/tools?device_id=" + deviceId;
        try {
            Object response = restTemplate.getForObject(url, Object.class);
            return new Result<>().ok(response);
        } catch (Exception e) {
            return new Result<>().error("Device offline or connection failed: " + e.getMessage());
        }
    }

    @PostMapping("/call")
    @Operation(summary = "远程调用设备MCP工具")
    public Result<Object> callTool(@RequestBody Map<String, Object> params) {
        String url = XIAOZHI_SERVER_URL + "/api/mcp/call";
        try {
            Object response = restTemplate.postForObject(url, params, Object.class);
            return new Result<>().ok(response);
        } catch (Exception e) {
            return new Result<>().error("Failed to send command: " + e.getMessage());
        }
    }

    @PostMapping("/chat")
    @Operation(summary = "远程对话")
    public Result<Object> chat(@RequestBody Map<String, Object> params) {
        String url = XIAOZHI_SERVER_URL + "/api/mcp/chat";
        try {
            Object response = restTemplate.postForObject(url, params, Object.class);
            return new Result<>().ok(response);
        } catch (Exception e) {
            return new Result<>().error("Failed to send chat: " + e.getMessage());
        }
    }
}
