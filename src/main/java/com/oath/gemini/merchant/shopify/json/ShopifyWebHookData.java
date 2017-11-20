package com.oath.gemini.merchant.shopify.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.Setter;

/**
 * @author tong 10/1/2017
 */
@JsonTypeName(value = "webhook")
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShopifyWebHookData extends WebHook {
}

@Getter
@Setter
class WebHook {
    private String topic;
    private String address;
    private String format = "json";
}