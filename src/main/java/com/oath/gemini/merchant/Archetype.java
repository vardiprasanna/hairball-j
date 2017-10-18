package com.oath.gemini.merchant;

import com.oath.gemini.merchant.db.StoreCampaignEntity;
import com.oath.gemini.merchant.ews.EWSClientService;
import com.oath.gemini.merchant.ews.EWSConstant;
import com.oath.gemini.merchant.ews.EWSEndpointEnum;
import com.oath.gemini.merchant.ews.EWSResponseData;
import com.oath.gemini.merchant.ews.json.AdGroupData;
import com.oath.gemini.merchant.ews.json.AdvertiserData;
import com.oath.gemini.merchant.ews.json.BidSetData;
import com.oath.gemini.merchant.ews.json.CampaignData;
import com.oath.gemini.merchant.ews.json.ProductRule;
import com.oath.gemini.merchant.ews.json.ProductSetData;
import com.oath.gemini.merchant.shopify.ShopifyClientService;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author tong on 10/1/2017
 */
@Slf4j
public class Archetype {
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
    public StoreCampaignEntity create() throws Exception {
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

        StoreCampaignEntity campaignEntity = new StoreCampaignEntity();

        campaignEntity.setAdvId(cmpData.getAdvertiserId());
        campaignEntity.setCampaignId(cmpData.getId());
        campaignEntity.setName(cmpData.getCampaignName());
        campaignEntity.setAdgroupId(adGroupData.getId());
        campaignEntity.setStartDate(parseTimestamp(adGroupData.getStartDateStr()));
        campaignEntity.setEndDate(parseTimestamp(adGroupData.getEndDateStr()));
        campaignEntity.setPrice(1.5f /*adGroupData.getEcpaGoal() */);
        campaignEntity.setBudget(cmpData.getBudget().floatValue());
        campaignEntity.setStatus(EWSConstant.StatusEnum.ACTIVE);
        return campaignEntity;
    }

    private Timestamp parseTimestamp(String timestamp) {
        if (timestamp != null) {
            return new Timestamp(Date.valueOf(LocalDate.parse(timestamp)).getTime());
        }
        return null;
    }

    private CampaignData newCampaign() throws Exception {
        EWSResponseData<CampaignData> cmpResponse = ews.get(CampaignData.class, EWSEndpointEnum.CAMPAIGN_BY_ADVERTISER, advertiserId);
        CampaignData cmpData = null;

        if (EWSResponseData.isNotEmpty(cmpResponse)) {
            for (CampaignData c : cmpResponse.getObjects()) {
                if (c.getCampaignName().contains(entityAutoGenName) && c.getStatus() != EWSConstant.StatusEnum.DELETED) {
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
            cmp.setBudget(BigDecimal.valueOf(50L));
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
        AdGroupData adGroupData = null;

        if (EWSResponseData.isNotEmpty(adGroupResponse)) {
            for (AdGroupData g : adGroupResponse.getObjects()) {
                if (g.getAdGroupName().contains(entityAutoGenName) && g.getStatus() != EWSConstant.StatusEnum.DELETED) {
                    adGroupData = g;
                    break;
                }
            }
        }
        if (adGroupData == null) {
            AdGroupData group = new AdGroupData();
            BidSetData bidSetData = new BidSetData();
            LocalDate dateTime = LocalDate.now();

            bidSetData.setChannel(EWSConstant.ChannelEnum.NATIVE);
            bidSetData.setPriceType(EWSConstant.PriceTypeEnum.CPC);
            bidSetData.setValue(0.1f);

            group.setStatus(EWSConstant.StatusEnum.PAUSED);
            group.setStartDateStr(dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE));
            group.setEndDateStr(dateTime.plusMonths(3).format(DateTimeFormatter.ISO_LOCAL_DATE));
            group.setAdvertiserId(cmp.getAdvertiserId());
            group.setCampaignId(cmp.getId());
            group.setAdGroupName(entityAutoGenName);
            group.setProductSetId(pset.getId());
            group.setEcpaGoal(bidSetData.getValue());
            group.getBidSet().setBids(new BidSetData[] { bidSetData });
            adGroupResponse = ews.create(AdGroupData.class, group, EWSEndpointEnum.ADGROUP_OPS);
            adGroupData = adGroupResponse.get(0);
        }

        return adGroupData;
    }

    private void newAd(AdGroupData adGroupData) {
    }

    private ProductSetData newProductSet() throws Exception {
        EWSResponseData<ProductSetData> psetResponse = ews.get(ProductSetData.class, EWSEndpointEnum.PRODUCT_SET_BY_ADVERTISER,
                advertiserId);

        if (EWSResponseData.isEmpty(psetResponse)) {
            ProductSetData pset = new ProductSetData();
            String filter = "{\"price\":{\"gt\":\"0.10\"}}"; // TODO: hard-coded price

            pset.setAdvertiserId(advertiserId);
            pset.setStatus(EWSConstant.StatusEnum.ACTIVE);
            pset.setFilter(filter);
            psetResponse = ews.create(ProductSetData.class, pset, EWSEndpointEnum.PRODUCT_SET_OPS);
        }
        return psetResponse.get(0);
    }

    private ProductRule newProductRule() throws Exception {
        try {
            EWSResponseData<ProductRule> psetResponse = null;

            try {
                ProductRule[] rule = { new ProductRule() };

                rule[0].setPixelId(10039241L); // TODO: a hard-coded pixel id
                rule[0].setAdvertiserId(advertiserId);
                psetResponse = ews.create(ProductRule.class, rule, EWSEndpointEnum.PRODUCT_RULE_OPS);
            } catch (Exception e) {
                return null;
            }

            return psetResponse.get(0);
        } catch (Exception e) {

        }
        return null;
    }
}
