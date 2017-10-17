package com.oath.gemini.merchant;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

/**
 * To enforce a role-based access
 * 
 * @author tong on 10/1/2017
 */
public class AppFeatureBinder implements DynamicFeature {
    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        if (resourceInfo.getResourceClass().isAnnotationPresent(RolesAllowed.class)) {
            context.register(RoleAuthentication.class);
        }
    }
}
