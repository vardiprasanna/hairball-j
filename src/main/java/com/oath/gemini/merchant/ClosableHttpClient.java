package com.oath.gemini.merchant;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClosableHttpClient extends HttpClient implements Closeable, AutoCloseable {
    private static int DEFAULT_THREADPOOL_TIMEOUT = 10;
    private Request request;

    public ClosableHttpClient() throws Exception {
        super(new SslContextFactory());
        super.start();
    }

    public Request newGET(String url) {
        this.request = super.newRequest(url);
        return request.timeout(DEFAULT_THREADPOOL_TIMEOUT, TimeUnit.SECONDS);
    }

    public Request newPOST(String url, Object content) throws JsonProcessingException {
        return newRequest(HttpMethod.POST, url, content);
    }

    public Request newRequest(HttpMethod method, String url, Object content) throws JsonProcessingException {
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

    public String send() throws Exception {
        return send(String.class);
    }

    @SuppressWarnings("unchecked")
    public <T> T send(Class<T> responseType) throws Exception {
        ContentResponse response = request.send();
        String responseBody = response.getContentAsString();

        // Process a response
        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            if (!String.class.isAssignableFrom(responseType)) {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(responseBody, responseType);
            } else {
                return (T) responseBody;
            }
        } else {
            log.error("received an unexpected status code=" + response.getStatus());

            if (HttpStatus.class.isAssignableFrom(responseType)) {
                HttpStatus error = (HttpStatus) responseType.newInstance();
                error.setStatus(response.getStatus());
                error.setMessage(responseBody);
                return (T) error;
            }
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        try {
            super.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}