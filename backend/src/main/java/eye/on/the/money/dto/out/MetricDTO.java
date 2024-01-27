package eye.on.the.money.dto.out;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

@Slf4j
@ToString
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Generated
public class MetricDTO {

    private Double tenDayAverageTradingVolume;
    private Double threeMonthAverageTradingVolume;
    private Double yearHigh;
    private LocalDate yearHighDate;
    private Double yearLow;
    private LocalDate yearLowDate;
    private Double peInclExtraTTM;

    @JsonProperty("tenDayAverageTradingVolume")
    public Double getTenDayAverageTradingVolume() {
        return tenDayAverageTradingVolume;
    }

    @JsonProperty("10DayAverageTradingVolume")
    public void setTenDayAverageTradingVolume(Double tenDayAverageTradingVolume) {
        this.tenDayAverageTradingVolume = tenDayAverageTradingVolume;
    }

    @JsonProperty("threeMonthAverageTradingVolume")
    public Double getThreeMonthAverageTradingVolume() {
        return threeMonthAverageTradingVolume;
    }

    @JsonProperty("3MonthAverageTradingVolume")
    public void setThreeMonthAverageTradingVolume(Double threeMonthAverageTradingVolume) {
        this.threeMonthAverageTradingVolume = threeMonthAverageTradingVolume;
    }

    @JsonProperty("yearHigh")
    public Double getYearHigh() {
        return yearHigh;
    }

    @JsonProperty("52WeekHigh")
    public void setYearHigh(Double yearHigh) {
        this.yearHigh = yearHigh;
    }

    @JsonProperty("yearHighDate")
    public LocalDate getYearHighDate() {
        return yearHighDate;
    }

    @JsonProperty("52WeekHighDate")
    public void setYearHighDate(LocalDate yearHighDate) {
        this.yearHighDate = yearHighDate;
    }

    @JsonProperty("yearLow")
    public Double getYearLow() {
        return yearLow;
    }

    @JsonProperty("52WeekLow")
    public void setYearLow(Double yearLow) {
        this.yearLow = yearLow;
    }

    @JsonProperty("yearLowDate")
    public LocalDate getYearLowDate() {
        return yearLowDate;
    }

    @JsonProperty("52WeekLowDate")
    public void setYearLowDate(LocalDate yearLowDate) {
        this.yearLowDate = yearLowDate;
    }

    public Double getPeInclExtraTTM() {
        return peInclExtraTTM;
    }

    public void setPeInclExtraTTM(Double peInclExtraTTM) {
        this.peInclExtraTTM = peInclExtraTTM;
    }
}
