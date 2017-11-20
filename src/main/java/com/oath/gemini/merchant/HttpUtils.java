package com.oath.gemini.merchant;

import java.util.Enumeration;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

/**
 * @author tong on 10/1/2017
 */
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
}
