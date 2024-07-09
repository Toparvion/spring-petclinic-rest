package org.springframework.samples.petclinic.service;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.consumer.RecordingStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.stereotype.Service;

/**
 *
 * @author Vladimir Plizga
 */
@Service
@ConditionalOnProperty("enable-vet-control")
public class VetControlService {
    private static final Logger log = LoggerFactory.getLogger(VetControlService.class);

    @Name("petclinic.DangerousAnimalRequest")
    @Label("Request for a dangerous animal")
    @Description("The event is fired upon request on a pet of any potentially toxic type")
    @Category("PetClinic")
    static class DangerousAnimalJfrEvent extends Event {

        @Label("PetId")
        int petId;

        @Label("PetType")
        String type;
    }

    public void notifyOnDangerousAnimal(Pet pet) {
        var event = new DangerousAnimalJfrEvent();

        event.petId = pet.getId();
        event.type = pet.getType().getName();

        event.commit();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void monitorVetControlEvents() throws InterruptedException {
        try (var rs = new RecordingStream()) {
            rs.enable("petclinic.DangerousAnimalRequest")
                .withStackTrace();
            rs.onEvent("petclinic.DangerousAnimalRequest", event ->
                log.warn("Dangerous animal requested: id={}, type={}", event.getInt("petId"), event.getString("type")));

            rs.startAsync();
            log.info("Vet control events monitoring started");
            rs.awaitTermination();
        }
    }
}
