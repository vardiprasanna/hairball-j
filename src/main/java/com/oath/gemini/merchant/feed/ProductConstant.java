package com.oath.gemini.merchant.feed;

public class ProductConstant {
    public enum AgeGroup {
        newborn, infant, toddler, kids, adult;
    }

    public enum Availability {
        IN_STOCK("in stock"), OUT_OF_STOCK("out of stock"), PREORDER("preorder"), AVAILABLE_FOR_ORDER("available for order");
        private String value;

        Availability(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public enum ConditionEnum {
        NEW, REFURBISHED, USED;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    public enum Gender {
        male, female, unisex;
    }

    public enum FeedFrequencyEnum {
        DAILY, WEEKLY, MONTHLY;
    }

    public enum FeedTypeEnum {
        DPA_ONE_TIME, DPA_RECURRING;
    }
}
