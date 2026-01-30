package com.omega.shop.controller;

import com.omega.shop.dto.SalesReportDto;
import com.omega.shop.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    /**
     * Страница отчетов
     */
    @GetMapping
    public String reportsPage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Integer days,
            Model model
    ) {
        try {
            List<SalesReportDto> salesReport;

            if (startDate != null && endDate != null) {
                // Отчет за указанный период
                salesReport = reportService.getSalesReport(startDate, endDate);
                model.addAttribute("startDate", startDate);
                model.addAttribute("endDate", endDate);
            } else if (days != null && days > 0) {
                // Отчет за последние N дней
                salesReport = reportService.getSalesReportLastDays(days);
                model.addAttribute("days", days);
            } else {
                // По умолчанию - текущий месяц
                salesReport = reportService.getSalesReportCurrentMonth();
                LocalDateTime now = LocalDateTime.now();
                model.addAttribute("startDate", now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0));
                model.addAttribute("endDate", now);
            }

            model.addAttribute("salesReport", salesReport);
            model.addAttribute("totalItems", salesReport.stream()
                    .mapToLong(SalesReportDto::getTotalSold)
                    .sum());

            return "admin/reports/sales";
        } catch (Exception e) {
            log.error("Ошибка генерации отчета: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Произошла ошибка при генерации отчета: " + e.getMessage());
            model.addAttribute("salesReport", List.of());
            model.addAttribute("totalItems", 0L);
            return "admin/reports/sales";
        }
    }
}

