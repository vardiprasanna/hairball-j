package com.oath.gemini.merchant.shopify.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonRootName("script_tags")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tag {
    private long id;

    @JsonProperty(required = true)
    private String event = "onload";

    @JsonProperty(required = true)
    private String src;
}
