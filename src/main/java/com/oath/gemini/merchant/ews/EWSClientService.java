package com.oath.gemini.merchant.ews;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oath.gemini.merchant.ClosableHttpClient;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EWSClientService {
    private final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private String refreshToken;

    public EWSClientService(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * Initialize a new campaign
     */
    public void archetype() {
        try {
            EWSResponseData<AdvertiserData> advResponse = get(AdvertiserData.class, EWSEndpointEnum.ADVERTISER);
            if (EWSResponseData.isEmpty(advResponse)) {
                log.error("Null advertiser. Eorros: " + advResponse.getErrors());
                return;
            }

            // Initiate a campaign if a specific one does not exist
            AdvertiserData advData = advResponse.get(0);
            EWSResponseData<CampaignData> cmpResponse = get(CampaignData.class, EWSEndpointEnum.CAMPAIGN_BY_ADVERTISER, advData.getId());
            CampaignData cmpData = null;

            if (EWSResponseData.isNotEmpty(cmpResponse)) {
                for (CampaignData c : cmpResponse.getObjects()) {
                    if (c.getCampaignName().contains("shopify")) {
                        cmpData = c;
                        break;
                    }
                }
            }
            if (cmpData == null) {
                cmpResponse = newCampaign(advResponse.get(0));
                if (EWSResponseData.isEmpty(cmpResponse)) {
                    log.error("Failed to create a new campaign");
                    return;
                }
                cmpData = cmpResponse.get(0);
            }

            // Initiate an ad group if does not exist
            EWSResponseData<AdGroupData> adGroupResponse = get(AdGroupData.class, EWSEndpointEnum.ADGROUP_BY_CAMPAIGN, cmpData.getId());
            AdGroupData adGroupData = null;

            if (EWSResponseData.isEmpty(adGroupResponse)) {
                adGroupResponse = newAdGroup(cmpData);
                if (EWSResponseData.isEmpty(adGroupResponse)) {
                    log.error("Failed to create a new ad group");
                    return;
                }
            }

            // Initiate a product ad if does not exist
            // EWSResponseData<AdData> adResponse = get(AdData.class, EWSEndpointEnum.ADGROUP_BY_CAMPAIGN, cmpData.getId());

            log.debug("" + advResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private EWSResponseData<CampaignData> newCampaign(AdvertiserData adv) {
        CampaignData cmp = new CampaignData();

        cmp.setAdvertiserId(adv.getId());
        cmp.setStatus(EWSConstant.StatusEnum.ACTIVE);
        cmp.setCampaignName("auto for shopify");
        cmp.setBudgetType("DAILY");
        cmp.setBudget(BigDecimal.valueOf(10L));
        cmp.setLanguage("en");
        cmp.setChannel(EWSConstant.ChannelEnum.NATIVE);
        cmp.setObjective(EWSConstant.ObjectiveEnum.VISIT_OFFER);
        cmp.setIsPartnerNetwork("TRUE");

        try {
            return create(CampaignData.class, cmp, EWSEndpointEnum.CAMPAIGN_OPS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private EWSResponseData<AdGroupData> newAdGroup(CampaignData cmp) {
        AdGroupData adGroupData = new AdGroupData();
        BidSetData bidSetData = new BidSetData();

        bidSetData.setChannel(EWSConstant.ChannelEnum.NATIVE);
        bidSetData.setPriceType(EWSConstant.PriceTypeEnum.CPC);
        bidSetData.setValue(0.1f);

        adGroupData.setStatus(EWSConstant.StatusEnum.PAUSED);
        adGroupData.setStartDateStr(dateFormat.format(new Date()));
        adGroupData.setEndDateStr(dateFormat.format(new Date()));
        adGroupData.setAdvertiserId(cmp.getAdvertiserId());
        adGroupData.setCampaignId(cmp.getId());
        adGroupData.setAdGroupName("auto for shopify");
        adGroupData.getBidSet().setBids(new BidSetData[] { bidSetData });

        try {
            return create(AdGroupData.class, adGroupData, EWSEndpointEnum.ADGROUP_OPS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Invoke HTTP POST to create a Gemini object
     */
    public <T> EWSResponseData<T> create(Class<T> responseType, Object requestBody, EWSEndpointEnum path, Object... params)
            throws Exception {
        return invoke(responseType, requestBody, HttpMethod.POST, path, params);
    }

    /**
     * Invoke HTTP DELETE to delete a Gemini object
     */
    public <T> EWSResponseData<T> delete(Class<T> responseType, EWSEndpointEnum path, Object... params) throws Exception {
        return invoke(responseType, null, HttpMethod.DELETE, path, params);
    }

    /**
     * Invoke HTTP GET to retrieve a requested Gemini object
     */
    public <T> EWSResponseData<T> get(Class<T> responseType, EWSEndpointEnum path, Object... params) throws Exception {
        return invoke(responseType, null, HttpMethod.GET, path, params);
    }

    /**
     * Invoke HTTP PUT to update a Gemini object
     */
    public <T> EWSResponseData<T> update(Class<T> responseType, Object requestBody, EWSEndpointEnum path, Object... params)
            throws Exception {
        return invoke(responseType, requestBody, HttpMethod.PUT, path, params);
    }

    /**
     * A main Gemini service function
     */
    @SuppressWarnings("unchecked")
    private <T> EWSResponseData<T> invoke(Class<T> responseType, Object requestBody, HttpMethod method, EWSEndpointEnum path,
            Object... params) throws Exception {

        EWSAccessTokenData tokens = EWSAuthentication.getAccessTokenFromRefreshToken(refreshToken);
        EWSResponseData<T> response = null;

        try (ClosableHttpClient httpClient = new ClosableHttpClient()) {
            Request request = httpClient.newRequest(method, path.toString(), requestBody, null, params);

            // Send a request
            request.header(HttpHeader.ACCEPT, "application/json");
            request.header(HttpHeader.CONTENT_TYPE, "application/json");
            request.header(HttpHeader.AUTHORIZATION, "Bearer " + tokens.getAccessToken());
            Map<String, String> res = httpClient.send(Map.class);
            T[] ewsObjects = (T[]) httpClient.send(Array.newInstance(responseType, 0).getClass());

            // Convert a raw response to a list of T objects
            if (res != null && res.get("response") != null) {
                ObjectMapper mapper = new ObjectMapper();

                response = mapper.convertValue(res, EWSResponseData.class);
                response.setObjects(ewsObjects);
            }
        }
        return response;
    }
}
