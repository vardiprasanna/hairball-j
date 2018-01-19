package com.oath.gemini.merchant.ews;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.oath.gemini.merchant.ClosableHttpClient;
import com.oath.gemini.merchant.HttpStatus;
import com.oath.gemini.merchant.db.DatabaseService;
import com.oath.gemini.merchant.db.StoreAcctEntity;
import com.oath.gemini.merchant.db.StoreCampaignEntity;
import com.oath.gemini.merchant.ews.EWSConstant.ReportingJobStatusEnum;
import com.oath.gemini.merchant.ews.json.CampaignData;
import com.oath.gemini.merchant.fe.UICampaignDTO;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
    @Inject
    DatabaseService databaseService;
    @Inject
    EWSAuthenticationService ewsAuthService;
    @Inject
    private Configuration config;

    @RolesAllowed({ "localhost" })
    @GET
    @Path("campaign/{cmpId}")
    public Response getCampaign(@PathParam("cmpId") long id) {
        HttpStatus httpStatus = new HttpStatus();

        try {
            StoreCampaignEntity storeCampaign = databaseService.findStoreCampaignByGeminiCampaignId(id);
            StoreAcctEntity storeAcct = databaseService.findByEntityId(StoreAcctEntity.class, storeCampaign.getStoreAcctId());
            EWSAccessTokenData tokens = ewsAuthService.getAccessTokenFromRefreshToken(storeAcct.getYahooAccessToken());
            EWSClientService ews = new EWSClientService(tokens);
            EWSResponseData<CampaignData> campaignResponse = ews.get(CampaignData.class, EWSEndpointEnum.CAMPAIGN_BY_ID, id);

            if (!campaignResponse.isOk() || campaignResponse.getObjects() == null || campaignResponse.getObjects().length != 1) {
                String error = "Failed to find a campaign={}" + id;

                log.error(error);
                httpStatus.setStatus(404);
                httpStatus.setBrief(error);
                return Response.status(Status.NOT_FOUND).entity(httpStatus).build();
            }

            CampaignData cmpData = campaignResponse.get(0);
            UICampaignDTO uiCmpDTO = new UICampaignDTO(storeCampaign);
            uiCmpDTO.setStatus(cmpData.getStatus());

            return Response.ok(uiCmpDTO).build();
        } catch (Exception e) {
            String error = "Failed to fetch the campaign=" + id;

            log.error(error);
            httpStatus.setStatus(-1);
            httpStatus.setBrief(error);
            httpStatus.setMessage(e.getMessage());
            return Response.serverError().entity(httpStatus).build();
        }
    }

    @RolesAllowed({ "localhost" })
    @POST
    @Path("reporting/{cmpId}")
    public Response getReport(@PathParam("cmpId") long id, String payload) {
        return Response.ok(data).build();

        /**
        EWSResponseData<EWSReportingJobData> ewsResponseData;
        Response response = Response.ok(data).build();
        HttpStatus httpStatus = new HttpStatus();

        try {
            StoreCampaignEntity storeCampaign = databaseService.findStoreCampaignByGeminiCampaignId(id);
            StoreAcctEntity storeAcct = databaseService.findByEntityId(StoreAcctEntity.class, storeCampaign.getStoreAcctId());
            EWSAccessTokenData tokens = ewsAuthService.getAccessTokenFromRefreshToken(storeAcct.getYahooAccessToken());
            EWSClientService ews = new EWSClientService(tokens);
            ewsResponseData = ews.job(EWSReportingJobData.class, payload, EWSEndpointEnum.REPORT_JOB_SUBMISSION);
            int maxWaitInSeconds = config.getInt("", 30);
            EWSReportingJobData jobStatus = null;

            _exit: for (int i = 0; ewsResponseData.isOk() && i < maxWaitInSeconds; i++) {
                jobStatus = ewsResponseData.get(0);

                switch (jobStatus.getStatus()) {
                case completed:
                    response = Response.ok(downloadReport(jobStatus.getJobResponse())).build();
                    break _exit;
                case submitted:
                case running:
                    Thread.sleep(1000);
                    ewsResponseData = ews.get(EWSReportingJobData.class, EWSEndpointEnum.REPORT_JOB_STATUS, jobStatus.getJobId(),
                            storeAcct.getGeminiNativeAcctId());
                    break;
                case failed:
                case killed:
                    String error = "Fetching a report with campaign=" + id + ": " + jobStatus.getJobResponse();

                    log.error(error);
                    httpStatus.setStatus(-1);
                    httpStatus.setBrief(jobStatus.getStatus().toString());
                    httpStatus.setMessage(error);
                    break _exit;
                }
            }
            if (!ewsResponseData.isOk()) {
                httpStatus = ewsResponseData;
            } else if (jobStatus == null || jobStatus.getStatus() != ReportingJobStatusEnum.completed) {
                String error = "timed out after " + maxWaitInSeconds + " seconds";

                log.warn(error);
                httpStatus.setStatus(-1);
                httpStatus.setBrief(error);
            }
        } catch (Exception e) {
            String error = "Failed to fetch a report with campaign=" + id;

            log.error(error);
            httpStatus.setStatus(-1);
            httpStatus.setBrief(error);
            httpStatus.setMessage(e.getMessage());
        }

        return (httpStatus.isOk() ? response : Response.serverError().entity(httpStatus).build());
        */
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

    public static String query = "{\"cube\":\"performance_stats\",\"fields\":[{\\\"field\\\":\\\"Day\\\"},{\"field\":\"Advertiser ID\"},{\"field\":\"Campaign ID\"},{\"field\":\"Impressions\"},{\"field\":\"Clicks\"},{\"field\":\"Conversions\"},{\"field\":\"Spend\"},{\"field\":\"Average CPC\"},{\"field\":\"Average CPM\"},{\"field\":\"Source\"}],\"filters\":[{\"field\":\"Advertiser ID\",\"operator\":\"=\",\"value\":1648887},{\"field\":\"Campaign ID\",\"operator\":\"IN\",\"values\":[363525108]},{\"field\":\"Day\",\"operator\":\"between\",\"from\":\"2018-01-11\",\"to\":\"2018-01-11\"}]}";
    public static String data = "[[\"Day\",\"Advertiser ID\",\"Campaign ID\",\"Impressions\",\"Clicks\",\"Conversions\",\"Spend\",\"Average CPC\",\"Average CPM\",\"Source\"],[\"2018-01-10\",1648887,363115331,13,0,5,1.7,0,0,1],[\"2018-01-11\",1648887,363525108,15,3,27,3.14,0.0799999982,15.9999996424,1]]";
}
