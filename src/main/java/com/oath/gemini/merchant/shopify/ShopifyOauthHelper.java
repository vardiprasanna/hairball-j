package com.oath.gemini.merchant.shopify;

import com.oath.gemini.merchant.AppConfiguration;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import lombok.Getter;
import lombok.Setter;

/**
 * @author tong on 10/1/2017
 */
public class ShopifyOauthHelper {
    private final static List<KeyBag> keyBags = new ArrayList<>();
    private final static String[][] encoder = { { "&", "%26" }, { "%", "%25" }, { "=", "%3D" } };

    public static String getApiKey(int keyEntry) {
        return keyBags.get(keyEntry).getApiKey();
    }

    public static String getSecreteKey(int keyEntry) {
        return keyBags.get(keyEntry).getSecreteKey();
    }

    /**
     * Return the index of a matched key
     */
    public static int matchHMac(String hmac, Map<String, List<String>> pairs) {
        return matchHMac(hmac, sortPairs(pairs));
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

    /**
     * According to Shopify's spec before to compute the hash, must sort query params in ascending order except 'hmac'.
     * 
     * <pre>
     * The characters & and % are replaced with %26 and %25 respectively in keys and values. Additionally the = character is
     * replaced with %3D in keys.
     * </pre>
     * 
     * @see the secion 'HMAC Validation' here: https://help.shopify.com/api/getting-started/authentication/oauth
     */
    private static String[] sortPairs(Map<String, List<String>> map) {
        List<String> sortedKeys = new ArrayList<>();
        map.keySet().forEach(key -> {
            if (!"hmac".equalsIgnoreCase(key)) {
                sortedKeys.add(key);
            }
        });

        // Will flatten a list of values of a key
        Collections.sort(sortedKeys);
        List<String> builder = new ArrayList<>();

        for (String key : sortedKeys) {
            List<String> values = map.get(key);
            String encodedKey = encodeShopifyParam(key);

            if (CollectionUtils.isEmpty(values)) {
                builder.add(encodedKey + "=");
            } else {
                values.forEach(v -> {
                    builder.add(encodedKey + "=" + encodeShopifyParam(v));
                });
            }
        }

        return builder.toArray(new String[0]);
    }

    /**
     * The characters & and % are replaced with %26 and %25 respectively in keys and values. Additionally the = character is
     * replaced with %3D in keys.
     * 
     * @see the secion 'HMAC Validation' here: https://help.shopify.com/api/getting-started/authentication/oauth
     */
    private static String encodeShopifyParam(String source) {
        for (int i = 0; i < encoder.length; i++) {
            source = source.replaceAll(encoder[i][0], encoder[i][1]);
        }
        return source;
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
            String secreteKeyList = config.getString("shopify.secret.key");
            String apiKeyList = config.getString("shopify.api.key");
            String appNameList = config.getString("shopify.app.name");

            String[] secreteKeys = StringUtils.split(secreteKeyList, ",");
            String[] apiKeys = StringUtils.split(apiKeyList, ",");
            String[] appNames = StringUtils.split(appNameList, ",");

            if (appNames.length != apiKeys.length || apiKeys.length != secreteKeys.length) {
                throw new RuntimeException("Unmatched number of shopify secrete keys, api keys, and app names");
            }
            for (int i = 0; i < secreteKeys.length; i++) {
                keyBags.add(new KeyBag(secreteKeys[i], apiKeys[i], appNames[i]));
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
        private String appName;
        private Mac mac;

        KeyBag(String secreteKey, String apiKey, String name) throws NoSuchAlgorithmException, InvalidKeyException {
            SecretKeySpec keySpec = new SecretKeySpec(secreteKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

            this.secreteKey = secreteKey;
            this.apiKey = apiKey;
            this.appName = name;
            this.mac = Mac.getInstance("HmacSHA256");
            this.mac.init(keySpec);
        }
    }
}
