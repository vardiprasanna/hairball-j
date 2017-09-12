package com.oath.gemini.merchant.shopify;

import com.oath.gemini.merchant.BaseHttpClientService;
import javax.inject.Singleton;
import org.eclipse.jetty.client.api.Request;
import lombok.Getter;
import lombok.Setter;

@Singleton
@Getter
@Setter
public class ShopifyClientService extends BaseHttpClientService {
    private String accessToken;
    private String shop;

    /**
     * @param shop
     *            e.g., dpa-bridge.myshopify.com
     * @param accessToken
     *            either an access token or an authorized code. The later is used only to fetch its access token
     */
    public ShopifyClientService(String shop, String accessToken) {
        this.shop = shop;
        this.accessToken = accessToken;
    }

    public Request headers(Request request) {
        return super.headers(request).header("X-Shopify-Access-Token", accessToken);
    }

    public String get(Enum<?> path, Object... params) throws Exception {
        return get(String.class, path, params);
    }

    public <T> T get(Class<T> responseType, Enum<?> path, Object... params) throws Exception {
        String shopPath = super.replacePositionedParams(path.toString(), this.shop);
        return super.get(responseType, shopPath, params);
    }

    public <T> T post(Class<T> responseType, Object requestBody, Enum<?> path, Object... params) throws Exception {
        String shopPath = super.replacePositionedParams(path.toString(), this.shop);
        return super.post(responseType, requestBody, shopPath, params);
    }
}
