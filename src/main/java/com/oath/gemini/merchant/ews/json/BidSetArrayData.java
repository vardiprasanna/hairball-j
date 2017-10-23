package com.oath.gemini.merchant.ews.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import lombok.Getter;
import lombok.Setter;

/**
 * @author tong on 10/1/2017
 */
@Getter
@Setter
public class BidSetArrayData implements Comparable<BidSetArrayData> {
    @JsonProperty(required = true)
    private BidSetData[] bids;

    @Override
    public int compareTo(BidSetArrayData o) {
        if (bids == null && o.bids == null) {
            return 0;
        }
        if ((bids == null ^ o.bids == null) || (bids.length != o.bids.length)) {
            return -1;
        }

        for (BidSetData b : bids) {
            if (!Arrays.stream(o.bids).anyMatch(c -> c.compareTo(b) == 0)) {
                return -1;
            }
        }
        return 0;
    }
}
