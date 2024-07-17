package org.springframework.samples.petclinic.service.perf.threads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import static java.lang.Math.divideExact;

/**
 * A service providing integration with the Global Pet Registry located at {@code pet-registry-url} property value.
 * The primary purpose of the service is to check whether a pet is registered or not. <p/>
 * Used in sample case #1.
 *
 * @author Vladimir Plizga
 */
@Service
@ConditionalOnProperty("enable-pet-registry")
public class PetRegistryService {
    private static final Logger log = LoggerFactory.getLogger(PetRegistryService.class);

    private final RestClient restClient;

    @Autowired
    public PetRegistryService(@Value("${pet-registry-url}") String registryUrl) {
        restClient = RestClient.create(registryUrl);
    }

    /**
     * @param pet a pet to check
     * @return {@code true} if the pet is known to the Global Pet Registry, or {@code false} otherwise
     */
    public boolean isPetRegistered(Pet pet) {
        log.trace("Querying registration status for pet {}", pet.getName());

        return restClient.get()
            .uri("/delay/{petId}", divideExact(pet.getId(), pet.getId()))
            .retrieve()
            .toBodilessEntity()
            .getStatusCode()
            .is2xxSuccessful();
    }
}
