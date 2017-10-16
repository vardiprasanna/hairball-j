package com.oath.gemini.merchant.cron;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import javax.inject.Inject;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.sf.corn.cps.CPScanner;
import net.sf.corn.cps.ClassFilter;

/**
 * @author tong on 10/1/2017
 */
@Slf4j
public class QuartzFeature implements Feature {
    @Inject
    private Configuration config;

    @Getter
    private static FeatureContext featureContext;

    public boolean configure(final FeatureContext context) {
        Map<String, Class<?>> jobClasses = scanJobClasses();
        boolean hasJob = false;

        if (jobClasses == null || jobClasses.isEmpty()) {
            return false;
        }

        // load properties
        Properties properties = new Properties();
        for (Iterator<String> keys = config.getKeys("org.quartz"); keys.hasNext();) {
            String key = keys.next();
            properties.setProperty(key, config.getString(key));
        }

        try {
            SchedulerFactory schFactory = new StdSchedulerFactory(properties);
            Scheduler sch = schFactory.getScheduler();

            for (Map.Entry<String, Class<?>> jc : jobClasses.entrySet()) {
                try {
                    JobDetail jobDetail = buildJob(jc.getValue());

                    sch.deleteJob(jobDetail.getKey());
                    sch.scheduleJob(jobDetail, buildCronTrigger(jc.getKey()));
                    hasJob = true;
                } catch (NoSuchMethodException | SecurityException e) {
                    log.error("fail to schedule this job class: " + jc.getValue().getName(), e);
                }
            }
            if (hasJob) {
                sch.start();
                QuartzFeature.featureContext = context;
            }

        } catch (SchedulerException e) {
            log.error("unable to start Quartz", e);
        }

        return hasJob;
    }

    /**
     * Return pairs of a cron expression property name and a job class
     */
    private Map<String, Class<?>> scanJobClasses() {
        Map<String, Class<?>> cronJobClasses = new HashMap<>();

        ClassFilter filter = new ClassFilter();
        filter.packageName("com.oath.gemini.merchant.*");
        filter.annotation(QuartzCronAnnotation.class);

        for (Class<?> jc : CPScanner.scanClasses(filter)) {
            String name = jc.getAnnotation(QuartzCronAnnotation.class).cron();
            cronJobClasses.put(name, jc);
        }

        return cronJobClasses;
    }

    /**
     * Build a cron trigger
     */
    private Trigger buildCronTrigger(String expression) {
        String cron = config.getString(expression);

        CronScheduleBuilder scheduler = CronScheduleBuilder.cronSchedule(cron);
        TriggerBuilder<CronTrigger> builder = newTrigger().withSchedule(scheduler);
        Trigger trigger = builder.build();

        return trigger;
    }

    /**
     * Build a cron job
     * 
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    private JobDetail buildJob(Class<?> target) throws NoSuchMethodException, SecurityException {
        String methodName = target.getAnnotation(QuartzCronAnnotation.class).method();

        // use a default method name if none is specified
        if (StringUtils.isBlank(methodName)) {
            methodName = "execute";
        }

        if (target.getMethod(methodName) != null) {
            log.info("Found a job entry method=" + methodName);
        }

        JobDataMap dataMap = new JobDataMap();
        dataMap.put("target", target);
        dataMap.put("method", methodName);

        return newJob(QuartzJobber.class).withIdentity(target.getName()).setJobData(dataMap).build();
    }
}
