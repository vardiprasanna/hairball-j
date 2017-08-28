package com.oath.gemini.merchant.shopify;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class AuthHelper {
    final static String SECRETE_KEY = "23a579690626c631eb4a49b4a4d7cb1f";
    final static String API_KEY = "62928398fafea63bad905e52e8410079";

    public static String generateHMac(String... pairs)
            throws NoSuchAlgorithmException, InvalidKeyException, DecoderException {

        // prepare message - the keys of pairs must be in lexicographical order
        StringBuilder msg = new StringBuilder();
        for (String p : pairs) {
            if (msg.length() > 0) {
                msg.append('&');
            }
            msg.append(p);
        }

        // produce hmac-sha256
        byte[] hmacBytes = mac.doFinal(msg.toString().getBytes());
        return Hex.encodeHexString(hmacBytes);
    }

    private AuthHelper() {
    }

    private final static SecretKeySpec keySpec = new SecretKeySpec(SECRETE_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    private final static Mac mac;

    static {
        try {
            mac = Mac.getInstance("HmacSHA256");
            mac.init(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
