package org.springframework.samples.petclinic.service.perf.memory.ai;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.datafaker.Faker;
import net.datafaker.providers.base.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.service.perf.FakeImpl;

/**
 *
 * @author Vladimir Plizga
 */
public class AiConversation {
    private static final Logger log = LoggerFactory.getLogger(AiConversation.class);
    private static final Faker DATA_FAKER = new Faker();

    private final List<String> messages = new ArrayList<>();
    private final Pet pet;

    public AiConversation(Pet pet) {
        this.pet = pet;
    }

    @FakeImpl("Mimics LLM chat model interaction by leveraging datafaker library to generate random text")
    public Summary consultAi(String symptoms) {
        log.debug("Consulting AI for petId={} with symptoms: {}", pet.getId(), symptoms);

        Text messageGenerator = DATA_FAKER.text();
        int cnt = 1_000;
        while (cnt-- > 0) {
            messages.add(messageGenerator.text(10_000));
        }
        return makeSummary(messages);
    }

    @FakeImpl("Models summarization of LLM interaction by means of composing a fixed set of random strings")
    public Summary makeSummary(List<String> messages) {
        log.debug("Generating summary from conversation consisting of {} messages", messages.size());

        String prescription = DATA_FAKER.medication().drugName();
        String diagnosisCode = DATA_FAKER.medicalProcedure().icd10();
        LocalDate nextVisit = LocalDate.now().plusDays(new Random().nextInt(30));

        return new Summary(diagnosisCode, prescription, nextVisit);
    }

    public class Summary {
        /**
         * Short diagnosis code in ICD10 format (like '94p5ArW')
         */
        private final String diagnosis;

        /**
         * Short name of a drug to take (like 'Gentaotic')
         */
        private final String prescription;

        /**
         * The date of next visit to a veterinarian (year, month, day)
         */
        private final LocalDate nextVisit;

        public Summary(String diagnosis, String prescription, LocalDate nextVisit) {
            this.diagnosis = diagnosis;
            this.prescription = prescription;
            this.nextVisit = nextVisit;
        }

        public String getDiagnosis() {
            return diagnosis;
        }

        public String getPrescription() {
            return prescription;
        }

        public LocalDate getNextVisit() {
            return nextVisit;
        }

        @Override
        public String toString() {
            return "Summary from AiConversation of %d messages: diagnosis: %s, prescription: %s, nextVisit: %s"
                .formatted(messages.size(), diagnosis, prescription, nextVisit);
        }
    }
}
