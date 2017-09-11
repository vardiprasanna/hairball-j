package com.oath.gemini.merchant.shopify.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ShopifyTokenRequest {
    // The API Key for the app (see the credentials section of this guide).
    @JsonProperty("client_id")
    String clientId;

    // The Secret Key for the app (see the credentials section of this guide).
    @JsonProperty("client_secret")
    String clientSecret;

    // The authorization code provided in the redirect described above.
    String code;
}
