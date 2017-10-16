package com.oath.gemini.merchant.cron;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.inject.Inject;
import javax.ws.rs.core.FeatureContext;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.InjecteeImpl;
import org.glassfish.jersey.ServiceLocatorProvider;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @author tong on 10/1/2017
 */
@Slf4j
public class QuartzJobber implements Job {
    @Override
    public void execute(JobExecutionContext execContext) throws JobExecutionException {
        JobDataMap dataMap = execContext.getMergedJobDataMap();
        String methodName = (String) dataMap.get("method");
        Object targetInstance = execContext.get("targetInstance");

        // Save the time of re-instantiation if a job is created as a singleton
        if (targetInstance == null) {
            targetInstance = create((Class<?>) dataMap.get("target"));
            if (targetInstance == null) {
                return;
            }
            execContext.put("targetInstance", targetInstance);
        }

        try {
            Method targetMethod = targetInstance.getClass().getMethod(methodName);
            targetMethod.invoke(targetInstance);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            log.error("fail to execute a target job: " + targetInstance.getClass().getName(), e);
            return;
        }

        log.debug("successfully invoke a target job: " + targetInstance.getClass().getName());
    }

    /**
     * Create a new target job, and inject dependencies
     */
    private Object create(Class<?> target) {
        try {
            return inject(target.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("fail to instantiate a target job class: " + target.getName(), e);
            return null;
        }
    }

    /**
     * Inject fields that are annotated with inject type
     */
    private Object inject(Object instance) {
        final FeatureContext context = QuartzFeature.getFeatureContext();
        final ServiceLocator serviceLocator = ServiceLocatorProvider.getServiceLocator(context);

        if (serviceLocator == null) {
            log.error("fail to fetch a service locator for: " + context);
            return instance;
        }

        for (Field field : instance.getClass().getDeclaredFields()) {
            if (field.getAnnotation(Inject.class) != null) {
                InjecteeImpl injectee = new InjecteeImpl(field.getType());
                ActiveDescriptor<?> desc = serviceLocator.getInjecteeDescriptor(injectee);

                if (desc != null) {
                    ServiceHandle<?> handler = serviceLocator.getServiceHandle(desc);

                    if (handler != null) {
                        Object svc = handler.getService();
                        if (field.getType().isInstance(svc)) {
                            try {
                                field.setAccessible(true);
                                field.set(instance, svc);
                            } catch (IllegalArgumentException | IllegalAccessException e) {
                                log.error("fail to set a field '" + field + "' with object " + svc);
                            }
                        }
                    } else {
                        log.error("fail to fetch a service handler for a descriptpr: " + desc);
                    }
                }
            }
        }

        return instance;
    }
}
