package org.springframework.samples.petclinic.service.perf.threads.care;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.samples.petclinic.repository.OwnerRepository;
import org.springframework.samples.petclinic.service.perf.threads.ThreadUtils;
import org.springframework.stereotype.Service;

import static java.util.stream.Collectors.joining;

/**
 * A service responsible for provision of pet care tips to their owners. <p/>
 * Used in sample case #3.
 *
 * @author Vladimir Plizga
 */
@Service
@ConditionalOnProperty("enable-care-tips")
public class CareTipsProvider {
    private static final Logger log = LoggerFactory.getLogger(CareTipsProvider.class);

    private final OwnerRepository ownerRepository;
    private final ExecutorService careThreadPool;

    @Autowired
    public CareTipsProvider(OwnerRepository ownerRepository, ExecutorService careThreadPool) {
        this.ownerRepository = ownerRepository;
        this.careThreadPool = careThreadPool;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void composeCareTips() throws Exception {
        List<OwnerCareTask> ownerCareTasks = ownerRepository.findAll()
            .stream()
            .map(owner -> new OwnerCareTask(owner, careThreadPool))
            .toList();

        log.debug("Proposing care tips for {} owners...", ownerCareTasks.size());

        String tipsText = careThreadPool.invokeAll(ownerCareTasks)
            .stream()
            .map(ThreadUtils::getTaskResult)
            .collect(joining("\n\n", "General care tips from Spring PetClinic:\n", ""));

        log.info(tipsText);
    }

}
