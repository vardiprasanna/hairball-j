package com.oath.gemini.merchant;

import java.util.Enumeration;
import java.util.Map;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriBuilder;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @author tong on 10/1/2017
 */
@Slf4j
public class HttpUtils {
    public static String dump(HttpServletRequest req) {
        StringBuilder buf = new StringBuilder();

        buf.append("<div>\n").append(dumpHeaders(req)).append("</div>\n");
        buf.append("<div>\n").append(getContent(req)).append("</div>\n");
        return buf.toString();
    }

    public static String dumpHeaders(HttpServletRequest req) {
        StringBuilder buf = new StringBuilder();
        Enumeration<String> heads = req.getHeaderNames();

        while (heads.hasMoreElements()) {
            String name = heads.nextElement();
            String val = req.getHeader(name);
            buf.append("  <p>").append(name).append('=').append(val).append("</p>\n");
        }
        return buf.toString();
    }

    public static String getContent(HttpServletRequest req) {
        StringBuilder buf = new StringBuilder();

        try (ServletInputStream reader = req.getInputStream()) {
            byte[] bytes = new byte[1000];
            int len = reader.readLine(bytes, 0, bytes.length);

            while (len > 0) {
                buf.append(new String(bytes, 0, len));
                len = reader.readLine(bytes, 0, bytes.length);
            }
        } catch (Exception e) {
            buf.append("content error: " + e.toString());
        }
        return buf.toString();
    }

    /**
     * number of queries should be even, with a name followed by its value
     */
    public static String buildQueries(String path, String... queries) {
        if ((queries.length & 1) == 0) {
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
        } else {
            RuntimeException e = new RuntimeException("The cont of query params must be event=" + queries.length);
            log.error(e.getMessage());
            throw e;
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
}
