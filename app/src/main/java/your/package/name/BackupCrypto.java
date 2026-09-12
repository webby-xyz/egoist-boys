package your.package.name;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class BackupCrypto {

    private static final int IV_SIZE = 12;
    private static final int TAG_SIZE = 128;

    public static byte[] encrypt(
            byte[] data,
            String password
    ) throws Exception {

        byte[] keyBytes = makeKey(password);

        byte[] iv = new byte[IV_SIZE];

        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        SecretKeySpec key =
                new SecretKeySpec(keyBytes, "AES");

        Cipher cipher =
                Cipher.getInstance("AES/GCM/NoPadding");

        GCMParameterSpec spec =
                new GCMParameterSpec(TAG_SIZE, iv);

        cipher.init(
                Cipher.ENCRYPT_MODE,
                key,
                spec
        );

        byte[] encrypted =
                cipher.doFinal(data);

        byte[] result =
                new byte[iv.length + encrypted.length];

        System.arraycopy(
                iv,
                0,
                result,
                0,
                iv.length
        );

        System.arraycopy(
                encrypted,
                0,
                result,
                iv.length,
                encrypted.length
        );

        return result;
    }

    public static byte[] decrypt(
            byte[] encryptedData,
            String password
    ) throws Exception {

        byte[] keyBytes = makeKey(password);

        byte[] iv =
                new byte[IV_SIZE];

        System.arraycopy(
                encryptedData,
                0,
                iv,
                0,
                IV_SIZE
        );

        int encryptedLength =
                encryptedData.length - IV_SIZE;

        byte[] encrypted =
                new byte[encryptedLength];

        System.arraycopy(
                encryptedData,
                IV_SIZE,
                encrypted,
                0,
                encryptedLength
        );

        SecretKeySpec key =
                new SecretKeySpec(
                        keyBytes,
                        "AES"
                );

        Cipher cipher =
                Cipher.getInstance(
                        "AES/GCM/NoPadding"
                );

        GCMParameterSpec spec =
                new GCMParameterSpec(
                        TAG_SIZE,
                        iv
                );

        cipher.init(
                Cipher.DECRYPT_MODE,
                key,
                spec
        );

        return cipher.doFinal(encrypted);
    }

    private static byte[] makeKey(
            String password
    ) throws Exception {

        java.security.MessageDigest digest =
                java.security.MessageDigest
                        .getInstance("SHA-256");

        return digest.digest(
                password.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }
}
