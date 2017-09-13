package com.oath.gemini.merchant;

import java.util.Map;
import javax.ws.rs.core.UriBuilder;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;

public abstract class BaseHttpClientService {
    /**
    public Request headers(Request request) {
        request.header(HttpHeader.ACCEPT, "application/json");
        request.header(HttpHeader.CONTENT_TYPE, "application/json");
        return request;
    }

    public <T> T get(Class<T> responseType, String path, Object... params) throws Exception {
        return invoke(responseType, null, HttpMethod.GET, path);
    }

    public <T> T post(Class<T> responseType, Object requestBody, String path, Object... params) throws Exception {
        return invoke(responseType, requestBody, HttpMethod.POST, path);
    }

    private <T> T invoke(Class<T> responseType, Object requestBody, HttpMethod method, String path, Object... params) throws Exception {
        try (ClosableHttpClient httpClient = new ClosableHttpClient()) {
            Request request = httpClient.newRequest(method, replacePositionedParams(path.toString(), params), requestBody);
            headers(request);
            return httpClient.send(responseType);
        }
    }

    public static String buildQueries(String path, Map<String, String> queries) {
        if (queries != null && queries.size() > 0) {
            UriBuilder uriBuilder = UriBuilder.fromPath(path);
            for (Map.Entry<String, String> entry : queries.entrySet()) {
                uriBuilder.queryParam(entry.getKey(), entry.getValue());
            }
            path = uriBuilder.toString();
        }
        return path;
    }

    public static String replacePositionedParams(String path, Object... params) {
        String tgt = path;

        if (tgt != null && params != null) {
            for (int i = 0; i < params.length; i++) {
                tgt = tgt.replace("${" + i + "}", params[0].toString());
            }
        }
        return tgt;
    }
    */
}
