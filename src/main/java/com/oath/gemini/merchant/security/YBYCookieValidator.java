package com.oath.gemini.merchant.security;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Map;
import javax.ws.rs.core.Cookie;
import com.oath.gemini.merchant.AppConfiguration;
import lombok.extern.slf4j.Slf4j;
import com.yahoo.bouncer.sso.CookieInfo;
import com.yahoo.bouncer.sso.CookieValidator;
import org.apache.commons.configuration.Configuration;

/**
 * @author tong on 10/1/2017
 */
@Slf4j
public class YBYCookieValidator {
    private static CookieValidator cookieValidator = new CookieValidator();
    private static String appId;
    private static String userArray;
    private static boolean initialized;
    static {
        Configuration config = AppConfiguration.getConfig();
        appId = config.getString("app.id");
        userArray = config.getString("app.users");
        try {
            cookieValidator.initialize();
            initialized = true;
        } catch (InvalidKeyException | NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
            log.warn("CookieValidator exception", e.getMessage());
            initialized = false;
        }
    }

    public boolean validateCookie(Map<String, Cookie> cookies) {
        if(initialized){
            Cookie cookie = cookies.get("YBY");
            if (cookie != null) {
                final CookieInfo cookieInfo = cookieValidator.authSig(cookie.getValue());
                if (cookieInfo != null) {
                    String[] userRoles = cookieInfo.getUserRoles();
                    boolean contains = Arrays.asList(userRoles).contains(appId);
                    String[] users = userArray.split(",");
                    return (contains && Arrays.asList(users).contains(cookieInfo.getUserId()));
                }
            }
        }
        return false;
    }
}
