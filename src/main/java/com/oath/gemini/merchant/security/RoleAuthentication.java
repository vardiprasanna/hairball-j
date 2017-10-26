package com.oath.gemini.merchant.security;

import java.io.IOException;
import java.net.InetAddress;
import java.security.Principal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @author tong on 10/1/2017
 */
@Slf4j
@Provider
@PreMatching
@Priority(Priorities.AUTHORIZATION)
public class RoleAuthentication implements ContainerRequestFilter {
    private static final Insider insider = new Insider();
    private static final Pattern regex = Pattern.compile("[?/&;,]?sig=(.*)[&;,]?$");

    private ContainerRequestContext requestContext;

    @Context
    private HttpServletRequest servletRequest;
    @Inject
    SigningService signingService;

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
            String host = servletRequest.getRemoteHost();

            try {
                switch (role) {
                case "localhost":
                    // Local call is allowed
                    InetAddress addr = InetAddress.getByName(host);

                    if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || host.equals("localhost")) {
                        return true;
                    }
                    break;

                case "YBY":
                    // Yahoo user is permitted
                    Map<String, Cookie> cookies = requestContext.getCookies();
                    boolean authorized = cookies.keySet().stream().anyMatch(k -> k.equals("YBY"));

                    if (authorized) {
                        return true;
                    }
                    break;

                case "SIG":
                    // A same remote host is allowed
                    HttpSession session = servletRequest.getSession();
                    if (session == null) {
                        break;
                    }

                    String query = servletRequest.getQueryString();
                    Matcher matcher = regex.matcher(query);
                    if (!matcher.find() || matcher.groupCount() != 1) {
                        break;
                    }

                    String clientSig = matcher.group(1);
                    Object sig = session.getAttribute("sig");
                    if (clientSig.equals(sig)) {
                        return true;
                    }
                    log.error("sig / client sig does not match from request: ", servletRequest.getRequestURI());
                    System.err.println("sig / client sig does not match from request: " + servletRequest.getRequestURI());
                    break;

                default:
                    log.error("Unknown role: ", role);
                }
            } catch (Exception e) {
                log.error("Failed to evaluate the role: {}", role, e);
                e.printStackTrace();
            }

            System.err.println("unauthorized from " + host);
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
