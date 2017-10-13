package com.oath.gemini.merchant.shopify;

import com.oath.gemini.merchant.ClosableHttpClient;
import com.oath.gemini.merchant.db.DatabaseService;
import com.oath.gemini.merchant.db.StoreAcctEntity;
import com.oath.gemini.merchant.db.StoreCampaignEntity;
import com.oath.gemini.merchant.db.StoreSysEntity;
import com.oath.gemini.merchant.ews.EWSAccessTokenData;
import com.oath.gemini.merchant.ews.EWSAuthenticationService;
import com.oath.gemini.merchant.ews.EWSClientService;
import com.oath.gemini.merchant.ews.EWSEndpointEnum;
import com.oath.gemini.merchant.ews.EWSResponseData;
import com.oath.gemini.merchant.ews.json.AdvertiserData;
import com.oath.gemini.merchant.shopify.json.ShopifyAccessToken;
import com.oath.gemini.merchant.shopify.json.ShopifyScriptTagData;
import com.oath.gemini.merchant.shopify.json.ShopifyShopData;
import com.oath.gemini.merchant.shopify.json.ShopifyTokenRequestData;
import com.oath.gemini.merchant.shopify.json.Tag;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;
import javax.inject.Inject;
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
import org.apache.commons.beanutils.PropertyUtils;
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
    @Inject
    DatabaseService databaseService;

    @Inject
    EWSAuthenticationService ewsAuthService;

    @Inject
    private Configuration config;

    /**
     * The user reaches here when he either initiates the installation of our app or clicks the app in Shopify admin console
     * 
     * A sample URL initiated from Shopify is: <br/>
     * http://localhost:4080/g/shopify/welcome?hmac=b63bcb2732d8d9a7b1cfa1624afdc92f36d456c32c0903de221978862626f8cb&shop=dpa-bridge.myshopify.com&timestamp=1503618688"
     * 
     * @return redirect to Shopify for asking the grant of the access scopes <br/>
     *         A sample URL initiated by this app is: <br/>
     *         https://dpa-bridge.myshopify.com/admin/oauth/authorize?client_id=62928398fafea63bad905e52e8410079&scope=read_orders&redirect_uri=http://localhost:4080/g/shopify/home&state=32087351187222&grant_options[]=tong,chen
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
     * The user is redirected to here when he either grants us the access of his Shopify data. <br/>
     * 
     * A sample URL initiated from Shopify is: <br/>
     * http://localhost:4080/g/shopify/home?code=22805a9745d6f27ea0b989818670976c&hmac=9b1d163afc0ea825e121505920d2f223cd90f89f98e027f6c21cb70d5a5fe2ce&shop=dpa-bridge.myshopify.com&timestamp=1503786189
     * 
     * @param _refresh is a Gemini's OAuth2 refresh token
     * @param _mc is a merchant access code
     */
    @GET
    @Path("home")
    public Response home(@Context HttpServletRequest req, @QueryParam("hmac") String hmac, @QueryParam("shop") String shop,
            @QueryParam("timestamp") String ts, @QueryParam("code") String code, @QueryParam("state") String state,
            @DefaultValue("") @QueryParam("_refresh") String _refresh, @DefaultValue("") @QueryParam("_mc") String _mc) throws Exception {

        System.err.println("home 1");
        // Verify the signature of the call
        try {
            String counterHmac = ShopifyOauthHelper.generateHMac("code=" + code, "shop=" + shop, "state=" + state, "timestamp=" + ts);
            if (!hmac.equals(counterHmac)) {
                return Response.status(Status.UNAUTHORIZED).build();
            }
        } catch (Exception e) {
            log.error("failed to validate the legitimate of the call", e);
            return Response.serverError().build();
        }
System.err.println("home 2");

        // If user denies our access of his Shopify data, we do nothing
        if ("denied".equalsIgnoreCase(_refresh)) {
            return Response.ok().build();
        }

System.err.println("home 3");
        // Ask for the access scopes if our app has not been installed yet
        ShopifyAccessToken tokens = fetchAuthToken(shop, code);

        if (tokens == null || StringUtils.isBlank(tokens.getAccessToken())) {
            // The shopify code may have expired when user clicks the Browser's Back button to re-play an earlier on-boarding
            log.error("a shopify code '{}' likely has expired", code);
            return Response.status(Status.BAD_REQUEST).build();
        }
System.err.println("home 5");

        // If Shopify's shop account does not exist, we certainly do not have his Yahoo's Refresh Token, and therefore asks him
        // to go through Yahoo's OAuth flow
        StoreAcctEntity storeAcct = databaseService.findStoreAcctByAccessToken(tokens.getAccessToken());
System.err.println("home 6");

        if (storeAcct != null) {
System.err.println("home 7");
            return Response.status(202).build();
         //   return setup(shop, storeAcct.getYahooAccessToken(), storeAcct.getStoreAccessToken());
        } else {
            // Redirect to Yahoo OAuth2 handler for user's Gemini access. Will will be redirected to here when OAuth2 done
            String requestAuth = config.getString("y.oauth.auth.request.url");
            String rd = new URI(req.getScheme(), config.getString("app.host"), "/g/shopify/ews", null).toString();

            rd = ClosableHttpClient.buildQueries(rd, "_mc", tokens.getAccessToken());
            rd = ClosableHttpClient.buildQueries(rd, "shop", shop);

            requestAuth = requestAuth.replace("${y.oauth.redirect}", URLEncoder.encode(rd, "UTF-8"));
            return Response.temporaryRedirect(URI.create(requestAuth)).build();
        }
    }

    /**
     * The user is redirected to here when he grants/denies this app's access of his Gemini data
     */
    @GET
    @Path("ews")
    public Response approve(@Context HttpServletRequest req, @DefaultValue("") @QueryParam("code") String code,
            @QueryParam("shop") String shop, @DefaultValue("") @QueryParam("_mc") String _mc) {
        if (StringUtils.isEmpty(code)) {
            // TODO: indicate that the user denies our access request
            return Response.ok().build();
        }

        try {
            String rd = new URI(req.getScheme(), config.getString("app.host"), "/g/shopify/ews", null).toString();
            EWSAccessTokenData tokens = ewsAuthService.getAccessTokenFromAuthCode(code, rd);

            // Redirect user to a campaign setup page
            if (tokens != null && tokens.getRefreshToken() != null) {
                String refreshToken = tokens.getRefreshToken();
                return setup(shop, refreshToken, _mc);
            } else {
                log.error("invalid EWS authorization code, which could have expired");
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Configure a shopper's campaign, product feed if necessary
     */
    private Response setup(String shop, String gRefreshToken, String shopifyRefreshToken) throws Exception {

        // By now, we have both Shopify and Yahoo access tokens. Let's persist this info locally
        ShopifyClientService ps = new ShopifyClientService(shop, shopifyRefreshToken);
        EWSAccessTokenData tokens = ewsAuthService.getAccessTokenFromRefreshToken(gRefreshToken);
        EWSClientService ews = new EWSClientService(tokens);
        StoreAcctEntity storeAcctEntity = registerStoreAccountIfRequired(ps, ews);
        StoreCampaignEntity storeCmpEntity = new ShopifyProductSetBuilder(ps, ews).upload(false);

        // Inject or modify a dot pixel to tracke user's product events
        injectScriptTag(shop, storeAcctEntity);

        // By now, we have configured a DPA campaign and its product feed on Gemini site. Let's persist this info locally
        storeCmpEntity.setStoreAcctId(storeAcctEntity.getId());
        registerStoreCampainIfRequired(ps, ews, storeCmpEntity);

        // All done. Take a user to this application's campaign configuration page such as budget, price, date range, etc
        String target = config.getString("campaign.setup.url");
        return Response.temporaryRedirect(URI.create(target)).build();
    }

    /**
     * To register Shopify as an e-commerce system if it has never been done before
     */
    private StoreSysEntity registerStoreSystemIfRequired() {
        StoreSysEntity storeSys = databaseService.findStoreSysByDoman("www.shopify.com");

        if (storeSys == null) {
            storeSys = new StoreSysEntity();
            storeSys.setDomain("www.shopify.com");
            storeSys.setDescription("shopify e-commerce system");
            storeSys.setName("shopify");
            databaseService.save(storeSys);
            storeSys = databaseService.findStoreSysByDoman("www.shopify.com");
        }
        return storeSys;
    }

    /**
     * To register a Shopify's shop, which typically happens when the shop installs our application.
     */
    private StoreAcctEntity registerStoreAccountIfRequired(ShopifyClientService ps, EWSClientService ews) throws Exception {
        ShopifyShopData shop = ps.get(ShopifyShopData.class, ShopifyEndpointEnum.SHOPIFY_SHOP_INFO);
        StoreSysEntity storeSysEntity = registerStoreSystemIfRequired();
        EWSResponseData<AdvertiserData> advResponse = ews.get(AdvertiserData.class, EWSEndpointEnum.ADVERTISER);
        String refreshToken = ews.getTokens().getRefreshToken();

        // Check whether this shop already exists
        StoreAcctEntity oldStoreAcct = new StoreAcctEntity();
        oldStoreAcct.setName(shop.getName());
        oldStoreAcct.setDomain(shop.getDomain());
        oldStoreAcct.setStoreAccessToken(ps.getAccessToken());
        oldStoreAcct.setYahooAccessToken(refreshToken);
        oldStoreAcct.setStoreNativeAcctId(Long.toString(shop.getId()));
        oldStoreAcct.setGeminiNativeAcctId((int) advResponse.get(0).getId());

        // Insert or update this shop's account
        oldStoreAcct = databaseService.findByAny(oldStoreAcct);

        if (oldStoreAcct == null) {
            StoreAcctEntity newStoreAcct = new StoreAcctEntity();
            newStoreAcct.setName(shop.getName());
            newStoreAcct.setDomain(shop.getDomain());
            newStoreAcct.setEmail(shop.getEmail());
            newStoreAcct.setStoreAccessToken(ps.getAccessToken());
            newStoreAcct.setYahooAccessToken(refreshToken);
            newStoreAcct.setStoreSysId(storeSysEntity.getId());
            newStoreAcct.setStoreNativeAcctId(Long.toString(shop.getId()));
            newStoreAcct.setGeminiNativeAcctId((int) advResponse.get(0).getId());
            newStoreAcct.setPixelId(1234);
            databaseService.save(newStoreAcct);
            return newStoreAcct;
        } else {
            // Only the following fields can be modified
            databaseService.replaceIfDummy(oldStoreAcct, "name", shop.getName());
            databaseService.replaceIfDummy(oldStoreAcct, "domain", shop.getDomain());

            oldStoreAcct.setEmail(shop.getEmail());
            oldStoreAcct.setStoreAccessToken(ps.getAccessToken());
            oldStoreAcct.setYahooAccessToken(refreshToken);
            databaseService.update(oldStoreAcct);
        }
        return oldStoreAcct;
    }

    /**
     * To register campaign info if we haven't done so; otherwise update an existing entity
     */
    private void registerStoreCampainIfRequired(ShopifyClientService ps, EWSClientService ews, StoreCampaignEntity cmpEntity)
            throws Exception {
        StoreCampaignEntity storedEntity = databaseService.findStoreCampaignById(cmpEntity.getCampaignId());

        if (storedEntity != null) {
            cmpEntity.setId(storedEntity.getId());
            Map<String, ?> src = PropertyUtils.describe(cmpEntity);
            Map<String, ?> dst = PropertyUtils.describe(cmpEntity);

            if (!src.equals(dst)) {
                for (Map.Entry<String, ?> e : dst.entrySet()) {
                    if (e.getValue() != null && !e.getKey().equals("class")) {
                        PropertyUtils.setProperty(storedEntity, e.getKey(), e.getValue());
                    }
                }
                databaseService.save(storedEntity);
            }
        } else {
            databaseService.save(cmpEntity);
        }
    }

    /**
     * POST https://{shop}.myshopify.com/admin/oauth/access_token with the following parameters provided in the body of the
     * request:
     * 
     * client_id - The API Key for the app (see the credentials section of this guide). <br/>
     * client_secret - The Secret Key for the app (see the credentials section of this guide). <br/>
     * code - The authorization code provided in the redirect described above. <br/>
     */
    private static ShopifyAccessToken fetchAuthToken(String shop, String authCode) throws Exception {
        ShopifyClientService ps = new ShopifyClientService(shop, authCode);
        ShopifyTokenRequestData reqestBody = new ShopifyTokenRequestData();

        // Prepare request POST content
        reqestBody.setClientId(ShopifyOauthHelper.API_KEY);
        reqestBody.setClientSecret(ShopifyOauthHelper.SECRET_KEY);
        reqestBody.setCode(authCode);
        return ps.post(ShopifyAccessToken.class, reqestBody, ShopifyEndpointEnum.SHOPIFY_FETCH_TOKEN);
    }

    /**
     * @see https://help.shopify.com/api/reference/scripttag
     */
    private Tag injectScriptTag(String shop, StoreAcctEntity storeAcctEntity) throws Exception {
        ShopifyClientService ps = new ShopifyClientService(shop, storeAcctEntity.getStoreAccessToken());

        // Do nothing if a given script has been inserted already
        Tag[] tags = ps.get(Tag[].class, ShopifyEndpointEnum.SHOPIFY_SCRIPT_TAG_ALL);
        String javascriptFile = config.getString("shopify.dot.pixel");
        javascriptFile = ClosableHttpClient.buildQueries(javascriptFile, "_dp", storeAcctEntity.getPixelId().toString());
        Tag found = null;

        if (tags != null) {
            for (Tag t : tags) {
                if (found == null && javascriptFile.equalsIgnoreCase(t.getSrc())) {
                    found = t;
                    continue;
                }
                // get rid of this obsoleted script because we only have one script tag
                ps.delete(String.class, ShopifyEndpointEnum.SHOPIFY_SCRIPT_TAG_OPS, t.getId());
            }
        }

        if (found != null) {
            return found;
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
     */
    private URI buildScopeRequestUrl(String shop, String redirectUrl) throws MalformedURLException {
        HashMap<String, String> params = new HashMap<>();

        params.put("client_id", ShopifyOauthHelper.API_KEY);
        params.put("scope", config.getString("shopify.access.scopes"));
        params.put("redirect_uri", redirectUrl);
        params.put("state", Long.toString(System.nanoTime()));

        String path = ShopifyEndpointEnum.SHOPIFY_REQUEST_ACCESS.toString().replace("${shop}", shop);
        path = ClosableHttpClient.buildQueries(path, params);
        return URI.create(path);
    }
}
