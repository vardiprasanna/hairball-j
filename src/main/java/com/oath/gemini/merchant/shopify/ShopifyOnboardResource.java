package com.oath.gemini.merchant.shopify;

import static com.oath.gemini.merchant.ClosableHttpClient.buildQueries;
import com.oath.gemini.merchant.AppConfiguration;
import com.oath.gemini.merchant.ClosableHttpClient;
import com.oath.gemini.merchant.ews.EWSClientService;
import com.oath.gemini.merchant.shopify.json.ShopifyAccessToken;
import com.oath.gemini.merchant.shopify.json.ShopifyScriptTagData;
import com.oath.gemini.merchant.shopify.json.ShopifyTokenRequestData;
import com.oath.gemini.merchant.shopify.json.Tag;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * When a store owner adds our app, it will go through the steps supported in this class. See the flow:
 * https://github.com/Shopify/omniauth-shopify-oauth2/wiki/Shopify-OAuth
 * 
 * @see the onboard flow - https://github.com/Shopify/omniauth-shopify-oauth2/wiki/Shopify-OAuth
 * @see more detail - https://help.shopify.com/api/getting-started/authentication/oauth#confirming-installation
 */
@Slf4j
@Singleton
@Resource
@Path("shopify")
public class ShopifyOnboardResource {
    private Configuration config = AppConfiguration.getConfig();

    // TODO: we will use database to persist refresh tokens
    private static Map<String, String> merchantTokenStorage = new HashMap<>();

    /**
     * The user reaches here when he initiates the installation of our app in Shopify admin console
     * 
     * A sample URL initiated from Shopify is: <br/>
     * http://localhost:4080/API/V1/shopify/welcome?hmac=b63bcb2732d8d9a7b1cfa1624afdc92f36d456c32c0903de221978862626f8cb&shop=dpa-bridge.myshopify.com&timestamp=1503618688"
     * 
     * @return redirect to Shopify for asking the grant of the access scopes <br/>
     *         A sample URL initiated by this app is: <br/>
     *         https://dpa-bridge.myshopify.com/admin/oauth/authorize?client_id=62928398fafea63bad905e52e8410079&scope=read_orders&redirect_uri=http://localhost:4080/API/V1/shopify/home&state=32087351187222&grant_options[]=tong,chen
     * 
     * @throws MalformedURLException
     * @throws URISyntaxException
     */
    @GET
    @Path("welcome")
    public Response install(@Context UriInfo info, @QueryParam("hmac") String hmac, @QueryParam("shop") String shop,
            @QueryParam("timestamp") String ts) throws MalformedURLException, URISyntaxException {
        try {
            String counterHmac = ShopifyOauthHelper.generateHMac("shop=" + shop, "timestamp=" + ts);
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
        String redirectUrl = path.substring(0, path.indexOf("shopify")) + "shopify/home";

        URI uri = buildScopeRequestUrl(shop, redirectUrl);
        return Response.temporaryRedirect(uri).build();
    }

    /**
     * The user lands here when he either grants our access or clicks our app link in his admin console. <br/>
     * 
     * A sample URL initiated from Shopify is: <br/>
     * http://localhost:4080/API/V1/shopify/home?code=22805a9745d6f27ea0b989818670976c&hmac=9b1d163afc0ea825e121505920d2f223cd90f89f98e027f6c21cb70d5a5fe2ce&shop=dpa-bridge.myshopify.com&timestamp=1503786189
     * 
     * @param _refresh is a Gemini's OAuth2 refresh token
     * @param _mc is a merchant access code
     */
    @GET
    @Path("home")
    public Response home(@Context HttpServletRequest req, @QueryParam("hmac") String hmac, @QueryParam("shop") String shop,
            @QueryParam("timestamp") String ts, @QueryParam("code") String code, @QueryParam("state") String state,
            @DefaultValue("") @QueryParam("_refresh") String _refresh, @DefaultValue("") @QueryParam("_mc") String _mc) throws Exception {

        try {
            String counterHmac = ShopifyOauthHelper.generateHMac("code=" + code, "shop=" + shop, "state=" + state, "timestamp=" + ts);
            if (!hmac.equals(counterHmac)) {
                return Response.status(Status.UNAUTHORIZED).build();
            }
        } catch (Exception e) {
            log.error("failed to validate the legitimate of the call", e);
            return Response.serverError().build();
        }

        String target = config.getString("URL_CONFIGURATION");

        if (StringUtils.isBlank(_refresh) || StringUtils.isBlank(_mc)) {
            // TODO: need to persist a shopper's token
            ShopifyAccessToken tokens = fetchAuthToken(shop, code);

            if (merchantTokenStorage.containsKey(tokens.getAccessToken())) {
                _mc = tokens.getAccessToken();
            } else {
                injectScriptTag(shop, tokens.getAccessToken());

                // Redirect to the OAuth2 handler for user's Gemini access. Will will be redirected to here when OAuth2 done
                String rd = req.getRequestURL().append('?').append(req.getQueryString()).toString();

                rd = buildQueries(rd, "_mc", tokens.getAccessToken());
                target = config.getString("URL_OAUTH2_YAHOO");
                target = buildQueries(target, "_rd", Base64.getEncoder().encodeToString(rd.getBytes()));
            }
        } else {
            ShopifyClientService ps = new ShopifyClientService(shop, _mc);
            EWSClientService ews = new EWSClientService(_refresh);
            new ShopifyProductSetBuilder(ps, ews).upload();
        }

        if ("denied".equalsIgnoreCase(_refresh)) {
            // TODO: handle a denied-access case
        } else if (!merchantTokenStorage.containsKey(_mc)) {
            merchantTokenStorage.put(_mc, _refresh);
        }

        return Response.temporaryRedirect(URI.create(target)).build();
    }

    /**
     * POST https://{shop}.myshopify.com/admin/oauth/access_token with the following parameters provided in the body of the
     * request:
     * 
     * client_id - The API Key for the app (see the credentials section of this guide). <br/>
     * client_secret - The Secret Key for the app (see the credentials section of this guide). <br/>
     * code - The authorization code provided in the redirect described above. <br/>
     * 
     * @throws Exception
     */
    private static ShopifyAccessToken fetchAuthToken(String shop, String authCode) throws Exception {
        ShopifyClientService ps = new ShopifyClientService(shop, authCode);
        ShopifyTokenRequestData reqestBody = new ShopifyTokenRequestData();

        // Prepare request POST content
        reqestBody.setClientId(ShopifyOauthHelper.API_KEY);
        reqestBody.setClientSecret(ShopifyOauthHelper.SECRETE_KEY);
        reqestBody.setCode(authCode);
        return ps.post(ShopifyAccessToken.class, reqestBody, ShopifyEndpointEnum.SHOPIFY_FETCH_TOKEN);
    }

    /**
     * @see https://help.shopify.com/api/reference/scripttag
     */
    private Tag injectScriptTag(String shop, String authCode) throws Exception {
        ShopifyClientService ps = new ShopifyClientService(shop, authCode);

        // Do nothing if a given script has been inserted already
        Tag[] tags = ps.get(Tag[].class, ShopifyEndpointEnum.SHOPIFY_SCRIPT_TAG_ALL);
        String javascriptFile = config.getString("DOT_PIXEL");

        if (tags != null) {
            for (Tag t : tags) {
                if (javascriptFile.equalsIgnoreCase(t.getSrc())) {
                    return t;
                }
            }
        }

        // Insert a new javascript file
        ShopifyScriptTagData tag = new ShopifyScriptTagData();
        tag.setSrc(javascriptFile);
        ps.post(String.class, tag, ShopifyEndpointEnum.SHOPIFY_SCRIPT_TAG);

        return tag;
    }

    /**
     * https://{shop}.myshopify.com/admin/oauth/authorize?client_id={api_key}&scope={scopes}&redirect_uri={redirect_uri}&state={nonce}&grant_options[]={option}
     * With these substitutions made:
     * 
     * {shop} - substitute this with the name of the user’s shop. <br/>
     * {api_key} - substitute this with the app’s API Key. <br/>
     * {scopes} - substitute this with a comma-separated list of scopes. For example, to write orders and read customers use
     * scope=write_orders,read_customers. https://help.shopify.com/api/getting-started/authentication/oauth#scopes <br/>
     * 
     * {redirect_uri} - (Required) substitute this with the URL where you want to redirect the users after they authorize
     * the client. The complete URL specified here must be identical to one of the Application Redirect URLs. Note: in older
     * applications, this parameter was optional, and redirected to the Application Callback URL when no other value was
     * specified. <br/>
     * 
     * {nonce} - a randomly selected value provided by your application, which is unique for each authorization request.
     * During the OAuth callback phase, your application must check that this value matches the one you provided during
     * authorization. This mechanism is important for the security of your application. <br/>
     * 
     * {option} - (Optional) substitute this with the value per-user if you would like to use the online access mode for API
     * requests. Leave this parameter blank (or omit it) for offline access mode (default).
     * 
     * @throws MalformedURLException
     */
    private URI buildScopeRequestUrl(String shop, String redirectUrl) throws MalformedURLException {
        HashMap<String, String> params = new HashMap<>();

        params.put("client_id", ShopifyOauthHelper.API_KEY);
        params.put("scope", config.getString("SHOPIFY_ACCESS_SCOPES"));
        params.put("redirect_uri", redirectUrl);
        params.put("state", Long.toString(System.nanoTime()));

        String path = ShopifyEndpointEnum.SHOPIFY_REQUEST_ACCESS.toString().replace("${shop}", shop);
        path = ClosableHttpClient.buildQueries(path, params);
        return URI.create(path);
    }
}
