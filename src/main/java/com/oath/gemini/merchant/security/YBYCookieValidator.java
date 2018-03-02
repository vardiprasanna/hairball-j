package com.oath.gemini.merchant.security;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.core.Cookie;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yahoo.bouncer.sso.CookieInfo;
import com.yahoo.bouncer.sso.CookieValidator;

public class YBYCookieValidator {
    private static final Logger LOG = LoggerFactory.getLogger(YBYCookieValidator.class);
    @Inject
    private Configuration config;
    @Inject
    CookieValidator cookieValidator;
    private static final String NEED_COOKIE_CHECK = "ybycookie.validate";

    public boolean validateCookie(Map<String, Cookie> cookies) {
        System.out.println("Validate Cookie is called");
        if (!cookies.keySet().stream().anyMatch(k -> k.equals("YBY"))) {
            return  false;
        }
        final String cookieValue = cookies.get("YBY").toString();

        if (cookies != null) {
            try {
                cookieValidator.initialize();
            } catch (InvalidKeyException | NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
                LOG.warn("CookieValidator exception", e.getMessage());
                return  false;
            }
        }
        final CookieInfo cookieInfo = cookieValidator.authSig(cookieValue);
        if (cookieInfo != null) {
            String[] userRoles = cookieInfo.getUserRoles();
            boolean contains = Arrays.asList(userRoles).contains("121.U");
            if (cookieInfo.getUserId().equals("pvardi") && contains) {
                return false;
            } else{
                return false;
            }

        } else {
            LOG.warn("Bouncer authentication failed");
            return false;
        }
    }
}
