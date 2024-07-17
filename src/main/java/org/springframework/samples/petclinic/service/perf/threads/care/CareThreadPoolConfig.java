package org.springframework.samples.petclinic.service.perf.threads.care;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A provider of thread pool for executing tasks related to care tips
 *
 * @author Vladimir Plizga
 */
@Configuration
public class CareThreadPoolConfig {

    @Bean
    public ExecutorService careTipsThreadPool() {
        int nThreads = Runtime.getRuntime().availableProcessors();
        return Executors.newFixedThreadPool(nThreads);
    }
}
