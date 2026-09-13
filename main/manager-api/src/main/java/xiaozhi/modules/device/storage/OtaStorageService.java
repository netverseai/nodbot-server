package xiaozhi.modules.device.storage;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xiaozhi.modules.sys.service.SysParamsService;

/**
 * OTA 固件对象存储服务（S3 兼容，适配 Cloudflare R2）。
 *
 * <p>通过系统参数热切换存储方式：</p>
 * <ul>
 *   <li>server.ota_storage = local：仅本地磁盘（默认，兼容现有部署）</li>
 *   <li>server.ota_storage = s3：本地 + R2 双写，下载走 R2 公开直链，借助 Cloudflare 边缘缓存加速</li>
 * </ul>
 *
 * <p>固件使用 MD5 内容寻址（object key = firmware/{md5}{ext}），天然不可变、可被 CDN 永久缓存，
 * 且公开 URL 不可枚举，兼顾安全。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtaStorageService {

    /** 触发切换为对象存储的参数值 */
    private static final String STORAGE_S3 = "s3";

    private final SysParamsService sysParamsService;

    private volatile MinioClient minioClient;

    private String getStr(String code) {
        String v = sysParamsService.getValue(code, true);
        if (StringUtils.isBlank(v) || "null".equalsIgnoreCase(v)) {
            return null;
        }
        return v;
    }

    /** 当前是否启用对象存储（S3/R2）。 */
    public boolean isEnabled() {
        return STORAGE_S3.equalsIgnoreCase(getStr(StorageParams.STORAGE));
    }

    /** 是否满足上传/下载所需的关键配置。 */
    public boolean isConfigured() {
        return isEnabled() && getStr(StorageParams.ENDPOINT) != null
                && getStr(StorageParams.ACCESS_KEY) != null
                && getStr(StorageParams.SECRET_KEY) != null
                && getStr(StorageParams.BUCKET) != null;
    }

    /** 获取 R2 公开访问域名（不含结尾斜杠）。 */
    public String getPublicDomain() {
        String domain = getStr(StorageParams.PUBLIC_DOMAIN);
        if (domain == null) {
            return null;
        }
        return domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
    }

    /**
     * 由本地固件文件名推导对象存储 key：firmware/{文件名}。
     * 文件名即 {md5}{ext}，内容寻址。
     */
    public String buildObjectKey(String localFileName) {
        return "firmware/" + new java.io.File(localFileName).getName();
    }

    /**
     * 组装固件对象对应的公开直链（按已存的对象 key，仅当数值由上传成功产生时才有意义）。
     *
     * @return 公开直链；未配置公开域名、未启用或 key 为空时返回 null
     */
    public String getPublicUrl(String objectKey) {
        String domain = getPublicDomain();
        if (!isConfigured() || domain == null || StringUtils.isBlank(objectKey)) {
            return null;
        }
        return domain + "/" + objectKey;
    }

    /**
     * 解析固件本地文件绝对路径。
     */
    public Path resolveLocalFile(String firmwarePath) {
        if (StringUtils.isBlank(firmwarePath)) {
            return null;
        }
        Path path;
        Path p = Path.of(firmwarePath);
        if (p.isAbsolute()) {
            path = p;
        } else {
            path = Path.of(System.getProperty("user.dir"), firmwarePath);
        }
        if (Files.exists(path) && Files.isRegularFile(path)) {
            return path;
        }
        return null;
    }

    /**
     * 将本地固件上传到对象存储（R2）。
     *
     * <p>仅在确需 R2 时调用（isConfigured()）。失败返回 null 而不抛异常，
     * 调用方据此决定是否写入 r2ObjectKey，避免「静默失败仍下发直链导致设备 404」。</p>
     *
     * @param firmwarePath 固件本地路径
     * @return 成功返回对象 key，失败返回 null
     */
    public String uploadLocalFile(String firmwarePath) {
        if (!isConfigured()) {
            return null;
        }
        Path localFile = resolveLocalFile(firmwarePath);
        if (localFile == null) {
            log.warn("Firmware local file not found, skip R2 upload: {}", firmwarePath);
            return null;
        }
        String localFileName = localFile.getFileName().toString();
        String objectKey = buildObjectKey(localFileName);
        try (InputStream inputStream = Files.newInputStream(localFile)) {
            MinioClient client = getClient();
            client.putObject(PutObjectArgs.builder()
                    .bucket(getStr(StorageParams.BUCKET))
                    .object(objectKey)
                    .stream(inputStream, Files.size(localFile), -1)
                    .contentType("application/octet-stream")
                    .build());
            log.info("Firmware uploaded to object storage, key: {}, size: {} bytes",
                    objectKey, Files.size(localFile));
            return objectKey;
        } catch (Exception e) {
            log.warn("Firmware upload to object storage failed, key: {}, reason: {}", objectKey, rootMessage(e));
            return null;
        }
    }

    /**
     * 删除对象存储中的固件（失败静默，不影响本地删除）。
     */
    public void removeObject(String objectKey) {
        if (!isConfigured() || StringUtils.isBlank(objectKey)) {
            return;
        }
        try {
            getClient().removeObject(io.minio.RemoveObjectArgs.builder()
                    .bucket(getStr(StorageParams.BUCKET))
                    .object(objectKey)
                    .build());
            log.info("Firmware removed from object storage, key: {}", objectKey);
        } catch (Exception e) {
            log.warn("Firmware remove from object storage failed, key: {}, reason: {}", objectKey, rootMessage(e));
        }
    }

    private MinioClient getClient() {
        MinioClient local = minioClient;
        if (local == null) {
            synchronized (this) {
                local = minioClient;
                if (local == null) {
                    local = buildClient();
                    minioClient = local;
                }
            }
        }
        return local;
    }

    private MinioClient buildClient() {
        String endpoint = getStr(StorageParams.ENDPOINT);
        String accessKey = getStr(StorageParams.ACCESS_KEY);
        String secretKey = getStr(StorageParams.SECRET_KEY);
        String region = getStr(StorageParams.REGION);
        if (StringUtils.isBlank(region)) {
            region = "auto";
        }
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .region(region)
                .build();
    }

    private static String rootMessage(Throwable e) {
        Throwable cur = e;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur.getMessage();
    }

    static final class StorageParams {
        static final String STORAGE = "server.ota_storage";
        static final String ENDPOINT = "server.ota_s3_endpoint";
        static final String ACCESS_KEY = "server.ota_s3_access_key_id";
        static final String SECRET_KEY = "server.ota_s3_access_key_secret";
        static final String BUCKET = "server.ota_s3_bucket";
        static final String PUBLIC_DOMAIN = "server.ota_s3_public_domain";
        static final String REGION = "server.ota_s3_region";
    }
}