package com.ticketbox.backend.worker.csv;

import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Component
public class CsvRowValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    public boolean isValid(CSVRecord record) {
        if (!record.isMapped("email") || !record.isMapped("fullName")) {
            return false;
        }

        String email = record.get("email");
        String fullName = record.get("fullName");

        if (!StringUtils.hasText(email) || !StringUtils.hasText(fullName)) {
            return false;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return false;
        }

        return true;
    }
}
