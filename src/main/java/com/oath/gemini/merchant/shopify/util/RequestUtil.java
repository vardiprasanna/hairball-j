package com.oath.gemini.merchant.shopify.util;

import com.oath.gemini.merchant.AppConfiguration;
import com.oath.gemini.merchant.ClosableHttpClient;
import java.net.MalformedURLException;
import java.util.Map;
import javax.ws.rs.core.UriBuilder;
import org.apache.commons.configuration.Configuration;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestUtil {
    public static String buildShopifyUrl(String shop, String pathName) throws MalformedURLException {
        Configuration config = AppConfiguration.getConfig();
        return config.getString(pathName).replace("{shop}", shop);
    }

    public static String buildShopifyUrl(String shop, String pathName, Map<String, String> params) throws MalformedURLException {
        String basePath = buildShopifyUrl(shop, pathName);

        if (params != null && params.size() > 0) {
            UriBuilder uriBuilder = UriBuilder.fromPath(basePath);
            for (Map.Entry<String, String> entry : params.entrySet()) {
                uriBuilder.queryParam(entry.getKey(), entry.getValue());
            }
            basePath = uriBuilder.toString();
        }
        return basePath;
    }

    public static String requestGet(String shop, String accessToken, String apiName) throws Exception {
        String apiUrl = buildShopifyUrl(shop, apiName);
        return invoke(accessToken, null, HttpMethod.PUT, apiUrl);
    }

    public static String requestPOST(String shop, String accessToken, String apiName, String bodyContent) throws Exception {
        String apiUrl = buildShopifyUrl(shop, apiName);
        return invoke(accessToken, bodyContent, HttpMethod.POST, apiUrl);
    }

    private static String invoke(String accessToken, Object requestBody, HttpMethod method, String path, Object... params)
            throws Exception {

        try (ClosableHttpClient httpClient = new ClosableHttpClient()) {
            Request request = httpClient.newRequest(method, path, requestBody);

            // Send a request
            request.header(HttpHeader.ACCEPT, "application/json");
            request.header(HttpHeader.CONTENT_TYPE, "application/json");
            request.header("X-Shopify-Access-Token", accessToken);
            ContentResponse response = request.send();
            String responseBody = null;

            // Process a response
            if (response.getStatus() >= 200 && response.getStatus() < 300) {
                responseBody = response.getContentAsString();
            } else {
                log.error("received an unexpected status code=" + response.getStatus());
            }
            return responseBody;
        }
    }
}
