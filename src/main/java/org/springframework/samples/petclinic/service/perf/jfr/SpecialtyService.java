package org.springframework.samples.petclinic.service.perf.jfr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.samples.petclinic.model.Specialty;
import org.springframework.samples.petclinic.service.perf.FakeImpl;
import org.springframework.stereotype.Service;

import static java.util.stream.Collectors.joining;

/**
 *
 * @author Vladimir Plizga
 */
@Service
@ConditionalOnProperty("enable-specialty")
public class SpecialtyService {
    private static final Logger log = LoggerFactory.getLogger(SpecialtyService.class);

    /**
     * Finds veterinarians in a (fake) external database and returns their descriptions in conjunction with
     * corresponding specialities
     */
    public Map<Specialty, String> findExternalVets(Collection<Specialty> specialties) {
        Map<Specialty, String> result = new HashMap<>();

        for (Specialty specialty : specialties) {
            String relatedInfo;
            try {
                List<String> relatedSpecs = collectAvailableDoctors();
                relatedInfo = extractRelatedInfo(relatedSpecs, specialty);
            }
            catch (Throwable e) {
                log.error("Failed to collect external vets", e);
                throw e;
            }

            if (!relatedInfo.isBlank()) {
                result.put(specialty, relatedInfo);
            }
        }

        log.info("Found {} related vets for {} specialties", result.size(), specialties.size());

        return result;
    }

    @FakeImpl("Emulates collecting of publicly available doctors")
    private List<String> collectAvailableDoctors() {
        List<String> result = new ArrayList<>();
        var dataFaker = new Faker();
        for (int i = 0; i < 1_000; i++) {
            result.add(dataFaker.text().text(5_000));
        }
        return result;
    }

    @FakeImpl("Simulates conscious selection of related doctors")
    private String extractRelatedInfo(List<String> relatedDoctors, Specialty specialty) {
        return relatedDoctors.stream()
            .filter(spec -> spec.contains("veterinarian") && spec.contains(specialty.getName()))
            .collect(joining("\n"));
    }

}
