package xiaozhi.modules.device.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.exception.ErrorCode;
import xiaozhi.common.user.UserDetail;
import xiaozhi.common.utils.Result;
import xiaozhi.modules.device.entity.DeviceEntity;
import xiaozhi.modules.device.service.DeviceService;
import xiaozhi.modules.security.user.SecurityUser;
import xiaozhi.modules.sys.service.SysParamsService;

import java.util.Map;

@Tag(name = "设备MCP控制")
@RequiredArgsConstructor
@RestController
@RequestMapping("/device/mcp")
public class McpController {

    private final RestTemplate restTemplate;
    private final DeviceService deviceService;
    private final SysParamsService sysParamsService;

    @Value("${xiaozhi.server.url:http://127.0.0.1:8003}")
    private String defaultXiaozhiServerUrl;

    /**
     * 获取动态配置的 Server URL
     */
    private String getXiaozhiServerUrl() {
        // 1. 优先从数据库/缓存读取 (参数键名: server.xiaozhi_url)
        String url = sysParamsService.getValue(Constant.SERVER_XIAOZHI_URL, true);
        if (StringUtils.isNotBlank(url)) {
            return url;
        }
        // 2. 数据库没配，则使用配置文件或默认值
        return defaultXiaozhiServerUrl;
    }

    @GetMapping("/tools/{deviceId}")
    @Operation(summary = "获取设备在线支持的工具列表")
    @RequiresPermissions("sys:role:normal")
    public Result<Object> getTools(@PathVariable String deviceId) {
        Result<DeviceEntity> checkResult = checkDeviceOwnership(deviceId);
        if (checkResult.getCode() != 0) {
            return new Result<>().error(checkResult.getCode(), checkResult.getMsg());
        }

        String url = getXiaozhiServerUrl() + "/api/mcp/tools?device_id=" + deviceId;
        try {
            Object response = restTemplate.getForObject(url, Object.class);
            return new Result<>().ok(response);
        } catch (Exception e) {
            return new Result<>().error("设备不在线或连接失败: " + e.getMessage());
        }
    }

    @PostMapping("/call")
    @Operation(summary = "远程调用设备MCP工具")
    @RequiresPermissions("sys:role:normal")
    public Result<Object> callTool(@RequestBody Map<String, Object> params) {
        String deviceId = (String) params.get("device_id");
        if (deviceId == null) {
            return new Result<>().error(ErrorCode.NOT_NULL, "device_id");
        }

        Result<DeviceEntity> checkResult = checkDeviceOwnership(deviceId);
        if (checkResult.getCode() != 0) {
            return new Result<>().error(checkResult.getCode(), checkResult.getMsg());
        }

        String url = getXiaozhiServerUrl() + "/api/mcp/call";
        try {
            Object response = restTemplate.postForObject(url, params, Object.class);
            return new Result<>().ok(response);
        } catch (Exception e) {
            return new Result<>().error("指令发送失败: " + e.getMessage());
        }
    }

    @PostMapping("/chat")
    @Operation(summary = "远程对话")
    @RequiresPermissions("sys:role:normal")
    public Result<Object> chat(@RequestBody Map<String, Object> params) {
        String deviceId = (String) params.get("device_id");
        if (deviceId == null) {
            return new Result<>().error(ErrorCode.NOT_NULL, "device_id");
        }

        Result<DeviceEntity> checkResult = checkDeviceOwnership(deviceId);
        if (checkResult.getCode() != 0) {
            return new Result<>().error(checkResult.getCode(), checkResult.getMsg());
        }

        String url = getXiaozhiServerUrl() + "/api/mcp/chat";
        try {
            Object response = restTemplate.postForObject(url, params, Object.class);
            return new Result<>().ok(response);
        } catch (Exception e) {
            return new Result<>().error("对话发送失败: " + e.getMessage());
        }
    }

    private Result<DeviceEntity> checkDeviceOwnership(String deviceId) {
        DeviceEntity device = deviceService.selectById(deviceId);
        if (device == null) {
            device = deviceService.getDeviceByMacAddress(deviceId);
        }

        if (device == null) {
            return new Result<DeviceEntity>().error(ErrorCode.DEVICE_NOT_EXIST, "设备不存在");
        }

        UserDetail user = SecurityUser.getUser();
        if (!device.getUserId().equals(user.getId())) {
            return new Result<DeviceEntity>().error(ErrorCode.FORBIDDEN, "无权操作该设备");
        }

        return new Result<DeviceEntity>().ok(device);
    }
}
