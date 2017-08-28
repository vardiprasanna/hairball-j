package com.oath.gemini.merchant.shopify;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.configuration.Configuration;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * When a store owner adds our app, it will go through the steps supported in this class. See the flow:
 * https://github.com/Shopify/omniauth-shopify-oauth2/wiki/Shopify-OAuth
 * 
 * @see the onboard flow - https://github.com/Shopify/omniauth-shopify-oauth2/wiki/Shopify-OAuth
 * @see more detail - https://help.shopify.com/api/getting-started/authentication/oauth#confirming-installation
 */
@Slf4j
@Resource
@Path("shopify")
public class OnboardResource {
    @Inject
    private Configuration config;
    private static int DEFAULT_THREADPOOL_TIMEOUT = 10;

    @POST
    @Path("test")
    public Response test() {
        return Response.ok().build();
    }

    /**
     * When a store owner clicks "GET" button in Shopify App page to initiate the installation of our app, Shopify
     * invokes this method. <br/>
     * 
     * A sample URL initiated from Shopify is: <br/>
     * http://localhost:4080/API/V1/shopify/auth?hmac=b63bcb2732d8d9a7b1cfa1624afdc92f36d456c32c0903de221978862626f8cb&shop=dpa-bridge.myshopify.com&timestamp=1503618688"
     * 
     * @return redirect to Shopify for asking the grant of the access scopes <br/>
     *         A sample URL initiated by this app is: <br/>
     *         https://dpa-bridge.myshopify.com/admin/oauth/authorize?client_id=62928398fafea63bad905e52e8410079&scope=read_orders&redirect_uri=http://localhost:4080/API/V1/shopify/permission&state=32087351187222&grant_options[]=tong,chen
     * 
     * @throws MalformedURLException
     * @throws URISyntaxException
     */
    @GET
    @Path("auth")
    public Response install(@Context UriInfo info, @QueryParam("hmac") String hmac, @QueryParam("shop") String shop,
            @QueryParam("timestamp") String ts) throws MalformedURLException, URISyntaxException {
        try {
            String counterHmac = AuthHelper.generateHMac("shop=" + shop, "timestamp=" + ts);
            if (!hmac.equals(counterHmac)) {
                return Response.status(Status.UNAUTHORIZED).build();
            }
        } catch (Exception e) {
            return Response.serverError().build();
        }

        /**
         * Redirect to the store owner for the grant of the access scopes
         */
        String path = info.getAbsolutePath().toString();
        String redirectUrl = path.substring(0, path.indexOf("shopify")) + "shopify/permission";

        URL url = buildScopeRequestUrl(shop, redirectUrl);
        return Response.temporaryRedirect(url.toURI()).build();
    }

    /**
     * Called when a store owner grants the access scopes requested by our app. <br/>
     * 
     * A sample URL initiated from Shopify is: <br/>
     * http://localhost:4080/API/V1/shopify/permission?code=22805a9745d6f27ea0b989818670976c&hmac=9b1d163afc0ea825e121505920d2f223cd90f89f98e027f6c21cb70d5a5fe2ce&shop=dpa-bridge.myshopify.com&timestamp=1503786189
     * 
     * @throws Exception
     */
    @GET
    @Path("permission")
    public Response grant(@Context UriInfo info, @QueryParam("hmac") String hmac, @QueryParam("shop") String shop,
            @QueryParam("timestamp") String ts, @QueryParam("code") String code, @QueryParam("state") String state)
            throws Exception {

        try {
            String counterHmac = AuthHelper.generateHMac("code=" + code, "shop=" + shop, "state=" + state,
                    "timestamp=" + ts);
            if (!hmac.equals(counterHmac)) {
                return Response.status(Status.UNAUTHORIZED).build();
            }
        } catch (Exception e) {
            return Response.serverError().build();
        }

        // TODO: need to persist a shopper's token
        URL shopifyUrl = buildAuthTokenRequestUrl(shop);
        AuthTokenResponseBody response = submit(shopifyUrl.toString(), code);

        // TODO: redirect to this app's setting page
        URL url = buildShopAppsUrl(shop);
        return Response.temporaryRedirect(url.toURI()).build();
    }

    /**
     * POST https://{shop}.myshopify.com/admin/oauth/access_token with the following parameters provided in the body of
     * the request:
     * 
     * client_id - The API Key for the app (see the credentials section of this guide). <br/>
     * client_secret - The Secret Key for the app (see the credentials section of this guide). <br/>
     * code - The authorization code provided in the redirect described above. <br/>
     * 
     * @throws Exception
     */
    public static AuthTokenResponseBody submit(String shopifyUrl, String authCode) throws Exception {
        AuthTokenRequestBody reqestBody = new AuthTokenRequestBody();

        // Prepare request POST content
        reqestBody.setClientId(AuthHelper.API_KEY);
        reqestBody.setClientSecret(AuthHelper.SECRETE_KEY);
        reqestBody.setCode(authCode);

        ObjectMapper mapper = new ObjectMapper();
        String content = mapper.writeValueAsString(reqestBody);
        StringContentProvider provider = new StringContentProvider(content);

        HttpClient httpClient = null;

        try {
            httpClient = new HttpClient(new SslContextFactory());
            httpClient.start();

            // Post a request
            Request request = httpClient.POST(shopifyUrl);
            request = prepareRequestHeader(request);
            request = request.content(provider);
            request = request.timeout(DEFAULT_THREADPOOL_TIMEOUT, TimeUnit.SECONDS);

            ContentResponse response = request.send();
            AuthTokenResponseBody result = null;

            // Process a response
            if (response.getStatus() == 200) {
                String responseBody = response.getContentAsString();
                result = mapper.readValue(responseBody, AuthTokenResponseBody.class);
            } else {
                log.error("received an unexpected status code=" + response.getStatus());
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (httpClient != null) {
                httpClient.stop();
            }
        }
    }

    private static Request prepareRequestHeader(Request request) {
        // if (!yapeeYcaDisable) {
        // try {
        // String ycaString = YCAUtil.getCert(yapeeYcaAppId);
        // request.header("Yahoo-App-Auth", ycaString);
        // } catch (Throwable e) {
        // log.warn("Unable to fetch a yca certifificat for appid: " + yapeeYcaAppId, e);
        // }
        // }
        return request.header(HttpHeader.CONTENT_TYPE, "application/json");
    }

    /**
     * https://{shop}.myshopify.com/admin/oauth/authorize?client_id={api_key}&scope={scopes}&redirect_uri={redirect_uri}&state={nonce}&grant_options[]={option}
     * With these substitutions made:
     * 
     * {shop} - substitute this with the name of the user’s shop. <br/>
     * {api_key} - substitute this with the app’s API Key. <br/>
     * {scopes} - substitute this with a comma-separated list of scopes. For example, to write orders and read customers
     * use scope=write_orders,read_customers. <br/>
     * 
     * {redirect_uri} - (Required) substitute this with the URL where you want to redirect the users after they
     * authorize the client. The complete URL specified here must be identical to one of the Application Redirect URLs.
     * Note: in older applications, this parameter was optional, and redirected to the Application Callback URL when no
     * other value was specified. <br/>
     * 
     * {nonce} - a randomly selected value provided by your application, which is unique for each authorization request.
     * During the OAuth callback phase, your application must check that this value matches the one you provided during
     * authorization. This mechanism is important for the security of your application. <br/>
     * 
     * {option} - (Optional) substitute this with the value per-user if you would like to use the online access mode for
     * API requests. Leave this parameter blank (or omit it) for offline access mode (default).
     * 
     * @throws MalformedURLException
     */
    private static URL buildScopeRequestUrl(String shop, String redirectUrl) throws MalformedURLException {
        StringBuilder url = new StringBuilder("https://").append(shop).append("/admin/oauth/authorize?");

        url.append("client_id=").append(AuthHelper.API_KEY);
        url.append("&scope=read_orders");
        url.append("&redirect_uri=").append(redirectUrl);
        url.append("&state=").append(System.nanoTime());
        return new URL(url.toString());
    }

    /**
     * https://{shop}.myshopify.com/admin/oauth/access_token without query parameters
     */
    private static URL buildAuthTokenRequestUrl(String shop) throws MalformedURLException {
        StringBuilder url = new StringBuilder("https://").append(shop).append("/admin/oauth/access_token");
        return new URL(url.toString());
    }

    /**
     * https://{shop}.myshopify.com/admin/apps
     */
    private static URL buildShopAppsUrl(String shop) throws MalformedURLException {
        StringBuilder url = new StringBuilder("https://").append(shop).append("/admin/apps");
        return new URL(url.toString());
    }
}
