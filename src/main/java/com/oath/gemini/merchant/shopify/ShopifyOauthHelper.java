package com.oath.gemini.merchant.shopify;

import com.oath.gemini.merchant.AppConfiguration;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.configuration.Configuration;
import lombok.Getter;
import lombok.Setter;

/**
 * @author tong on 10/1/2017
 */
public class ShopifyOauthHelper {
    private final static List<KeyBag> keyBags = new ArrayList<>();

    public static String getApiKey(int keyEntry) {
        return keyBags.get(keyEntry).getApiKey();
    }

    public static String getSecreteKey(int keyEntry) {
        return keyBags.get(keyEntry).getSecreteKey();
    }

    /**
     * Return the index of a matched key
     */
    public static int matchHMac(String hmac, String... pairs) {
        for (int i = 0; i < keyBags.size(); i++) {
            KeyBag m = keyBags.get(i);
            String target = Hex.encodeHexString(generateHMacBytes(m.getMac(), pairs));
            if (hmac.equals(target)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Return the index of a matched key
     */
    public static int matchHMac64(String hmac, String... pairs) {
        for (int i = 0; i < keyBags.size(); i++) {
            KeyBag m = keyBags.get(i);
            String target = Base64.getEncoder().encodeToString(generateHMacBytes(m.getMac(), pairs));
            if (hmac.equals(target)) {
                return i;
            }
        }
        return -1;
    }

    private static byte[] generateHMacBytes(Mac mac, String... pairs) {
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
        return hmacBytes;
    }

    private ShopifyOauthHelper() {
    }

    static {
        try {
            Configuration config = AppConfiguration.getConfig();
            String API_KEY = config.getString("shopify.api.key");
            String SECRET_KEY = config.getString("shopify.secret.key");

            String[] secreteKeys = { "8284e3af0c09dc218c9db3770c742882", "23a579690626c631eb4a49b4a4d7cb1f",
                    "f2280639664b0d205f9c1c731980538f" };
            String[] apiKeys = { "10361692342b6e6b3cc4ac6f18b8931f", "62928398fafea63bad905e52e8410079",
                    "92067f220643e535e3cbc65259f65683" };

            for (int i = 0; i < secreteKeys.length; i++) {
                keyBags.add(new KeyBag(secreteKeys[i], apiKeys[i]));
            }

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Getter
    @Setter
    private static class KeyBag {
        private String secreteKey;
        private String apiKey;
        private Mac mac;

        KeyBag(String secreteKey, String apiKey) throws NoSuchAlgorithmException, InvalidKeyException {
            SecretKeySpec keySpec = new SecretKeySpec(secreteKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

            this.secreteKey = secreteKey;
            this.apiKey = apiKey;
            this.mac = Mac.getInstance("HmacSHA256");
            this.mac.init(keySpec);
        }
    }
}
