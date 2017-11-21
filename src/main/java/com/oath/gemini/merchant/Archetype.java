package com.oath.gemini.merchant;

import com.oath.gemini.merchant.db.DatabaseService;
import com.oath.gemini.merchant.db.StoreAcctEntity;
import com.oath.gemini.merchant.db.StoreCampaignEntity;
import com.oath.gemini.merchant.ews.EWSClientService;
import com.oath.gemini.merchant.ews.EWSConstant;
import com.oath.gemini.merchant.ews.EWSConstant.StatusEnum;
import com.oath.gemini.merchant.ews.EWSEndpointEnum;
import com.oath.gemini.merchant.ews.EWSResponseData;
import com.oath.gemini.merchant.ews.json.AdGroupData;
import com.oath.gemini.merchant.ews.json.AdvertiserData;
import com.oath.gemini.merchant.ews.json.BidSetArrayData;
import com.oath.gemini.merchant.ews.json.BidSetData;
import com.oath.gemini.merchant.ews.json.CampaignData;
import com.oath.gemini.merchant.ews.json.ProductFeedData;
import com.oath.gemini.merchant.ews.json.ProductRuleData;
import com.oath.gemini.merchant.ews.json.ProductSetData;
import com.oath.gemini.merchant.shopify.ShopifyClientService;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author tong on 10/1/2017
 */
@Slf4j
public class Archetype {
    private DatabaseService databaseService;
    private EWSClientService ews;
    private String entityAutoGenName;

    @Getter
    private long advertiserId;

    public Archetype(ShopifyClientService svc, EWSClientService ews, DatabaseService ds) throws Exception {
        this.ews = ews;
        this.databaseService = ds;
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
    public StoreCampaignEntity create(StoreAcctEntity acctEntity) throws Exception {
        // Initiate a campaign if a specific one does not exist
        CampaignData cmpData = newCampaign();

        // Initiate a product set
        ProductSetData pset = newProductSet();

        // Initiate a product rule
        newProductRule(acctEntity);

        // Initiate an ad group if does not exist
        AdGroupData adGroupData = newAdGroup(cmpData, pset);

        StoreCampaignEntity campaignEntity = new StoreCampaignEntity();
        Float price = adGroupData.getBidSet().getBids()[0].getValue();

        campaignEntity.setAdvId(cmpData.getAdvertiserId());
        campaignEntity.setCampaignId(cmpData.getId());
        campaignEntity.setName(cmpData.getCampaignName());
        campaignEntity.setAdgroupId(adGroupData.getId());
        campaignEntity.setStartDate(parseTimestamp(adGroupData.getStartDateStr()));
        campaignEntity.setEndDate(parseTimestamp(adGroupData.getEndDateStr()));
        campaignEntity.setPrice(price);
        campaignEntity.setBudget(cmpData.getBudget().floatValue());
        campaignEntity.setStatus(EWSConstant.StatusEnum.ACTIVE);
        return campaignEntity;
    }

    /**
     * Sunset a shop owner after it uninstalls our app
     */
    public void tearDown(StoreAcctEntity acctEntity) throws Exception {
        StoreCampaignEntity cmpEntity = new StoreCampaignEntity();
        cmpEntity.setStoreAcctId(acctEntity.getId());
        cmpEntity.setStatus(StatusEnum.ACTIVE);

        List<StoreCampaignEntity> cmpEntities = databaseService.findAllByAny(cmpEntity);
        if (CollectionUtils.isEmpty(cmpEntities)) {
            log.warn("No active campaign found for shop {}", acctEntity.getDomain());
        }

        for (StoreCampaignEntity sce : cmpEntities) {
            if (tearAdGroup(sce.getAdgroupId()) == null) {
                continue;
            }
            if (tearCampaign(sce.getCampaignId()) == null) {
                continue;
            }
            tearProductFeed(sce.getProductFeedId());
            
            sce.setStatus(StatusEnum.PAUSED);
            databaseService.update(sce);
        }
    }

    /**
     * Deactivate a Gemini campaign
     */
    private CampaignData tearCampaign(long cmpId) throws Exception {
        EWSResponseData<CampaignData> campaignResponse = ews.get(CampaignData.class, EWSEndpointEnum.CAMPAIGN_BY_ID, cmpId);

        if (!campaignResponse.isOk() || campaignResponse.getObjects() == null || campaignResponse.getObjects().length != 1) {
            log.error("Failed to find a campaign for advid={}, campaign={}", advertiserId, cmpId);
            return null;
        }

        CampaignData cmpData = campaignResponse.get(0);

        if (cmpData.getStatus() != StatusEnum.DELETED) {
            cmpData.setStatus(StatusEnum.DELETED);
            campaignResponse = ews.update(CampaignData.class, cmpData, EWSEndpointEnum.ADGROUP_OPS);

            if (!campaignResponse.isOk()) {
                log.error("Failed to deactivate a campaign for advid={}, campaign={}", advertiserId, cmpId);
                return null;
            }
            cmpData = campaignResponse.get(0);
        }
        return cmpData;
    }

    /**
     * Deactivate a Gemini adgroup
     */
    private AdGroupData tearAdGroup(long adgroupId) throws Exception {
        EWSResponseData<AdGroupData> adGroupResponse = ews.get(AdGroupData.class, EWSEndpointEnum.ADGROUP_BY_ID, adgroupId);

        if (!adGroupResponse.isOk() || adGroupResponse.getObjects() == null || adGroupResponse.getObjects().length != 1) {
            log.error("Failed to find an ad group for advid={}, adgroupd={}", advertiserId, adgroupId);
            return null;
        }

        AdGroupData adGroupData = adGroupResponse.get(0);

        if (adGroupData.getStatus() != StatusEnum.DELETED) {
            adGroupData.setStatus(StatusEnum.DELETED);
            adGroupResponse = ews.update(AdGroupData.class, adGroupData, EWSEndpointEnum.ADGROUP_OPS);

            if (!adGroupResponse.isOk()) {
                log.error("Failed to deactivate an adgroup for advid={}, adgroupd={}", advertiserId, adgroupId);
                return null;
            }
            adGroupData = adGroupResponse.get(0);
        }
        return adGroupData;
    }

    /**
     * Remove a product feed
     */
    private ProductFeedData tearProductFeed(long feedId) {
        return null; // TODO
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
            BidSetArrayData bidSet = new BidSetArrayData();
            BidSetData bidSetData = new BidSetData();
            LocalDate dateTime = LocalDate.now();

            bidSetData.setChannel(EWSConstant.ChannelEnum.NATIVE);
            bidSetData.setPriceType(EWSConstant.PriceTypeEnum.CPC);
            bidSetData.setValue(0.1f); // TODO
            bidSet.setBids(new BidSetData[] { bidSetData });

            group.setStatus(EWSConstant.StatusEnum.ACTIVE);
            group.setStartDateStr(dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE));
            group.setEndDateStr(dateTime.plusMonths(3).format(DateTimeFormatter.ISO_LOCAL_DATE));
            group.setAdvertiserId(cmp.getAdvertiserId());
            group.setCampaignId(cmp.getId());
            group.setAdGroupName(entityAutoGenName);
            group.setProductSetId(pset.getId());
            // group.setEcpaGoal(5.0f); // TODO
            // group.setBiddingStrategy(EWSConstant.BiddingStrategyEnum.OPT_CONVERSION);
            // group.setAdvancedGeoPos(EWSConstant.AdvancedGeoPosEnum.DEFAULT);
            // group.setAdvancedGeoNeg(EWSConstant.AdvancedGeoNegEnum.DEFAULT);
            group.setBidSet(bidSet);
            adGroupResponse = ews.create(AdGroupData.class, group, EWSEndpointEnum.ADGROUP_OPS);
            adGroupData = adGroupResponse.get(0);
        }

        return adGroupData;
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

    private ProductRuleData newProductRule(StoreAcctEntity acctEntity) throws Exception {
        EWSResponseData<ProductRuleData> psetResponse = null;

        try {
            ProductRuleData[] rule = { new ProductRuleData() };
            rule[0].setPixelId(acctEntity.getPixelId().longValue());
            rule[0].setAdvertiserId(advertiserId);
            psetResponse = ews.create(ProductRuleData.class, rule, EWSEndpointEnum.PRODUCT_RULE_OPS);
        } catch (Exception e) {
            return null;
        }
        if (!psetResponse.isOk()) {
            return null;
        }

        return psetResponse.get(0);
    }
}
