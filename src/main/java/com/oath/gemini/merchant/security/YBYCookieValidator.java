package com.oath.gemini.merchant.security;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.GregorianCalendar;
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
    private static String AppId;
    private static String userArray;
    static {
        Configuration config = AppConfiguration.getConfig();
        AppId = config.getString("app.id");
        userArray = config.getString("app.users");
    }

    static boolean init() {
        try {
            cookieValidator.initialize();
        } catch (InvalidKeyException | NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
            log.warn("CookieValidator exception", e.getMessage());
            return false;
        }
        return true;
    }

    public boolean validateCookie(Map<String, Cookie> cookies) {

        Cookie cookie = cookies.get("YBY");
        if (cookie != null) {
            final CookieInfo cookieInfo = cookieValidator.authSig(cookie.getValue());
            if (cookieInfo != null) {
                String[] userRoles = cookieInfo.getUserRoles();
                boolean contains = Arrays.asList(userRoles).contains(AppId);
                String[] users = userArray.split(",");
                return (contains && Arrays.asList(users).contains(cookieInfo.getUserId()));
            }
        }
        return false;
    }
}
