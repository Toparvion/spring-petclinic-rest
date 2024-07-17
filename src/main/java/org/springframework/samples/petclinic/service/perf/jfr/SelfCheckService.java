package org.springframework.samples.petclinic.service.perf.jfr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import static java.io.File.pathSeparator;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.function.Predicate.not;

/**
 *
 * @author Vladimir Plizga
 */
@Service
@ConditionalOnProperty("enable-self-check")
public class SelfCheckService {
    private static final Logger log = LoggerFactory.getLogger(SelfCheckService.class);

    @EventListener(ApplicationReadyEvent.class)
    public void performSelfCheck() {
        // first examine all the JAR signatures with the classpath
        long startTime = System.currentTimeMillis();
        List<CheckResult> checkResults = Arrays.stream(
                System.getProperty("java.class.path").split(pathSeparator))
            .map(Path::of)
            .filter(not(Files::isDirectory))
            .map(this::checkForSignature)
            .toList();
        boolean allRight = checkResults.stream().allMatch(CheckResult::isOk);
        long tookTime = System.currentTimeMillis() - startTime;
        log.info("Self-check result: {} (took: {} ms)", (allRight ? "OK" : "FAIL"), tookTime);

        // then dump the overall footprint for further checking
        Path dumpPath = Path.of(System.getProperty("user.dir"), "dumps", "self-check-dump.bin");
        try (OutputStream outputStream = Files.newOutputStream(dumpPath)) {
            int totalSize = 0;
            for (CheckResult checkResult : checkResults) {
                ByteArrayOutputStream accumulator = checkResult.accumulator();
                byte[] byteArray = accumulator.toByteArray();
                totalSize += byteArray.length;
                outputStream.write(byteArray);
            }
            log.debug("Self-check saved {} bytes to {}", totalSize, dumpPath);
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to store self check dump", e);
        }
    }

    private CheckResult checkForSignature(Path jarPath) {
        try {
            try (JarFile jarFile = new JarFile(jarPath.toFile(), true, ZipFile.OPEN_READ)) {
                Set<Certificate> certificates = new HashSet<>();
                ByteArrayOutputStream accumulator = new ByteArrayOutputStream();
                jarFile.stream()
                    .filter(jarEntry -> !jarEntry.isDirectory())
                    .forEach(entry -> {
                        readEntry(jarFile, entry, accumulator);
                        CodeSigner[] codeSigners = entry.getCodeSigners();
                        if (codeSigners != null) {
                            for (CodeSigner cs : entry.getCodeSigners()) {
                                certificates.addAll(cs.getSignerCertPath().getCertificates());
                            }
                        }
                        Certificate[] entryCerts = entry.getCertificates();
                        if (entryCerts != null) {
                            certificates.addAll(asList(entryCerts));
                        }
                    });

                return CheckResult.ok(jarPath, certificates, accumulator);
            }
        } catch (IOException e) {
            log.error("Failed to read JAR file {}", jarPath, e);
            return CheckResult.fail(jarPath, e.getMessage());
        }
    }

    private void readEntry(JarFile jf, JarEntry je, ByteArrayOutputStream accumulator) {
        try (InputStream is = jf.getInputStream(je)) {
            byte[] buffer = new byte[8192];
            while (is.read(buffer, 0, buffer.length) != -1) {
                accumulator.write(buffer);
            }
        } catch (IOException e) {
            log.error("Failed to read entry '{}' from file '{}'", jf, je, e);
        }
    }

    private record CheckResult(boolean isOk,
                               Path path,
                               Set<Certificate> certs,
                               ByteArrayOutputStream accumulator,
                               String error)
        implements Comparable<CheckResult> {

        public static CheckResult ok(Path path, Set<Certificate> certs, ByteArrayOutputStream accumulator) {
            return new CheckResult(true, path, certs, accumulator, null);
        }

        public static CheckResult fail(Path path, String error) {
            return new CheckResult(false, path, emptySet(), new ByteArrayOutputStream(), error);
        }

        @Override
        @SuppressWarnings("NullableProblems")
        public int compareTo(CheckResult other) {
            if (other == null) {
                return -1;
            }
            return path.compareTo(other.path());
        }
    }
}
