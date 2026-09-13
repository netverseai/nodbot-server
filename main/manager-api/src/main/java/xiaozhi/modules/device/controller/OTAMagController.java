package xiaozhi.modules.device.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.servlet.http.HttpServletResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.exception.ErrorCode;
import xiaozhi.common.page.PageData;
import xiaozhi.common.redis.RedisKeys;
import xiaozhi.common.redis.RedisUtils;
import xiaozhi.common.utils.Result;
import xiaozhi.common.validator.ValidatorUtils;
import xiaozhi.modules.device.entity.OtaEntity;
import xiaozhi.modules.device.service.OtaService;
import xiaozhi.modules.device.util.OtaSignUtil;
import xiaozhi.modules.sys.service.SysParamsService;

@Tag(name = "设备管理", description = "OTA 相关接口")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/otaMag")
public class OTAMagController {
    private static final Logger logger = LoggerFactory.getLogger(OTAController.class);
    private final OtaService otaService;
    private final RedisUtils redisUtils;
    private final SysParamsService sysParamsService;

    @GetMapping
    @Operation(summary = "分页查询 OTA 固件信息")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", required = true)
    })
    @RequiresPermissions("sys:role:superAdmin")
    public Result<PageData<OtaEntity>> page(@Parameter(hidden = true) @RequestParam Map<String, Object> params) {
        ValidatorUtils.validateEntity(params);
        PageData<OtaEntity> page = otaService.page(params);
        return new Result<PageData<OtaEntity>>().ok(page);
    }

    @GetMapping("{id}")
    @Operation(summary = "信息 OTA 固件信息")
    @RequiresPermissions("sys:role:superAdmin")
    public Result<OtaEntity> get(@PathVariable("id") String id) {
        OtaEntity data = otaService.selectById(id);
        return new Result<OtaEntity>().ok(data);
    }

    @PostMapping
    @Operation(summary = "保存 OTA 固件信息")
    @RequiresPermissions("sys:role:superAdmin")
    public Result<Void> save(@RequestBody OtaEntity entity) {
        if (entity == null) {
            return new Result<Void>().error(ErrorCode.FIRMWARE_INFO_NOT_EMPTY);
        }
        if (StringUtils.isBlank(entity.getFirmwareName())) {
            return new Result<Void>().error(ErrorCode.FIRMWARE_NAME_NOT_EMPTY);
        }
        if (StringUtils.isBlank(entity.getType())) {
            return new Result<Void>().error(ErrorCode.FIRMWARE_TYPE_NOT_EMPTY);
        }
        if (StringUtils.isBlank(entity.getVersion())) {
            return new Result<Void>().error(ErrorCode.FIRMWARE_VERSION_NOT_EMPTY);
        }
        try {
            otaService.save(entity);
            return new Result<Void>();
        } catch (RuntimeException e) {
            return new Result<Void>().error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "OTA 删除")
    @RequiresPermissions("sys:role:superAdmin")
    public Result<Void> delete(@PathVariable("id") String[] ids) {
        if (ids == null || ids.length == 0) {
            return new Result<Void>().error(ErrorCode.FIRMWARE_ID_NOT_EMPTY);
        }
        otaService.delete(ids);
        return new Result<Void>();
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改 OTA 固件信息")
    @RequiresPermissions("sys:role:superAdmin")
    public Result<?> update(@PathVariable("id") String id, @RequestBody OtaEntity entity) {
        if (entity == null) {
            return new Result<>().error(ErrorCode.FIRMWARE_INFO_NOT_EMPTY);
        }
        entity.setId(id);
        try {
            otaService.update(entity);
            return new Result<>();
        } catch (RuntimeException e) {
            return new Result<>().error(e.getMessage());
        }
    }

    @PostMapping("/resyncR2/{id}")
    @Operation(summary = "重新同步固件到 R2")
    @RequiresPermissions("sys:role:superAdmin")
    public Result<Void> resyncR2(@PathVariable("id") String id) {
        if (otaService.resyncR2(id)) {
            return new Result<Void>().ok(null);
        }
        return new Result<Void>().error("R2 未配置或同步失败，请检查对象存储配置后重试");
    }

    @GetMapping("/getDownloadUrl/{id}")
    @Operation(summary = "获取 OTA 固件下载链接")
    @RequiresPermissions("sys:role:superAdmin")
    public Result<String> getDownloadUrl(@PathVariable("id") String id) {
        String secret = sysParamsService.getValue(Constant.SERVER_SECRET, true);
        String token = OtaSignUtil.sign(id, secret, OtaSignUtil.DEFAULT_TTL_MILLIS);
        return new Result<String>().ok(token);
    }

    @GetMapping("/download/{token}")
    @Operation(summary = "下载固件文件（本地存储/管理台通道）")
    public StreamingResponseBody downloadFirmware(@PathVariable("token") String token,
            @RequestHeader(value = "Range", required = false) String range,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
            HttpServletResponse response) throws IOException {

        // 1. 校验签名 token；兼容历史随机 uuid（Redis 映射）
        String firmwareId = OtaSignUtil.verify(token, getServerSecret());
        if (firmwareId == null && !OtaSignUtil.isSignedToken(token)) {
            firmwareId = (String) redisUtils.get(RedisKeys.getOtaIdKey(token));
        }
        if (StringUtils.isBlank(firmwareId)) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return null;
        }

        // 2. 查询固件
        OtaEntity otaEntity = otaService.selectById(firmwareId);
        if (otaEntity == null || StringUtils.isBlank(otaEntity.getFirmwarePath())) {
            logger.warn("Firmware not found or path is empty for ID: {}", firmwareId);
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return null;
        }

        // 3. 解析本地文件路径
        Path path = resolveFirmwarePath(otaEntity);
        if (path == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return null;
        }

        long fileSize = Files.size(path);
        String fileName = buildDownloadFilename(otaEntity);

        // 4. ETag / 缓存控制（允许 CDN 缓存该响应）
        String etag = "\"" + otaEntity.getVersion() + "-" + fileSize + "-"
                + path.toFile().lastModified() + "\"";
        response.setHeader(HttpHeaders.ETAG, etag);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=86400");
        response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
        response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                "Content-Range, Accept-Ranges, Content-Length, ETag");

        if (StringUtils.isNotBlank(ifNoneMatch) && etag.equals(ifNoneMatch)) {
            response.setStatus(HttpStatus.NOT_MODIFIED.value());
            return null;
        }

        // 5. 解析 Range
        long start = 0;
        long end = fileSize - 1;
        boolean partial = false;
        RangeInfo rangeInfo = parseRange(range, fileSize);
        if (rangeInfo != null) {
            start = rangeInfo.start;
            end = rangeInfo.end;
            partial = true;
        }
        if (start > end || start >= fileSize) {
            response.setStatus(416);
            response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize);
            response.setContentLength(0);
            return out -> {
            };
        }

        // 6. 组装响应头
        if (partial) {
            response.setStatus(HttpStatus.PARTIAL_CONTENT.value());
            response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize);
        } else {
            response.setStatus(HttpStatus.OK.value());
        }
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
        response.setContentLengthLong(end - start + 1);

        logger.info("Providing firmware stream, id: {}, range: {}-{}/{}", firmwareId, start, end, fileSize);

        // 7. 磁盘流式输出，不整包读入内存
        long from = start;
        long to = end;
        final String streamFirmwareId = firmwareId;
        return outputStream -> {
            try {
                copyRange(path, from, to, outputStream);
            } catch (IOException e) {
                logger.error("Streaming firmware output failed for ID: {}", streamFirmwareId, e);
            }
        };
    }

    /** 从文件中拷贝 [start, end] 区间字节到输出流。 */
    private void copyRange(Path path, long start, long end, java.io.OutputStream outputStream) throws IOException {
        long remaining = end - start + 1;
        try (InputStream inputStream = Files.newInputStream(path)) {
            if (start > 0) {
                inputStream.skipNBytes(start);
            }
            byte[] buffer = new byte[64 * 1024];
            int n;
            while (remaining > 0 && (n = inputStream.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                outputStream.write(buffer, 0, n);
                remaining -= n;
            }
        }
    }

    /** 解析 Range 头，仅支持单区间 bytes=start-end / bytes=start-；返回 null 表示不限制。 */
    private static class RangeInfo {
        long start;
        long end;
    }

    private RangeInfo parseRange(String range, long fileSize) {
        if (StringUtils.isBlank(range) || !range.startsWith("bytes=")) {
            return null;
        }
        Matcher matcher = Pattern.compile("bytes=(\\d*)-(\\d*)").matcher(range);
        if (!matcher.matches()) {
            return null;
        }
        RangeInfo info = new RangeInfo();
        try {
            String startStr = matcher.group(1);
            String endStr = matcher.group(2);
            boolean hasStart = StringUtils.isNotBlank(startStr);
            boolean hasEnd = StringUtils.isNotBlank(endStr);
            if (hasStart) {
                info.start = Long.parseLong(startStr);
            }
            if (hasEnd) {
                info.end = Long.parseLong(endStr);
            } else {
                info.end = fileSize - 1;
            }
            if (!hasStart && hasEnd) {
                info.start = Math.max(0, fileSize - info.end);
                info.end = fileSize - 1;
            }
            if (info.end > fileSize - 1) {
                info.end = fileSize - 1;
            }
        } catch (NumberFormatException e) {
            // 恶意/畸形 Range 头（如超长数字）直接忽略，按整文件下发
            return null;
        }
        return info;
    }

    /** 解析固件本地文件路径（主路径 + 兜底 firmware 目录）。 */
    private Path resolveFirmwarePath(OtaEntity otaEntity) {
        String firmwarePath = otaEntity.getFirmwarePath();
        Path path;
        if (Paths.get(firmwarePath).isAbsolute()) {
            path = Paths.get(firmwarePath);
        } else {
            path = Paths.get(System.getProperty("user.dir"), firmwarePath);
        }
        if (Files.exists(path) && Files.isRegularFile(path)) {
            return path;
        }
        String fileName = new File(firmwarePath).getName();
        Path altPath = Paths.get(System.getProperty("user.dir"), "firmware", fileName);
        if (Files.exists(altPath) && Files.isRegularFile(altPath)) {
            return altPath;
        }
        logger.error("Firmware file not found at either path: {} or {}",
                path.toAbsolutePath(), altPath.toAbsolutePath());
        return null;
    }

    /** 拼接安全的下载文件名。 */
    private String buildDownloadFilename(OtaEntity otaEntity) {
        String originalFilename = otaEntity.getType() + "_" + otaEntity.getVersion();
        String firmwarePath = otaEntity.getFirmwarePath();
        if (firmwarePath.contains(".")) {
            String extension = firmwarePath.substring(firmwarePath.lastIndexOf("."));
            originalFilename += extension;
        }
        return originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String getServerSecret() {
        return sysParamsService.getValue(Constant.SERVER_SECRET, true);
    }

    @PostMapping("/upload")
    @Operation(summary = "上传固件文件")
    @RequiresPermissions("sys:role:superAdmin")
    public Result<String> uploadFirmware(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return new Result<String>().error(ErrorCode.FIRMWARE_UPLOAD_FILE_NOT_EMPTY);
        }

        // 检查文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return new Result<String>().error(ErrorCode.FIRMWARE_UPLOAD_NAME_NOT_EMPTY);
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!extension.equals(".bin") && !extension.equals(".apk")) {
            return new Result<String>().error(ErrorCode.FIRMWARE_UPLOAD_TYPE_NOT_SUPPORTED);
        }

        try {
            // 计算文件的MD5值
            String md5 = calculateMD5(file);

            // 设置存储路径
            String uploadDir = "uploadfile";
            Path uploadPath = Paths.get(uploadDir);

            // 如果目录不存在，创建目录
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 使用MD5作为文件名，固定使用.bin扩展名
            String uniqueFileName = md5 + extension;
            Path filePath = uploadPath.resolve(uniqueFileName);

            // 检查文件是否已存在；不存在则保存到本地。
            // R2 同步在保存固件记录时统一进行（见 OtaServiceImpl），避免此处静默失败仍下发直链。
            if (!Files.exists(filePath)) {
                Files.copy(file.getInputStream(), filePath);
            }

            // 返回本地文件路径
            return new Result<String>().ok(filePath.toString());
        } catch (IOException | NoSuchAlgorithmException e) {
            return new Result<String>().error(ErrorCode.FIRMWARE_UPLOAD_FAILED);
        }
    }

    private String calculateMD5(MultipartFile file) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(file.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
