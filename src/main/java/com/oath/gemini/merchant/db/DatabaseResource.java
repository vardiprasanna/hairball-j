package com.oath.gemini.merchant.db;

import static com.oath.gemini.merchant.HttpUtils.badRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oath.gemini.merchant.ClosableFTPClient;
import com.oath.gemini.merchant.cron.QuartzCronAnnotation;
import com.oath.gemini.merchant.ews.EWSAccessTokenData;
import com.oath.gemini.merchant.ews.EWSAuthenticationService;
import com.oath.gemini.merchant.ews.EWSClientService;
import com.oath.gemini.merchant.ews.EWSConstant;
import com.oath.gemini.merchant.ews.EWSEndpointEnum;
import com.oath.gemini.merchant.ews.EWSResponseData;
import com.oath.gemini.merchant.ews.json.AdGroupData;
import com.oath.gemini.merchant.ews.json.BidSetArrayData;
import com.oath.gemini.merchant.ews.json.BidSetData;
import com.oath.gemini.merchant.ews.json.CampaignData;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * This service is to access and backup database
 * 
 * @author tong on 10/1/2017
 */
@Slf4j
@Singleton
@Resource
@Produces(MediaType.APPLICATION_JSON)
@JsonInclude(Include.NON_NULL)
@Path("database")
@QuartzCronAnnotation(cron = "db.backup.cron", method = "backup")
public class DatabaseResource {
    @Inject
    DatabaseService databaseService;
    @Inject
    EWSAuthenticationService ewsAuthService;

    public DatabaseResource() {
    }

    public DatabaseResource(DatabaseService databaseService, EWSAuthenticationService ewsAuthService) {
        this.databaseService = databaseService;
        this.ewsAuthService = ewsAuthService;
    }

    @RolesAllowed({ "SIG", "YBY", "localhost" })
    @GET
    @Path("acct/{id:.*}")
    public List<StoreAcctEntity> listAccounts(@PathParam("id") @DefaultValue("") String id) {
        return listAll(StoreAcctEntity.class, id);
    }

    @RolesAllowed({ "SIG", "YBY", "localhost" })
    @GET
    @Path("campaign/{id:.*}")
    public List<StoreCampaignEntity> listCampaigns(@PathParam("id") @DefaultValue("") String id) {
        return listAll(StoreCampaignEntity.class, id);
    }

    @RolesAllowed({ "SIG", "YBY", "localhost" })
    @PUT
    @Path("campaign/{id}/update")
    public Response modifyCampaign(@PathParam("id") String id, @Context HttpServletRequest req, StoreCampaignEntity modifiedStoreCampaign) {
        StoreCampaignEntity originStoreCampaign = listOne(StoreCampaignEntity.class, id);
        if (originStoreCampaign == null) {
            return badRequest("Missing a unique campaigns: ", id);
        }

        StoreAcctEntity storeAcct = listOne(StoreAcctEntity.class, originStoreCampaign.getStoreAcctId().toString());
        if (storeAcct == null) {
            return badRequest("Missing store account for the campaigns: ", id);
        }

        // Update the corresponding Gemini adgroup first
        try {
            modifiedStoreCampaign.setAdgroupId(originStoreCampaign.getAdgroupId());
            modifiedStoreCampaign.setCampaignId(originStoreCampaign.getCampaignId());
            Response status = updateAdGroup(storeAcct.getYahooAccessToken(), modifiedStoreCampaign);

            if (status.getStatus() != 200) {
                return status;
            }
        } catch (Exception e) {
            return badRequest("Failed to retrieve EWS token for the campaign: ", id, e);
        }

        // Update the store campaign record
        try {
            if (DatabaseService.copyNonNullProperties(originStoreCampaign, modifiedStoreCampaign)) {
                databaseService.update(originStoreCampaign);
            }
        } catch (Exception e) {
            return badRequest("failed to copy properties", e);
        }
        return Response.ok(originStoreCampaign).build();
    }

    /**
     * This function can be triggered either via the scheduler or through a REST service call
     */
    @GET
    @Path("backup")
    public Response backup() throws IOException {
        java.nio.file.Path path = null;

        try {
            path = Files.createTempDirectory("hairball-");
        } catch (Exception e) {
            return badRequest("failed to create a temporary database backup dir", e);
        }

        log.info("back db to temp dir {}", path);
        try {
            databaseService.backup(path.toString());
        } catch (Exception e) {
            return badRequest("failed to backup database locally", e);
        }

        List<String> successful = new ArrayList<>();
        List<String> failure = new ArrayList<>();

        try (ClosableFTPClient ftpClient = new ClosableFTPClient(); Stream<java.nio.file.Path> files = Files.list(path)) {
            files.forEach(local -> {
                try {
                    String prefix = InetAddress.getLocalHost().getHostName();
                    String baseName = prefix + "__" + local.getName(local.getNameCount() - 1).toString();
                    java.nio.file.Path remoteFile = Paths.get("/backup/", baseName);
                    failure.add(remoteFile.toString());

                    ftpClient.copyTo(local.toString(), remoteFile.toString());
                    log.info("back '{}' to ftp backup folder", local.toString());
                    Files.delete(local);

                    successful.add(remoteFile.toString());
                    failure.remove(remoteFile.toString());

                } catch (Exception e) {
                    log.error("failed to ftp a local database file '{}'", local, e);
                }
            });
        } finally {
            // Remove this temporary directory
            Files.delete(path);
        }

        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ObjectNode objectNode = mapper.createObjectNode();

        if (successful.size() > 0) {
            ArrayNode arrayNode = mapper.createArrayNode();
            successful.stream().forEach(f -> arrayNode.add(f));
            objectNode.set("successful", arrayNode);
        }
        if (failure.size() > 0) {
            ArrayNode arrayNode = mapper.createArrayNode();
            successful.stream().forEach(f -> arrayNode.add(f));
            objectNode.set("failure", arrayNode);
        }

        return Response.ok(objectNode).build();
    }

    private <T> List<T> listAll(Class<T> entityClass, String id) {
        if (StringUtils.isNotBlank(id) && NumberUtils.isDigits(id)) {
            T result = databaseService.findByEntityId(entityClass, Integer.parseInt(id));
            return (result != null ? Arrays.asList(result) : null);
        }
        return databaseService.listAll(entityClass);
    }

    private <T> T listOne(Class<T> entityClass, String id) {
        List<T> list = listAll(entityClass, id);
        return (list != null && list.size() == 1 ? list.get(0) : null);
    }

    /**
     * Update a Gemini campaign and an adgroup
     */
    public Response updateAdGroup(String gRefreshToken, StoreCampaignEntity modifiedStoreCampaign) throws Exception {
        EWSAccessTokenData tokens = ewsAuthService.getAccessTokenFromRefreshToken(gRefreshToken);
        EWSClientService ews = new EWSClientService(tokens);
        EWSResponseData<AdGroupData> adGroupResponse = ews.get(AdGroupData.class, EWSEndpointEnum.ADGROUP_BY_ID,
                modifiedStoreCampaign.getAdgroupId());
        String campaignIdStr = modifiedStoreCampaign.getCampaignId().toString();

        if (!adGroupResponse.isOk() || adGroupResponse.getObjects() == null || adGroupResponse.getObjects().length != 1) {
            return badRequest(adGroupResponse, "Failed to retrieve adgroup for the campaign: ", campaignIdStr);
        }

        // Update the adgroup
        AdGroupData originalGroupData = adGroupResponse.get(0);
        AdGroupData modifiedAdGroupData = new AdGroupData();

//        if (modifiedStoreCampaign.getStartDate() != null) {
//            modifiedAdGroupData.setStartDateStr(geminiDateFormat.format(modifiedStoreCampaign.getStartDate()));
//        }
//        if (modifiedStoreCampaign.getEndDate() != null) {
//            modifiedAdGroupData.setEndDateStr(geminiDateFormat.format(modifiedStoreCampaign.getEndDate()));
//        }
        if (modifiedStoreCampaign.getStatus() != null) {
            modifiedAdGroupData.setStatus(modifiedStoreCampaign.getStatus());
        }
        if (modifiedStoreCampaign.getPrice() != null) {
            BidSetArrayData bidSet = new BidSetArrayData();
            BidSetData bidSetData = new BidSetData();

            bidSetData.setChannel(EWSConstant.ChannelEnum.NATIVE);
            bidSetData.setPriceType(EWSConstant.PriceTypeEnum.CPC);
            bidSetData.setValue(modifiedStoreCampaign.getPrice());
            bidSet.setBids(new BidSetData[] { bidSetData });
            modifiedAdGroupData.setBidSet(bidSet);
        }

        if (DatabaseService.copyNonNullProperties(originalGroupData, modifiedAdGroupData)) {
            adGroupResponse = ews.update(AdGroupData.class, originalGroupData, EWSEndpointEnum.ADGROUP_OPS);

            if (!adGroupResponse.isOk()) {
                return badRequest(adGroupResponse, "Failed to update adgroup for the campaign: ", campaignIdStr);
            }
        }

        // Update the campaign
        if (modifiedStoreCampaign.getBudget() != null || modifiedStoreCampaign.getStatus() != null) {
            EWSResponseData<CampaignData> campaignResponse = ews.get(CampaignData.class, EWSEndpointEnum.CAMPAIGN_BY_ID,
                    modifiedStoreCampaign.getCampaignId());

            if (!campaignResponse.isOk() || campaignResponse.getObjects() == null || campaignResponse.getObjects().length != 1) {
                return badRequest(campaignResponse, "Failed to retrieve the campaign object: ", campaignIdStr);
            }
            CampaignData originalCampaignData = campaignResponse.get(0);
            CampaignData modifiedCampaignData = new CampaignData();

            if (modifiedStoreCampaign.getBudget() != null) {
                modifiedCampaignData.setBudget(BigDecimal.valueOf(modifiedStoreCampaign.getBudget().doubleValue()));
            }
            if (modifiedStoreCampaign.getStatus() != null) {
                modifiedCampaignData.setStatus(modifiedStoreCampaign.getStatus());
            }
            if (DatabaseService.copyNonNullProperties(originalCampaignData, modifiedCampaignData)) {
                campaignResponse = ews.update(CampaignData.class, originalCampaignData, EWSEndpointEnum.CAMPAIGN_OPS);

                if (!campaignResponse.isOk()) {
                    return badRequest(campaignResponse, "Failed to update the campaign object: ", campaignIdStr);
                }
            }
        }

        return Response.ok().build();
    }

    private static SimpleDateFormat geminiDateFormat = new SimpleDateFormat("yyyy-MM-dd");
}
