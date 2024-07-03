package org.springframework.samples.petclinic.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vladimir Plizga
 */
public class VetPortfolio {
    private final String company;
    private final String role;
    private static final List<byte[]> documentPages = new ArrayList<>();
    private final LocalDate fromDate;
    private final LocalDate toDate;

    public VetPortfolio(String company, String role, List<byte[]> documentsPages, LocalDate fromDate, LocalDate toDate) {
        this.company = company;
        this.role = role;
        documentPages.addAll(documentsPages);
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    public String getCompany() {
        return company;
    }

    public String getRole() {
        return role;
    }

    public List<byte[]> getDocumentPages() {
        return documentPages;
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
            "company='" + company + '\'' +
            ", role='" + role + '\'' +
            '}';
    }
}
