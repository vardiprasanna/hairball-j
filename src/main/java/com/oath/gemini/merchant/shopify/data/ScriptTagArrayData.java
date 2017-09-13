package com.oath.gemini.merchant.shopify.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScriptTagArrayData {
    @JsonProperty("script_tags")
    private Tag[] tags;
}
