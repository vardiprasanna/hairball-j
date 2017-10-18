package com.oath.gemini.merchant.shopify;

import com.oath.gemini.merchant.ClosableHttpClient;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import lombok.Getter;
import lombok.Setter;

/**
 * @author tong on 10/1/2017
 */
@Singleton
@Getter
@Setter
public class ShopifyClientService {
    private String accessToken;
    private String shop;
    private String shopName;

    /**
     * @param shop e.g., dpa-bridge.myshopify.com
     * @param accessToken either an access token or an authorized code. The later is used only to fetch its access token
     */
    public ShopifyClientService(String shop, String accessToken) {
        this.shop = shop;
        this.accessToken = accessToken;

        int dot = shop.indexOf('.');
        shopName = dot >= 0 ? shop.substring(0, dot) : shop;
    }

    public Request headers(Request request) {
        request.header(HttpHeader.ACCEPT, MediaType.APPLICATION_JSON);
        request.header(HttpHeader.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        return request.header("X-Shopify-Access-Token", accessToken);
    }

    public <T> T delete(Class<T> responseType, Enum<?> path, Object... macros) throws Exception {
        try (ClosableHttpClient httpClient = new ClosableHttpClient()) {
            String shopifyPath = path.toString().replace("${shop}", shop);
            Request request = httpClient.newRequest(HttpMethod.DELETE, shopifyPath, null, null, macros);
            headers(request);
            return httpClient.send(responseType);
        }
    }

    public String get(Enum<?> path) throws Exception {
        return get(String.class, path);
    }

    public <T> T get(Class<T> responseType, Enum<?> path, Object... macros) throws Exception {
        try (ClosableHttpClient httpClient = new ClosableHttpClient()) {
            String shopifyPath = path.toString().replace("${shop}", shop);
            Request request = httpClient.newGET(shopifyPath, macros);
            headers(request);
            return httpClient.send(responseType);
        }
    }

    public <T> T post(Class<T> responseType, Object requestBody, Enum<?> path) throws Exception {
        try (ClosableHttpClient httpClient = new ClosableHttpClient()) {
            String shopifyPath = path.toString().replace("${shop}", shop);
            Request request = httpClient.newPOST(shopifyPath, requestBody);
            headers(request);
            return httpClient.send(responseType);
        }
    }
}
