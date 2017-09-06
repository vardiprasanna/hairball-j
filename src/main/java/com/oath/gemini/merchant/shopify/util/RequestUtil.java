package com.oath.gemini.merchant.shopify.util;

import java.net.MalformedURLException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.UriBuilder;
import org.apache.commons.configuration.Configuration;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import com.oath.gemini.merchant.AppConfiguration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestUtil {
    private static int DEFAULT_THREADPOOL_TIMEOUT = 10;

    public static String buildShopifyUrl(String shop, String pathName) throws MalformedURLException {
        Configuration config = AppConfiguration.getConfig();
        return config.getString(pathName).replace("{shop}", shop);
    }

    public static String buildShopifyUrl(String shop, String pathName, Map<String, String> params)
            throws MalformedURLException {
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

    public static Request prepareRequestHeader(Request request) {
        // if (!yapeeYcaDisable) {
        // try {
        // String ycaString = YCAUtil.getCert(yapeeYcaAppId);
        // request.header("Yahoo-App-Auth", ycaString);
        // } catch (Throwable e) {
        // log.warn("Unable to fetch a yca certifificat for appid: " + yapeeYcaAppId, e);
        // }
        // }
        return request.header(HttpHeader.CONTENT_TYPE, "application/json");
    }

    public static Request prepareRequestHeader(Request request, String accessToken) {
        return prepareRequestHeader(request).header("X-Shopify-Access-Token", accessToken);
    }

    public static String requestGet(String shop, String accessToken, String apiName) throws Exception {
        HttpClient httpClient = null;

        try {
            httpClient = new HttpClient(new SslContextFactory());
            httpClient.start();

            // Issue a GET request
            String apiUrl = buildShopifyUrl(shop, apiName);
            Request request = httpClient.newRequest(apiUrl);
            request = RequestUtil.prepareRequestHeader(request, accessToken);
            request = request.timeout(DEFAULT_THREADPOOL_TIMEOUT, TimeUnit.SECONDS);

            ContentResponse response = request.send();
            String responseBody = null;

            // Process a response
            if (response.getStatus() == 200) {
                responseBody = response.getContentAsString();
            } else {
                log.error("received an unexpected status code=" + response.getStatus());
            }
            return responseBody;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (httpClient != null) {
                httpClient.stop();
            }
        }
    }

    public static String requestPOST(String shop, String accessToken, String apiName, String bodyContent) throws Exception {
        StringContentProvider provider = new StringContentProvider(bodyContent);
        HttpClient httpClient = null;

        try {
            httpClient = new HttpClient(new SslContextFactory());
            httpClient.start();

            // Issue a POST request
            String apiUrl = buildShopifyUrl(shop, apiName);
            Request request = httpClient.POST(apiUrl);
            request = RequestUtil.prepareRequestHeader(request, accessToken);
            request = request.content(provider);
            request = request.timeout(DEFAULT_THREADPOOL_TIMEOUT, TimeUnit.SECONDS);

            ContentResponse response = request.send();
            String responseBody = null;

            // Process a response
            if (response.getStatus() >= 200 && response.getStatus() < 300) {
                responseBody = response.getContentAsString();
            } else {
                log.error("received an unexpected status code=" + response.getStatus());
            }
            return responseBody;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (httpClient != null) {
                httpClient.stop();
            }
        }
    }
}
