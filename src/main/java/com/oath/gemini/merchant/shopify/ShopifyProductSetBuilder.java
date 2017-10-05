package com.oath.gemini.merchant.shopify;

import com.oath.gemini.merchant.Archetype;
import com.oath.gemini.merchant.ClosableFTPClient;
import com.oath.gemini.merchant.ews.EWSClientService;
import com.oath.gemini.merchant.ews.EWSConstant.PrdAvailabilityEnum;
import com.oath.gemini.merchant.ews.EWSConstant.PrdFeedTypeEnum;
import com.oath.gemini.merchant.ews.EWSEndpointEnum;
import com.oath.gemini.merchant.ews.EWSResponseData;
import com.oath.gemini.merchant.ews.json.ProductFeedData;
import com.oath.gemini.merchant.ews.json.ProductRecordData;
import com.oath.gemini.merchant.shopify.json.ShopifyProductData;
import com.oath.gemini.merchant.shopify.json.ShopifyProductImageData;
import com.oath.gemini.merchant.shopify.json.ShopifyProductVariantData;
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
public class ShopifyProductSetBuilder {
    private static final Object[] CSV_HEADER = { "id", "title", "description", "image_link", "link", "availability", "condition", "price",
            "gtin", "mpn", "brand", "product_type" };

    private ShopifyClientService svc;
    private EWSClientService ews;
    private String localFile;
    private String remoteFile;

    public ShopifyProductSetBuilder(ShopifyClientService svc, EWSClientService ews) {
        this.svc = svc;
        this.ews = ews;

        String baseName = svc.getShopName();
        localFile = baseName + ".csv";
        remoteFile = "/shopify/" + baseName + ".csv";
    }

    /**
     * Upload a feed only if no feed exists
     */
    public ProductFeedData uploadOnce() throws Exception {
        Archetype archeType = new Archetype(svc, ews);
        EWSResponseData<ProductFeedData> response;

        response = ews.get(ProductFeedData.class, EWSEndpointEnum.PRODUCT_FEED_BY_ADVERTISER, archeType.getAdvertiserId());
        if (response != null && response.isOk()) {
            archeType.create();
            return response.get(0);
        }
        return null; // upload();
    }

    /**
     * Produce a Gemini product feed if it has never been done before for this shopper
     */
    public ProductFeedData upload() throws Exception {
        ShopifyProductData[] products = svc.get(ShopifyProductData[].class, ShopifyEndpointEnum.SHOPIFY_PROD_ALL);
        List<ProductRecordData> geminiProducts = new ArrayList<>();

        if (products != null) {
            CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");

            try (FileWriter csvWriter = new FileWriter(localFile, false);
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
                    geminiProduct.setAvailability(PrdAvailabilityEnum.IN_STOCK);
                    geminiProduct.setPrice(Float.toString(variants[0].getPrice()));
                    geminiProduct.setMpn(variants[0].getSku());
                    geminiProduct.setGtin(variants[0].getBarcode());
                    geminiProduct.setBrand(p.getBrand());
                    geminiProduct.setProduct_type(p.getProduct_type());

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
            ftpClient.copyTo(localFile, remoteFile);
        }

        ProductFeedData feedData = new ProductFeedData();
        Archetype archeType = new Archetype(svc, ews);

        feedData.setAdvertiserId(archeType.getAdvertiserId());
        feedData.setUserName(ClosableFTPClient.username);
        feedData.setPassword(ClosableFTPClient.password);
        feedData.setFeedType(PrdFeedTypeEnum.DPA_RECURRING);
        feedData.setFileName(remoteFile);
        feedData.setFeedUrl("ftp://" + ClosableFTPClient.host);

        EWSResponseData<ProductFeedData> response = ews.create(ProductFeedData.class, feedData, EWSEndpointEnum.PRODUCT_FEED);
        if (response != null && response.isOk()) {
            archeType.create();
            return response.get(0);
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
