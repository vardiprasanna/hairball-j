package com.oath.gemini.merchant.ews;

import static com.oath.gemini.merchant.ClosableHttpClient.buildQueries;
import com.oath.gemini.merchant.ClosableHttpClient;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.Enumeration;
import javax.inject.Singleton;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.glassfish.jersey.server.ResourceConfig;
import lombok.extern.slf4j.Slf4j;;

/**
 * @see the general flow and an example in https://developer.yahoo.com/oauth2/guide/flows_authcode/
 */
@Slf4j
@Singleton
@Path("")
public class EWSAuthenticationResource extends ResourceConfig {
    private static Configuration config;
    private static String refreshBaseOAuth;

    public EWSAuthenticationResource(Configuration config) {
        register(this);
        EWSAuthenticationResource.config = config;

        // Yahoo web-application - "hairball" (see https://developer.yahoo.com/apps)
        refreshBaseOAuth = config.getString("y.oauth.token.basic");
        refreshBaseOAuth = "Basic " + Base64.getEncoder().encodeToString(refreshBaseOAuth.getBytes());
    }

    @GET
    @Path("status")
    public Response status() {
        return Response.ok("echo").build();
    }

    /**
     * Request the user's authorization
     */
    @GET
    @Path("signon")
    public Response signOn(@Context HttpServletRequest req, @QueryParam("_rd") String rd) throws UnsupportedEncodingException {
        String requestAuth = config.getString("y.oauth.auth.request.url");

        req.getSession().setAttribute("_rd", rd);
        return Response.temporaryRedirect(URI.create(requestAuth)).build();
    }

    /**
     * Oauth server redirects to here along with the user's authorization code
     */
    @GET
    @Path("approval")
    public Response approve(@Context HttpServletRequest req, @DefaultValue("") @QueryParam("code") String code) {
        if (StringUtils.isEmpty(code)) {
            // TODO: indicate that the user denies our access request
            return Response.ok(dump(req)).build();
        }

        try {
            EWSAccessTokenData tokens = getAccessTokenFromAuthCode(code);

            // Redirect user to a campaign setup page
            if (tokens != null) {
                String refreshToken = tokens.getRefreshToken();
                String rd = (String) req.getSession().getAttribute("_rd");

                if (StringUtils.isNotBlank(rd)) {
                    byte[] homeUrl = Base64.getDecoder().decode(rd.getBytes());
                    String uri = buildQueries(new String(homeUrl), "_refresh", refreshToken);
                    return Response.temporaryRedirect(URI.create(uri)).build();
                } else {
                    log.error("missing the redirect url");
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return Response.ok(dump(req)).build();
    }

    /**
     * Get an access token from an authorization code
     */
    public static EWSAccessTokenData getAccessTokenFromAuthCode(String authCode) throws Exception {
        String requestTokenBody = config.getString("y.oauth.token.request.by.auth.code");
        String bodyContent = requestTokenBody.replace("${code}", authCode);
        return getAccessToken(bodyContent);
    }

    /**
     * Get an access token from a fresh token
     */
    public static EWSAccessTokenData getAccessTokenFromRefreshToken(String refreshToken) throws Exception {
        String refreshTokenBody = config.getString("y.oauth.token.request.by.refresh.token");
        String bodyContent = refreshTokenBody.replace("${refresh_token}", refreshToken);
        return getAccessToken(bodyContent);
    }

    /**
     * Get both an access token and a refresh token
     */
    private static EWSAccessTokenData getAccessToken(String bodyContent) throws Exception {
        EWSAccessTokenData response = null;

        try (ClosableHttpClient httpClient = new ClosableHttpClient()) {
            String baseUrl = config.getString("app.root.url");
            bodyContent = bodyContent.replace("${y.oauth.redirect}", URLEncoder.encode(baseUrl, "UTF-8"));

            // Issue a POST request
            Request request = httpClient.newPOST(config.getString("y.oauth.token.request.url"), bodyContent);

            // Prepare headers
            request.header(HttpHeader.CONTENT_TYPE, "application/x-www-form-urlencoded");
            request.header(HttpHeader.AUTHORIZATION, refreshBaseOAuth);
            response = httpClient.send(EWSAccessTokenData.class);
        }
        return response;
    }

    private static String dump(HttpServletRequest req) {
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
}
