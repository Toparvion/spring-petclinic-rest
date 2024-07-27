package org.springframework.samples.petclinic.service.perf.memory;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vladimir Plizga
 */
public class VetPortfolio {
    private final String company;
    private final String role;
    private static final List<ByteBuffer> documentPages = new ArrayList<>();
    private final LocalDate fromDate;
    private final LocalDate toDate;

    public VetPortfolio(String company, String role, List<ByteBuffer> documentsPages, LocalDate fromDate, LocalDate toDate) {
        this.company = company;
        this.role = role;
        documentPages.addAll(documentsPages);
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    @SuppressWarnings("unused")         // reserved for further development
    public String getCompany() {
        return company;
    }

    public String getRole() {
        return role;
    }

    @SuppressWarnings("unused")         // reserved for further development
    public List<ByteBuffer> getDocumentPages() {
        return documentPages;
    }

    @SuppressWarnings("unused")         // reserved for further development
    public LocalDate getFromDate() {
        return fromDate;
    }

    @SuppressWarnings("unused")         // reserved for further development
    public LocalDate getToDate() {
        return toDate;
    }
}
