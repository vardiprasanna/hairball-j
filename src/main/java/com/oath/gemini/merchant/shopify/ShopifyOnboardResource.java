package com.oath.gemini.merchant.shopify;

import static com.oath.gemini.merchant.HttpUtils.buildQueries;
import com.oath.gemini.merchant.Archetype;
import com.oath.gemini.merchant.HttpUtils;
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
import com.oath.gemini.merchant.ews.json.DotTagData;
import com.oath.gemini.merchant.fe.UIAccountDTO;
import com.oath.gemini.merchant.security.SigningService;
import com.oath.gemini.merchant.shopify.json.ShopifyAccessTokenData;
import com.oath.gemini.merchant.shopify.json.ShopifyScriptTagData;
import com.oath.gemini.merchant.shopify.json.ShopifyShopData;
import com.oath.gemini.merchant.shopify.json.ShopifyStoreFrontTokenData;
import com.oath.gemini.merchant.shopify.json.ShopifyStoreFrontTokensData;
import com.oath.gemini.merchant.shopify.json.ShopifyTokenRequestData;
import com.oath.gemini.merchant.shopify.json.ShopifyWebHookData;
import com.oath.gemini.merchant.shopify.json.ShopifyWebHooksData;
import com.oath.gemini.merchant.shopify.json.Tag;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
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
 * @author tong on 10/1/2017
 */
@Slf4j
@Singleton
@Resource
@Path("shopify")
public class ShopifyOnboardResource {
    @Inject
    DatabaseService databaseService;
    @Inject
    SigningService signingService;
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
    public Response install(@Context UriInfo info, @QueryParam("hmac") String hmac, @QueryParam("shop") String shop) {
        int keyEntry = -1;

        try {
            if ((keyEntry = ShopifyOauthHelper.matchHMac(hmac, info.getQueryParameters())) < 0) {
                return Response.status(Status.UNAUTHORIZED).entity("<h3>Unauthorized-1 due to a mismatched key</h3>").build();
            }
        } catch (Exception e) {
            return Response.serverError().entity("Unauthorized-1 due to a mismatched key").build();
        }

        /**
         * Redirect to the store owner for the grant of the access scopes
         */
        try {
            String path = info.getAbsolutePath().toString();
            String redirectUrl = HttpUtils.forceToUseHttps(path.substring(0, path.indexOf("/shopify/")) + "/shopify/home");
            URI uri = buildScopeRequestUrl(keyEntry, shop, redirectUrl);
            return Response.temporaryRedirect(uri).build();
        } catch (Exception e) {
            log.error("failed for the legitimacy of the call", info.getAbsolutePath());
            return Response.serverError().entity(e.toString()).build();
        }
    }

    /**
     * The user is redirected to here when he either grants us the access of his Shopify data. <br/>
     *
     * A sample URL initiated from Shopify is: <br/>
     * http://localhost:4080/g/shopify/home?code=22805a9745d6f27ea0b989818670976c&hmac=9b1d163afc0ea825e121505920d2f223cd90f89f98e027f6c21cb70d5a5fe2ce&shop=dpa-bridge.myshopify.com&timestamp=1503786189
     */
    @GET
    @Path("home")
    public Response home(@Context UriInfo info, @Context HttpServletRequest req, @QueryParam("hmac") String hmac,
                         @QueryParam("shop") String shop, @QueryParam("code") String code, @QueryParam("state") String state) {
        int keyEntry = -1;

        // Verify the signature of the call
        try {
            if ((keyEntry = ShopifyOauthHelper.matchHMac(hmac, info.getQueryParameters())) < 0) {
                return Response.status(Status.UNAUTHORIZED).entity("<h3>Unauthorized-2 due to a mismatched key</h3>").build();
            }
        } catch (Exception e) {
            log.error("failed for the legitimacy of the call", e);
            return Response.serverError().entity("failed for the legitimacy of the call").build();
        }

        try {
            // Ask for the access scopes if our app has not been installed yet
            ShopifyAccessTokenData tokens = fetchAuthToken(keyEntry, shop, code);

            if (tokens == null || StringUtils.isBlank(tokens.getAccessToken())) {
                // The shopify code may have expired when user clicks the Browser's Back button to re-play an earlier on-boarding
                log.error("a shopify code '{}' likely has expired", code);
                return Response.status(Status.BAD_REQUEST).entity("a shopify code has expired").build();
            }

            // If Shopify's shop account does not exist, we certainly do not have his Yahoo's Refresh Token, and therefore asks him
            // to go through Yahoo's OAuth flow
            StoreAcctEntity storeAcct = databaseService.findStoreAcctByDomain(shop);

            if (storeAcct != null) {
                EWSAccessTokenData ewsTokens = ewsAuthService.getAccessTokenFromRefreshToken(storeAcct.getYahooAccessToken());
                // If the refresh token is invalid, user needs to be authenticated again
                if (ewsTokens.isOk()) {
                    return setupOrRepaireIfRequired(req, shop, ewsTokens, tokens.getAccessToken());
                }
            }

            // Redirect to Yahoo OAuth2 handler for user's Gemini access. Will be redirected to here when OAuth2 is done
            // Note: ensure SSL because SSL may have been terminated before a traffic server reaches to us
            String requestAuth = config.getString("y.oauth.auth.request.url");
            String rd = new URI(req.getScheme(), config.getString("app.host"), "/g/shopify/ews", null).toString();

            rd = HttpUtils.forceToUseHttps(rd);
            rd = buildQueries(rd, "_mc", tokens.getAccessToken());
            rd = buildQueries(rd, "shop", shop);

            requestAuth = requestAuth.replace("${y.oauth.redirect}", URLEncoder.encode(rd, "UTF-8"));
            return Response.temporaryRedirect(URI.create(requestAuth)).build();

        } catch (Exception e) {
            log.error("failed to validate the legitimate of the call", req.getRequestURI());
            return Response.serverError().entity(e.toString()).build();
        }
    }

    /**
     * The user is redirected to here when he grants/denies this app's access of his Gemini data
     */
    @GET
    @Path("ews")
    public Response approve(@Context HttpServletRequest req, @DefaultValue("") @QueryParam("code") String code,
                            @QueryParam("shop") String shop, @DefaultValue("") @QueryParam("_mc") String _mc) {
        if (StringUtils.isBlank(code)) {
            // TODO: indicate that the user denies our access request
            return Response.ok("Aborted!").build();
        }

        try {
            String rd = new URI("https"/* req.getScheme() */, config.getString("app.host"), "/g/shopify/ews", null).toString();
            EWSAccessTokenData tokens = ewsAuthService.getAccessTokenFromAuthCode(code, rd);

            // Redirect user to a campaign setup page
            if (tokens != null && tokens.getRefreshToken() != null) {
                return setupOrRepaireIfRequired(req, shop, tokens, _mc);
            } else {
                log.error("invalid EWS authorization code, which could have expired");
                return Response.status(Status.UNAUTHORIZED).entity("failed to retrieve EWS oAuth token").build();
            }
        } catch (Exception e) {
            log.error("failed to validate the legitimate of the call", req.getRequestURI());
            return Response.serverError().entity(e.getMessage() != null ? e.getMessage() : e.toString()).build();
        }
    }

    /**
     * When a shop owner uninstalls this app, Shopify will notify us via the server side call
     *
     * <pre>
     * Your webhook acknowledges that it received data by sending a 200 OK response. Any response outside of the 200 range
     * will let Shopify know that you did not receive your webhook, including 301 Redirect. Shopify does not follow
     * redirects for webhook notifications and will consider a redirection as an error response.
     *
     * Shopify has implemented a 5-second timeout period and a retry period for subscriptions. We wait 5 seconds for a
     * response to each request, and if there isn't one or we get an error, we retry the connection to a total of 19 times
     * over the next 48 hours. A webhook will be deleted if there are 19 consecutive failures for the exact same webhook.
     * You should monitor the admin of your webhook tool for failing webhooks.
     * </pre>
     */
    @Path("uninstall")
    public Response uninstall(@Context HttpServletRequest req, @HeaderParam("X-Shopify-Topic") String topics,
                              @HeaderParam("X-Shopify-Shop-Domain") String shop, @HeaderParam("X-Shopify-Hmac-Sha256") String hmac) {
        String data = HttpUtils.getContent(req);

        if (ShopifyOauthHelper.matchHMac64(hmac, data) < 0) {
            return Response.status(Status.UNAUTHORIZED).entity("<h3>Unauthorized uninstallation due to a mismatched key</h3>").build();
        }
        try {
            StoreAcctEntity acctEntity = new StoreAcctEntity();
            acctEntity.setDomain(shop);
            acctEntity.setIsDeleted(false);
            acctEntity = databaseService.findByAny(acctEntity);

            if (acctEntity != null) {
                ShopifyClientService ps = new ShopifyClientService(shop, acctEntity.getStoreAccessToken());
                EWSAccessTokenData tokens = ewsAuthService.getAccessTokenFromRefreshToken(acctEntity.getYahooAccessToken());
                EWSClientService ews = new EWSClientService(tokens);

                new Archetype(ps, ews, databaseService).tearDown(acctEntity);
            } else {
                log.warn("No store account found for shop {}", shop);
            }

        } catch (Exception e) {
            log.error("Failed to update the database and/or Gemini");
            return Response.serverError().entity(e.toString()).build();
        }

        return Response.ok().build();
    }

    /**
     * UI wants to fetch an account DTO object if the url represents a valid Shopify user
     */
    @GET
    @Path("query")
    public Response queryShopifyAccount(@Context HttpServletRequest req, @Context UriInfo info, @QueryParam("hmac") String hmac,
            @QueryParam("shop") String shop) {
        int keyEntry = -1;

        try {
            if ((keyEntry = ShopifyOauthHelper.matchHMac(hmac, info.getQueryParameters())) < 0) {
                return Response.status(Status.UNAUTHORIZED).entity("<h3>Unauthorized-1 due to a mismatched key</h3>").build();
            }
        } catch (Exception e) {
            return Response.serverError().entity("Unauthorized-1 due to a mismatched key").build();
        }

        StoreAcctEntity storeAcct = databaseService.findStoreAcctByDomain(shop);
        UIAccountDTO accountDTO = mapToAccountDTO(storeAcct);

        try {
            // Prepare Yahoo authentication URI
            String yahooAuthUrl = config.getString("y.oauth.auth.request.url");
            String rd = new URI(req.getScheme(), config.getString("app.host"), "/g/shopify/ews", null).toString();

            rd = HttpUtils.forceToUseHttps(rd);
            rd = buildQueries(rd, "shop", shop);

            yahooAuthUrl = yahooAuthUrl.replace("${y.oauth.redirect}", URLEncoder.encode(rd, "UTF-8"));
            accountDTO.setYahooAuthUrl(yahooAuthUrl);

            // Prepare Shopify authentication URI
            String path = info.getAbsolutePath().toString();
            String redirectUrl = HttpUtils.forceToUseHttps(path.substring(0, path.indexOf("/shopify/")) + "/shopify/home");
            URI uri = buildScopeRequestUrl(keyEntry, shop, redirectUrl);
            accountDTO.setStoreAuthUrl(uri.toString());
        } catch (Exception e) {
            log.error("failed for constructing shopify and yahoo authentication URL", e);
        }
        return Response.ok(accountDTO).build();
    }

    /**
     * UI wants to fetch an account DTO object where the code is Yahoo's oAuth authentication code after a user signs in
     */
    @GET
    @Path("yauth")
    public Response loginShopify(@Context HttpServletRequest req, @Context UriInfo info, @DefaultValue("") @QueryParam("code") String code,
            @QueryParam("shop") String shop) {
        EWSAccessTokenData tokens;
        StoreAcctEntity storeAcct;

        try {
            String rd = new URI("https"/* req.getScheme() */, config.getString("app.host"), "/g/shopify/ews", null).toString();
            tokens = ewsAuthService.getAccessTokenFromAuthCode(code, rd);

            // Redirect user to a campaign setup page
            if (tokens == null || tokens.getRefreshToken() == null) {
                log.error("invalid EWS authorization code, which could have expired");
                return Response.status(Status.UNAUTHORIZED).entity("failed to retrieve EWS oAuth token").build();
            }

            // Create a placeholder for a partially installed account
            EWSClientService ews = new EWSClientService(tokens);
            storeAcct = registerStoreAccountIfRequired(shop, ews);
        } catch (Exception e) {
            log.error("failed to validate the legitimate of the call", req.getRequestURI());
            return Response.serverError().entity(e.getMessage() != null ? e.getMessage() : e.toString()).build();
        }

        UIAccountDTO accountDTO = mapToAccountDTO(storeAcct);
        int keyEntry = 0; // TODO - remove this dependency

        try {
            // Prepare Yahoo authentication URI
            String yahooAuthUrl = config.getString("y.oauth.auth.request.url");
            String rd = new URI(req.getScheme(), config.getString("app.host"), "/g/shopify/ews", null).toString();

            rd = HttpUtils.forceToUseHttps(rd);
            rd = buildQueries(rd, "shop", shop);

            yahooAuthUrl = yahooAuthUrl.replace("${y.oauth.redirect}", URLEncoder.encode(rd, "UTF-8"));
            accountDTO.setYahooAuthUrl(yahooAuthUrl);

            // Prepare Shopify authentication URI
            String path = info.getAbsolutePath().toString();
            String redirectUrl = HttpUtils.forceToUseHttps(path.substring(0, path.indexOf("/shopify/")) + "/shopify/home");
            URI uri = buildScopeRequestUrl(keyEntry, shop, redirectUrl);
            accountDTO.setStoreAuthUrl(uri.toString());
        } catch (Exception e) {
            log.error("failed for constructing shopify and yahoo authentication URL", e);
        }

        return Response.ok(accountDTO).build();
    }

    @GET
    @Path("sauth")
    public Response lastStep(@Context UriInfo info, @Context HttpServletRequest req, @QueryParam("hmac") String hmac,
            @QueryParam("shop") String shop, @QueryParam("code") String code, @QueryParam("state") String state) {
        home(info, req, hmac, shop, code, state);

        StoreAcctEntity storeAcct = databaseService.findStoreAcctByDomain(shop);
        return Response.ok(mapToAccountDTO(storeAcct)).build();
    }

    private UIAccountDTO mapToAccountDTO(StoreAcctEntity storeAcct) {
        UIAccountDTO acct = new UIAccountDTO(storeAcct);

        try {
            // Check whether Yahoo refresh token is still good
            EWSAccessTokenData tokens = ewsAuthService.getAccessTokenFromRefreshToken(storeAcct.getYahooAccessToken());
            acct.setIsYahooTokenValid(true);

            // Check whether the token is still good for accessing a Gemini account.
            EWSClientService ews = new EWSClientService(tokens);
            EWSResponseData<AdvertiserData> advResponse = ews.get(AdvertiserData.class, EWSEndpointEnum.ADVERTISER);

            if (!EWSResponseData.isEmpty(advResponse)) {
                // TODO - pass this info back to UI
            }

            StoreCampaignEntity storeCmpEntity = databaseService.findByAcctId(StoreCampaignEntity.class, storeAcct.getId());
            if (storeCmpEntity != null) {
                acct.setGeminiNativeCampaignId(storeCmpEntity.getCampaignId());
            }
        } catch (Exception e) {
        }

        // TODO: Check whether Shopify refresh token is still good
        acct.setIsStoreTokenValid(StringUtils.isNotBlank(acct.getStoreAccessToken()));
        return acct;
    }

    /**
     * Configure a shopper's campaign, product feed if necessary
     */
    private Response setupOrRepaireIfRequired(HttpServletRequest req, String shop, EWSAccessTokenData tokens, String shopifyRefreshToken)
            throws Exception {

        // By now, we have both Shopify and Yahoo access tokens. Let's persist this info locally
        ShopifyClientService ps = new ShopifyClientService(shop, shopifyRefreshToken);
        EWSClientService ews = new EWSClientService(tokens);
        StoreAcctEntity storeAcctEntity = registerStoreAccountIfRequired(ps, ews);
        StoreCampaignEntity storeCmpEntity = null; // TODO databaseService.findByAcctId(StoreCampaignEntity.class, storeAcctEntity.getId());

        if (storeCmpEntity == null) {
            Archetype archeType = new Archetype(ps, ews, databaseService);
            ShopifyProductSetBuilder feedBuilder = new ShopifyProductSetBuilder(ps);

            // Upload the product feed if it has never been done so
            String remoteFTPFileName = feedBuilder.uploadFeedIfRequiredAsync();

            // Establish a first campaign if it has never been done so
            storeCmpEntity = archeType.create(storeAcctEntity, remoteFTPFileName);

            // By now, we have configured a DPA campaign and its product feed on Gemini site. Let's persist this info locally
            storeCmpEntity = registerStoreCampaignIfRequired(ps, ews, storeCmpEntity);
        }

        // Inject or modify a dot pixel to track user's product events
        injectScriptTag(ps, storeAcctEntity);

        // Register an app uninstallation event listener
        registerWebhook(ps, req);

        // All done. Take a user to this application's campaign configuration page such as budget, price, date range, etc
        // Note: ensure SSL because SSL may have been terminated before a traffic server reaches to us
        HttpSession session = req.getSession(true);
        String cmpId = storeCmpEntity.getId().toString();
        String sig = signingService.sign("h", req.getRemoteHost());

        StringBuffer urlBuffer = req.getRequestURL();
        int endPath = urlBuffer.indexOf("/", 8); // skip "https://" or "http://"
        String target = HttpUtils.forceToUseHttps(endPath > 0 ? urlBuffer.substring(0, endPath) : urlBuffer.toString());

        target += config.getString("campaign.setup.url", "/setup/campaign.html");
        target = buildQueries(target, "cmp", cmpId, "adv", storeAcctEntity.getGeminiNativeAcctId().toString(), "sig", sig);
        session.setAttribute("sig", sig);

        Response.ResponseBuilder builder = Response.temporaryRedirect(URI.create(target));
        builder.header("X-Frame-Options", "ALLOWALL");
        return builder.build();
    }

    /**
     * Create if necessary, and then fetch a store-front (UI) token, which is intended to learn the topography of products
     */
    @SuppressWarnings("unused")
    private String retrieveStoreFrontToken(ShopifyClientService ps) throws Exception {
        ShopifyStoreFrontTokensData[] storeFrontTokens = ps.get(ShopifyStoreFrontTokensData[].class,
                ShopifyEndpointEnum.SHOPIFY_UI_TOKEN_ALL);

        if (storeFrontTokens != null && storeFrontTokens.length > 0) {
            for (ShopifyStoreFrontTokensData t : storeFrontTokens) {
                if (t.getScope().contains("unauthenticated_read_collection_listings")
                        && t.getScope().contains("unauthenticated_read_product_listings")) {
                    return t.getAccessToken();
                }
            }
        }

        // Create an anonymous public access token
        ShopifyStoreFrontTokenData newToken = new ShopifyStoreFrontTokenData();

        newToken.setTitle("hairball");
        newToken = ps.post(ShopifyStoreFrontTokenData.class, newToken, ShopifyEndpointEnum.SHOPIFY_UI_TOKEN_ALL);
        return newToken.getAccessToken();
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
        EWSResponseData<AdvertiserData> advResponse = ews.get(AdvertiserData.class, EWSEndpointEnum.ADVERTISER);
        if (advResponse.size() <= 0) {
            throw new Exception("Unable to retrieve the Gemini account, which must be registered separately if hasn't been done yet.");
        }

        ShopifyShopData shop = ps.get(ShopifyShopData.class, ShopifyEndpointEnum.SHOPIFY_SHOP_INFO);
        StoreSysEntity storeSysEntity = registerStoreSystemIfRequired();
        String refreshToken = ews.getTokens().getRefreshToken();
        Long geminiNativeAcctId = advResponse.get(0).getId();

        // Check whether this shop already exists
        StoreAcctEntity oldStoreAcct = new StoreAcctEntity();
        oldStoreAcct.setName(shop.getName());
        oldStoreAcct.setDomain(shop.getDomain());
        oldStoreAcct.setStoreAccessToken(ps.getAccessToken());
        oldStoreAcct.setYahooAccessToken(refreshToken);
        oldStoreAcct.setStoreNativeAcctId(Long.toString(shop.getId()));
        oldStoreAcct.setGeminiNativeAcctId(geminiNativeAcctId.intValue());

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
            newStoreAcct.setGeminiNativeAcctId(geminiNativeAcctId.intValue());
            newStoreAcct.setPixelId(extractDotTag(ews, geminiNativeAcctId).getId().intValue());
            databaseService.save(newStoreAcct);
            return newStoreAcct;
        } else {
            // Only the following fields can be modified
            databaseService.replaceIfDummyOrBlank(oldStoreAcct, "name", shop.getName());
            databaseService.replaceIfDummyOrBlank(oldStoreAcct, "domain", shop.getDomain());
            databaseService.replaceIfDummyOrBlank(oldStoreAcct, "storeNativeAcctId", Long.toString(shop.getId()));

            oldStoreAcct.setEmail(shop.getEmail());
            oldStoreAcct.setStoreAccessToken(ps.getAccessToken());
            oldStoreAcct.setYahooAccessToken(refreshToken);
            databaseService.update(oldStoreAcct);
        }
        return oldStoreAcct;
    }

    /**
     * To register a Shopify's shop, which typically happens when the shop installs our application.
     */
    private StoreAcctEntity registerStoreAccountIfRequired(String shop, EWSClientService ews) throws Exception {
        EWSResponseData<AdvertiserData> advResponse = ews.get(AdvertiserData.class, EWSEndpointEnum.ADVERTISER);
        if (advResponse.size() <= 0) {
            throw new Exception("Unable to retrieve the Gemini account, which must be registered separately if hasn't been done yet.");
        }

        StoreSysEntity storeSysEntity = registerStoreSystemIfRequired();
        String refreshToken = ews.getTokens().getRefreshToken();
        Long geminiNativeAcctId = advResponse.get(0).getId();

        // Check whether this shop already exists
        StoreAcctEntity oldStoreAcct = new StoreAcctEntity();
        oldStoreAcct.setName(shop);
        oldStoreAcct.setDomain(shop);
        oldStoreAcct.setYahooAccessToken(refreshToken);
        oldStoreAcct.setGeminiNativeAcctId(geminiNativeAcctId.intValue());

        // Insert or update this shop's account
        oldStoreAcct = databaseService.findByAny(oldStoreAcct);

        if (oldStoreAcct == null) {
            StoreAcctEntity newStoreAcct = new StoreAcctEntity();
            newStoreAcct.setName(shop);
            newStoreAcct.setDomain(shop);
            newStoreAcct.setEmail("dummy@shopify.com");
            newStoreAcct.setYahooAccessToken(refreshToken);
            newStoreAcct.setStoreSysId(storeSysEntity.getId());
            newStoreAcct.setGeminiNativeAcctId(geminiNativeAcctId.intValue());
            newStoreAcct.setPixelId(extractDotTag(ews, geminiNativeAcctId).getId().intValue());
            databaseService.save(newStoreAcct);
            return newStoreAcct;
        } else {
            // Only the following fields can be modified
            oldStoreAcct.setYahooAccessToken(refreshToken);
            databaseService.update(oldStoreAcct);
        }
        return oldStoreAcct;
    }

    /**
     * To register campaign info if we haven't done so; otherwise update an existing entity
     */
    private StoreCampaignEntity registerStoreCampaignIfRequired(ShopifyClientService ps, EWSClientService ews,
                                                                StoreCampaignEntity cmpEntity) throws Exception {
        StoreCampaignEntity storedEntity = databaseService.findStoreCampaignByGeminiCampaignId(cmpEntity.getCampaignId());

        if (storedEntity == null) {
            // insert a new campaign record
            String decoratedName = cmpEntity.getName() + "-" + cmpEntity.getCampaignId();

            cmpEntity.setName(decoratedName);
            databaseService.save(cmpEntity);
            storedEntity = cmpEntity;
        } else if (DatabaseService.copyNonNullProperties(storedEntity, cmpEntity, "name")) {
            // updated an existing campaign record
            databaseService.update(storedEntity);
        }
        return storedEntity;
    }

    /**
     * Set the event listener when our app is uninstalled
     */
    private void registerWebhook(ShopifyClientService ps, HttpServletRequest req) throws Exception {
        ShopifyWebHooksData[] webhooks = ps.get(ShopifyWebHooksData[].class, ShopifyEndpointEnum.SHOPIFY_WEBHOOK_ALL);
        ShopifyWebHookData webhook = new ShopifyWebHookData();
        String address = req.getRequestURL().toString();

        webhook.setAddress(address.substring(0, address.indexOf("/shopify/")) + "/shopify/uninstall");
        webhook.setTopic("app/uninstalled");

        if (webhooks != null) {
            for (ShopifyWebHooksData wh : webhooks) {
                if (wh.getTopic().equals(webhook.getTopic()) && wh.getAddress().equals(webhook.getAddress())) {
                    return;
                }
            }
        }

        try {
            ps.post(ShopifyWebHookData.class, webhook, ShopifyEndpointEnum.SHOPIFY_WEBHOOK_ALL);
        } catch (Exception e) {
            log.warn("Failed to register '{}' for the event '{}'", webhook.getAddress(), webhook.getTopic(), e);
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
    private static ShopifyAccessTokenData fetchAuthToken(int keyEntry, String shop, String authCode) throws Exception {
        ShopifyClientService ps = new ShopifyClientService(shop, authCode);
        ShopifyTokenRequestData reqestBody = new ShopifyTokenRequestData();

        // Prepare request POST content
        reqestBody.setClientId(ShopifyOauthHelper.getApiKey(keyEntry));
        reqestBody.setClientSecret(ShopifyOauthHelper.getSecreteKey(keyEntry));
        reqestBody.setCode(authCode);
        return ps.post(ShopifyAccessTokenData.class, reqestBody, ShopifyEndpointEnum.SHOPIFY_FETCH_TOKEN);
    }

    /**
     * Extract the dot Tags and create one if it doesn't exist
     */
    public DotTagData extractDotTag(EWSClientService ews, Long advertiserId) throws Exception {
        DotTagData pixel = null;
        EWSResponseData<DotTagData> tagEWSResponseData = ews.get(DotTagData.class, EWSEndpointEnum.DOT_TAG_BY_ADVERTISER, advertiserId);
        if (EWSResponseData.isNotEmpty(tagEWSResponseData)) {
            for (DotTagData tag1 : tagEWSResponseData.getObjects()) {
                if (tag1.isDefaultPixel()) {
                    pixel = tag1;
                    break;
                }
            }
        }

        // if the tag doesn't exist for the advertisers create new one
        if (pixel == null) {
            // Let Gemini know how to access this Tag
            DotTagData dt = new DotTagData();
            dt.setAdvertiserId(advertiserId);
            dt.setName("default dot tag for " + advertiserId);
            dt.setDefaultPixel(true);
            dt.setId(1234L);
            pixel = dt;

            //To DO test the creation of DOT Tag once again for Missing mdm id for advertiser
            //tagEWSResponseData = ews.create(DotTag.class, dt, EWSEndpointEnum.DOT_TAG_BY_ADVERTISER, advertiserId);
            //pixel = tagEWSResponseData.get(0);
        }
        return pixel;
    }

    /**
     * @see https://help.shopify.com/api/reference/scripttag
     */
    private Tag injectScriptTag(ShopifyClientService ps, StoreAcctEntity storeAcctEntity) throws Exception {
        // Do nothing if a given script has been inserted already
        Tag[] tags = ps.get(Tag[].class, ShopifyEndpointEnum.SHOPIFY_SCRIPT_TAG_ALL);
        String javascriptFile = config.getString("shopify.dot.pixel");
        javascriptFile = buildQueries(javascriptFile, "_dp", storeAcctEntity.getPixelId().toString());
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
    private URI buildScopeRequestUrl(int keyEntry, String shop, String redirectUrl) throws MalformedURLException {
        HashMap<String, String> params = new HashMap<>();

        params.put("client_id", ShopifyOauthHelper.getApiKey(keyEntry));
        params.put("scope", config.getString("shopify.access.scopes"));
        params.put("redirect_uri", redirectUrl);
        params.put("state", Long.toString(System.nanoTime()));

        String path = ShopifyEndpointEnum.SHOPIFY_REQUEST_ACCESS.toString().replace("${shop}", shop);
        path = buildQueries(path, params);
        return URI.create(path);
    }
}