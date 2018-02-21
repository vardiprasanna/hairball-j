package com.oath.gemini.merchant;

import com.fasterxml.jackson.databind.JsonNode;
import com.oath.gemini.merchant.db.DatabaseService;
import com.oath.gemini.merchant.db.StoreAcctEntity;
import com.oath.gemini.merchant.db.StoreCampaignEntity;
import com.oath.gemini.merchant.ews.EWSClientService;
import com.oath.gemini.merchant.ews.EWSConstant;
import com.oath.gemini.merchant.ews.EWSConstant.PrdFeedTypeEnum;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
    private AdvertiserData advertiserData;
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

        advertiserData = advResponse.get(0);
        advertiserId = advertiserData.getId();
        entityAutoGenName = svc.getShopName() + "-autogen";
    }

    /**
     * Initialize a new campaign
     */
    public StoreCampaignEntity create(StoreAcctEntity acctEntity, String remoteFTPFileName) throws Exception {
        // Don't proceed if user's gemini account is not active
        if (advertiserData.getStatus() != EWSConstant.StatusEnum.ACTIVE) {
            throw new InactiveGeminiAcountException();
        }

        // Initiate a campaign if a specific one does not exist
        CampaignData cmpData = newCampaign();

        // Initiate a product feed
        ProductFeedData prodFeedData = newProductFeed(remoteFTPFileName);

        // Initiate a product set
        ProductSetData prodSetData = newProductSet();

        // Initiate a product rule
        @SuppressWarnings("unused")
        ProductRuleData prodRuleData = newProductRule(acctEntity);

        // Initiate an ad group if does not exist
        AdGroupData adGroupData = newAdGroup(cmpData, prodSetData);

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
        campaignEntity.setProductFeedId(prodFeedData.getId());
        campaignEntity.setStoreAcctId(acctEntity.getId());
        return campaignEntity;
    }

    /**
     * Sunset a shop owner after it uninstalls our app
     */
    public void tearDown(StoreAcctEntity acctEntity) throws Exception {
        StoreCampaignEntity cmpEntity = new StoreCampaignEntity();
        cmpEntity.setStoreAcctId(acctEntity.getId());

        List<StoreCampaignEntity> cmpEntities = databaseService.findAllByAny(cmpEntity);
        if (CollectionUtils.isEmpty(cmpEntities)) {
            log.warn("No active campaign found for shop {}", acctEntity.getDomain());
        }

        for (StoreCampaignEntity sce : cmpEntities) {
            AdGroupData adGroupData = tearAdGroup(sce.getAdgroupId());

            if (adGroupData != null) {
                tearProductSet(adGroupData.getProductSetId());
            }

            tearProductFeed(sce.getProductFeedId());
            tearCampaign(sce.getCampaignId());

            sce.setStatus(StatusEnum.PAUSED);
            databaseService.delete(sce);
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
        return changeStatus(CampaignData.class, campaignResponse.get(0), StatusEnum.PAUSED, EWSEndpointEnum.CAMPAIGN_OPS);
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
        return changeStatus(AdGroupData.class, adGroupResponse.get(0), StatusEnum.PAUSED, EWSEndpointEnum.ADGROUP_OPS);
    }

    /**
     * Deactivate a product feed
     */
    private ProductFeedData tearProductFeed(long feedId) throws Exception {
        EWSResponseData<ProductFeedData> feedResponse = ews.get(ProductFeedData.class, EWSEndpointEnum.PRODUCT_FEED_BY_ID, feedId);

        if (!feedResponse.isOk() || feedResponse.getObjects() == null || feedResponse.getObjects().length != 1) {
            log.error("Failed to find an product feed for advid={}, feed={}", advertiserId, feedId);
            return null;
        }
        // Note: Gemini supports only DELETE operation, and therefore the op below would fail
        return changeStatus(ProductFeedData.class, feedResponse.get(0), StatusEnum.PAUSED, EWSEndpointEnum.PRODUCT_FEED_OPS);
    }

    /**
     * Deactivate a product set
     */
    private ProductSetData tearProductSet(long productSetId) throws Exception {
        return null; // TODO
    }

    private CampaignData newCampaign() throws Exception {
        EWSResponseData<CampaignData> cmpResponse = ews.get(CampaignData.class, EWSEndpointEnum.CAMPAIGN_BY_ADVERTISER, advertiserId);
        CampaignData cmpData = null;

        if (EWSResponseData.isNotEmpty(cmpResponse)) {
            for (CampaignData c : cmpResponse.getObjects()) {
                if (c.getCampaignName().contains(entityAutoGenName) && c.getStatus() != EWSConstant.StatusEnum.DELETED) {
                    cmpData = c; // changeStatus(CampaignData.class, c, StatusEnum.ACTIVE, EWSEndpointEnum.CAMPAIGN_OPS);
                    break;
                }
            }
        }
        if (cmpData == null) {
            CampaignData cmp = new CampaignData();

            cmp.setAdvertiserId(advertiserId);
            cmp.setStatus(EWSConstant.StatusEnum.PAUSED);
            cmp.setCampaignName(entityAutoGenName);
            cmp.setBudgetType("DAILY");
            cmp.setBudget(BigDecimal.valueOf(20L));
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
                    adGroupData = changeStatus(AdGroupData.class, g, StatusEnum.ACTIVE, EWSEndpointEnum.ADGROUP_OPS);
                    if (adGroupData == null) {
                        adGroupData = g;
                    }
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
            bidSetData.setValue(Math.min(cmp.getBudget().floatValue() / 50, 0.2f)); // Gemini requires budget to be minimum 50 times
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

    private ProductFeedData newProductFeed(String feedFileName) throws Exception {
        EWSResponseData<ProductFeedData> feedResponse = ews.get(ProductFeedData.class, EWSEndpointEnum.PRODUCT_FEED_BY_ADVERTISER,
                advertiserId);
        ProductFeedData productFeedData = null;

        if (feedResponse != null && feedResponse.isOk()) {
            for (ProductFeedData fs : feedResponse.getObjects()) {
                if (fs.getFileName().equals(feedFileName) && fs.getStatus() != StatusEnum.DELETED) {
                    productFeedData = changeStatus(ProductFeedData.class, fs, StatusEnum.ACTIVE, EWSEndpointEnum.PRODUCT_FEED_OPS);
                    if (productFeedData == null) {
                        productFeedData = fs;
                    }
                }
            }
        }

        if (productFeedData == null) {
            // Let Gemini know how to access this product feed
            ProductFeedData feedData = new ProductFeedData();

            feedData.setAdvertiserId(advertiserId);
            feedData.setUserName(ClosableFTPClient.username);
            feedData.setPassword(ClosableFTPClient.password);
            feedData.setFeedType(PrdFeedTypeEnum.DPA_RECURRING);
            feedData.setFileName(feedFileName);
            feedData.setFeedUrl("ftp://" + ClosableFTPClient.host);

            feedResponse = ews.create(ProductFeedData.class, feedData, EWSEndpointEnum.PRODUCT_FEED_OPS);
            productFeedData = feedResponse.get(0);
        }
        return productFeedData;
    }

    private ProductSetData newProductSet() throws Exception {
        EWSResponseData<ProductSetData> psetResponse = ews.get(ProductSetData.class, EWSEndpointEnum.PRODUCT_SET_BY_ADVERTISER,
                advertiserId);

        if (EWSResponseData.isEmpty(psetResponse)) {
            ProductSetData pset = new ProductSetData();

            String filter = "{\"color\":{\"eq\":\"red\"}}"; // TODO: hard-coded color

            ObjectMapper mapper = new ObjectMapper();
            JsonNode filterObj = mapper.readTree(filter);
            pset.setAdvertiserId(advertiserId);
            pset.setStatus(EWSConstant.StatusEnum.ACTIVE);
            pset.setName(entityAutoGenName);
            pset.setFilter(filterObj);
            psetResponse = ews.create(ProductSetData.class, pset, EWSEndpointEnum.PRODUCT_SET_OPS);
        }
        return psetResponse.get(0);
    }

    private ProductRuleData newProductRule(StoreAcctEntity acctEntity) throws Exception {
        EWSResponseData<ProductRuleData> ruleResponse = ews.get(ProductRuleData.class, EWSEndpointEnum.PRODUCT_RULE_BY_ADVERTISER,
                advertiserId);

        // Do nothing if the advertiser already has a product rule
        if (EWSResponseData.isNotEmpty(ruleResponse)) {
            if (ruleResponse.isOk() && ruleResponse.size() > 0) {
                return ruleResponse.get(0);
            }
        }

        try {
            ProductRuleData[] rule = { new ProductRuleData() };
            rule[0].setPixelId(acctEntity.getPixelId().longValue());
            rule[0].setAdvertiserId(advertiserId);
            ruleResponse = ews.create(ProductRuleData.class, rule, EWSEndpointEnum.PRODUCT_RULE_OPS);
        } catch (Exception e) {
            log.error("Failed to create a product rule for advertiser={}", advertiserId, e);
            return null;
        }
        return (!ruleResponse.isOk() ? null : ruleResponse.get(0));
    }

    private <T> T changeStatus(Class<T> clazz, T entity, StatusEnum status, EWSEndpointEnum endpoint) throws Exception {
        String currentStatus = BeanUtils.getProperty(entity, "status");

        // Do nothing if the status is same
        if (StringUtils.isNumeric(currentStatus)) {
            if (StatusEnum.values()[Integer.parseInt(currentStatus)] == status) {
                return entity;
            }
        } else if (status.name().equals(currentStatus)) {
            return entity;
        }

        BeanUtils.setProperty(entity, "status", status);
        EWSResponseData<T> response = ews.update(clazz, entity, endpoint);

        if (!response.isOk()) {
            Object id = BeanUtils.getProperty(entity, "id");
            log.error("Failed to deactivate an entity for advid={}, entity={}", advertiserId, id);
            return null;
        }
        return response.get(0);
    }

    private Timestamp parseTimestamp(String timestamp) {
        if (timestamp != null) {
            return new Timestamp(Date.valueOf(LocalDate.parse(timestamp)).getTime());
        }
        return null;
    }
}
