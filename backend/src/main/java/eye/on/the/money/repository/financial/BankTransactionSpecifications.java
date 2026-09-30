package eye.on.the.money.repository.financial;

import eye.on.the.money.dto.in.BankTransactionQuery;
import eye.on.the.money.model.financial.BankTransaction;
import eye.on.the.money.util.LikePatterns;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BankTransactionSpecifications {

    private static final List<String> SEARCH_FIELDS =
            List.of("partnerName", "partnerAccount", "memo", "type", "bankTransactionId");

    private BankTransactionSpecifications() {
    }

    public static Specification<BankTransaction> matching(Long userId, BankTransactionQuery query) {
        return (root, criteriaQuery, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("user").get("id"), userId));
            if (query.search() != null && !query.search().isBlank()) {
                predicates.add(search(root, builder, query.search().trim()));
            }
            if (query.flag() != null) {
                predicates.add(switch (query.flag()) {
                    case TAXABLE -> builder.isTrue(root.get("taxable"));
                    case NOT_TAXABLE -> builder.isFalse(root.get("taxable"));
                    case EXCLUDED -> builder.isTrue(root.get("excluded"));
                    case COUNTED -> builder.isFalse(root.get("excluded"));
                });
            }
            if (query.categoryId() != null) {
                predicates.add(query.categoryId() == BankTransactionQuery.UNCATEGORIZED
                        ? builder.isNull(root.get("category"))
                        : builder.equal(root.get("category").get("id"), query.categoryId()));
            }
            if (query.from() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("bookingDate"), query.from()));
            }
            if (query.to() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("bookingDate"), query.to()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate search(Root<BankTransaction> root, CriteriaBuilder builder, String search) {
        String pattern = LikePatterns.contains(search.toLowerCase(Locale.ROOT));
        List<Predicate> any = new ArrayList<>();
        for (String field : SEARCH_FIELDS) {
            any.add(builder.like(builder.lower(root.get(field)), pattern, LikePatterns.ESCAPE));
        }
        any.add(builder.like(root.get("amount").cast(String.class), pattern, LikePatterns.ESCAPE));
        return builder.or(any.toArray(Predicate[]::new));
    }
}
