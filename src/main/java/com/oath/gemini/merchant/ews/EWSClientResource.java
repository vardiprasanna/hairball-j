package com.oath.gemini.merchant.ews;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.oath.gemini.merchant.ClosableHttpClient;
import com.oath.gemini.merchant.HttpStatus;
import com.oath.gemini.merchant.db.DatabaseResource;
import com.oath.gemini.merchant.db.DatabaseService;
import com.oath.gemini.merchant.db.StoreAcctEntity;
import com.oath.gemini.merchant.db.StoreCampaignEntity;
import com.oath.gemini.merchant.ews.EWSConstant.ReportingJobStatusEnum;
import com.oath.gemini.merchant.ews.json.AdvertiserData;
import com.oath.gemini.merchant.ews.json.CampaignData;
import com.oath.gemini.merchant.fe.UIAccountDTO;
import com.oath.gemini.merchant.fe.UICampaignDTO;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import lombok.extern.slf4j.Slf4j;

/**
 * It is an internal service for UI
 * 
 * @author tong on 1/1/2018
 */
@Slf4j
@Singleton
@Resource
@Produces(MediaType.APPLICATION_JSON)
@JsonInclude(Include.NON_NULL)
@Path("ui")
public class EWSClientResource {
    // Used to categorize an error
    public static final int ERR_GENERAL = -1;
    public static final int ERR_LOCAL_DB = -2;
    public static final int ERR_AUTH = -3;
    public static final int ERR_EWS = -4;

    @Inject
    DatabaseService databaseService;
    @Inject
    EWSAuthenticationService ewsAuthService;
    @Inject
    private Configuration config;

    /**
     * Return an account object with a given Gemini id
     */
    @GET
    @Path("account/{id}")
    public Response getAccount(@PathParam("id") int id) {
        StoreAcctEntity storeAcct;

        // Fetch the info stored locally in this app
        try {
            storeAcct = new StoreAcctEntity();
            storeAcct.setGeminiNativeAcctId(id);
            storeAcct = databaseService.findByAny(storeAcct);
            if (storeAcct == null) {
                return errorResponse(ERR_LOCAL_DB, Status.NOT_FOUND, "No account found with this campaign id=%s", id);
            }
        } catch (Exception e) {
            return errorResponse(ERR_LOCAL_DB, Status.INTERNAL_SERVER_ERROR, "Failed to fetch the campaign=%s: %s", id, e.getMessage());
        }

        return Response.ok(mapToAccountDTO(storeAcct)).build();
    }

    @GET
    @Path("campaign/{cmpId}")
    public Response getCampaign(@PathParam("cmpId") long id) {
        StoreCampaignEntity storeCampaign;
        StoreAcctEntity storeAcct;

        // Fetch the info stored locally in this app
        try {
            storeCampaign = databaseService.findStoreCampaignByGeminiCampaignId(id);
            if (storeCampaign == null) {
                return errorResponse(ERR_LOCAL_DB, Status.NOT_FOUND, "No campaign found with this id=%s", id);
            }
            storeAcct = databaseService.findByEntityId(StoreAcctEntity.class, storeCampaign.getStoreAcctId());
            if (storeAcct == null) {
                return errorResponse(ERR_LOCAL_DB, Status.NOT_FOUND, "No account found with this campaign id=%s", id);
            }
        } catch (Exception e) {
            return errorResponse(ERR_LOCAL_DB, Status.INTERNAL_SERVER_ERROR, "Failed to fetch the campaign=%s: %s", id, e.getMessage());
        }

        // Fetch the access token to be used to invoke Gemini
        EWSAccessTokenData tokens;
        try {
            tokens = ewsAuthService.getAccessTokenFromRefreshToken(storeAcct.getYahooAccessToken());
            if (!tokens.isOk()) {
                return errorResponse(ERR_AUTH, Status.INTERNAL_SERVER_ERROR, "Failed to authenticate: %s", tokens.getMessage());
            }
        } catch (Exception e) {
            return errorResponse(ERR_AUTH, Status.INTERNAL_SERVER_ERROR, "Failed to authenticate: %s", e.getMessage());
        }

        // Fill the campaign object for UI consumption
        EWSResponseData<CampaignData> campaignResponse;
        AdvertiserData advData = null;

        try {
            EWSClientService ews = new EWSClientService(tokens);
            EWSResponseData<AdvertiserData> advResponse = ews.get(AdvertiserData.class, EWSEndpointEnum.ADVERTISER);
            campaignResponse = ews.get(CampaignData.class, EWSEndpointEnum.CAMPAIGN_BY_ID, id);

            if (advResponse.isOk() && !EWSResponseData.isEmpty(advResponse)) {
                advData = advResponse.get(0);
            }
            if (!campaignResponse.isOk() || EWSResponseData.isEmpty(campaignResponse)) {
                return errorResponse(ERR_EWS, Status.fromStatusCode(campaignResponse.getStatus()),
                        "No campaign found in Gemini with this id=%s", id);
            }
        } catch (Exception e) {
            return errorResponse(ERR_EWS, Status.INTERNAL_SERVER_ERROR, "Failed to access Gemini: %s", e.getMessage());
        }

        CampaignData cmpData = campaignResponse.get(0);
        UICampaignDTO uiCmpDTO = new UICampaignDTO(storeCampaign);

        uiCmpDTO.setCampaignStatus(cmpData.getStatus());
        if (advData != null) {
            uiCmpDTO.setAdvStatus(advData.getStatus());
            uiCmpDTO.setAdvName(advData.getName());
        }
        return Response.ok(uiCmpDTO).build();
    }

    @PUT
    @Path("campaign/{cmpId}")
    public Response updateCampaign(@PathParam("cmpId") long id, UICampaignDTO cmpDTO) {
        StoreCampaignEntity storeCampaign, modifiedStoreCampaign;
        StoreAcctEntity storeAcct;
        boolean isModified = false;

        try {
            modifiedStoreCampaign = new StoreCampaignEntity();
            storeCampaign = databaseService.findStoreCampaignByGeminiCampaignId(id);

            if (storeCampaign == null) {
                return errorResponse(ERR_LOCAL_DB, Status.NOT_FOUND, "No campaign found with this id=%s", id);
            }
            if (cmpDTO.getBudget() >= 0f && (storeCampaign.getBudget() == null || storeCampaign.getBudget() != cmpDTO.getBudget())) {
                modifiedStoreCampaign.setBudget(cmpDTO.getBudget());
                isModified = true;
            }
            if (cmpDTO.getPrice() >= 0f && (storeCampaign.getPrice() == null || storeCampaign.getPrice() != cmpDTO.getPrice())) {
                modifiedStoreCampaign.setPrice(cmpDTO.getPrice());
                isModified = true;
            }
            if (cmpDTO.getCampaignStatus() != null) {
                modifiedStoreCampaign.setStatus(cmpDTO.getCampaignStatus());
                isModified = true;
            }

            if (isModified) {
                storeAcct = databaseService.findByEntityId(StoreAcctEntity.class, storeCampaign.getStoreAcctId());
                if (storeAcct == null) {
                    return errorResponse(ERR_LOCAL_DB, Status.NOT_FOUND, "No account found with this campaign id=%s", id);
                }

                DatabaseResource db = new DatabaseResource(this.databaseService, this.ewsAuthService);

                modifiedStoreCampaign.setAdgroupId(storeCampaign.getAdgroupId());
                modifiedStoreCampaign.setCampaignId(storeCampaign.getCampaignId());
                db.updateAdGroup(storeAcct.getYahooAccessToken(), modifiedStoreCampaign);

                databaseService.update(storeCampaign);
            }
        } catch (Exception e) {
            return errorResponse(ERR_LOCAL_DB, Status.INTERNAL_SERVER_ERROR, "Failed to fetch the campaign=%s: %s", id, e.getMessage());
        }

        return Response.ok(cmpDTO).build();
    }

    @POST
    @Path("reporting/{cmpId}")
    public Response getReport(@PathParam("cmpId") long id, String payload) {
//        if (true) {
//            return Response.ok(data).build();
//        }

        StoreCampaignEntity storeCampaign;
        StoreAcctEntity storeAcct;

        // Fetch the info stored locally in this app
        try {
            storeCampaign = databaseService.findStoreCampaignByGeminiCampaignId(id);
            if (storeCampaign == null) {
                return errorResponse(ERR_LOCAL_DB, Status.NOT_FOUND, "No campaign found with this id=%s", id);
            }
            storeAcct = databaseService.findByEntityId(StoreAcctEntity.class, storeCampaign.getStoreAcctId());
            if (storeAcct == null) {
                return errorResponse(ERR_LOCAL_DB, Status.NOT_FOUND, "No account found with this campaign id=%s", id);
            }
        } catch (Exception e) {
            return errorResponse(ERR_LOCAL_DB, Status.INTERNAL_SERVER_ERROR, "Failed to fetch the campaign=%s: %s", id, e.getMessage());
        }

        // Fetch the access token to be used to invoke Gemini
        EWSAccessTokenData tokens;
        try {
            tokens = ewsAuthService.getAccessTokenFromRefreshToken(storeAcct.getYahooAccessToken());
        } catch (Exception e) {
            return errorResponse(ERR_AUTH, Status.INTERNAL_SERVER_ERROR, "Failed to authenticate: %s", e.getMessage());
        }

        EWSResponseData<EWSReportingJobData> ewsResponseData;
        int maxWaitInSeconds = config.getInt("", 10);

        try {
            EWSClientService ews = new EWSClientService(tokens);
            ewsResponseData = ews.job(EWSReportingJobData.class, payload, EWSEndpointEnum.REPORT_JOB_SUBMISSION);
            EWSReportingJobData jobStatus = null;

            for (int i = 0; ewsResponseData.isOk() && i < maxWaitInSeconds; i++) {
                jobStatus = ewsResponseData.get(0);

                switch (jobStatus.getStatus()) {
                case completed:
                    return Response.ok(downloadReport(jobStatus.getJobResponse())).build();
                case submitted:
                case running:
                    Thread.sleep(1000);
                    ewsResponseData = ews.get(EWSReportingJobData.class, EWSEndpointEnum.REPORT_JOB_STATUS, jobStatus.getJobId(),
                            storeAcct.getGeminiNativeAcctId());
                    break;
                case failed:
                    return errorResponse(ERR_EWS, Status.INTERNAL_SERVER_ERROR, "Failed to fetch a report for campaign=%s", id);
                case killed:
                    return errorResponse(ERR_EWS, Status.INTERNAL_SERVER_ERROR, "Killed to fetch a report for campaign=%s", id);
                }
            }
            if (!ewsResponseData.isOk()) {
                return errorResponse(ERR_EWS, Status.INTERNAL_SERVER_ERROR, "Failed to access a Gemini report", ewsResponseData.getBrief());
            } else if (jobStatus == null || jobStatus.getStatus() != ReportingJobStatusEnum.completed) {
                return errorResponse(ERR_EWS, Status.INTERNAL_SERVER_ERROR, "timed out after %d seconds", maxWaitInSeconds);
            }
        } catch (Exception e) {
            return errorResponse(ERR_EWS, Status.INTERNAL_SERVER_ERROR, "Failed to access a Gemini report: %s", e.getMessage());
        }

        return Response.status(Status.NO_CONTENT).build();
    }

    /**
     * Fill partial UI account
     */
    private UIAccountDTO mapToAccountDTO(StoreAcctEntity storeAcct) {
        UIAccountDTO acct = new UIAccountDTO(storeAcct);

        try {
            // Check whether Yahoo refresh token is still good
            EWSAccessTokenData tokens = ewsAuthService.getAccessTokenFromRefreshToken(storeAcct.getYahooAccessToken());
            acct.setIsYahooTokenValid(tokens.isOk());

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
     * Fetch a CSV file, and convert its content to an array of arrays. The first row in the result array is a header row
     */
    private static String downloadReport(String downloadUrl) throws IOException, Exception {
        try (ClosableHttpClient httpClient = new ClosableHttpClient()) {
            Request request = httpClient.newGET(downloadUrl);
            request.header(HttpHeader.ACCEPT, MediaType.TEXT_PLAIN);
            String data = httpClient.send(String.class);

            if (StringUtils.isNotBlank(data)) {
                ObjectMapper mapper = new ObjectMapper();
                ArrayNode outerArray = mapper.createArrayNode();
                Reader in = new StringReader(data);
                Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);

                for (CSVRecord record : records) {
                    ArrayNode memberRow = mapper.createArrayNode();
                    record.forEach(c -> memberRow.add(c.toString()));
                    outerArray.add(memberRow);
                }

                data = outerArray.toString();
            }
            return data;
        }
    }

    private static Response errorResponse(int category, Response.Status status, String brief, Object... args) {
        HttpStatus httpStatus = new HttpStatus();

        if (args != null && args.length > 0) {
            brief = String.format(brief, args);
        }
        log.error(brief);
        httpStatus.setStatus(category);
        httpStatus.setBrief(brief);
        return Response.status(status).entity(httpStatus).build();
    }

    public static String query = "{\"cube\":\"performance_stats\",\"fields\":[{\\\"field\\\":\\\"Day\\\"},{\"field\":\"Advertiser ID\"},{\"field\":\"Campaign ID\"},{\"field\":\"Impressions\"},{\"field\":\"Clicks\"},{\"field\":\"Conversions\"},{\"field\":\"Spend\"},{\"field\":\"Average CPC\"},{\"field\":\"Average CPM\"},{\"field\":\"Source\"}],\"filters\":[{\"field\":\"Advertiser ID\",\"operator\":\"=\",\"value\":1648887},{\"field\":\"Campaign ID\",\"operator\":\"IN\",\"values\":[363525108]},{\"field\":\"Day\",\"operator\":\"between\",\"from\":\"2018-01-11\",\"to\":\"2018-01-11\"}]}";
    public static String data = "[[\"Day\",\"Advertiser ID\",\"Campaign ID\",\"Impressions\",\"Clicks\",\"Conversions\",\"Spend\",\"Average CPC\",\"Average CPM\",\"Source\"],[\"2018-01-10\",1648887,363115331,13,0,5,1.7,0,0,1],[\"2018-01-11\",1648887,363525108,15,3,27,3.14,0.0799999982,15.9999996424,1]]";
}
