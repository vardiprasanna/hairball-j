package com.oath.gemini.merchant.shopify;

import com.oath.gemini.merchant.ClosableFTPClient;
import com.oath.gemini.merchant.ews.EWSClientService;
import com.oath.gemini.merchant.ews.EWSEndpointEnum;
import com.oath.gemini.merchant.ews.EWSResponseData;
import com.oath.gemini.merchant.feed.Archetype;
import com.oath.gemini.merchant.feed.ProductConstant;
import com.oath.gemini.merchant.feed.ProductConstant.FeedTypeEnum;
import com.oath.gemini.merchant.feed.ProductFeedData;
import com.oath.gemini.merchant.feed.ProductRecordData;
import com.oath.gemini.merchant.shopify.data.ShopifyProductData;
import com.oath.gemini.merchant.shopify.data.ShopifyProductImageData;
import com.oath.gemini.merchant.shopify.data.ShopifyProductVariantData;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import lombok.extern.slf4j.Slf4j;

/**
 * Build a product listing from calling Shopify prouduct services
 */
@Slf4j
public class ProductListingBuilder {
    private static final Object[] CSV_HEADER = { "id", "title", "description", "image_link", "link", "availability", "condition", "price",
            "mpn" };

    private ShopifyClientService svc;
    private EWSClientService ews;

    public ProductListingBuilder(ShopifyClientService svc, EWSClientService ews) {
        this.svc = svc;
        this.ews = ews;
    }

    /**
     * Produce a Gemini product feed if it has never been done before for this shopper
     * 
     * TODO: check whether the shop's feed has ever been produced
     */
    public ProductFeedData upload() throws Exception {
        ShopifyProductData[] products = svc.get(ShopifyProductData[].class, ShopifyEndpointEnum.SHOPIFY_PROD_ALL);
        List<ProductRecordData> geminiProducts = new ArrayList<>();

        if (products != null) {
            CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");

            try (FileWriter csvWriter = new FileWriter("shopify-test.csv", false);
                    CSVPrinter csvFilePrinter = new CSVPrinter(csvWriter, csvFileFormat)) {

                // Create CSV file header
                csvFilePrinter.printRecord(CSV_HEADER);

                ShopifyProductVariantData[] variants;
                ShopifyProductImageData[] images;
                ProductRecordData geminiProduct = new ProductRecordData();

                for (ShopifyProductData p : products) {
                    variants = svc.get(ShopifyProductVariantData[].class, ShopifyEndpointEnum.SHOPIFY_PROD_VARIANTS, p.getId());
                    images = svc.get(ShopifyProductImageData[].class, ShopifyEndpointEnum.SHOPIFY_PROD_IMAGES, p.getId());

                    if (isEmpty(variants) || isEmpty(variants)) {
                        log.warn("shopify product {} is missing variant and/or images", p.getId());
                        continue;
                    }

                    p.setVariants(variants);
                    p.setImages(images);

                    geminiProduct.setId(p.getId());
                    geminiProduct.setTitle(p.getTitle());
                    geminiProduct.setDescription(p.getDescription());
                    geminiProduct.setImage_link(images[0].getSrc());
                    geminiProduct.setLink(svc.getShop());
                    geminiProduct.setAvailability(ProductConstant.Availability.IN_STOCK);
                    geminiProduct.setPrice(Float.toString(variants[0].getPrice()));
                    geminiProduct.setMpn(variants[0].getSku());
                    geminiProduct.setGtin(variants[0].getBarcode());

                    // Output one product
                    csvFilePrinter.printRecord(toRecordArray(geminiProduct));
                    geminiProducts.add(geminiProduct);
                }
            } catch (Exception e) {
                log.error("Failed to generate a shopify product feed", e);
                throw e;
            }
        }

        try (ClosableFTPClient ftpClient = new ClosableFTPClient()) {
            ftpClient.copyTo("shopify-test.csv", "/shopify/dpa-bridge.csv");
        }

        ProductFeedData feedData = new ProductFeedData();
        Archetype archeType = new Archetype(ews);

        feedData.setAdvertiserId(archeType.getAdvertiserId());
        feedData.setUserName(ClosableFTPClient.username);
        feedData.setPassword(ClosableFTPClient.password);
        feedData.setFeedType(FeedTypeEnum.DPA_ONE_TIME);
        feedData.setFileName("shopify-test.csv");
        feedData.setFeedUrl(ClosableFTPClient.host);

        EWSResponseData<ProductFeedData> response = ews.create(ProductFeedData.class, feedData, EWSEndpointEnum.PRODUCT_FEED);
        if (response != null && response.isOk()) {
            archeType.create(geminiProducts);
            return response.getObjects()[0];
        }
        return null;
    }

    private static <T> boolean isEmpty(T[] array) {
        return array == null || array.length == 0;
    }

    private static Object[] toRecordArray(ProductRecordData geminiProduct) throws Exception {
        String[] record = new String[CSV_HEADER.length];
        Field f;
        Object v;

        for (int i = 0; i < CSV_HEADER.length; i++) {
            if ("description".equals(CSV_HEADER[i])) {
                record[i] = "description placeholder";
            } else {
                f = geminiProduct.getClass().getDeclaredField((String) CSV_HEADER[i]);
                f.setAccessible(true);
                v = f.get(geminiProduct);
                record[i] = (v != null) ? v.toString() : null;
            }
        }
        return record;
    }
}
