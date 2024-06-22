package org.springframework.samples.petclinic.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.samples.petclinic.util.ThreadUtils.pauseForSeconds;

/**
 * Basic functionality to track pets' pedigrees. Currently is in BETA testing on a single pet only (so far). <br/>
 * Supports both automatic (by schedule) and manual (by request) pedigree updating which currently comes down to
 * changing the pet name from its raw form to the titled one, e.g. "Basil" -> "Basil the Second", and vice versa.
 *
 * @author Vladimir Plizga
 */
@Service
@ConditionalOnProperty(name = "pedigree-enabled", havingValue = "true")
public class PedigreeService {
    private static final Logger log = LoggerFactory.getLogger(PedigreeService.class);

    /**
     * The name of the pet that is initially included into beta testing of pedigree tracking feature
     */
    private static final String BETA_TEST_PET = "Basil";        // hamster
    private static final String NEW_TITLE = " The Second";

    private final DataSource dataSource;

    /**
     * In-memory cache of human-readable texts of pets pedigrees.<p/>
     * Key: pet id as in the database <br/>
     * Value: raw text of the pet pedigree
     */
    private final Map<Integer, String> pedigreeTextCache = Collections.synchronizedMap(new HashMap<>());

    @Autowired
    public PedigreeService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean isPetInBetaTesting(String petName) {
        return petName.contains(BETA_TEST_PET);
    }

    /**
     * Updates pedigree for the given pet (identified by name). The pet must be included to BETA testing.
     * @see #isPetInBetaTesting
     * @param currentName the name of the pet to update
     */
    public void updatePedigreeByRequest(String currentName) {
        log.debug("Manually updating pedigree for pet '{}'...", currentName);

        // synchronize access to the cache since we iterate over it inside `replaceAllNameEntries()` method
        synchronized (pedigreeTextCache) {
            // invert pet name for titled to raw form and vice versa to emulate some business logic
            String newName = currentName.equals(BETA_TEST_PET)
                ? currentName + NEW_TITLE
                : currentName.substring(0, BETA_TEST_PET.length());
            replaceAllNameEntries(pedigreeTextCache, currentName, newName);
            log.debug("Pedigree cache updated. Updating the name in DB...");

            doInTransaction(connection -> updateName(currentName, newName, connection));
            log.info("Manual update is done for pet '{}' (formerly '{}')", newName, currentName);
        }
    }

    @Scheduled(fixedDelay = 5, timeUnit = SECONDS)
    public void updatePedigreeBySchedule() {
        log.debug("Auto updating pedigree for pet '{}'...", BETA_TEST_PET);

        // wrap both query and update into a transaction to preserve data consistency
        doInTransaction(connection -> {
            String currentName = findCurrentName(BETA_TEST_PET, connection);
            String newName = currentName.equals(BETA_TEST_PET)
                ? currentName + NEW_TITLE
                : currentName.substring(0, BETA_TEST_PET.length());
            updateName(currentName, newName, connection);

            log.debug("Database record updated. Updating pedigree cache...");
            pauseForSeconds(5);     // ensure the changes are propagated fully

            // synchronize access to the cache according to its javadoc
            synchronized (pedigreeTextCache) {
                replaceAllNameEntries(pedigreeTextCache, currentName, newName);
                log.info("Pet name changed from '{}' to '{}'", currentName, newName);
            }
        });
    }

    /**
     * Finds all entries of given current name, replaces them with the new name and updates the cache accordingly.
     * @param cache the cache to update (implicit result)
     * @param currentName pet name to find and change
     * @param newName pet name that should be used instead of current one
     */
    private static void replaceAllNameEntries(Map<Integer, String> cache, String currentName, String newName) {
        Map<Integer, String> newTexts = new HashMap<>();
        Iterator<Map.Entry<Integer, String>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, String> entry = iterator.next();
            String currentPedigreeText = entry.getValue();
            if (currentPedigreeText.contains(currentName)) {
                String newText = currentPedigreeText.replaceAll(currentName, newName);
                iterator.remove();
                newTexts.put(entry.getKey(), newText);
            }
        }
        cache.putAll(newTexts);
    }

    /**
     * Executes given {@linkplain SqlAction action} in a separate transaction to ensure it's not affected by others
     * @param action single or multiple SQL queries (select and update) to execute
     */
    private void doInTransaction(SqlAction action) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            try {
                action.execute(connection);
                connection.commit();
            }
            catch (Exception e) {
                connection.rollback();
                throw new RuntimeException("Failed to execute SQL action", e);
            } finally {
                connection.setAutoCommit(true);
            }
        }
        catch (SQLException e) {
            throw new RuntimeException("Failed to get connection from data source", e);
        }
    }

    @FunctionalInterface
    private interface SqlAction {
        void execute(Connection connection) throws Exception;
    }

    private static String findCurrentName(String namePrefix, Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("select name from pets where name like ?")) {
            ps.setString(1, namePrefix + "%");
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString(1);
            } else {
                throw new IllegalArgumentException("No pets found with name '" + namePrefix + "'");
            }
        }
    }

    private static void updateName(String currentName, String newName, Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("update pets set name = ? where name = ?")) {
            ps.setString(1, newName);
            ps.setString(2, currentName);
            int updatedRowsCount = ps.executeUpdate();
            log.trace("{} rows updated by when changing name from '{}' to '{}'", updatedRowsCount, currentName, newName);
        }
    }
}
