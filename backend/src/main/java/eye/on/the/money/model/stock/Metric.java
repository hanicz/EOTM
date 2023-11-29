package eye.on.the.money.model.stock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Slf4j
@ToString
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Generated
public class Metric {

    private Double tenDayAverageTradingVolume;
    private Double threeMonthAverageTradingVolume;
    private Double yearHigh;
    private Date yearHighDate;
    private Double yearLow;
    private Date yearLowDate;
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
    public Date getYearHighDate() {
        return yearHighDate;
    }

    @JsonProperty("52WeekHighDate")
    public void setYearHighDate(Date yearHighDate) {
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
    public Date getYearLowDate() {
        return yearLowDate;
    }

    @JsonProperty("52WeekLowDate")
    public void setYearLowDate(Date yearLowDate) {
        this.yearLowDate = yearLowDate;
    }

    public Double getPeInclExtraTTM() {
        return peInclExtraTTM;
    }

    public void setPeInclExtraTTM(Double peInclExtraTTM) {
        this.peInclExtraTTM = peInclExtraTTM;
    }
}
