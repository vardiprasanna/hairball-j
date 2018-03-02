package com.oath.gemini.merchant.security;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Map;
import javax.ws.rs.core.Cookie;
import lombok.extern.slf4j.Slf4j;
import com.yahoo.bouncer.sso.CookieInfo;
import com.yahoo.bouncer.sso.CookieValidator;

/**
 * @author tong on 10/1/2017
 */
@Slf4j
public class YBYCookieValidator {
    private static CookieValidator cookieValidator = new CookieValidator();

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
                boolean contains = Arrays.asList(userRoles).contains("121.U");
                return (contains && cookieInfo.getUserId().equals("pvardi"));
            }
        }
        return false;
    }
}
