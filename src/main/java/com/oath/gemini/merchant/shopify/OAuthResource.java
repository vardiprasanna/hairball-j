package com.oath.gemini.merchant.shopify;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Enumeration;
import javax.inject.Singleton;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.apache.commons.configuration.Configuration;
import org.glassfish.jersey.server.ResourceConfig;

@Singleton
@Path("")
public class OAuthResource extends ResourceConfig {
    private Configuration config;

    public OAuthResource(Configuration config) {
        this.config = config;
        register(this);
    }

    /**
     * Redirect a user to Yahoo oauth server, and request the user's access permission
     */
    @GET
    @Path("signon")
    public Response signOn() throws UnsupportedEncodingException {
        StringBuilder buf = new StringBuilder();

        buf.append("https://api.login.yahoo.com/oauth2/request_auth?response_type=code&language=en-us");
        buf.append("&client_id=dj0yJmk9NEJVRHRaRnpWa09SJmQ9WVdrOVREQktiREUzTjJrbWNHbzlNQS0tJnM9Y29uc3VtZXJzZWNyZXQmeD1iYQ--");
        buf.append("&redirect_uri=").append(URLEncoder.encode("https://hairball.herokuapp.com/oauth/redirect", "UTF-8"));

        Response.ResponseBuilder builder = Response.temporaryRedirect(URI.create(buf.toString()));
        builder.header("incognito", true);
        return builder.build();
    }

    /**
     * Oauth server redirects to here with the user's permission response
     */
    @POST
    @Path("approval")
    public Response approve(@Context HttpServletRequest req) {
        return Response.ok(dump(req)).build();
    }

    @POST
    @Path("redirect")
    public Response redirectPost(@Context HttpServletRequest req) {
        return redirectGet(req);
    }

    @GET
    @Path("redirect")
    public Response redirectGet(@Context HttpServletRequest req) {
        String content = REDIRECT_FORM.replace("${body}", dumpContent(req));
        return Response.ok(content).build();
    }

    public static String dump(HttpServletRequest req) {
        StringBuilder buf = new StringBuilder();

        buf.append("<div>\n").append(dumpHeaders(req)).append("</div>\n");
        buf.append("<div>\n").append(dumpContent(req)).append("</div>\n");
        return buf.toString();
    }

    private static String dumpHeaders(HttpServletRequest req) {
        StringBuilder buf = new StringBuilder();
        Enumeration<String> heads = req.getHeaderNames();

        while (heads.hasMoreElements()) {
            String name = heads.nextElement();
            String val = req.getHeader(name);
            buf.append("  <p>").append(name).append('=').append(val).append("</p>\n");
        }
        return buf.toString();
    }

    private static String dumpContent(HttpServletRequest req) {
        StringBuilder buf = new StringBuilder();

        try (ServletInputStream reader = req.getInputStream()) {
            byte[] bytes = new byte[1000];
            int len = reader.readLine(bytes, 0, bytes.length);

            if (len > 0) {
                buf.append(new String(bytes, 0, len));
            }
        } catch (Exception e) {
            buf.append("content error: " + e.toString());
        }
        return buf.toString();
    }

    private final static String REDIRECT_FORM = "<html><body onload='setTimeout(function() { document.oauth_status.submit() }, 5000)'><form action='http://localhost:4080/oauth/approval' name='oauth_status' method='post'><input name='dropbox' value='${body}' /></form></body></html>";
}
