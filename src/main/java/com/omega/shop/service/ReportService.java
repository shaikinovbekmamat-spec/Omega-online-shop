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
     * Получить отчет о продажах по продавцам за период
     */
    @Transactional(readOnly = true)
    public List<SalesReportDto> getSalesReport(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Генерация отчета о продажах по продавцам с {} по {}", startDate, endDate);

        try {
            List<Object[]> results = orderItemRepository.getSalesReportBySeller(
                    startDate, 
                    endDate, 
                    Order.OrderStatus.DELIVERED
            );

            log.info("Получено {} записей продавцов из БД для отчета (только завершенные заказы)", results.size());
            
            if (results.isEmpty()) {
                log.warn("Нет данных о продажах за период с {} по {}. Проверьте наличие завершенных заказов (DELIVERED).", startDate, endDate);
            }

            List<SalesReportDto> report = results.stream()
                    .map(row -> {
                        SalesReportDto dto = new SalesReportDto();
                        
                        // Информация о продавце
                        if (row[0] != null) {
                            try {
                                Number sellerId = (Number) row[0];
                                if (sellerId != null && sellerId.longValue() > 0) {
                                    dto.setSellerId(sellerId.longValue());
                                }
                            } catch (Exception e) {
                                log.warn("Ошибка получения sellerId: {}", e.getMessage());
                            }
                        }
                        
                        if (row[1] != null && !row[1].toString().isEmpty()) {
                            dto.setSellerName((String) row[1]);
                        } else {
                            dto.setSellerName("Без продавца");
                        }
                        
                        if (row[2] != null) {
                            dto.setSellerEmail((String) row[2]);
                        }
                        
                        // Количество проданных товаров
                        if (row[3] != null) {
                            dto.setTotalSold(((Number) row[3]).longValue());
                        } else {
                            dto.setTotalSold(0L);
                        }
                        
                        // Сумма продаж
                        if (row[4] != null) {
                            if (row[4] instanceof java.math.BigDecimal) {
                                dto.setTotalSalesAmount((java.math.BigDecimal) row[4]);
                            } else if (row[4] instanceof Number) {
                                dto.setTotalSalesAmount(java.math.BigDecimal.valueOf(((Number) row[4]).doubleValue()));
                            }
                        } else {
                            dto.setTotalSalesAmount(java.math.BigDecimal.ZERO);
                        }
                        
                        // Расчет зарплаты (10% от суммы продаж)
                        if (dto.getTotalSalesAmount() != null) {
                            java.math.BigDecimal salary = dto.getTotalSalesAmount()
                                    .multiply(java.math.BigDecimal.valueOf(0.10))
                                    .setScale(2, java.math.RoundingMode.HALF_UP);
                            dto.setSalary(salary);
                        } else {
                            dto.setSalary(java.math.BigDecimal.ZERO);
                        }
                        
                        return dto;
                    })
                    .collect(Collectors.toList());

            log.info("Сформирован отчет с {} продавцами", report.size());
            return report;
        } catch (Exception e) {
            log.error("Ошибка при генерации отчета: {}", e.getMessage(), e);
            // Возвращаем пустой список вместо выбрасывания исключения
            return List.of();
        }
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

    /**
     * Получить отчет о продажах конкретного продавца за период
     * Включает все статусы заказов (не только DELIVERED), чтобы показать полную картину
     */
    @Transactional(readOnly = true)
    public SalesReportDto getSellerSalesReport(com.omega.shop.entity.User seller, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Генерация отчета о продажах для продавца {} с {} по {}", seller.getUsername(), startDate, endDate);

        try {
            // Сначала пробуем получить только завершенные заказы (DELIVERED)
            List<Object[]> resultsDelivered = orderItemRepository.getSalesReportForSeller(
                    seller,
                    startDate,
                    endDate,
                    Order.OrderStatus.DELIVERED
            );
            
            // Если нет завершенных заказов, получаем все заказы продавца для информации
            List<Object[]> results = resultsDelivered;
            if (resultsDelivered.isEmpty()) {
                log.debug("Не найдено завершенных заказов для продавца {}, ищем все заказы", seller.getUsername());
                // Получаем все заказы продавца (любого статуса кроме CANCELLED)
                results = orderItemRepository.getSalesReportForSellerAllStatuses(
                        seller,
                        startDate,
                        endDate,
                        Order.OrderStatus.CANCELLED
                );
            }

            SalesReportDto sellerReport = results.stream()
                    .findFirst()
                    .map(row -> {
                        SalesReportDto dto = new SalesReportDto();
                        dto.setSellerId(seller.getId());
                        dto.setSellerName(seller.getUsername());
                        dto.setSellerEmail(seller.getEmail());

                        if (row[3] != null) {
                            dto.setTotalSold(((Number) row[3]).longValue());
                        } else {
                            dto.setTotalSold(0L);
                        }

                        if (row[4] != null) {
                            if (row[4] instanceof java.math.BigDecimal) {
                                dto.setTotalSalesAmount((java.math.BigDecimal) row[4]);
                            } else if (row[4] instanceof Number) {
                                dto.setTotalSalesAmount(java.math.BigDecimal.valueOf(((Number) row[4]).doubleValue()));
                            }
                        } else {
                            dto.setTotalSalesAmount(java.math.BigDecimal.ZERO);
                        }

                        if (dto.getTotalSalesAmount() != null) {
                            java.math.BigDecimal salary = dto.getTotalSalesAmount()
                                    .multiply(java.math.BigDecimal.valueOf(0.10))
                                    .setScale(2, java.math.RoundingMode.HALF_UP);
                            dto.setSalary(salary);
                        } else {
                            dto.setSalary(java.math.BigDecimal.ZERO);
                        }

                        return dto;
                    })
                    .orElseGet(() -> {
                        // Если продаж нет, возвращаем пустой отчет
                        SalesReportDto dto = new SalesReportDto();
                        dto.setSellerId(seller.getId());
                        dto.setSellerName(seller.getUsername());
                        dto.setSellerEmail(seller.getEmail());
                        dto.setTotalSold(0L);
                        dto.setTotalSalesAmount(java.math.BigDecimal.ZERO);
                        dto.setSalary(java.math.BigDecimal.ZERO);
                        return dto;
                    });

            log.info("Сформирован отчет для продавца {}: продано {} товаров на сумму {}", 
                    seller.getUsername(), sellerReport.getTotalSold(), sellerReport.getTotalSalesAmount());
            return sellerReport;
        } catch (Exception e) {
            log.error("Ошибка при генерации отчета для продавца: {}", e.getMessage(), e);
            SalesReportDto dto = new SalesReportDto();
            dto.setSellerId(seller.getId());
            dto.setSellerName(seller.getUsername());
            dto.setSellerEmail(seller.getEmail());
            dto.setTotalSold(0L);
            dto.setTotalSalesAmount(java.math.BigDecimal.ZERO);
            dto.setSalary(java.math.BigDecimal.ZERO);
            return dto;
        }
    }

    /**
     * Получить отчет о продажах продавца за последние N дней
     */
    public SalesReportDto getSellerSalesReportLastDays(com.omega.shop.entity.User seller, int days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);
        return getSellerSalesReport(seller, startDate, endDate);
    }

    /**
     * Получить отчет о продажах продавца за текущий месяц
     */
    public SalesReportDto getSellerSalesReportCurrentMonth(com.omega.shop.entity.User seller) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endDate = now;
        return getSellerSalesReport(seller, startDate, endDate);
    }
}

