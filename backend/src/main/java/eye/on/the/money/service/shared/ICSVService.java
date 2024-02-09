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
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public interface ICSVService {

    default <T extends CSVHelper> void printRecords(List<T> dtoList, Writer writer) {
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            if (!dtoList.isEmpty()) {
                csvPrinter.printRecord(dtoList.getFirst().getHeaders());
            }
            for (CSVHelper record : dtoList) {
                csvPrinter.printRecord(record.getCSVRecord());
            }
        } catch (IOException e) {
            throw new CSVException("Failed to create CSV file: " + e.getMessage(), e);
        }
    }

    default CSVParser getParser(MultipartFile file, String[] headers) throws IOException {
        BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
        return new CSVParser(fileReader, CSVFormat.Builder.create()
                .setHeader(headers)
                .setSkipHeaderRecord(true)
                .setDelimiter(",")
                .setTrim(true)
                .setIgnoreHeaderCase(true)
                .build());
    }
}
