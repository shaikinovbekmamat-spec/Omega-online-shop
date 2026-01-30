package com.omega.shop.service;

import com.omega.shop.dto.SalesReportDto;
import com.omega.shop.entity.Order;
import com.omega.shop.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final OrderItemRepository orderItemRepository;

    /**
     * Получить отчет о продажах товаров за период
     */
    @Transactional(readOnly = true)
    public List<SalesReportDto> getSalesReport(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Генерация отчета о продажах с {} по {}", startDate, endDate);

        List<Object[]> results = orderItemRepository.getSalesReport(
                startDate, 
                endDate, 
                Order.OrderStatus.CANCELLED
        );

        return results.stream()
                .map(row -> {
                    SalesReportDto dto = new SalesReportDto();
                    dto.setProductId(((Number) row[0]).longValue());
                    dto.setProductName((String) row[1]);
                    dto.setCategoryName((String) row[2]);
                    dto.setTotalSold(((Number) row[3]).longValue());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Получить отчет о продажах за последние N дней
     */
    public List<SalesReportDto> getSalesReportLastDays(int days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);
        return getSalesReport(startDate, endDate);
    }

    /**
     * Получить отчет о продажах за текущий месяц
     */
    public List<SalesReportDto> getSalesReportCurrentMonth() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endDate = now;
        return getSalesReport(startDate, endDate);
    }
}

