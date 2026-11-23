package com.yunzhongyi.medical.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * 密码工具：PBKDF2WithHmacSHA256 哈希（JDK 内置，无需额外依赖）。
 * <p>
 * 存储格式：{@code PBKDF2$65536$<Base64 盐>$<Base64 哈希>}。
 * 历史种子密码为明文占位（如 admin / 123456）：{@link #matches} 仍能校验，
 * 登录成功后由 {@link com.yunzhongyi.medical.auth.AuthService} 自动重写为哈希，
 * 旧明文只在迁移前短暂存在。
 */
public final class PasswordUtil {

    private static final String PREFIX = "PBKDF2";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /** 是否已是 PBKDF2 哈希格式 */
    public static boolean isHashed(String stored) {
        return stored != null && stored.startsWith(PREFIX + "$");
    }

    /** 对明文密码做 PBKDF2 哈希 */
    public static String hash(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("密码不能为空");
        }
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(raw.toCharArray(), salt, ITERATIONS);
        return PREFIX + "$" + ITERATIONS + "$"
                + Base64.getEncoder().withoutPadding().encodeToString(salt) + "$"
                + Base64.getEncoder().withoutPadding().encodeToString(hash);
    }

    /**
     * 校验明文密码与存储值是否一致。
     * 存储值为 PBKDF2 哈希时按哈希校验；历史明文占位按明文比对（供登录迁移用）。
     */
    public static boolean matches(String raw, String stored) {
        if (raw == null || stored == null) {
            return false;
        }
        if (!isHashed(stored)) {
            // 历史种子明文占位：比对通过后由调用方重写为哈希
            return MessageDigest.isEqual(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    stored.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        try {
            String[] parts = stored.split("\\$");
            if (parts.length != 4) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(raw.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("密码哈希失败", e);
        }
    }
}
