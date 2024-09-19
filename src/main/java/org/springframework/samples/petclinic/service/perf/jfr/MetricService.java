package org.springframework.samples.petclinic.service.perf.jfr;

import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.samples.petclinic.service.perf.FakeImpl;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

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

    private static final int BUFFERING_THRESHOLD = 90;

    private final List<ByteBuffer> rawMetrics = new ArrayList<>();

    private final MeterRegistry registry;

    public MetricService(MeterRegistry registry) {
        this.registry = registry;
    }

    @Scheduled(fixedDelay = 500, timeUnit = MILLISECONDS)
    public void collectMetrics() {
        long memoryUsed = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        long memoryMax = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();
        double usedPercent = (double) memoryUsed / (double) memoryMax * 100;
        if (usedPercent <= BUFFERING_THRESHOLD) {
            var metricBuffer = gatherMetricData();
            rawMetrics.add(metricBuffer);
            log.debug("Collected new portion of metrics. Current number of entries: {}", rawMetrics.size());
        }
        else {
            log.debug("Metrics buffer is not updated due high memory consumption: %.2f%%".formatted(usedPercent));
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
        Gauge.builder("metricsCount", rawMetrics::size)
            .baseUnit("pcs")
            .register(registry);
    }
}
