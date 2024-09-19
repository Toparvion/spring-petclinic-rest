package org.springframework.samples.petclinic.service.perf.memory.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.lang.Nullable;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.service.ClinicService;
import org.springframework.samples.petclinic.service.perf.memory.ai.AiConversation.Summary;
import org.springframework.stereotype.Service;

/**
 *
 * @author Vladimir Plizga
 */
@Service
@EnableCaching
@ConditionalOnProperty("enable-diagnostics")
public class DiagnosisService {
    private static final Logger log = LoggerFactory.getLogger(DiagnosisService.class);

    private final ClinicService clinicService;

    @Autowired
    public DiagnosisService(ClinicService clinicService) {
        this.clinicService = clinicService;
    }

    @Nullable
    @Cacheable("summaries")         // implicitly creates a simple cache based on ConcurrentHashMap
    public Summary diagnoseWithAi(int petId, String symptoms) {
        Pet pet = clinicService.findPetById(petId);
        if (pet == null) {
            return null;
        }

        AiConversation conversation = new AiConversation(pet);
        Summary summary = conversation.consultAi(symptoms);

        log.info("Diagnosis for pet {}: {}", pet.getId(), summary);

        return summary;
    }
}
