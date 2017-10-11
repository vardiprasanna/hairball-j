package com.oath.gemini.merchant.ews;

import com.oath.gemini.merchant.ClosableHttpClient;
import java.net.URLEncoder;
import java.util.Base64;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.configuration.Configuration;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;;

/**
 * @see the general flow and an example in https://developer.yahoo.com/oauth2/guide/flows_authcode/
 */
@Singleton
public class EWSAuthenticationService {
    @Inject
    private Configuration config;

    /**
     * Get an access token from an authorization code
     */
    public EWSAccessTokenData getAccessTokenFromAuthCode(String authCode) throws Exception {
        String requestTokenBody = config.getString("y.oauth.token.request.by.auth.code");
        String bodyContent = requestTokenBody.replace("${code}", authCode);
        return getAccessToken(bodyContent);
    }

    /**
     * Get an access token from a fresh token
     */
    public EWSAccessTokenData getAccessTokenFromRefreshToken(String refreshToken) throws Exception {
        String refreshTokenBody = config.getString("y.oauth.token.request.by.refresh.token");
        String bodyContent = refreshTokenBody.replace("${refresh_token}", refreshToken);
        return getAccessToken(bodyContent);
    }

    /**
     * Get both an access token and a refresh token
     */
    private EWSAccessTokenData getAccessToken(String bodyContent) throws Exception {
        EWSAccessTokenData response = null;

        try (ClosableHttpClient httpClient = new ClosableHttpClient()) {
            String baseUrl = config.getString("app.root.url");
            bodyContent = bodyContent.replace("${y.oauth.redirect}", URLEncoder.encode(baseUrl, "UTF-8"));

            // Issue a POST request
            Request request = httpClient.newPOST(config.getString("y.oauth.token.request.url"), bodyContent);

            // Prepare headers
            String refreshBaseOAuth;
            refreshBaseOAuth = config.getString("y.oauth.token.basic");
            refreshBaseOAuth = "Basic " + Base64.getEncoder().encodeToString(refreshBaseOAuth.getBytes());

            request.header(HttpHeader.CONTENT_TYPE, "application/x-www-form-urlencoded");
            request.header(HttpHeader.AUTHORIZATION, refreshBaseOAuth);
            response = httpClient.send(EWSAccessTokenData.class);
        }
        return response;
    }
}
