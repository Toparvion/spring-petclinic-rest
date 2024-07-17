package org.springframework.samples.petclinic.service.perf.threads;

import static java.lang.ProcessBuilder.Redirect.INHERIT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Vladimir Plizga
 */
@Service
@ConditionalOnProperty("enable-owner-info")
public class OwnerInfoService {
    private static final Logger log = LoggerFactory.getLogger(OwnerInfoService.class);

    public void checkOwnerInfo(String ownerTelephone) {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
        String scriptFileName = isWindows
            ? "getinfo.cmd"
            : "getinfo.sh";
        Path scriptTempPath = Path.of(System.getProperty("java.io.tmpdir"), scriptFileName);
        try {
            InputStream scriptInStream = this.getClass().getResourceAsStream(scriptFileName);
            Assert.notNull(scriptInStream, "Script '%s' not found in application resources".formatted(scriptFileName));

            OutputStream scriptOutStream = Files.newOutputStream(scriptTempPath);
            FileCopyUtils.copy(scriptInStream, scriptOutStream);
            if (!isWindows) {
                Process chmod = Runtime.getRuntime().exec(new String[] {"chmod", "+x", scriptTempPath.toString()});
                chmod.waitFor();
            }

            String[] command = isWindows
                ? "cmd.exe /c %s %s".formatted(scriptTempPath, ownerTelephone).split(" ")
                : "/bin/sh -c %s %s".formatted(scriptTempPath, ownerTelephone).split(" ");
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectError(INHERIT);
            pb.redirectOutput(INHERIT);
            Process process = pb.start();

            process.waitFor();
        }
        catch (IOException e) {
            log.error("Failed to check owner info", e);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        finally {
            try {
                Files.deleteIfExists(scriptTempPath);
            }
            catch (IOException e) {
                log.error("Failed to delete temporary file {}", scriptTempPath, e);
            }
        }

    }
}
