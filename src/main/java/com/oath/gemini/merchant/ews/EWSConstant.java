package com.oath.gemini.merchant.ews;

public class EWSConstant {
    /**
     * The status field is reserved for mutable entity state and user transitions. Note that the REJECTED value here is set
     * by the system editorial review. See the diagram below for an illustration of how this works. See also the ad level
     * statuses table below for more details on this field.
     */
    public enum StatusEnum {
        ACTIVE, PAUSED, DELETED, EXPIRED, REJECTED
    }

    /////////////////////////////////////////////////////////////////
    // Begin of Ad specific//////////////////////////////////////////
    /////////////////////////////////////////////////////////////////
    public enum EditorialStatusEnum {
        NOT_REVIEWED, PENDING_REVIEW, APPROVED, REJECTED
    }

    public enum AdAssetTypeEnum {
        MULTI_IMAGE
    }

    /////////////////////////////////////////////////////////////////
    // Begin of AdGroup specific ////////////////////////////////////
    /////////////////////////////////////////////////////////////////
    public enum BiddingStrategyEnum {
        OPT_CONVERSION, DEFAULT
    }

    /**
     * The pricing type. Supported types are:
     * 
     * CPC - only if campaign objective is VISIT_WEB or REENGAGE_APP <br/>
     * CPM - only if campaign objective is PROMOTE_BRAND <br/>
     * CPC - only for video ads in INSTALL_APP campaigns <br/>
     * CPM, CPV - only for video ads in PROMOTE_BRAND campaigns
     */
    public enum PriceTypeEnum {
        CPC, CPM, CPV
    }

    /////////////////////////////////////////////////////////////////
    // Begin of Campaign specific ///////////////////////////////////
    /////////////////////////////////////////////////////////////////
    public enum EffectiveStatusEnum {
        ACTIVE, PAUSED, DELETED, ENDED, LIFETIME_BUDGET_SPENT, AWAITING_FUNDS, AWAITING_START_DATE
    }

    public enum ObjectiveEnum {
        VISIT_WEB, VISIT_OFFER, PROMOTE_BRAND, INSTALL_APP, REENGAGE_APP
    }

    public enum BudgetTypeEnum {
        LIFETIME, DAILY
    }

    public enum ChannelEnum {
        NATIVE, SEARCH, SEARCH_AND_NATIVE
    }

    public enum SubChannelEnum {
        SRN_AND_SEARCHl, SRN_ONLY, DEFAULT
    }

    /////////////////////////////////////////////////////////////////
    // Begin of Campaign and AdGroup specific ///////////////////////
    /////////////////////////////////////////////////////////////////
    public enum AdvancedGeoPosEnum {
        LOCATION_OF_PRESENCE, LOCATION_OF_INTEREST, DEFAULT
    }

    public enum AdvancedGeoNegEnum {
        LOCATION_OF_PRESENCE, DEFAULT
    }

    /////////////////////////////////////////////////////////////////
    // Product and Feed specific ////////////////////////////////////
    /////////////////////////////////////////////////////////////////
    public enum PrdAgeGroupEnum {
        newborn, infant, toddler, kids, adult;
    }

    public enum PrdAvailabilityEnum {
        IN_STOCK("in stock"), OUT_OF_STOCK("out of stock"), PREORDER("preorder"), AVAILABLE_FOR_ORDER("available for order");
        private String value;

        PrdAvailabilityEnum(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public enum PrdConditionEnum {
        NEW, REFURBISHED, USED;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    public enum PrdGenderNum {
        male, female, unisex;
    }

    public enum PrdFeedFrequencyEnum {
        DAILY, WEEKLY, MONTHLY;
    }

    public enum PrdFeedTypeEnum {
        DPA_ONE_TIME, DPA_RECURRING;
    }
}
