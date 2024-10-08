package org.springframework.samples.petclinic.service.perf.threads;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.repository.PetRepository;
import org.springframework.samples.petclinic.service.perf.FakeImpl;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * A service for calculating disease risks for pets and providing corresponding recommendations of visits to the clinic.
 * Used in sample case #2.
 *
 * @author Vladimir Plizga
 */
@Service
@EnableScheduling           // to enable background risks recalculation
@ConditionalOnProperty("enable-risk-ai")
public class DiseaseRiskAiService {
    private static final Logger log = LoggerFactory.getLogger(DiseaseRiskAiService.class);

    private final PetRepository petRepository;

    /**
     * A lock to prevent inconsistency of data derived from the risks. Any number of threads can read the risks
     * simultaneously but only one is permitted to change them, and if the changing is in progress, no one is allowed
     * to read.
     */
    private final ReentrantReadWriteLock risksLock = new ReentrantReadWriteLock();

    /**
     * Simple cache of recommended visits computed upon risks. Gets populated lazily, e.g. only when someone asks for
     * recommendations. Gets cleared when the risks are re-calculated.
     */
    private final Map<Integer, List<Visit>> visitCache = new ConcurrentHashMap<>();


    @Autowired
    public DiseaseRiskAiService(PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    public List<Visit> fetchRecommendedVisits(int petId) {
        return visitCache.computeIfAbsent(petId, this::recommendVisits);
    }

    @FakeImpl("Simulates visit recommendations by returning a single visit with fixed contents")
    private List<Visit> recommendVisits(int petId) {
        risksLock.readLock().lock();
        try {
            log.debug("Composing visit recommendations for petId={}...", petId);

            Pet pet = petRepository.findById(petId);

            var visit = new Visit();
            visit.setDate(LocalDate.now().plusWeeks(2));
            visit.setPet(pet);
            visit.setDescription("Preventive vaccination (recommended by AI)");
            visit.setId((int) (Instant.now().getEpochSecond() % 100));

            log.info("Visit recommendation list for pet '{}' (petId={}) composed", pet.getName(), petId);
            return List.of(visit);
        }
        finally {
            risksLock.readLock().unlock();
        }
    }

    @Scheduled(fixedDelay = 5, timeUnit = SECONDS)
    public void recalculateDiseaseRisks() {
        risksLock.writeLock().lock();
        try {
            log.debug("Recalculating diseases risks...");

            doAiMagic();

            log.info("Risks of diseases recalculated");
        }
        finally {
            risksLock.writeLock().unlock();
        }
    }

    @FakeImpl("Emulates an inference of AI by means of pausing the thead for a comparable amount of time")
    private void doAiMagic() {
        try {
            Thread.sleep(5_000);        // emulate ML logic operation
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        finally {
            visitCache.clear();   // don't forget to reset previously recommended visits as they might become obsolete
        }
    }
}
