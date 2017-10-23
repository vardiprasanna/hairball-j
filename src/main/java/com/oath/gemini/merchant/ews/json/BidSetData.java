package com.oath.gemini.merchant.ews.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oath.gemini.merchant.ews.EWSConstant.ChannelEnum;
import com.oath.gemini.merchant.ews.EWSConstant.PriceTypeEnum;
import lombok.Getter;
import lombok.Setter;

/**
 * @see https://developer.yahoo.com/gemini/guide/adgroup.html#bidset-object
 * @author tong on 10/1/2017
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BidSetData implements Comparable<BidSetData> {
    /**
     * The supply channel where the ads will run. Value can be:
     * 
     * SEARCH - for mobile search ads. <br/>
     * NATIVE - for native ads in native content streams.
     */
    @JsonProperty(required = true)
    private ChannelEnum channel;

    /**
     * CPC, CPM, CPV, etc
     */
    @JsonProperty(required = true)
    private PriceTypeEnum priceType;

    /**
     * The bid amount. Refer to Data Dictionary to look up currency by type.
     */
    @JsonProperty(required = true)
    private Float value;

    @Override
    public int compareTo(BidSetData o) {
        if (value == null ^ o.value == null) {
            return -1;
        }
        if (channel == null ^ o.channel == null) {
            return -1;
        }
        if (priceType == null ^ o.priceType == null) {
            return -1;
        }

        if (value != null && value.compareTo(o.value) != 0) {
            return -1;
        }
        if (channel != null && channel.compareTo(o.channel) != 0) {
            return -1;
        }
        if (priceType != null && priceType.compareTo(o.priceType) != 0) {
            return -1;
        }
        return 0;
    }
}
