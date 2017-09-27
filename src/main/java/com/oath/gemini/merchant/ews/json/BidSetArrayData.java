package com.oath.gemini.merchant.ews.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BidSetArrayData {
    @JsonProperty(required = true)
    private BidSetData[] bids;
}