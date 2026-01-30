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
}

