package com.oath.gemini.merchant;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.UriBuilder;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClosableHttpClient extends HttpClient implements Closeable, AutoCloseable {
    private static int DEFAULT_THREADPOOL_TIMEOUT = 10;
    private Request request;

    public ClosableHttpClient() throws Exception {
        super(new SslContextFactory());
        super.start();
    }

    public Request newGET(String url, Map<String, String> queries, Object... macros) throws Exception {
        return newRequest(HttpMethod.GET, url, null, queries, macros);
    }

    public Request newGET(String url, Object... macros) throws Exception {
        return newRequest(HttpMethod.GET, url, null, null, macros);
    }

    public Request newPOST(String url, Object content, Map<String, String> queries, Object... macros) throws Exception {
        return newRequest(HttpMethod.POST, url, content, queries, macros);
    }

    public Request newPOST(String url, Object content, Object... macros) throws Exception {
        return newRequest(HttpMethod.POST, url, content, null, macros);
    }

    public Request newRequest(HttpMethod method, String url, Object content, Map<String, String> queries, Object... macros)
            throws JsonProcessingException {
        url = replacePositionedParams(url, macros);
        url = buildQueries(url, queries);
        this.request = newRequest(url);

        if (method != null) {
            request.method(method);
        }
        if (content != null) {
            StringContentProvider provider;

            if (content instanceof String) {
                provider = new StringContentProvider((String) content);
            } else {
                ObjectMapper mapper = new ObjectMapper();
                ObjectWriter writer = mapper.writerFor(content.getClass());

                provider = new StringContentProvider(writer.writeValueAsString(content));
                request.header(HttpHeader.CONTENT_TYPE, "application/json");
                // provider.forEach(c -> System.out.println(new String(c.array())));
            }
            request.content(provider);
        }
        return request.timeout(DEFAULT_THREADPOOL_TIMEOUT, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    public <T> T send(Class<T> responseType) throws Exception {
        ContentResponse response = request.send();
        String responseBody = response.getContentAsString();
        T result = null;

        // Process a response
        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            if (!String.class.isAssignableFrom(responseType)) {
                ObjectMapper mapper = new ObjectMapper();

                if (responseType.isArray()) {
                    result = (T) convertToArray(mapper, responseType, responseBody);
                } else {
                    result = mapper.readValue(responseBody, responseType);
                }
            } else {
                result = (T) responseBody;
            }
        } else {
            log.error("received an unexpected status code=" + response.getStatus());

            if (HttpStatus.class.isAssignableFrom(responseType)) {
                HttpStatus error = (HttpStatus) responseType.newInstance();
                error.setStatus(response.getStatus());
                error.setMessage(responseBody);
                result = (T) error;
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object[] convertToArray(ObjectMapper mapper, Class<?> responseType, String responseBody) throws Exception {
        Class<?> memberClass = responseType.getComponentType();
        JsonRootName rootName = memberClass.getAnnotation(JsonRootName.class);
        String propName = memberClass.getSimpleName();

        if (rootName != null) {
            propName = rootName.value();
        }

        Map<String, String> wrapped = mapper.readValue(responseBody, Map.class);
        Object[] result = null;

        if (wrapped != null && wrapped.containsKey(propName)) {
            Object rawResponse = wrapped.get(propName);

            if (rawResponse instanceof List<?>) {
                List<?> rawObjectList = (List<?>) rawResponse;
                result = (Object[]) Array.newInstance(memberClass, rawObjectList.size());

                for (int i = 0; i < rawObjectList.size(); i++) {
                    result[i] = mapper.convertValue(rawObjectList.get(i), memberClass);
                }
            } else if (rawResponse instanceof Object[]) {
                Object[] rawObjectList = (Object[]) rawResponse;
                result = (Object[]) Array.newInstance(memberClass, rawObjectList.length);

                for (int i = 0; i < rawObjectList.length; i++) {
                    result[i] = mapper.convertValue(rawObjectList[i], memberClass);
                }
            } else {
                result = (Object[]) Array.newInstance(responseType, 1);
                result[0] = mapper.convertValue(rawResponse, memberClass);
            }
        }

        return result;
    }

    @Override
    public void close() throws IOException {
        try {
            super.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * number of queries should be even, with a name followed by its value
     */
    public static String buildQueries(String path, String... queries) {
        if (queries != null && (queries.length & 1) == 0) {
            UriBuilder uriBuilder = UriBuilder.fromUri(path);
            for (int i = 0; i < queries.length; i += 2) {
                if (StringUtils.isBlank(queries[i])) {
                    log.error("the {}-th param key is null", i);
                } else if (StringUtils.isBlank(queries[i + 1])) {
                    uriBuilder.queryParam(queries[i], "");
                } else {
                    uriBuilder.queryParam(queries[i], queries[i + 1]);
                }
            }
            path = uriBuilder.toString();
        }
        return path;
    }

    public static String buildQueries(String path, Map<String, String> queries) {
        if (queries != null && queries.size() > 0) {
            UriBuilder uriBuilder = UriBuilder.fromUri(path);
            for (Map.Entry<String, String> entry : queries.entrySet()) {
                uriBuilder.queryParam(entry.getKey(), entry.getValue());
            }
            path = uriBuilder.toString();
        }
        return path;
    }

    public static String replacePositionedParams(String path, Object... macros) {
        String tgt = path;

        if (tgt != null && macros != null) {
            for (int i = 0; i < macros.length; i++) {
                tgt = tgt.replace("${" + i + "}", macros[0].toString());
            }
        }
        return tgt;
    }
}
