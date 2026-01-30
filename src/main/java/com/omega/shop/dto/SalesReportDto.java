package com.omega.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportDto {
    private Long productId;
    private String productName;
    private String categoryName;
    private Long totalSold;
    // Информация о продавце
    private Long sellerId;
    private String sellerName;
    private String sellerEmail;
    // Сумма продаж и зарплата
    private java.math.BigDecimal totalSalesAmount; // Общая сумма продаж
    private java.math.BigDecimal salary; // Зарплата (10% от суммы продаж)
}






