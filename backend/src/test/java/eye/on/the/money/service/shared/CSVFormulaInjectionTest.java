package eye.on.the.money.service.shared;

import eye.on.the.money.dto.CSVHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.List;

class CSVFormulaInjectionTest implements ICSVService {

    private record Row(Object[] cells) implements CSVHelper {
        public Object[] getHeaders() {
            return new Object[]{"Memo", "Amount"};
        }

        public Object[] getCSVRecord() {
            return this.cells;
        }
    }

    private String export(Object... cells) {
        StringWriter writer = new StringWriter();
        this.printRecords(List.of(new Row(cells)), writer);
        return writer.toString();
    }

    @Test
    void prefixesEqualsFormula() {
        Assertions.assertTrue(this.export("=1+1", 10.0).contains("'=1+1"));
    }

    @Test
    void prefixesCommandFormula() {
        Assertions.assertTrue(this.export("=cmd|'/c calc'!A1", 10.0).contains("'=cmd|"));
    }

    @Test
    void prefixesAtFormula() {
        Assertions.assertTrue(this.export("@SUM(A1)", 10.0).contains("'@SUM(A1)"));
    }

    @Test
    void prefixesTabAndCarriageReturn() {
        Assertions.assertTrue(this.export("\t=1+1", 10.0).contains("'\t=1+1"));
        Assertions.assertTrue(this.export("\r=1+1", 10.0).contains("'\r=1+1"));
    }

    @Test
    void prefixesNonNumericLeadingSign() {
        Assertions.assertTrue(this.export("-1+1)*cmd", 10.0).contains("'-1+1)*cmd"));
    }

    @Test
    void leavesNegativeNumberStringsAlone() {
        String csv = this.export("-1234.56", 10.0);
        Assertions.assertTrue(csv.contains("-1234.56"));
        Assertions.assertFalse(csv.contains("'-1234.56"));
    }

    @Test
    void leavesTypedNumbersAlone() {
        String csv = this.export("Salary", -1234.56);
        Assertions.assertTrue(csv.contains("-1234.56"));
        Assertions.assertFalse(csv.contains("'-1234.56"));
    }

    @Test
    void leavesOrdinaryTextAlone() {
        String csv = this.export("Grocery shopping", 10.0);
        Assertions.assertTrue(csv.contains("Grocery shopping"));
        Assertions.assertFalse(csv.contains("'Grocery"));
    }

    @Test
    void leavesEmptyCellsAlone() {
        Assertions.assertFalse(this.export("", 10.0).contains("'"));
    }
}
