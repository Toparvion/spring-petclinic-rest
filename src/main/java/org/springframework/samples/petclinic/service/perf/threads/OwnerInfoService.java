package org.springframework.samples.petclinic.service.perf.threads;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import static java.lang.ProcessBuilder.Redirect.INHERIT;

/**
 * @author Vladimir Plizga
 */
@Service
@ConditionalOnProperty("enable-owner-info")
public class OwnerInfoService {
    private static final Logger log = LoggerFactory.getLogger(OwnerInfoService.class);

    private Path scriptTempPath;

    private ProcessBuilder processBuilder;

    @PostConstruct
    void prepareExternalApplication() {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
        String scriptFileName = isWindows
            ? "getinfo.cmd"
            : "getinfo.sh";
        scriptTempPath = Path.of(System.getProperty("java.io.tmpdir"), scriptFileName);

        try {
            InputStream scriptInStream = this.getClass().getResourceAsStream(scriptFileName);
            Assert.notNull(scriptInStream, "Script '%s' not found in application resources".formatted(scriptFileName));

            OutputStream scriptOutStream = Files.newOutputStream(scriptTempPath);
            FileCopyUtils.copy(scriptInStream, scriptOutStream);
            if (!isWindows) {
                Runtime.getRuntime()
                    .exec(new String[] {"chmod", "+x", scriptTempPath.toString()})
                    .waitFor();
            }

            String[] command = isWindows
                ? "cmd.exe /c %s".formatted(scriptTempPath).split(" ")
                : "/bin/sh -c %s".formatted(scriptTempPath).split(" ");

            processBuilder = new ProcessBuilder(command);
            processBuilder.redirectError(INHERIT);
            processBuilder.redirectOutput(INHERIT);

            log.debug("Prepared external application '{}' with command '{}'", scriptTempPath, command);
        }
        catch (IOException e) {
            log.error("Failed to prepare external script", e);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    public void checkOwnerInfo(String ownerTelephone) {

        try {
            processBuilder.command().add(ownerTelephone);

            Process process = processBuilder.start();
            log.debug("External process launched...");

            int result = process.waitFor();
            log.info("Number of owner accounts in other services: {}", result);
        }
        catch (IOException e) {
            log.error("Failed to check owner info", e);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    void removeTempScriptFile() {
        try {
            Files.deleteIfExists(scriptTempPath);
            log.debug("Removed temporary script file: {}", scriptTempPath);
        }
        catch (IOException e) {
            log.error("Failed to delete temporary file {}", scriptTempPath, e);
        }
    }
}
