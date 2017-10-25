package com.oath.gemini.merchant;

import com.oath.gemini.merchant.security.RoleAuthentication;
import java.lang.reflect.Method;
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
        Class<?> clazz = resourceInfo.getResourceClass();
        String cname = clazz.getName();

        if (cname.startsWith("com.oath.gemini.merchant")) {
            if (clazz.isAnnotationPresent(RolesAllowed.class)) {
                context.register(RoleAuthentication.class);
            } else {
                Method method = resourceInfo.getResourceMethod();
                if (method.isAnnotationPresent(RolesAllowed.class)) {
                    context.register(RoleAuthentication.class);
                }
            }
        }
    }
}
