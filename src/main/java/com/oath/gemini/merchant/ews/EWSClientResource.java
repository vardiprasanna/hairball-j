package com.oath.gemini.merchant.ews;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.oath.gemini.merchant.ClosableHttpClient;
import com.oath.gemini.merchant.db.DatabaseService;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;

/**
 * It is an internal service for UI
 * 
 * @author tong on 1/1/2018
 */
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
    @Path("campaign/{id:.*}")
    public Response getCampaign() {
        return Response.serverError().entity("A placeholder").build();
    }

    @RolesAllowed({ "localhost" })
    @POST
    @Path("reporting")
    public Response report(@Context HttpServletRequest req, String query) {
        EWSResponseData<EWSReportingJobData> ewsResponseData;
        Response response = Response.ok(data).build();

        /** TODO
        try {
            EWSAccessTokenData tokens = null; // ewsAuthService.getAccessTokenFromRefreshToken(storeAcct.getYahooAccessToken());
            EWSClientService ews = new EWSClientService(tokens);
            ewsResponseData = ews.job(EWSReportingJobData.class, query, EWSEndpointEnum.REPORT_JOB_SUBMISSION);
            int maxWaitInSeconds = config.getInt("", 30);

            _exit: for (int i = 0; ewsResponseData.isOk() && i < maxWaitInSeconds; i++) {
                EWSReportingJobData jobStatus = ewsResponseData.get(0);

                switch (jobStatus.getStatus()) {
                case completed:
                    response = Response.ok(downloadReport(jobStatus.getJobResponse())).build();
                    break _exit;
                case submitted:
                case running:
                    Thread.sleep(1000);
                    ewsResponseData = ews.get(EWSReportingJobData.class, EWSEndpointEnum.REPORT_JOB_STATUS, jobStatus.getJobId(), 1648887);
                    break;
                case failed:
                case killed:
                    response = Response.serverError().entity("The report fetching is " + jobStatus.getStatus().toString()).build();
                    break _exit;
                }
            }
            if (!ewsResponseData.isOk()) {
                response = Response.serverError().entity(ewsResponseData.getMessage()).build();
            }
        } catch (Exception e) {
            response = Response.serverError().entity(e.getMessage()).build();
        }
        */

        return response;
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
    public static String data = "[[\"Advertiser ID\",\"Campaign ID\",\"Impressions\",\"Clicks\",\"Conversions\",\"Spend\",\"Average CPC\",\"Average CPM\",\"Source\"],[\"2017-12-10\",1648887,363115331,13,0,5,0,0,0,1],[\"2017-12-11\",1648887,363525108,15,3,27,0.2399999946,0.0799999982,15.9999996424,1]]";
}
