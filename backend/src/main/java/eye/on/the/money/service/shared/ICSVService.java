package eye.on.the.money.service.shared;

import eye.on.the.money.dto.CSVHelper;
import eye.on.the.money.exception.CSVException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public interface ICSVService {

    String FORMULA_TRIGGERS = "=@\t\r";
    String SIGN_TRIGGERS = "-+";

    default <T extends CSVHelper> void printRecords(List<T> dtoList, Writer writer) {
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            if (!dtoList.isEmpty()) {
                csvPrinter.printRecord(this.neutralize(dtoList.getFirst().getHeaders()));
            }
            for (CSVHelper record : dtoList) {
                csvPrinter.printRecord(this.neutralize(record.getCSVRecord()));
            }
        } catch (IOException e) {
            throw new CSVException("Failed to create CSV file: " + e.getMessage(), e);
        }
    }

    private Object[] neutralize(Object[] cells) {
        Object[] neutralized = new Object[cells.length];
        for (int index = 0; index < cells.length; index++) {
            neutralized[index] = (cells[index] instanceof String text) ? this.neutralizeCell(text) : cells[index];
        }
        return neutralized;
    }

    private String neutralizeCell(String cell) {
        if (cell.isEmpty()) {
            return cell;
        }
        char first = cell.charAt(0);
        if (FORMULA_TRIGGERS.indexOf(first) >= 0) {
            return "'" + cell;
        }
        if (SIGN_TRIGGERS.indexOf(first) >= 0 && !this.isNumeric(cell)) {
            return "'" + cell;
        }
        return cell;
    }

    private boolean isNumeric(String cell) {
        try {
            Double.parseDouble(cell);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    default Long resolveAccountId(Map<String, Long> accountIdsByName, String accountName) {
        if (accountName == null || accountName.isBlank()) {
            throw new CSVException("Account is missing from the CSV file");
        }
        Long accountId = accountIdsByName.get(accountName);
        if (accountId == null) {
            throw new CSVException("Unknown account: " + accountName);
        }
        return accountId;
    }

    default CSVParser getParser(MultipartFile file, String[] headers) throws IOException {
        return this.getParser(file, headers, ',', StandardCharsets.UTF_8);
    }

    default CSVParser getParser(MultipartFile file, String[] headers, char delimiter, Charset charset) throws IOException {
        Reader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), charset));
        return new CSVParser(fileReader, CSVFormat.Builder.create()
                .setHeader(headers)
                .setSkipHeaderRecord(true)
                .setDelimiter(delimiter)
                .setTrim(true)
                .setIgnoreHeaderCase(true).get());
    }
}
