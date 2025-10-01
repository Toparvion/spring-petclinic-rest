package org.springframework.samples.petclinic.service.perf.profile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.samples.petclinic.rest.dto.PetDto;
import org.springframework.samples.petclinic.service.perf.FakeImpl;
import org.springframework.stereotype.Service;

import static java.util.stream.Collectors.joining;

/**
 *
 * @author Vladimir Plizga
 */
@Service
@ConditionalOnProperty("enable-processing")
public class PetListPostProcessor {
    private static final Logger log = LoggerFactory.getLogger(PetListPostProcessor.class);

    private static final int LINEAGE_DEPTH = Integer.getInteger("depth", 34);

    /**
     * The mapping between pet names and their pedigree strengths (the number of ancestors).
     * Key: pet name, value: its pedigree strength.
     * The Properties format is chosen for easy export to XML.
     */
    private final Properties pedigreeStrengths = new Properties();

    /**
     * A list of medications that currently can be found in the clinic
     */
    private final List<String> availableDrugs = preloadAvailableDrugs();

    /**
     * Human-readable pet identifiers combining both the name and the ID for each pet
     */
    private final Set<String> petUniqueIds = new HashSet<>();

    public void postProcessPetList(List<PetDto> pets) {

        logPets(pets);

        generateUniqueIDs(pets);

        loadCompatibleDrugs(pets);

        checkForSpecialNames(pets);

        computePedigreeStrengths(pets);
    }

    /**
     * Outputs the pet list in JSON format to the log on TRACE level
     */
    private void logPets(List<PetDto> pets) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
            mapper.registerModule(new JavaTimeModule());
            String json = mapper.writeValueAsString(pets);
            log.trace("Pets JSON: " + json);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates human-readable pet identifiers combining both the name and the ID for each pet and stores them into
     * {@link #petUniqueIds} for further use
     */
    private void generateUniqueIDs(List<PetDto> pets) {
        pets.parallelStream()
            .map(pet -> pet.getName() + "::" + pet.getId())
            .peek(id -> System.out.println(Thread.currentThread().getName() + ": " + id))   // for debug
            .forEach(petUniqueIds::add);
    }

    /**
     * Traverses {@link #availableDrugs} and for each pet checks if it's allowed to give the drug to the pet
     */
    private void loadCompatibleDrugs(List<PetDto> pets) {
        List<String> compatibleDrugs = new ArrayList<>();
        for (String availableDrug : availableDrugs) {
            for (PetDto pet : pets) {
                // the following check ensures that the animal is not prohibited from taking that drug
                if (pet.getBirthDate().isBefore(LocalDate.now().minusYears(3))) {
                    compatibleDrugs.add(availableDrug);
                }
            }
        }
        log.trace("Found {} compatible drugs for {} pets", compatibleDrugs.size(), pets.size());
    }

    /**
     * Collects the names of the pets into a single text and check if it contains a special name pattern
     */
    private void checkForSpecialNames(List<PetDto> pets) {
        String petNames = pets.stream()
            .map(PetDto::getName)
            .map(name -> name.concat(name).concat(name))
            .collect(joining("", "", "!"));

        Pattern pat = Pattern.compile("^(\\w+\\s?)*$");

        Matcher m = pat.matcher(petNames);
        if (m.matches()) {
            log.info("Found special name: {}", m.group(1));
        }
    }

    /**
     * Computes and updates the pedigree strengths for a list of pets.
     * The pedigree strength for a pet is determined by calculating the number of the pet's ancestors.
     */
    private void computePedigreeStrengths(List<PetDto> pets) {
        for (PetDto pet : pets) {
            pedigreeStrengths.compute(pet.getName(),
                (oldVal, newVal) -> fib(LINEAGE_DEPTH));
        }
    }

    private long fib(int n) {
        if (n <= 1) return n;
        return fib(n - 1) + fib(n - 2);
    }

    @FakeImpl("Simulates loading of a list of available animal drugs from an external source")
    private List<String> preloadAvailableDrugs() {
        List<String> compatibleDrugs = new ArrayList<>();
        var faker = new Faker();
        for (int i = 0; i < faker.random().nextInt(1500, 2000); i++) {
            compatibleDrugs.add(faker.medication().drugName());
        }
        return compatibleDrugs;
    }

    @PostConstruct
    public void afterPropertiesSet() {
        log.info("PetListPostProcessor initialized");
    }

    @SuppressWarnings("unused")         // for future integration
    public Set<String> getPetUniqueIds() {
        return petUniqueIds;
    }
}
