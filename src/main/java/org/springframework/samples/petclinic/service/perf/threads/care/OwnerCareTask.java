package org.springframework.samples.petclinic.service.perf.threads.care;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.service.perf.threads.ThreadUtils;

import static java.util.stream.Collectors.joining;

/**
 *
 * @author Vladimir Plizga
 */
public class OwnerCareTask implements Callable<String> {
    private static final Logger log = LoggerFactory.getLogger(OwnerCareTask.class);

    private final Owner owner;

    private final ExecutorService careThreadPool;

    public OwnerCareTask(Owner owner, ExecutorService careThreadPool) {
        this.owner = owner;
        this.careThreadPool = careThreadPool;
    }

    @Override
    public String call() throws Exception {
        List<PetCareTask> petCareTasks = owner.getPets().stream()
            .map(PetCareTask::new)
            .toList();

        log.debug("Proposing care tips for {} pet(s) of owner {}...", petCareTasks.size(), owner.getFirstName());

        return careThreadPool.invokeAll(petCareTasks)
            .stream()
            .map(ThreadUtils::getTaskResult)
            .collect(joining("\n\t- ",
                "Dear %s, here is your pet care tip(s):\n\t- ".formatted(owner.getFirstName()),
                ""));
    }

}
