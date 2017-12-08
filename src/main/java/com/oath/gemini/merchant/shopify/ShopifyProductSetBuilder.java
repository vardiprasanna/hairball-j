package com.oath.gemini.merchant.shopify;

import com.oath.gemini.merchant.ClosableFTPClient;
import com.oath.gemini.merchant.ews.EWSConstant.PrdAvailabilityEnum;
import com.oath.gemini.merchant.ews.json.ProductRecordData;
import com.oath.gemini.merchant.shopify.json.ShopifyProductData;
import com.oath.gemini.merchant.shopify.json.ShopifyProductImageData;
import com.oath.gemini.merchant.shopify.json.ShopifyProductVariantData;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.ArrayUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Build a product listing from calling Shopify prouduct services
 * 
 * @author tong on 10/1/2017
 */
@Slf4j
public class ShopifyProductSetBuilder {
    private static final Object[] CSV_HEADER = { "id", "title", "description", "image_link", "link", "availability", "condition", "price",
            "gtin", "mpn", "brand", "product_type" };

    private ShopifyClientService svc;
    private String localFile;
    private String remoteFile;
    private String productsRootUrl;
    private static final ExecutorService feedExecutor = Executors.newFixedThreadPool(1);

    public ShopifyProductSetBuilder(ShopifyClientService svc) {
        this.svc = svc;

        String baseName = svc.getShopName();
        localFile = baseName + ".csv";
        remoteFile = "/shopify/" + baseName + ".csv";

        // Build the root URL of all products
        productsRootUrl = svc.getShop().toLowerCase();

        if (!productsRootUrl.matches("^(http|https)://.*")) {
            productsRootUrl = "https://" + productsRootUrl;
        }

        int pathStart = productsRootUrl.indexOf('/', 8);
        if (pathStart > 0) {
            productsRootUrl = productsRootUrl.substring(0, pathStart);
        }
        productsRootUrl += "/products/";
    }

    /**
     * Produce a Gemini product feed if it has never been done before for this shopper. Since the feed could be huge, so run
     * it in background such that it will not interfere the onboarding of the Shopify app
     */
    public String uploadFeedIfRequired() throws Exception {
        feedExecutor.submit(new Callable<Boolean>() {
            public Boolean call() {
                try {
                    uploadFeedTask();
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });
        return remoteFile;
    }

    private void uploadFeedTask() throws Exception {
        // Download products from Shopify and then upload the result to FTP server for Gemini
        try (ClosableFTPClient ftpClient = new ClosableFTPClient()) {
            if (!ftpClient.exits(remoteFile)) {
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

                            if (ArrayUtils.isEmpty(variants)) {
                                log.warn("shopify product {} is missing of variant objects", p.getId());
                                continue;
                            }
                            if (ArrayUtils.isEmpty(images)) {
                                log.warn("shopify product {} is missing of image objects", p.getId());
                                continue;
                            }

                            p.setVariants(variants);
                            p.setImages(images);

                            geminiProduct.setId(p.getId());
                            geminiProduct.setTitle(p.getTitle());
                            geminiProduct.setDescription(p.getDescription());
                            geminiProduct.setImage_link(images[0].getSrc());
                            geminiProduct.setLink(productsRootUrl + p.getHandle());
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

                // Copy a local file to FTP server
                ftpClient.copyTo(localFile, remoteFile);
            }
        }
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
