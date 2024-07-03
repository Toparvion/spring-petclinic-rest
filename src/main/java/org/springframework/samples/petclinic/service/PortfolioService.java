package org.springframework.samples.petclinic.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.samples.petclinic.model.Vet;
import org.springframework.samples.petclinic.model.VetPortfolio;
import org.springframework.stereotype.Service;

/**
 * @author Vladimir Plizga
 */
@Service
@ConditionalOnProperty("enable-portfolio")
public class PortfolioService {
    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);

    private final Set<VetPortfolio> portfolioStorage = new HashSet<>();     // temporal in-memory storage
    private final boolean isSavingEnabled;

    private final ThreadLocal<VetPortfolio> sharedPortfolio = new ThreadLocal<>();
    private final boolean isSharingEnabled;

    public PortfolioService(@Value("${save-portfolio:false}") boolean isSavingEnabled,
                            @Value("${share-portfolio:false}") boolean isSharingEnabled) {
        this.isSavingEnabled = isSavingEnabled;
        this.isSharingEnabled = isSharingEnabled;
        log.debug("Portfolio saving enabled: {}", isSavingEnabled);
        log.debug("Portfolio sharing enabled: {}", isSharingEnabled);
    }

    public void loadVetPortfolio(Vet vet) {
        VetPortfolio portfolio = fetchPortfolioFor(vet);

        log.info("Loaded portfolio: {}", portfolio);

        if (isSavingEnabled) {
            portfolioStorage.add(portfolio);        // HashSet won't allow duplicates
            log.info("Current portfolio storage size: {}", portfolioStorage.size());
        }

        if (isSharingEnabled) {
            sharedPortfolio.set(portfolio);
            log.info("Portfolio has been shared");
        }
    }

    private VetPortfolio fetchPortfolioFor(Vet vet) {
        // We currently support only the last entry of portfolio - current internship
        var documentScan = new byte[1 << 23];     // emulating loading of approx. 8MB
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
}
