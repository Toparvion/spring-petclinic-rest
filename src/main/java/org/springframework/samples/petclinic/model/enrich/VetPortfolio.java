package org.springframework.samples.petclinic.model.enrich;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vladimir Plizga
 */
public class VetPortfolio {
    private final String institution;
    private final String grade;
    private static final List<byte[]> diplomaPages = new ArrayList<>();
    private final LocalDate fromDate;
    private final LocalDate toDate;

    public VetPortfolio(String institution, String grade, List<byte[]> diplomaPages, LocalDate fromDate,
        LocalDate toDate) {
        this.institution = institution;
        this.grade = grade;
        this.diplomaPages.addAll(diplomaPages);
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    public String getInstitution() {
        return institution;
    }

    public String getGrade() {
        return grade;
    }

    public List<byte[]> getDiplomaPages() {
        return diplomaPages;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    @Override
    public String toString() {
        return "VetPortfolio{" +
            "institution='" + institution + '\'' +
            ", grade='" + grade + '\'' +
            '}';
    }
}
