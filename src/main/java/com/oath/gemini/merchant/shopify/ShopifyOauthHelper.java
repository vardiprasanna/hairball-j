package com.oath.gemini.merchant.shopify;

import com.oath.gemini.merchant.AppConfiguration;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.configuration.Configuration;

public class ShopifyOauthHelper {
    public final static String SECRET_KEY;
    public final static String API_KEY;

    public static String generateHMac(String... pairs) throws NoSuchAlgorithmException, InvalidKeyException, DecoderException {

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

    private ShopifyOauthHelper() {
    }

    private final static SecretKeySpec keySpec;
    private final static Mac mac;

    static {
        try {
            Configuration config = AppConfiguration.getConfig();
            API_KEY = config.getString("shopify.api.key");
            SECRET_KEY = config.getString("shopify.secret.key");

            keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac = Mac.getInstance("HmacSHA256");
            mac.init(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
