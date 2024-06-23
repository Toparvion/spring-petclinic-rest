package org.springframework.samples.petclinic.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 *
 * @author Vladimir Plizga
 */
public class ThreadUtils {

    public static String getTaskResult(Future<String> future) {
        try {
            return future.get();
        }
        catch (InterruptedException | ExecutionException e) {
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
}
