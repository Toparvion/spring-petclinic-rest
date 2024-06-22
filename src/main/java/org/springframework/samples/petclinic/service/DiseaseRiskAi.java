package org.springframework.samples.petclinic.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.random.RandomGeneratorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.repository.OwnerRepository;
import org.springframework.samples.petclinic.repository.PetRepository;
import org.springframework.samples.petclinic.service.care.OwnerCareTask;
import org.springframework.samples.petclinic.util.ThreadUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;

/**
 *
 * @author Vladimir Plizga
 */
@Service
public class DiseaseRiskAi {
    private static final Logger log = LoggerFactory.getLogger(DiseaseRiskAi.class);

    private final PetRepository petRepository;
    private final OwnerRepository ownerRepository;

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

    private final ExecutorService careThreadPool;

    @Autowired
    public DiseaseRiskAi(PetRepository petRepository, OwnerRepository ownerRepository, ExecutorService careThreadPool) {
        this.petRepository = petRepository;
        this.ownerRepository = ownerRepository;
        this.careThreadPool = careThreadPool;
    }

    public List<Visit> fetchRecommendedVisits(int petId) {
        return visitCache.computeIfAbsent(petId, this::recommendVisits);
    }

    private List<Visit> recommendVisits(int petId) {
        risksLock.readLock().lock();
        try {
            log.debug("Composing visit recommendations for petId={}...", petId);

            Pet pet = petRepository.findById(petId);

            var visit = new Visit();
            visit.setDate(LocalDate.now().plusWeeks(2));
            visit.setPet(pet);
            visit.setDescription("Preventive vaccination (recommended by AI)");
            visit.setId(RandomGeneratorFactory.getDefault().create().nextInt());

            log.info("Visit recommendation list for pet '{}' (petId={}) composed", pet.getName(), petId);
            return List.of(visit);
        }
        finally {
            risksLock.readLock().unlock();
        }
    }

    @Scheduled(fixedDelayString = "${risk.recalculate.period.seconds:5000}",
        initialDelayString = "${risk.recalculate.delay.seconds:3600}",
        timeUnit = SECONDS)
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

    @EventListener(value = ApplicationReadyEvent.class,
                   condition = "event.args.length > 0 && event.args[0] == '--recommend-care'")
    public void composeCareRecommendations() throws Exception {
        List<OwnerCareTask> ownerCareTasks = ownerRepository.findAll()
            .stream()
            .map(owner -> new OwnerCareTask(owner, careThreadPool))
            .toList();

        log.debug("Proposing care recommendations for {} owners...", ownerCareTasks.size());

        String recommendationsText = careThreadPool.invokeAll(ownerCareTasks)
            .stream()
            .map(ThreadUtils::getCareTaskResult)
            .collect(joining("\n\n", "Care recommendations from Spring PetClinic:\n", ""));

        log.info(recommendationsText);
    }
}
