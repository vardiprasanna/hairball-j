package com.oath.gemini.merchant;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.Map;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import org.apache.commons.lang3.StringUtils;

/**
 * @author tong on 10/1/2017
 */
@Provider
@PreMatching
@Priority(Priorities.AUTHORIZATION)
public class RoleAuthentication implements ContainerRequestFilter {
    private static final Insider insider = new Insider();
    private ContainerRequestContext requestContext;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        this.requestContext = requestContext;
        requestContext.setSecurityContext(new InsiderContext());
    }

    private class InsiderContext implements SecurityContext {
        @Override
        public Principal getUserPrincipal() {
            return insider;
        }

        @Override
        public boolean isUserInRole(String role) {
            String host = requestContext.getUriInfo().getRequestUri().getHost();

            // Local call is allowed
            try {
                InetAddress addr = InetAddress.getByName(host);
                if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()) {
                    return true;
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            // Yahoo user is permitted
            Map<String, Cookie> cookies = requestContext.getCookies();
            if (cookies != null) {
                for (Map.Entry<String, Cookie> entry : cookies.entrySet()) {
                    System.out.println(entry.getKey());
                }
                return cookies.keySet().stream().anyMatch(k -> k.equals("YBY"));
            }
            return false;
        }

        @Override
        public boolean isSecure() {
            String scheme = requestContext.getUriInfo().getRequestUri().getScheme();
            return (StringUtils.equalsIgnoreCase("https", scheme));
        }

        @Override
        public String getAuthenticationScheme() {
            return "custom";
        }
    }

    private static class Insider implements Principal {
        @Override
        public String getName() {
            return "insider";
        }
    }
}
