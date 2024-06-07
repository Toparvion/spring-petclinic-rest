package org.springframework.samples.petclinic.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 *
 * @author Vladimir Plizga
 */
@Configuration
@EnableScheduling           // to enable background risks recalculation
public class RisksAiConfig {

    @Bean
    public ExecutorService careRecommendationsThreadPool() {
        return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2);
    }
}
