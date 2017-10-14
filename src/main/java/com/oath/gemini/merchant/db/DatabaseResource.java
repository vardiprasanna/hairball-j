package com.oath.gemini.merchant.db;

import java.util.Arrays;
import java.util.List;
import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hibernate.SessionFactory;

/**
 * This service is to access and backup database
 * 
 * @author tong on 10/1/2017
 */
@Singleton
@Resource
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("YBY")
@Path("database")
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

    private <T> List<T> list(Class<T> entityClass, String id) {
        if (StringUtils.isNotBlank(id) && NumberUtils.isDigits(id)) {
            T result = databaseService.findById(entityClass, Integer.parseInt(id));
            return (result != null ? Arrays.asList(result) : null);
        }
        return databaseService.listAll(entityClass);
    }
}
