package com.oath.gemini.merchant.shopify;

import static com.oath.gemini.merchant.ClosableHttpClient.buildQueries;
import com.oath.gemini.merchant.ClosableHttpClient;
import com.oath.gemini.merchant.db.DatabaseService;
import com.oath.gemini.merchant.db.StoreAcctEntity;
import com.oath.gemini.merchant.db.StoreCampaignEntity;
import com.oath.gemini.merchant.db.StoreSysEntity;
import com.oath.gemini.merchant.ews.EWSClientService;
import com.oath.gemini.merchant.shopify.json.ShopifyAccessToken;
import com.oath.gemini.merchant.shopify.json.ShopifyScriptTagData;
import com.oath.gemini.merchant.shopify.json.ShopifyShopData;
import com.oath.gemini.merchant.shopify.json.ShopifyTokenRequestData;
import com.oath.gemini.merchant.shopify.json.Tag;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.Base64;
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
import org.apache.commons.beanutils.PropertyUtilsBean;
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
    private Configuration config;

    /**
     * The user reaches here when he either initiates the installation of our app or clicks the app in Shopify admin console
     * 
     * A sample URL initiated from Shopify is: <br/>
     * http://localhost:4080/API/V1/shopify/welcome?hmac=b63bcb2732d8d9a7b1cfa1624afdc92f36d456c32c0903de221978862626f8cb&shop=dpa-bridge.myshopify.com&timestamp=1503618688"
     * 
     * @return redirect to Shopify for asking the grant of the access scopes <br/>
     *         A sample URL initiated by this app is: <br/>
     *         https://dpa-bridge.myshopify.com/admin/oauth/authorize?client_id=62928398fafea63bad905e52e8410079&scope=read_orders&redirect_uri=http://localhost:4080/API/V1/shopify/home&state=32087351187222&grant_options[]=tong,chen
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

        // If user denies our access of his Shopify data, we do nothing
        if ("denied".equalsIgnoreCase(_refresh)) {
            return Response.ok().build();
        }

        // Ask for the access scopes if our app has not been installed yet
        if (StringUtils.isBlank(_refresh) || StringUtils.isBlank(_mc)) {
            ShopifyAccessToken tokens = fetchAuthToken(shop, code);

            if (tokens == null || StringUtils.isBlank(tokens.getAccessToken())) {
                // The shopify code may have expired when user clicks the Browser's Back button to re-play an earlier on-boarding
                log.error("a shopify code '{}' likely has expired", code);
                return Response.status(Status.BAD_REQUEST).build();
            }

            // If Shopify's shop account does not exist, we certainly do not have his Yahoo's Refresh Token, and therefore asks him
            // to go through Yahoo's OAuth flow
            StoreAcctEntity storeAcct = databaseService.findStoreAcctByAccessToken(tokens.getAccessToken());

            if (storeAcct != null) {
                _mc = storeAcct.getStoreAccessToken();
                _refresh = storeAcct.getYahooAccessToken();
            } else {
                injectScriptTag(shop, tokens.getAccessToken());

                // Redirect to Yahoo OAuth2 handler for user's Gemini access. Will will be redirected to here when OAuth2 done
                String rd = req.getRequestURL().append('?').append(req.getQueryString()).toString();
                String target = config.getString("yahoo.oauth2.url");

                rd = buildQueries(rd, "_mc", tokens.getAccessToken());
                target = buildQueries(target, "_rd", Base64.getEncoder().encodeToString(rd.getBytes()));
                return Response.temporaryRedirect(URI.create(target)).build();
            }
        }

        // By now, we have both Shopify and Yahoo access tokens. Let's persist this info locally
        ShopifyClientService ps = new ShopifyClientService(shop, _mc);
        EWSClientService ews = new EWSClientService(_refresh);
        StoreAcctEntity storeAcctEntity = registerStoreAccountIfRequired(ps, ews);
        StoreCampaignEntity storeCmpEntity = new ShopifyProductSetBuilder(ps, ews).upload(false);

        // By now, we have configured a DPA campaign and its product feed on Gemini site. Let's persist this info locally
        storeCmpEntity.setStoreAcctId(storeAcctEntity.getId());
        registerStoreCampainIfRequired(ps, ews, storeCmpEntity);

        // All done. Take a user to this application's campaign configuration page such as budget, price, date range, etc
        String target = config.getString("campaign.setting.url");
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
        StoreAcctEntity storeAcct = databaseService.findStoreAcctByAccessToken(ps.getAccessToken());

        if (storeAcct == null) {
            storeAcct = new StoreAcctEntity();
            storeAcct.setName(shop.getName());
            storeAcct.setDomain(shop.getDomain());
            storeAcct.setEmail(shop.getEmail());
            storeAcct.setStoreAccessToken(ps.getAccessToken());
            storeAcct.setYahooAccessToken(ews.getRefreshToken());
            storeAcct.setStoreSysId(storeSysEntity.getId());
            storeAcct.setStoreNativeAcctId(Long.toString(shop.getId()));
            databaseService.save(storeAcct);
        } else {
            boolean isChanged = false;

            if (!StringUtils.equals(ps.getAccessToken(), storeAcct.getStoreAccessToken())
                    || !StringUtils.equals(ews.getRefreshToken(), storeAcct.getYahooAccessToken())) {

                storeAcct.setStoreAccessToken(ps.getAccessToken());
                storeAcct.setYahooAccessToken(ews.getRefreshToken());
                storeAcct.setUpdatedDate(new Timestamp(System.currentTimeMillis()));
                isChanged = true;
            }
            if (!StringUtils.equals(shop.getEmail(), storeAcct.getEmail()) || !StringUtils.equals(shop.getName(), storeAcct.getName())
                    || !StringUtils.equals(shop.getDomain(), storeAcct.getDomain())) {
                storeAcct.setName(shop.getName());
                storeAcct.setDomain(shop.getDomain());
                storeAcct.setEmail(shop.getEmail());
                isChanged = true;
            }
            if (isChanged) {
                databaseService.save(storeAcct);
            }
        }

        return storeAcct;
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
        String javascriptFile = config.getString("shopify.dot.pixel");

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
