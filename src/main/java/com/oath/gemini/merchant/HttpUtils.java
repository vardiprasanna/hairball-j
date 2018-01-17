package com.oath.gemini.merchant;

import com.oath.gemini.merchant.ews.EWSResponseData;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
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

    public static String forceToUseHttps(String url) throws URISyntaxException {
        if (url.startsWith("http://")) {
            System.err.format("force to overwrite a non-SSL='%s'", url);
            UriBuilder builder = UriBuilder.fromUri(new URI(url));
            builder.scheme("https");
            builder.port(-1); // use a default SSL port
            url = builder.build().toString();
        }
        return url;
    }

    /**
     * Dump a request for debugging purpose
     */
    public static Response badRequest(String format, Object... params) {
        return badRequest(Status.BAD_REQUEST.getStatusCode(), null, format, params);
    }

    public static Response badRequest(EWSResponseData<?> response, String format, Object... params) {
        return badRequest(response.getStatus(), response.getErrors(), format, params);
    }

    public static Response badRequest(int status, String detail, String format, Object... params) {
        log.error(format, params);

        StringBuilder sb = new StringBuilder(format);
        HttpStatus error = new HttpStatus();

        for (Object m : params) {
            sb.append(m);
        }

        error.setStatus(status);
        error.setBrief(sb.toString());

        if (StringUtils.isNotEmpty(detail)) {
            error.setMessage(detail);
        }
        return Response.status(status).entity(error).build();
    }
}
