package org.springframework.samples.petclinic.service.perf.jfr;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.samples.petclinic.service.perf.FakeImpl;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Sample metrics provider designed to be used with {@link SpecialtyService}
 *
 * @author Vladimir Plizga
 */
@Component
@EnableScheduling           // to enable background risks recalculation
@ConditionalOnProperty("enable-specialty")
public class MetricService {
    private static final Logger log = LoggerFactory.getLogger(MetricService.class);

    private static final int BUFFERING_THRESHOLD = 89;

    private final List<ByteBuffer> rawMetrics = new ArrayList<>();
    private boolean bufferFull = false;

    private final MeterRegistry registry;

    public MetricService(MeterRegistry registry) {
        this.registry = registry;
    }

    @Scheduled(fixedDelay = 500, timeUnit = MILLISECONDS)
    public void collectMetrics() {
        MemoryUsage memoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        long memoryUsed = memoryUsage.getUsed();
        long memoryMax = memoryUsage.getMax();
        double usedPercent = (double) memoryUsed / (double) memoryMax * 100;
        if (usedPercent <= BUFFERING_THRESHOLD) {
            var metricBuffer = gatherMetricData();
            rawMetrics.add(metricBuffer);
            if (bufferFull) {
                log.info("Continuing to collect metrics from count: {}", rawMetrics.size());
            }
            bufferFull = false;
        }
        else {
            if (!bufferFull) {
                log.info("Metrics buffer is full with %d entries (memory used: %.2f%%)"
                    .formatted(rawMetrics.size(), usedPercent));
            }
            bufferFull = true;
        }
    }

    @FakeImpl("Produces artificial data corresponding to a real metric buffer")
    private static ByteBuffer gatherMetricData() {
        return ByteBuffer.allocate(1 << 18);  // 250 KB
    }

    /**
     * Exposes metrics count as Micrometer entry named {@code metricsCount} (available through Actuator)
     */
    @EventListener(ApplicationReadyEvent.class)
    public void registerMetrics() {
        // available as http://localhost:9966/petclinic/actuator/metrics/petclinic.metrics.count
        Gauge.builder("petclinic.metrics.count", rawMetrics::size)
            .baseUnit("pcs")
            .register(registry);

        // available as http://localhost:9966/petclinic/actuator/metrics/petclinic.metrics.full
        Gauge.builder("petclinic.metrics.full", () -> bufferFull ? 1 : 0)
            .register(registry);
    }
}
