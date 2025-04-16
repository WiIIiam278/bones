/*
 * MIT License
 *
 * Copyright (c) 2024 William278
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.william278.backend.service;

import com.google.common.collect.Maps;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import net.william278.backend.database.model.Transaction;
import net.william278.backend.database.repository.ProjectRepository;
import net.william278.backend.database.repository.TransactionRepository;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
public class ChartService {

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("dd MMM")
            .withLocale(Locale.UK).withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM")
            .withLocale(Locale.UK).withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy")
            .withLocale(Locale.UK).withZone(ZoneId.systemDefault());

    private final ProjectRepository projects;
    private final TransactionRepository transactions;

    @Autowired
    public ChartService(ProjectRepository projects, TransactionRepository transactions) {
        this.projects = projects;
        this.transactions = transactions;
    }

    @NotNull
    public Chart getTransactionsChart(int numberPastDays, int daysPerDataPoint) {
        final List<Transaction> found = transactions
                .findAllByTimestampAfterAndProjectGrantIsNotNullOrderByTimestampDesc(Instant.now().minus(numberPastDays, ChronoUnit.DAYS));
        final Map<String, Series> series = Maps.newHashMap();
        projects.findAllByRestrictedTrue().forEach(p -> series
                .put(p.getSlug(), new Series(p.getMetadata().getName(), "bar", "Total", new ArrayList<>())));
        final List<String> xAxisLabels = Lists.newArrayList();

        // Mutable data for iteration
        Instant periodStart = Instant.now().minus(daysPerDataPoint, ChronoUnit.DAYS);
        Instant periodEnd = Instant.now();
        BigDecimal total = BigDecimal.ZERO;
        String currentXAxisLabel;
        for (Transaction transaction : found) {
            final Series s = series.get(transaction.getProjectGrantSlug());
            if (s == null) {
                continue;
            }

            if (transaction.getTimestamp().isBefore(periodStart)) {
                periodStart = periodStart.minus(daysPerDataPoint, ChronoUnit.DAYS);
                periodEnd = periodEnd.minus(daysPerDataPoint, ChronoUnit.DAYS);

                currentXAxisLabel = getLabelFor(periodStart, periodEnd, daysPerDataPoint);
                for (Series other : series.values()) {
                    other.data().add(0d);
                }
                xAxisLabels.add(currentXAxisLabel);
            }
            series.put(transaction.getProjectGrantSlug(), s);

            // Add the data point
            if (s.data().isEmpty()) {
                s.data().add(roundCurrency(transaction.getAmount().doubleValue()));
            } else {
                s.data().set(s.data().size() - 1, roundCurrency(s.data.getLast() + transaction.getAmount().doubleValue()));
            }
            total = total.add(transaction.getAmount());
        }

        final Series sTotal = new Series("Total", "line", null, new ArrayList<>());
        series.values().forEach(s -> {
            for (int i = 0; i < xAxisLabels.size(); i++) {
                if (sTotal.data().size() > i) {
                    sTotal.data().set(i, roundCurrency(sTotal.data().get(i) + s.data().get(i)));
                } else {
                    sTotal.data().add(roundCurrency(s.data().get(i)));
                }
            }
        });
        series.put("Total", sTotal);

        return new Chart(
                new Axis("category", xAxisLabels.reversed(), false),
                new Axis("value"),
                series.values().stream().peek(s -> Collections.reverse(s.data())).toList(),
                NumberFormat.getCurrencyInstance(Locale.UK).format(total)
        );
    }

    private static double roundCurrency(double value) {
        long factor = (long) Math.pow(10, 2);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    @NotNull
    private static String getLabelFor(final Instant start, final Instant end, int dayGroupings) {
        final String startDay = DAY_FORMATTER.format(start);
        return switch (dayGroupings) {
            case 30 -> {
                final String month = MONTH_FORMATTER.format(start);
                if (month.equals("Jan")) {
                    yield "Jan " + YEAR_FORMATTER.format(start);
                }
                yield month;
            }
            case 365, 366 -> YEAR_FORMATTER.format(start);
            default -> startDay;
        };
    }

    @Schema(description = "A chart containing axis and data for visualisation")
    public record Chart(Axis xAxis, Axis yAxis, List<Series> series, String totalValue) {
    }

    @Schema(description = "An axis of a chart")
    public record Axis(String type, List<String> data, boolean boundaryGap) {
        public Axis(String type) {
            this(type, null, false);
        }
    }

    @Schema(description = "A data point series for a chart")
    public record Series(String name, String type, @Nullable String stack, List<Double> data) {
    }

}
