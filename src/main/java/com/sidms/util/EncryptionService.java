package com.sidms.util;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * AES-256-GCM encryption service for protecting sensitive member data.
 *
 * <p>Implementation details:</p>
 * <ul>
 *   <li>Algorithm: AES/GCM/NoPadding (authenticated encryption)</li>
 *   <li>Key size: 256 bits (32 bytes), loaded from environment variable</li>
 *   <li>IV: 12 bytes, randomly generated per encryption and prepended to ciphertext</li>
 *   <li>Auth tag length: 128 bits</li>
 *   <li>Output format: Base64(IV + ciphertext + tag)</li>
 * </ul>
 */
@Service
public class EncryptionService {

    private static final Logger log = LoggerFactory.getLogger(EncryptionService.class);

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;   // 96 bits — NIST recommended
    private static final int GCM_TAG_LENGTH = 128;  // bits

    @Value("${sidms.encryption.key:}")
    private String base64Key;

    private SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    public void init() {
        if (base64Key == null || base64Key.isBlank()) {
            log.warn("SIDMS_AES_KEY is not set — generating a random ephemeral key. "
                    + "Data encrypted in this session will NOT be decryptable after restart!");
            byte[] randomKey = new byte[32];
            secureRandom.nextBytes(randomKey);
            this.secretKey = new SecretKeySpec(randomKey, ALGORITHM);
        } else {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            if (keyBytes.length != 32) {
                throw new IllegalArgumentException(
                        "SIDMS_AES_KEY must be exactly 32 bytes (256 bits) when Base64-decoded. "
                                + "Got " + keyBytes.length + " bytes.");
            }
            this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            log.info("AES-256-GCM encryption key loaded successfully.");
        }
    }

    /**
     * Encrypts the given plaintext using AES-256-GCM.
     *
     * @param plaintext the data to encrypt
     * @return Base64-encoded string containing IV + ciphertext + auth tag, or null if input is null
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Prepend IV to ciphertext: [IV (12 bytes)][ciphertext + tag]
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypts the given AES-256-GCM ciphertext.
     *
     * @param ciphertext Base64-encoded string containing IV + ciphertext + auth tag
     * @return the original plaintext, or null if input is null
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(ciphertext);

            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);

            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] plaintext = cipher.doFinal(encrypted);
            return new String(plaintext, java.nio.charset.StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
