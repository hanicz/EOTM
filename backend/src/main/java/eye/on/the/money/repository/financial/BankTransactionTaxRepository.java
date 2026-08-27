package eye.on.the.money.repository.financial;

import eye.on.the.money.model.financial.BankTransactionTax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BankTransactionTaxRepository extends JpaRepository<BankTransactionTax, Long> {

    @Query("select t from BankTransactionTax t join fetch t.bankTransaction b join fetch b.currency "
            + "where b.user.id = :userId and b.taxable = true order by b.bookingDate desc")
    List<BankTransactionTax> findTaxableByUserId(@Param("userId") Long userId);

    @Query("select t from BankTransactionTax t join fetch t.bankTransaction b join fetch b.currency "
            + "where b.user.id = :userId and b.id in :ids")
    List<BankTransactionTax> findByUserIdAndBankTransactionIdIn(@Param("userId") Long userId,
                                                                   @Param("ids") List<Long> ids);

    void deleteByBankTransactionIdIn(List<Long> ids);
}
