package com.oath.gemini.merchant.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.configuration.Configuration;

/**
 * It signs a host alone with the query parameters
 * 
 * @author tong 10/1/2017
 */
@Singleton
public class SigningService {
    private final Mac mac;

    @Inject
    public SigningService(Configuration config) {
        mac = initMac(config.getString("y.oauth.secret.id"));
    }

    private Mac initMac(String secretKey) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keySpec);
            return mac;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Generate a hash string by using a built-in Mac
     */
    public String sign(String... pairs) throws NoSuchAlgorithmException, InvalidKeyException, DecoderException {
        return sign(mac, pairs);
    }

    /**
     * Generate a hash string by using a custom Mac
     */
    private String sign(Mac mac, String... pairs) throws NoSuchAlgorithmException, InvalidKeyException, DecoderException {
        // prepare message - the order of the input strings is sensitive when does comparison
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
}
