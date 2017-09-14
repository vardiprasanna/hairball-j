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
public class EWSAuthentication extends ResourceConfig {
    // Either an installed-app or a web-app
    private static String requestAuth;
    private static String requestTokenBody;
    private static String refreshTokenBody;
    private static String refreshToken;

    private static String SECRET_ID;
    private static String CLIENT_ID;
    private static String OAUTH_BASE64;

    public EWSAuthentication(Configuration config) {
        register(this);

        try {
            init(0);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Switch an oauth application between two modes: 1 - use an installed-app; 0 - use a web-app
     * 
     * @throws UnsupportedEncodingException
     */
    @GET
    @Path("debug")
    public void init(@DefaultValue("1") @QueryParam("app") int type) throws UnsupportedEncodingException {
        String redirectUrl;
        boolean isRemoteAuth = (type == 0);

        if (isRemoteAuth) {
            // Web-application - "remote-hyperloop"
            redirectUrl = URLEncoder.encode("http://hairball.herokuapp.com/", "UTF-8");
            refreshToken = "AIN4sVkOwLPwTC69vuoRPQavAa3BTV_y.do2eeZ4qLLDlEDl";
            SECRET_ID = "0b596571b0ba8eba179cfa91551a19e40379c8a8";
            CLIENT_ID = "dj0yJmk9NEJVRHRaRnpWa09SJmQ9WVdrOVREQktiREUzTjJrbWNHbzlNQS0tJnM9Y29uc3VtZXJzZWNyZXQmeD1iYQ--";
        } else {
            // Installed application - "test-shopify"
            redirectUrl = "oob";
            refreshToken = "AIN4sVkOwLPwTC69vuoRPQavAa3BTV_y.do2eeZ4qLLDlEDl";
            SECRET_ID = "e1b910f04b4ac795f126cba1881116b8e7281f81";
            CLIENT_ID = "dj0yJmk9R1J3U1NPMjYxSE1ZJmQ9WVdrOVdWSXlNbGhRTlRJbWNHbzlNQS0tJnM9Y29uc3VtZXJzZWNyZXQmeD1iNQ--";
        }
        OAUTH_BASE64 = "Basic " + Base64.getEncoder().encodeToString((CLIENT_ID + ":" + SECRET_ID).getBytes());

        StringBuilder buf = new StringBuilder();
        buf.append("https://api.login.yahoo.com/oauth2/request_auth?response_type=code&language=en-us");
        buf.append("&client_id=").append(CLIENT_ID);
        buf.append("&redirect_uri=").append(redirectUrl);

        requestAuth = buf.toString();
        requestTokenBody = "grant_type=authorization_code&code=${code}&redirect_uri=" + redirectUrl;
        refreshTokenBody = "grant_type=refresh_token&refresh_token=${refresh_token}&redirect_uri=" + redirectUrl;
    }

    /**
     * Request the user's authorization
     */
    @GET
    @Path("signon")
    public Response signOn(@Context HttpServletRequest req, @QueryParam("_rd") String rd) throws UnsupportedEncodingException {
        req.getSession().setAttribute("_rd", rd);
        return Response.temporaryRedirect(URI.create(requestAuth)).build();
    }

    /**
     * Get an access token from an authorization code
     */
    public static EWSAccessTokenData getAccessTokenFromAuthCode(String authCode) throws Exception {
        String bodyContent = requestTokenBody.replace("${code}", authCode);
        return getAccessToken(bodyContent);
    }

    /**
     * Get an access token from a fresh token
     */
    public static EWSAccessTokenData getAccessTokenFromRefreshToken(String refreshToken) throws Exception {
        String bodyContent = refreshTokenBody.replace("${refresh_token}", refreshToken);
        return getAccessToken(bodyContent);
    }

    /**
     * Get both an access token and a refresh token
     */
    private static EWSAccessTokenData getAccessToken(String bodyContent) throws Exception {
        EWSAccessTokenData response = null;

        try (ClosableHttpClient httpClient = new ClosableHttpClient()) {
            // Issue a POST request
            Request request = httpClient.newPOST("https://api.login.yahoo.com/oauth2/get_token", bodyContent);

            // Prepare headers
            request.header(HttpHeader.CONTENT_TYPE, "application/x-www-form-urlencoded");
            request.header(HttpHeader.AUTHORIZATION, OAUTH_BASE64);
            response = httpClient.send(EWSAccessTokenData.class);
        }
        return response;
    }

    /**
     * Oauth server redirects to here along with the user's authorization code
     */
    @GET
    @Path("approval")
    public Response approve(@Context HttpServletRequest req, @DefaultValue("") @QueryParam("code") String code) {
        if (StringUtils.isEmpty(code)) {
            // TODO: indicate that the user denies our access request
        }
        try {
            EWSAccessTokenData tokens = getAccessTokenFromAuthCode(code);

            // Redirect user to a campaign setup page
            if (tokens != null) {
                refreshToken = tokens.getRefreshToken();
                new EWSClientService(refreshToken).archetype();

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
