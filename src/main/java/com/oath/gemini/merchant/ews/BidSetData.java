package com.oath.gemini.merchant.ews;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @see https://developer.yahoo.com/gemini/guide/adgroup.html#bidset-object
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BidSetData {
    /**
     * The supply channel where the ads will run. Value can be:
     * 
     * SEARCH - for mobile search ads. <br/>
     * NATIVE - for native ads in native content streams.
     */
    @JsonProperty(required = true)
    private EWSConstant.ChannelEnum channel;

    /**
     * CPC, CPM, CPV, etc
     */
    @JsonProperty(required = true)
    private EWSConstant.PriceTypeEnum priceType;

    /**
     * The bid amount. Refer to Data Dictionary to look up currency by type.
     */
    @JsonProperty(required = true)
    private float value;
}
