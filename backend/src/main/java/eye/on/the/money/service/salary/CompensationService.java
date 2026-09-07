package eye.on.the.money.service.salary;

import eye.on.the.money.dto.in.CompensationCompareDTO;
import eye.on.the.money.dto.in.CompensationEditDTO;
import eye.on.the.money.dto.in.CompensationItemInputDTO;
import eye.on.the.money.dto.out.CompensationItemDTO;
import eye.on.the.money.dto.out.CompensationPackageDTO;
import eye.on.the.money.dto.out.SalaryDTO;
import eye.on.the.money.dto.out.SalaryNetDTO;
import eye.on.the.money.exception.ValidationException;
import eye.on.the.money.model.salary.*;
import eye.on.the.money.repository.salary.CompensationRepository;
import eye.on.the.money.service.shared.SalaryTaxCalculator;
import eye.on.the.money.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CompensationService {

    private static final BigDecimal MONTHS_IN_YEAR = BigDecimal.valueOf(12);
    private static final BigDecimal PERCENT = BigDecimal.valueOf(100);

    private final CompensationRepository compensationRepository;
    private final SalaryService salaryService;
    private final SalaryTaxCalculator salaryTaxCalculator;
    private final UserService userService;

    public Optional<CompensationPackageDTO> getCurrentPackage(Long userId) {
        return this.currentSalary(userId).map(salary -> this.buildPackage(this.baseOf(salary),
                this.compensationRepository.findByUserIdOrderByNameAscIdAsc(userId).stream()
                        .map(ItemSource::of).toList()));
    }

    public CompensationPackageDTO comparePackage(CompensationCompareDTO compareDTO) {
        List<CompensationItemInputDTO> inputs = compareDTO.items() == null ? List.of() : compareDTO.items();
        inputs.forEach(input -> this.rejectInvalidAmount(input.amountMode(), input.monthlyAmount(), input.percent()));

        return this.buildPackage(this.baseOf(compareDTO), inputs.stream().map(ItemSource::of).toList());
    }

    @Transactional
    public CompensationItemDTO createItem(Long userId, CompensationEditDTO editDTO) {
        this.rejectInvalidAmount(editDTO.amountMode(), editDTO.monthlyAmount(), editDTO.percent());

        CompensationItem item = CompensationItem.builder()
                .name(editDTO.name().trim())
                .amountMode(editDTO.amountMode())
                .monthlyAmount(editDTO.monthlyAmount())
                .percent(editDTO.percent())
                .taxTreatment(editDTO.taxTreatment())
                .note(this.trimToNull(editDTO.note()))
                .user(this.userService.getReference(userId))
                .build();

        return this.priceAgainstCurrent(userId, this.compensationRepository.saveAndFlush(item));
    }

    @Transactional
    public CompensationItemDTO updateItem(Long userId, Long id, CompensationEditDTO editDTO) {
        this.rejectInvalidAmount(editDTO.amountMode(), editDTO.monthlyAmount(), editDTO.percent());

        CompensationItem item = this.compensationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("Compensation item not found: " + id));

        item.setName(editDTO.name().trim());
        item.setAmountMode(editDTO.amountMode());
        item.setMonthlyAmount(editDTO.monthlyAmount());
        item.setPercent(editDTO.percent());
        item.setTaxTreatment(editDTO.taxTreatment());
        item.setNote(this.trimToNull(editDTO.note()));

        return this.priceAgainstCurrent(userId, this.compensationRepository.saveAndFlush(item));
    }

    @Transactional
    public void deleteItemsByIds(Long userId, List<Long> ids) {
        this.compensationRepository.deleteByUserIdAndIdIn(userId, ids);
    }

    private Optional<SalaryDTO> currentSalary(Long userId) {
        List<SalaryDTO> salaries = this.salaryService.getSalaries(userId);
        return salaries.stream().filter(salary -> salary.getValidTo() == null).findFirst()
                .or(() -> salaries.stream().findFirst());
    }

    private CompensationItemDTO priceAgainstCurrent(Long userId, CompensationItem item) {
        Base base = this.currentSalary(userId).map(this::baseOf).orElse(null);
        String currencyId = base == null ? null : base.currencyId();
        BigDecimal grossAnnual = base == null ? BigDecimal.ZERO : base.grossAnnual();

        return this.price(ItemSource.of(item), currencyId, grossAnnual);
    }

    private Base baseOf(SalaryDTO salary) {
        return new Base(salary.getNote(), salary.getCurrencyId(), salary.getBasis(), salary.getDependents(),
                salary.getValidFrom(), salary.getValidTo(), salary.getGrossMonthly(), salary.getGrossAnnual(),
                salary.getNetMonthly(), salary.getNetAnnual());
    }

    private Base baseOf(CompensationCompareDTO compareDTO) {
        String currencyId = compareDTO.currencyId();
        BigDecimal grossMonthly = SalaryBasis.ANNUAL == compareDTO.baseBasis()
                ? compareDTO.baseAmount().divide(MONTHS_IN_YEAR, 2, RoundingMode.HALF_UP)
                : compareDTO.baseAmount();
        BigDecimal grossAnnual = SalaryBasis.ANNUAL == compareDTO.baseBasis()
                ? compareDTO.baseAmount()
                : compareDTO.baseAmount().multiply(MONTHS_IN_YEAR);
        SalaryNetDTO net = this.salaryTaxCalculator.calculate(grossMonthly, currencyId, compareDTO.dependents());

        return new Base(this.trimToNull(compareDTO.label()), currencyId, compareDTO.baseBasis(),
                compareDTO.dependents(), null, null, grossMonthly, grossAnnual,
                net.getNetMonthly(), net.getNetMonthly().multiply(MONTHS_IN_YEAR));
    }

    private CompensationPackageDTO buildPackage(Base base, List<ItemSource> sources) {
        List<CompensationItemDTO> items = sources.stream()
                .map(source -> this.price(source, base.currencyId(), base.grossAnnual())).toList();

        BigDecimal itemsGrossMonthly = items.stream().map(CompensationItemDTO::getGrossMonthly)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal itemsNetMonthly = items.stream().map(CompensationItemDTO::getNetMonthly)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGrossMonthly = base.grossMonthly().add(itemsGrossMonthly);
        BigDecimal totalNetMonthly = base.netMonthly().add(itemsNetMonthly);

        return CompensationPackageDTO.builder()
                .label(base.label())
                .currencyId(base.currencyId())
                .basis(base.basis())
                .dependents(base.dependents())
                .validFrom(base.validFrom())
                .validTo(base.validTo())
                .baseGrossMonthly(base.grossMonthly())
                .baseGrossAnnual(base.grossAnnual())
                .baseNetMonthly(base.netMonthly())
                .baseNetAnnual(base.netAnnual())
                .items(items)
                .totalGrossMonthly(totalGrossMonthly)
                .totalGrossAnnual(totalGrossMonthly.multiply(MONTHS_IN_YEAR))
                .totalNetMonthly(totalNetMonthly)
                .totalNetAnnual(totalNetMonthly.multiply(MONTHS_IN_YEAR))
                .build();
    }

    private CompensationItemDTO price(ItemSource source, String currencyId, BigDecimal baseGrossAnnual) {
        BigDecimal monthly = this.resolveMonthly(source, baseGrossAnnual);
        BigDecimal netMonthly = switch (source.taxTreatment()) {
            case TAXED_AS_SALARY -> this.salaryTaxCalculator.calculate(monthly, currencyId, 0).getNetMonthly();
            case TAX_FREE, RECEIVED_NET -> this.salaryTaxCalculator.round(monthly, currencyId);
        };
        BigDecimal grossMonthly = CompensationTaxTreatment.RECEIVED_NET == source.taxTreatment()
                ? null
                : this.salaryTaxCalculator.round(monthly, currencyId);

        return CompensationItemDTO.builder()
                .id(source.id())
                .name(source.name())
                .amountMode(source.amountMode())
                .monthlyAmount(source.monthlyAmount())
                .percent(source.percent())
                .taxTreatment(source.taxTreatment())
                .note(source.note())
                .currencyId(currencyId)
                .grossMonthly(grossMonthly)
                .grossAnnual(grossMonthly == null ? null : grossMonthly.multiply(MONTHS_IN_YEAR))
                .netMonthly(netMonthly)
                .netAnnual(netMonthly.multiply(MONTHS_IN_YEAR))
                .build();
    }

    private BigDecimal resolveMonthly(ItemSource source, BigDecimal baseGrossAnnual) {
        if (CompensationAmountMode.PERCENT_OF_ANNUAL == source.amountMode()) {
            return baseGrossAnnual.multiply(source.percent())
                    .divide(PERCENT.multiply(MONTHS_IN_YEAR), 2, RoundingMode.HALF_UP);
        }
        return source.monthlyAmount();
    }

    private void rejectInvalidAmount(CompensationAmountMode amountMode, BigDecimal monthlyAmount,
                                     BigDecimal percent) {
        if (CompensationAmountMode.PERCENT_OF_ANNUAL == amountMode) {
            if (percent == null) {
                throw new ValidationException("A percentage is needed when the item is a share of the annual salary");
            }
            if (monthlyAmount != null) {
                throw new ValidationException("An item is either a monthly amount or a percentage, not both");
            }
            return;
        }
        if (monthlyAmount == null) {
            throw new ValidationException("A monthly amount is needed when the item is not a percentage");
        }
        if (percent != null) {
            throw new ValidationException("An item is either a monthly amount or a percentage, not both");
        }
    }

    private String trimToNull(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record Base(String label, String currencyId, SalaryBasis basis, int dependents, LocalDate validFrom,
                        LocalDate validTo, BigDecimal grossMonthly, BigDecimal grossAnnual, BigDecimal netMonthly,
                        BigDecimal netAnnual) {
    }

    private record ItemSource(Long id, String name, CompensationAmountMode amountMode, BigDecimal monthlyAmount,
                              BigDecimal percent, CompensationTaxTreatment taxTreatment, String note) {

        private static ItemSource of(CompensationItem item) {
            return new ItemSource(item.getId(), item.getName(), item.getAmountMode(), item.getMonthlyAmount(),
                    item.getPercent(), item.getTaxTreatment(), item.getNote());
        }

        private static ItemSource of(CompensationItemInputDTO input) {
            return new ItemSource(null, input.name().trim(), input.amountMode(), input.monthlyAmount(),
                    input.percent(), input.taxTreatment(), null);
        }
    }
}
