package org.springframework.samples.petclinic.service.care;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.samples.petclinic.model.Pet;

/**
 *
 * @author Vladimir Plizga
 */
public class PetCareTask implements Callable<String> {
    private static final Logger log = LoggerFactory.getLogger(PetCareTask.class);

    private final Pet pet;

    public PetCareTask(Pet pet) {
        this.pet = pet;
    }

    @Override
    public String call() {
        String name = pet.getName();
        log.trace("Composing care tips for pet '{}'...", name);

        // doing some heavy stuff

        log.info("Care tips for pet '{}' proposed", name);

        return "Your %s should sleep more".formatted(name);
    }
}
