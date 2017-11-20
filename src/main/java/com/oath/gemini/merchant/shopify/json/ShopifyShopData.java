package com.oath.gemini.merchant.shopify.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.Setter;

/**
 * @author tong 10/1/2017
 */
@Getter
@Setter
@JsonTypeName("shop")
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME, visible = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShopifyShopData {
    private long id; // 690933842
    private String name; // "Apple Computers"

    @JsonProperty("shop_owner")
    private String shopOwner; // "Steve Jobs"

    private String email; // "steve@apple.com"

    @JsonProperty("customer_email")
    private String customerEmail; // "customers@apple.com"

    private String domain; // "shop.apple.com"

    @JsonProperty("myshopify_domain")
    private String myshopifyDomain; // "apple.myshopify.com"

    private String address1; // "1 Infinite Loop"
    private String address2; // "Suite 100" private String city; // "Cupertino"
    private String city; // Cupertino
    private String province; // "California"

    @JsonProperty("province_code")
    private String provinceCode; // "CA"

    private String country; // "US"

    @JsonProperty("country_code")
    private String countryCode; // "US"

    @JsonProperty("country_name")
    private String countryName; // "United States"

    private String phone; // "1231231234"

    @JsonProperty("primary_locale")
    private String primaryLocale; // "en"

    private String currency; // "USD"
    private String timezone; // "(GMT-05:00) Eastern Time (US & Canada)"
}
