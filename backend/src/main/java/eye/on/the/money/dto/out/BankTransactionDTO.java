package eye.on.the.money.dto.out;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import eye.on.the.money.dto.CSVHelper;
import eye.on.the.money.model.financial.BankTransaction;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@Builder
@Slf4j
@ToString
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
public class BankTransactionDTO implements CSVHelper {

    public static final String BOOKING_DATE = "Booking Date";
    public static final String BANK_TRANSACTION_ID = "Bank Transaction Id";
    public static final String TYPE = "Type";
    public static final String ACCOUNT_NUMBER = "Account Number";
    public static final String ACCOUNT_NAME = "Account Name";
    public static final String PARTNER_ACCOUNT = "Partner Account";
    public static final String PARTNER_NAME = "Partner Name";
    public static final String AMOUNT = "Amount";
    public static final String CURRENCY = "Currency";
    public static final String MEMO = "Memo";

    private static final char NO_BREAK_SPACE = (char) 0x00A0;
    private static final char NARROW_NO_BREAK_SPACE = (char) 0x202F;
    private static final char GROUPING_SEPARATOR = '.';
    private static final char DECIMAL_SEPARATOR = ',';

    public static final String[] KH_HEADERS = {
            BOOKING_DATE, BANK_TRANSACTION_ID, TYPE, ACCOUNT_NUMBER, ACCOUNT_NAME, PARTNER_ACCOUNT, PARTNER_NAME,
            AMOUNT, CURRENCY, MEMO,
            "Payer Name", "Payer Id Type", "Payer Id",
            "Beneficiary Name", "Beneficiary Id Type", "Beneficiary Id",
            "Partner Unique Id", "Organisation Id",
            "Transfer Purpose", "Transfer Purpose Category", "Partner Secondary Account Id"
    };

    private Long id;
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate bookingDate;
    private String bankTransactionId;
    private String type;
    private String accountNumber;
    private String accountName;
    private String partnerAccount;
    private String partnerName;
    private Double amount;
    private String currencyId;
    private String memo;
    private boolean excluded;
    private boolean taxable;

    @Override
    @JsonIgnore
    public Object[] getHeaders() {
        return new String[]{"Id", BOOKING_DATE, BANK_TRANSACTION_ID, TYPE, ACCOUNT_NUMBER, ACCOUNT_NAME,
                PARTNER_ACCOUNT, PARTNER_NAME, AMOUNT, CURRENCY, MEMO, "Excluded", "Taxable"};
    }

    @Override
    @JsonIgnore
    public Object[] getCSVRecord() {
        return new Object[]{this.getId(), this.getBookingDate(), this.getBankTransactionId(), this.getType(),
                this.getAccountNumber(), this.getAccountName(), this.getPartnerAccount(), this.getPartnerName(),
                this.getAmount(), this.getCurrencyId(), this.getMemo(), this.isExcluded(), this.isTaxable()};
    }

    public static BankTransactionDTO createFromKHRecord(CSVRecord csvRecord, DateTimeFormatter formatter) {
        return BankTransactionDTO.builder()
                .bookingDate(LocalDate.parse(value(csvRecord, BOOKING_DATE), formatter))
                .bankTransactionId(value(csvRecord, BANK_TRANSACTION_ID))
                .type(value(csvRecord, TYPE))
                .accountNumber(value(csvRecord, ACCOUNT_NUMBER))
                .accountName(value(csvRecord, ACCOUNT_NAME))
                .partnerAccount(value(csvRecord, PARTNER_ACCOUNT))
                .partnerName(value(csvRecord, PARTNER_NAME))
                .amount(parseAmount(value(csvRecord, AMOUNT)))
                .currencyId(value(csvRecord, CURRENCY).toUpperCase())
                .memo(truncate(value(csvRecord, MEMO)))
                .build();
    }

    private static String value(CSVRecord csvRecord, String header) {
        return csvRecord.isSet(header) ? csvRecord.get(header).trim() : "";
    }

    private static String truncate(String memo) {
        return memo.length() > BankTransaction.MEMO_MAX_LENGTH ? memo.substring(0, BankTransaction.MEMO_MAX_LENGTH) : memo;
    }

    private static Double parseAmount(String amount) {
        StringBuilder normalised = new StringBuilder();
        for (char character : amount.toCharArray()) {
            if (Character.isWhitespace(character) || character == NO_BREAK_SPACE
                    || character == NARROW_NO_BREAK_SPACE || character == GROUPING_SEPARATOR) {
                continue;
            }
            normalised.append(character == DECIMAL_SEPARATOR ? '.' : character);
        }
        if (normalised.isEmpty()) {
            throw new IllegalArgumentException("Missing amount");
        }
        return Double.parseDouble(normalised.toString());
    }
}
