package eye.on.the.money.service.mail;

import eye.on.the.money.dto.out.AssetClassValueDTO;
import eye.on.the.money.dto.out.MonthlyCashFlowDTO;
import eye.on.the.money.dto.out.MonthlyReportDTO;
import eye.on.the.money.dto.out.NetWorthDTO;
import eye.on.the.money.util.HtmlUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.lang.String.format;

@Component
@Slf4j
public class MonthlyReportHtmlBuilder {

    private static final int MAX_TRADE_ROWS = 20;
    private static final String POSITIVE = "#3b6d11";
    private static final String NEGATIVE = "#a32d2d";
    private static final String INK = "#1b1b1b";
    private static final String MUTED = "#888780";
    private static final String LABEL = "#5f5e5a";
    private static final String RULE = "1px solid #f1efe8";
    private static final DateTimeFormatter MONTH_LABEL =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_LABEL =
            DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);

    public String periodLabel(MonthlyReportDTO report) {
        return YearMonth.of(report.getYear(), report.getMonth()).format(MONTH_LABEL);
    }

    public String html(MonthlyReportDTO report) {
        StringBuilder body = new StringBuilder();
        body.append(this.header(report));
        body.append(this.netWorthSection(report));
        body.append(this.allocationSection(report));
        body.append(this.activitySection(report));
        body.append(this.cashFlowSection(report));
        body.append(this.footer(report));

        return "<div style=\"background:#f6f4ee;padding:32px 16px;font-family:Arial,Helvetica,sans-serif;\">"
                + "<table role=\"presentation\" align=\"center\" width=\"480\" style=\"max-width:480px;width:100%;"
                + "background:#ffffff;border-radius:10px;border:1px solid #e3e0d5;border-collapse:collapse;\">"
                + body
                + "</table>"
                + "</div>";
    }

    public String plainText(MonthlyReportDTO report) {
        NetWorthDTO netWorth = report.getNetWorth();
        List<String> lines = new ArrayList<>();
        lines.add("Eye OTM monthly report - " + this.periodLabel(report));
        lines.add("");
        lines.add(format("Net worth: %s", this.money(netWorth.getTotalWorth(), report.getCurrency())));
        lines.add(format("Invested: %s", this.money(netWorth.getTotalSpent(), report.getCurrency())));
        lines.add(format("Change: %s", this.percent(netWorth.getTotalChangePct())));
        lines.add("");
        lines.add("Allocation");
        for (AssetClassValueDTO asset : netWorth.getAssets()) {
            lines.add(format("  %s: %s (%s)", asset.getAssetClass(),
                    this.money(asset.getWorth(), report.getCurrency()), this.percent(asset.getChangePct())));
        }

        MonthlyReportDTO.ActivitySection activity = report.getActivity();
        lines.add("");
        lines.add("Activity");
        lines.add(format("  Trades: %d", activity.tradeCount()));
        for (MonthlyReportDTO.AmountRow row : activity.dividendTotals()) {
            lines.add(format("  Dividends: %s", this.amount(row.amount(), row.currencyId())));
        }
        for (MonthlyReportDTO.AmountRow row : activity.interestTotals()) {
            lines.add(format("  Interest: %s", this.amount(row.amount(), row.currencyId())));
        }

        if (!report.getCashFlow().isEmpty()) {
            lines.add("");
            lines.add("Cash flow");
            for (MonthlyCashFlowDTO flow : report.getCashFlow()) {
                lines.add(format("  %s: in %s, out %s, net %s", flow.getCurrencyId(),
                        this.amount(flow.getMoneyIn(), flow.getCurrencyId()),
                        this.amount(flow.getMoneyOut(), flow.getCurrencyId()),
                        this.amount(flow.getNet(), flow.getCurrencyId())));
            }
        }
        return String.join("\n", lines);
    }

    private String header(MonthlyReportDTO report) {
        return "<tr><td style=\"background:#1b1b1b;padding:20px 28px;border-radius:10px 10px 0 0;\">"
                + "<span style=\"color:#ef9f27;font-size:20px;font-weight:bold;\">Eye OTM</span>"
                + "<span style=\"color:#c9c6bd;font-size:13px;float:right;padding-top:6px;\">"
                + HtmlUtil.escape(this.periodLabel(report)) + "</span>"
                + "</td></tr>";
    }

    private String netWorthSection(MonthlyReportDTO report) {
        NetWorthDTO netWorth = report.getNetWorth();
        BigDecimal change = netWorth.getTotalChangePct();
        return "<tr><td style=\"padding:28px 28px 8px;\">"
                + this.eyebrow("Portfolio")
                + "<h1 style=\"margin:0 0 20px;font-size:26px;color:" + INK + ";\">"
                + HtmlUtil.escape(this.money(netWorth.getTotalWorth(), report.getCurrency())) + "</h1>"
                + this.table(
                        this.row("Invested", this.money(netWorth.getTotalSpent(), report.getCurrency()), INK)
                        + this.row("Change", this.percent(change), this.colorFor(change)))
                + "</td></tr>";
    }

    private String allocationSection(MonthlyReportDTO report) {
        NetWorthDTO netWorth = report.getNetWorth();
        BigDecimal total = netWorth.getTotalWorth();
        StringBuilder rows = new StringBuilder();
        for (AssetClassValueDTO asset : netWorth.getAssets()) {
            if (asset.getWorth().signum() == 0) continue;
            rows.append(this.row(
                    asset.getAssetClass() + " · " + this.share(asset.getWorth(), total),
                    this.money(asset.getWorth(), report.getCurrency()),
                    this.colorFor(asset.getChangePct())));
        }
        if (rows.isEmpty()) {
            return this.section("Allocation", this.empty("Nothing held this month."));
        }
        return this.section("Allocation", this.table(rows.toString()));
    }

    private String activitySection(MonthlyReportDTO report) {
        MonthlyReportDTO.ActivitySection activity = report.getActivity();
        if (activity.isEmpty()) {
            return this.section("Activity", this.empty("No trades, dividends or interest this month."));
        }

        List<String> trades = new ArrayList<>();
        activity.stockTrades().forEach(trade -> trades.add(this.tradeRow(
                trade.getTransactionDate().format(DAY_LABEL), this.side(trade.getBuySell()),
                trade.getShortName(), trade.getAmount(), trade.getCurrencyId())));
        activity.etfTrades().forEach(trade -> trades.add(this.tradeRow(
                trade.getTransactionDate().format(DAY_LABEL), this.side(trade.getBuySell()),
                trade.getShortName(), trade.getAmount(), trade.getCurrencyId())));
        activity.cryptoTrades().forEach(trade -> trades.add(this.tradeRow(
                trade.getTransactionDate().format(DAY_LABEL), this.side(trade.getBuySell()),
                trade.getSymbol(), trade.getAmount(), trade.getCurrencyId())));
        activity.securityTrades().forEach(trade -> trades.add(this.tradeRow(
                trade.getTransactionDate().format(DAY_LABEL), this.side(trade.getBuySell()),
                trade.getSecurityName(), trade.getAmount(), trade.getCurrencyId())));
        activity.forexTrades().forEach(trade -> trades.add(this.tradeRow(
                trade.getTransactionDate().format(DAY_LABEL), "FX",
                trade.getFromCurrencyId() + " to " + trade.getToCurrencyId(),
                trade.getFromAmount(), trade.getFromCurrencyId())));

        StringBuilder rows = new StringBuilder();
        trades.stream().limit(MAX_TRADE_ROWS).forEach(rows::append);
        if (trades.size() > MAX_TRADE_ROWS) {
            rows.append(this.noteRow(format("+%d more", trades.size() - MAX_TRADE_ROWS)));
        }
        for (MonthlyReportDTO.AmountRow row : activity.dividendTotals()) {
            rows.append(this.row("Dividends", this.amount(row.amount(), row.currencyId()), POSITIVE));
        }
        for (MonthlyReportDTO.AmountRow row : activity.interestTotals()) {
            rows.append(this.row("Interest", this.amount(row.amount(), row.currencyId()), POSITIVE));
        }
        return this.section("Activity", this.table(rows.toString()));
    }

    private String cashFlowSection(MonthlyReportDTO report) {
        if (report.getCashFlow().isEmpty()) {
            return "";
        }
        StringBuilder rows = new StringBuilder();
        for (MonthlyCashFlowDTO flow : report.getCashFlow()) {
            rows.append(this.row("In", this.amount(flow.getMoneyIn(), flow.getCurrencyId()), INK));
            rows.append(this.row("Out", this.amount(flow.getMoneyOut(), flow.getCurrencyId()), INK));
            Double net = flow.getNet();
            rows.append(this.row("Net", this.amount(net, flow.getCurrencyId()),
                    net != null && net < 0 ? NEGATIVE : POSITIVE));
            Double saved = flow.getSavedPercent();
            if (saved != null) {
                rows.append(this.row("Saved", format("%.1f%%", saved), saved < 0 ? NEGATIVE : POSITIVE));
            }
        }
        return this.section("Cash flow", this.table(rows.toString()));
    }

    private String footer(MonthlyReportDTO report) {
        StringBuilder notes = new StringBuilder();
        List<String> unconverted = report.getNetWorth().getUnconvertedCurrencies();
        if (unconverted != null && !unconverted.isEmpty()) {
            notes.append("<p style=\"margin:0 0 8px;font-size:13px;color:" + NEGATIVE + ";line-height:1.5;\">"
                    + "No exchange rate for " + HtmlUtil.escape(String.join(", ", unconverted))
                    + ", so those holdings are missing from the totals.</p>");
        }
        notes.append("<p style=\"margin:0;font-size:13px;color:" + MUTED + ";line-height:1.5;\">"
                + "You can turn this report off on the Alerts &amp; Reports page.</p>");
        return "<tr><td style=\"padding:8px 28px 28px;border-top:" + RULE + ";\">" + notes + "</td></tr>";
    }

    private String section(String title, String content) {
        return "<tr><td style=\"padding:20px 28px 8px;\">" + this.eyebrow(title) + content + "</td></tr>";
    }

    private String eyebrow(String text) {
        return "<p style=\"margin:0 0 6px;font-size:12px;color:" + MUTED
                + ";text-transform:uppercase;letter-spacing:0.05em;\">" + HtmlUtil.escape(text) + "</p>";
    }

    private String table(String rows) {
        return "<table role=\"presentation\" style=\"width:100%;border-collapse:collapse;\">" + rows + "</table>";
    }

    private String row(String label, String value, String valueColor) {
        return "<tr>"
                + "<td style=\"padding:10px 0;border-top:" + RULE + ";color:" + LABEL + ";font-size:13px;\">"
                + HtmlUtil.escape(label) + "</td>"
                + "<td style=\"padding:10px 0;border-top:" + RULE + ";text-align:right;font-weight:600;color:"
                + valueColor + ";\">" + HtmlUtil.escape(value) + "</td>"
                + "</tr>";
    }

    private String tradeRow(String date, String side, String name, Double value, String currency) {
        return this.row(date + "  " + side + "  " + name, this.amount(value, currency), INK);
    }

    private String noteRow(String text) {
        return "<tr><td colspan=\"2\" style=\"padding:10px 0;border-top:" + RULE + ";color:" + MUTED
                + ";font-size:13px;\">" + HtmlUtil.escape(text) + "</td></tr>";
    }

    private String empty(String text) {
        return "<p style=\"margin:0;font-size:13px;color:" + MUTED + ";\">" + HtmlUtil.escape(text) + "</p>";
    }

    private String side(String buySell) {
        return "S".equals(buySell) ? "SELL" : "BUY";
    }

    private String colorFor(BigDecimal value) {
        if (value == null || value.signum() == 0) return INK;
        return value.signum() > 0 ? POSITIVE : NEGATIVE;
    }

    private String share(BigDecimal worth, BigDecimal total) {
        if (total == null || total.signum() == 0) return "0%";
        return worth.multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP) + "%";
    }

    private String percent(BigDecimal value) {
        if (value == null) return "-";
        return (value.signum() > 0 ? "+" : "") + value.setScale(2, RoundingMode.HALF_UP) + "%";
    }

    private String money(BigDecimal value, String currency) {
        if (value == null) return "-";
        return this.formatter().format(value) + " " + currency;
    }

    private String amount(Double value, String currency) {
        if (value == null) return "-";
        return this.formatter().format(value) + " " + (currency == null ? "" : currency.toUpperCase());
    }

    private NumberFormat formatter() {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.ENGLISH);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        return formatter;
    }
}
