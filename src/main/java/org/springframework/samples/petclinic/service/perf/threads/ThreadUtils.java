package org.springframework.samples.petclinic.service.perf.threads;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author Vladimir Plizga
 */
public final class ThreadUtils {

    public static String getTaskResult(Future<String> future) {
        try {
            return future.get();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static void pauseForSeconds(int seconds) {
        try {
            Thread.sleep(seconds * 1_000L);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private ThreadUtils() { /* not instantiable */ }
}
