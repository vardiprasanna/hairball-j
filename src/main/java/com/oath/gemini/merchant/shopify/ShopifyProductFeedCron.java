package com.oath.gemini.merchant.shopify;

import com.oath.gemini.merchant.cron.QuartzCronAnnotation;
import com.oath.gemini.merchant.db.DatabaseService;
import com.oath.gemini.merchant.db.StoreAcctEntity;
import com.oath.gemini.merchant.db.StoreSysEntity;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@QuartzCronAnnotation(cron = "shopify.feed.cron", method = "update")
public class ShopifyProductFeedCron {
    @Inject
    DatabaseService databaseService;
    private static final ExecutorService feedExecutor = Executors.newFixedThreadPool(1);

    public void update() throws Exception {
        StoreSysEntity sysEntity = databaseService.findStoreSysByDoman("www.shopify.com");
        if (sysEntity == null) {
            log.debug("No shopify 'www.shopify.com' system entity found!");
            return;
        }

        StoreAcctEntity filter = new StoreAcctEntity();
        filter.setStoreSysId(sysEntity.getId());
        filter.setIsDeleted(false);

        List<StoreAcctEntity> accounts = databaseService.findAllByAny(filter);

        if (CollectionUtils.isEmpty(accounts)) {
            log.debug("No registered shopify store found!");
            return;
        }

        for (StoreAcctEntity a : accounts) {
            if (StringUtils.isBlank(a.getStoreAccessToken())) {
                continue;
            }
            ShopifyClientService svc = new ShopifyClientService(a.getDomain(), a.getStoreAccessToken());
            ShopifyProductSetBuilder builder = new ShopifyProductSetBuilder(svc);
            feedExecutor.submit(new FeedDeltaTask(builder));
        }
    }

    private class FeedDeltaTask implements Callable<Integer> {
        private ShopifyProductSetBuilder builder;

        public FeedDeltaTask(ShopifyProductSetBuilder builder) {
            this.builder = builder;
        }

        public Integer call() {
            try {
                return builder.uploadFeedDelta(120);
            } catch (Exception e) {
                log.error("Failed to fetch the update of products. It will be retried at next cron run");
            }
            return 0;
        }
    }
}
