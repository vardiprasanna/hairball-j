package com.oath.gemini.merchant.shopify.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopifyAccessToken {
    // is an API access token that can be used to access the shop’s data as long as the client is installed. Clients
    // should store the token somewhere to make authenticated requests for a shop’s data. For example:
    // "f85632530bf277ec9ac6f649fc327f17"
    @JsonProperty("access_token")
    String accessToken;

    // is the list of access scope that were granted to the application and are associated with the access token. Due to
    // the nature of OAuth, it's always possible for a merchant to change the requested scope in the URL during the
    // authorize phase, so the application should ensure that all required scopes are granted before using the access
    // token. For example: "write_orders,read_customers"
    String scope;
}
