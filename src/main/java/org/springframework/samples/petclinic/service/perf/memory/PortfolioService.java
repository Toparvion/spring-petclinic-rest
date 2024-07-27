package org.springframework.samples.petclinic.service.perf.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.Vet;
import org.springframework.samples.petclinic.repository.PetRepository;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Sample service to showcase various memory leakages.
 * Used in cases from 2.1 to 2.3.
 *
 * @author Vladimir Plizga
 */
@Service
@ConditionalOnProperty("enable-portfolio")
public class PortfolioService {
    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);

    /**
     * Temporal storage of loaded portfolios
     */
    private final Set<VetPortfolio> portfolioStorage = new HashSet<>();     // temporal in-memory storage
    private final boolean isSavingEnabled;

    /**
     * A request-scoped storage of portfolios for accessing them from various corners of the application
     */
    private final ThreadLocal<VetPortfolio> sharedPortfolio = new ThreadLocal<>();
    private final boolean isSharingEnabled;

    // For heap dump analysis demonstration
    private final PetRepository petRepository;
    private final Set<Pet> petsCache = new HashSet<>();

    public PortfolioService(@Value("${save-portfolio:false}") boolean isSavingEnabled,
                            @Value("${share-portfolio:false}") boolean isSharingEnabled,
                            PetRepository petRepository) {
        this.isSavingEnabled = isSavingEnabled;
        this.isSharingEnabled = isSharingEnabled;
        this.petRepository = petRepository;
        log.debug("Portfolio saving enabled: {}", isSavingEnabled);
        log.debug("Portfolio sharing enabled: {}", isSharingEnabled);
    }

    public void processVetPortfolio(Vet vet) {
        VetPortfolio portfolio = loadPortfolioFor(vet);

        log.info("Loaded portfolio with company: {}", portfolio.getCompany());

        if (isSavingEnabled) {
            portfolioStorage.add(portfolio);        // HashSet won't allow duplicates so its size shouldn't grow
            log.info("Current portfolio storage size: {}", portfolioStorage.size());
        }

        if (isSharingEnabled) {
            sharedPortfolio.set(portfolio);
            log.info("Portfolio has been shared");
        }
    }

    private VetPortfolio loadPortfolioFor(Vet vet) {
        log.debug("Loading portfolio for vet: {} {}", vet.getFirstName(), vet.getLastName());

        // We currently support only the last entry of portfolio - current internship
        var documentScan = ByteBuffer.allocate(1 << 23);        // emulating loading of approx. 8MB

        return new VetPortfolio(
            "Spring PetClinic",
            "intern",
            List.of(documentScan),       // single page only
            LocalDate.now().minusMonths(6),
            null        // means "up to this day"
        );
    }

    @SuppressWarnings("unused")     // for future usage
    public Set<VetPortfolio> getPortfolioStorage() {
        return portfolioStorage;
    }

    @SuppressWarnings("unused")     // for future usage
    public Optional<VetPortfolio> getSharedPortfolio() {
        return Optional.ofNullable(sharedPortfolio.get());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmupPetCache() {
        petsCache.addAll(petRepository.findAll());
        log.debug("Pets cache warmed up (size={})", petsCache.size());
    }
}
