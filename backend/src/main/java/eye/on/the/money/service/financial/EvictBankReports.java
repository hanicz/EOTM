package eye.on.the.money.service.financial;

import org.springframework.cache.annotation.CacheEvict;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@CacheEvict(cacheNames = {BankReportCaches.MONTHLY, BankReportCaches.INCOME, BankReportCaches.CATEGORY,
        BankReportCaches.YEARLY}, key = "#userId")
public @interface EvictBankReports {
}
