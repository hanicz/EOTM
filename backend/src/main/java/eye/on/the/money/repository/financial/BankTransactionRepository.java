package eye.on.the.money.repository.financial;

import eye.on.the.money.dto.out.MonthlyCashFlowDTO;
import eye.on.the.money.dto.out.MonthlyIncomeDTO;
import eye.on.the.money.model.financial.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    List<BankTransaction> findByUserIdOrderByBookingDateDesc(Long userId);

    List<BankTransaction> findByUserIdOrderByBookingDate(Long userId);

    List<BankTransaction> findByUserIdAndTaxableTrueOrderByBookingDateDesc(Long userId);

    List<BankTransaction> findByUserIdAndIdIn(Long userId, List<Long> ids);

    void deleteByUserIdAndIdIn(Long userId, List<Long> ids);

    Optional<BankTransaction> findByIdAndUserId(Long id, Long userId);

    Optional<BankTransaction> findByUserIdAndBankTransactionIdAndBookingDateAndTypeAndAmountAndMemo(
            Long userId, String bankTransactionId, LocalDate bookingDate, String type, Double amount, String memo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE BankTransaction b SET b.excluded = :excluded WHERE b.user.id = :userId AND b.id IN :ids")
    int updateExcludedByUserIdAndIdIn(@Param("userId") Long userId, @Param("ids") List<Long> ids,
                                         @Param("excluded") boolean excluded);

    @Query("""
            SELECT new eye.on.the.money.dto.out.MonthlyCashFlowDTO(
                YEAR(b.bookingDate),
                MONTH(b.bookingDate),
                b.currency.id,
                SUM(CASE WHEN b.amount > 0 THEN b.amount ELSE 0.0 END),
                SUM(CASE WHEN b.amount < 0 THEN b.amount ELSE 0.0 END))
            FROM BankTransaction b
            WHERE b.user.id = :userId AND b.excluded = false
            GROUP BY YEAR(b.bookingDate), MONTH(b.bookingDate), b.currency.id
            ORDER BY YEAR(b.bookingDate) DESC, MONTH(b.bookingDate) DESC, b.currency.id
            """)
    List<MonthlyCashFlowDTO> findMonthlyCashFlow(@Param("userId") Long userId);

    @Query("""
            SELECT new eye.on.the.money.dto.out.MonthlyCashFlowDTO(
                YEAR(b.bookingDate),
                MONTH(b.bookingDate),
                b.currency.id,
                SUM(CASE WHEN b.amount > 0 THEN b.amount ELSE 0.0 END),
                SUM(CASE WHEN b.amount < 0 THEN b.amount ELSE 0.0 END))
            FROM BankTransaction b
            WHERE b.user.id = :userId AND b.excluded = false
                AND b.bookingDate BETWEEN :from AND :to
            GROUP BY YEAR(b.bookingDate), MONTH(b.bookingDate), b.currency.id
            ORDER BY YEAR(b.bookingDate) DESC, MONTH(b.bookingDate) DESC, b.currency.id
            """)
    List<MonthlyCashFlowDTO> findCashFlowBetween(@Param("userId") Long userId,
                                                 @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT new eye.on.the.money.dto.out.MonthlyIncomeDTO(
                YEAR(b.bookingDate),
                MONTH(b.bookingDate),
                b.currency.id,
                COALESCE(NULLIF(TRIM(b.partnerName), ''), b.type),
                SUM(b.amount),
                COUNT(b))
            FROM BankTransaction b
            WHERE b.user.id = :userId AND b.excluded = false AND b.amount > 0
            GROUP BY YEAR(b.bookingDate), MONTH(b.bookingDate), b.currency.id,
                     COALESCE(NULLIF(TRIM(b.partnerName), ''), b.type)
            ORDER BY YEAR(b.bookingDate) DESC, MONTH(b.bookingDate) DESC, b.currency.id, SUM(b.amount) DESC
            """)
    List<MonthlyIncomeDTO> findMonthlyIncome(@Param("userId") Long userId);
}
