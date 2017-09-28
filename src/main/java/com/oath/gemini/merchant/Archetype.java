package com.oath.gemini.merchant;

import com.oath.gemini.merchant.ews.EWSClientService;
import com.oath.gemini.merchant.ews.EWSConstant;
import com.oath.gemini.merchant.ews.EWSEndpointEnum;
import com.oath.gemini.merchant.ews.EWSResponseData;
import com.oath.gemini.merchant.ews.json.AdGroupData;
import com.oath.gemini.merchant.ews.json.AdvertiserData;
import com.oath.gemini.merchant.ews.json.BidSetData;
import com.oath.gemini.merchant.ews.json.CampaignData;
import com.oath.gemini.merchant.ews.json.ProductRecordData;
import com.oath.gemini.merchant.ews.json.ProductRule;
import com.oath.gemini.merchant.ews.json.ProductSetData;
import com.oath.gemini.merchant.shopify.ShopifyClientService;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Archetype {
    private final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private EWSClientService ews;
    private String entityAutoGenName;

    @Getter
    private long advertiserId;

    public Archetype(ShopifyClientService svc, EWSClientService ews) throws Exception {
        this.ews = ews;
        EWSResponseData<AdvertiserData> advResponse = ews.get(AdvertiserData.class, EWSEndpointEnum.ADVERTISER);

        if (EWSResponseData.isEmpty(advResponse)) {
            log.error("Null advertiser. Eorros: " + advResponse.getErrors());
            throw new RuntimeException("No advertiser found");
        }

        AdvertiserData advData = advResponse.get(0);
        advertiserId = advData.getId();
        entityAutoGenName = svc.getShopName() + "-autogen";
    }

    /**
     * Initialize a new campaign
     */
    public void create(List<ProductRecordData> products) {
        try {
            // Initiate a campaign if a specific one does not exist
            CampaignData cmpData = newCampaign();

            // Initiate a product set
            ProductSetData pset = newProductSet();

            // Initiate a product rule
            newProductRule();

            // Initiate an ad group if does not exist
            AdGroupData adGroupData = newAdGroup(cmpData, pset);

            // Initiate a product ad if does not exist
            newAd(adGroupData);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private CampaignData newCampaign() throws Exception {
        EWSResponseData<CampaignData> cmpResponse = ews.get(CampaignData.class, EWSEndpointEnum.CAMPAIGN_BY_ADVERTISER, advertiserId);
        CampaignData cmpData = null;

        if (EWSResponseData.isNotEmpty(cmpResponse)) {
            for (CampaignData c : cmpResponse.getObjects()) {
                if (c.getCampaignName().contains(entityAutoGenName)) {
                    cmpData = c;
                    break;
                }
            }
        }
        if (cmpData == null) {
            CampaignData cmp = new CampaignData();

            cmp.setAdvertiserId(advertiserId);
            cmp.setStatus(EWSConstant.StatusEnum.ACTIVE);
            cmp.setCampaignName(entityAutoGenName);
            cmp.setBudgetType("DAILY");
            cmp.setBudget(BigDecimal.valueOf(10L));
            cmp.setLanguage("en");
            cmp.setChannel(EWSConstant.ChannelEnum.NATIVE);
            cmp.setObjective(EWSConstant.ObjectiveEnum.VISIT_OFFER);
            cmp.setIsPartnerNetwork("TRUE");
            cmpResponse = ews.create(CampaignData.class, cmp, EWSEndpointEnum.CAMPAIGN_OPS);
            cmpData = cmpResponse.get(0);
        }

        return cmpData;
    }

    private AdGroupData newAdGroup(CampaignData cmp, ProductSetData pset) throws Exception {
        EWSResponseData<AdGroupData> adGroupResponse = ews.get(AdGroupData.class, EWSEndpointEnum.ADGROUP_BY_CAMPAIGN, cmp.getId());

        if (EWSResponseData.isEmpty(adGroupResponse)) {
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
            adGroupData.setAdGroupName(entityAutoGenName);
            adGroupData.setProductSetId(pset.getId());
            adGroupData.getBidSet().setBids(new BidSetData[] { bidSetData });
            adGroupResponse = ews.create(AdGroupData.class, adGroupData, EWSEndpointEnum.ADGROUP_OPS);
        }

        return adGroupResponse.get(0);
    }

    private void newAd(AdGroupData adGroupData) {
    }

    private ProductSetData newProductSet() throws Exception {
        EWSResponseData<ProductSetData> psetResponse = ews.get(ProductSetData.class, EWSEndpointEnum.PRODUCT_SET_BY_ADVERTISER,
                advertiserId);

        if (EWSResponseData.isEmpty(psetResponse)) {
            ProductSetData pset = new ProductSetData();
            String filter = "{\"price\":{\"gt\":\"0.05\"}}"; // TODO: hard-coded price

            pset.setAdvertiserId(advertiserId);
            pset.setStatus(EWSConstant.StatusEnum.ACTIVE);
            pset.setFilter(filter);
            psetResponse = ews.create(ProductSetData.class, pset, EWSEndpointEnum.PRODUCT_SET_OPS);
        }
        return psetResponse.get(0);
    }

    private ProductRule newProductRule() throws Exception {
        EWSResponseData<ProductRule> psetResponse = ews.get(ProductRule.class, EWSEndpointEnum.PRODUCT_RULE_OPS, advertiserId);

        if (EWSResponseData.isEmpty(psetResponse)) {
            ProductRule rule = new ProductRule();

            rule.setPixelId(10039241L); // TODO: a hard-coded pixel id
            rule.setAdvertiserId(advertiserId);
            psetResponse = ews.create(ProductRule.class, rule, EWSEndpointEnum.PRODUCT_RULE_OPS);
        }
        return psetResponse.get(0);
    }
}
