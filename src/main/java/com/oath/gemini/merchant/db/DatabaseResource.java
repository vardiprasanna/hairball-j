package com.oath.gemini.merchant.db;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.oath.gemini.merchant.ClosableFTPClient;
import com.oath.gemini.merchant.cron.QuartzCronAnnotation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hibernate.SessionFactory;
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
@RolesAllowed("YBY")
@Path("database")
@QuartzCronAnnotation(cron = "db.backup.cron", method = "backup")
public class DatabaseResource {
    @Inject
    DatabaseService databaseService;

    @Inject
    protected SessionFactory sessionFactory;

    @GET
    @Path("acct/{id:.*}")
    public List<StoreAcctEntity> listAccounts(@PathParam("id") @DefaultValue("") String id) {
        return list(StoreAcctEntity.class, id);
    }

    @GET
    @Path("campaign/{id:.*}")
    public List<StoreCampaignEntity> listCampaigns(@PathParam("id") @DefaultValue("") String id) {
        return list(StoreCampaignEntity.class, id);
    }

    @PUT
    @Path("campaign/{id}/update")
    public Response modifyCampaign(@PathParam("id") String id, @Context HttpServletRequest req, StoreCampaignEntity userData) {
        List<StoreCampaignEntity> campaigns = list(StoreCampaignEntity.class, id);

        if (campaigns == null || campaigns.size() != 1) {
            return Response.status(Status.BAD_REQUEST).entity("{error: 'Not found or found more than one campaigns'}").build();
        }

        StoreCampaignEntity targetEntity = campaigns.get(0);
        try {
            if (DatabaseService.copyNonNullProperties(targetEntity, userData)) {
                databaseService.update(targetEntity);
            }
        } catch (Exception e) {
            log.error("failed to copy properties", e);
            return Response.status(Status.BAD_REQUEST).entity("{error: 'failed to copy properties'}").build();
        }
        return Response.ok(targetEntity).build();
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
            System.err.println("#1 failed to create a temporary database backup dir\n" + e.toString());
            log.error("failed to create a temporary database backup dir", e);
            return Response.serverError().build();
        }

        log.info("back db to temp dir {}", path);
        try {
            databaseService.backup(path.toString());
        } catch (Exception e) {
            System.err.println("#2 failed to create a temporary database backup dir\n" + e.toString());
            throw e;
        }

        try (ClosableFTPClient ftpClient = new ClosableFTPClient(); Stream<java.nio.file.Path> files = Files.list(path)) {
            files.forEach(local -> {
                try {
                    String baseName = local.getName(local.getNameCount() - 1).toString();
                    java.nio.file.Path remoteFile = Paths.get("/backup/", baseName);

                    ftpClient.copyTo(local.toString(), remoteFile.toString());
                    log.info("back '{}' to ftp backup folder", local.toString());
                    Files.delete(local);
                } catch (Exception e) {
                    System.err.println("#3 failed to create a temporary database backup dir\n" + e.toString());
                    e.printStackTrace();
                }
            });
        } finally {
            // Remove this temporary directory
            Files.delete(path);
        }

        return Response.ok().build();
    }

    private <T> List<T> list(Class<T> entityClass, String id) {
        if (StringUtils.isNotBlank(id) && NumberUtils.isDigits(id)) {
            T result = databaseService.findByEntityId(entityClass, Integer.parseInt(id));
            return (result != null ? Arrays.asList(result) : null);
        }
        return databaseService.listAll(entityClass);
    }
}
