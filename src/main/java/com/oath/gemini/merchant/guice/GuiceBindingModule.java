package com.oath.gemini.merchant.guice;

import org.apache.commons.configuration.Configuration;
import com.google.inject.AbstractModule;

public class GuiceBindingModule extends AbstractModule {
    private final Configuration config;

    public GuiceBindingModule(Configuration config) {
        this.config = config;
    }

    @Override
    protected void configure() {
        bind(Configuration.class).toInstance(config);
    }
}
