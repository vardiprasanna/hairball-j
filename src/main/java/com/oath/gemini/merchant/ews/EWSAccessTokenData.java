package com.oath.gemini.merchant.ews;

import org.jvnet.hk2.annotations.Optional;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oath.gemini.merchant.HttpStatus;
import lombok.Getter;

/**
 * <pre>
 *  example
 * {
 *   "access_token":"Jzxbkqqcvjqik2IMxGFEE1cuaos--",
 *   "token_type":"bearer",
 *   "expires_in":3600,
 *   "refresh_token":"AOiRUlJn_qOmByVGTmUpwcMKW3XDcipToOoHx2wRoyLgJC_RFlA-",
 *   "xoauth_yahoo_guid":"JT4FACLQZI2OCE"
 * }
 * </pre>
 * 
 * @See https://developer.yahoo.com/oauth2/guide/flows_authcode/
 * @author tong on 10/1/2017
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class EWSAccessTokenData extends HttpStatus {
    @JsonProperty("access_token")
    private String accessToken;

    @Optional @JsonProperty("refresh_token")
    private String refreshToken;
}
