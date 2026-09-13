package xiaozhi.modules.device.util;

import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;

import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;

/**
 * OTA 固件下载签名工具。
 *
 * <p>用于「本地存储 / 管理台下载」通道，生成无状态、带有效期的签名 Token，
 * 替代原先「随机 UUID + Redis + 下载次数限制」，避免 Redis 成为热路径并提升可扩展性。</p>
 *
 * <p>当接入 R2 时，设备走 {@code OtaStorageService} 下发的稳定公开直链，不再依赖本类；
 * 本类仍为本地/管理台下载通道提供访问控制。</p>
 *
 * <p>Token 格式：{base64url(固件ID)}.{过期毫秒时间戳}.{hex(HMAC-SHA256)}</p>
 */
public class OtaSignUtil {

    private OtaSignUtil() {
    }

    private static final String SECRET_PREFIX = "ota:";

    /** 下载链接默认有效时长：7 天（毫秒）。 */
    public static final long DEFAULT_TTL_MILLIS = 7L * 24 * 60 * 60 * 1000;

    /** 生成签名 Token。 */
    public static String sign(String firmwareId, String secret, long ttlMillis) {
        String safeSecret = resolveSecret(secret);
        long expires = System.currentTimeMillis() + ttlMillis;
        String idPart = encodeId(firmwareId);
        String body = idPart + "." + expires;
        return body + "." + hmac(safeSecret, body);
    }

    /** 校验签名 Token，返回固件ID；无效或已过期返回 null。 */
    public static String verify(String token, String secret) {
        if (StringUtils.isBlank(token) || StringUtils.isBlank(secret)) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }
        String safeSecret = resolveSecret(secret);
        String idPart = parts[0];
        long expires;
        try {
            expires = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
        if (System.currentTimeMillis() >= expires) {
            return null;
        }
        String body = idPart + "." + expires;
        if (!constantTimeEquals(hmac(safeSecret, body), parts[2])) {
            return null;
        }
        return decodeId(idPart);
    }

    /** 是否为签名 Token 格式（区别于历史随机 UUID）。 */
    public static boolean isSignedToken(String token) {
        return StringUtils.isNotBlank(token) && token.split("\\.").length == 3;
    }

    private static String encodeId(String id) {
        return java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(id.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeId(String idPart) {
        try {
            return new String(java.util.Base64.getUrlDecoder().decode(idPart), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String hmac(String secret, String data) {
        HMac mac = new HMac(HmacAlgorithm.HmacSHA256, secret.getBytes(StandardCharsets.UTF_8));
        return mac.digestHex(data);
    }

    private static String resolveSecret(String secret) {
        if (StringUtils.isBlank(secret)) {
            return SECRET_PREFIX + "default_secret";
        }
        return SECRET_PREFIX + secret;
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}