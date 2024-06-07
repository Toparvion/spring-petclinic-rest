package org.springframework.samples.petclinic.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 *
 * @author Vladimir Plizga
 */
public class ThreadUtils {

    public static String getCareTaskResult(Future<String> future) {
        try {
            return future.get();
        }
        catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
