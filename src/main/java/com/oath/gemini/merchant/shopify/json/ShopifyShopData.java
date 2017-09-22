package com.oath.gemini.merchant.shopify.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonTypeName("shop")
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME, visible = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShopifyShopData {
    private long id; // 690933842
    private String name; // "Apple Computers"
    private String shop_owner; // "Steve Jobs"
    private String email; // "steve@apple.com"
    private String customer_email; // "customers@apple.com"
    private String domain; // "shop.apple.com"
    private String myshopify_domain; // "apple.myshopify.com"

    private String address1; // "1 Infinite Loop"
    private String address2; // "Suite 100" private String city; // "Cupertino"
    private String city; // Cupertino
    private String province; // "California"
    private String province_code; // "CA"
    private String country; // "US"
    private String country_code; // "US"
    private String country_name; // "United States"
    private String phone; // "1231231234"

    private String primary_locale; // "en"
    private String currency; // "USD"
    private String timezone; // "(GMT-05:00) Eastern Time (US & Canada)"
}
