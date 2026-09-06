package eye.on.the.money.service.shared;

import eye.on.the.money.exception.CSVException;
import eye.on.the.money.util.Numbers;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public interface IExcelService {

    default Workbook getWorkbook(MultipartFile file) throws IOException {
        try (InputStream stream = file.getInputStream()) {
            return new HSSFWorkbook(new POIFSFileSystem(stream));
        }
    }

    default Sheet getFirstSheet(Workbook workbook) {
        if (workbook.getNumberOfSheets() == 0) {
            throw new CSVException("The Excel file has no sheets");
        }
        return workbook.getSheetAt(0);
    }

    default Map<String, Integer> headerIndexes(Row headerRow) {
        if (headerRow == null) {
            throw new CSVException("The Excel file has no header row");
        }
        Map<String, Integer> indexes = new HashMap<>();
        for (int column = headerRow.getFirstCellNum(); column < headerRow.getLastCellNum(); column++) {
            String header = this.stringValue(headerRow, column);
            if (!header.isEmpty()) {
                indexes.put(header, column);
            }
        }
        return indexes;
    }

    default Integer requiredColumn(Map<String, Integer> indexes, String header) {
        Integer column = indexes.get(header);
        if (column == null) {
            throw new CSVException("Missing column: " + header);
        }
        return column;
    }

    default String stringValue(Row row, Integer column) {
        Cell cell = (row == null || column == null) ? null : row.getCell(column);
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            double value = cell.getNumericCellValue();
            return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
        }
        return cell.getCellType() == CellType.STRING ? cell.getStringCellValue().trim() : "";
    }

    default Double numericValue(Row row, Integer column) {
        Cell cell = (row == null || column == null) ? null : row.getCell(column);
        if (cell != null && cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }
        return Numbers.parseHungarian(this.stringValue(row, column));
    }

    default boolean isBlankRow(Row row) {
        if (row == null) {
            return true;
        }
        for (int column = row.getFirstCellNum(); column < row.getLastCellNum(); column++) {
            if (!this.stringValue(row, column).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    default CSVException excelParseFailure(int rowNumber, Exception cause) {
        return new CSVException(rowNumber > 0
                ? "Failed to parse the Excel file at row " + rowNumber
                : "Failed to parse the Excel file", cause);
    }
}
