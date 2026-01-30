package com.omega.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderDto {

    @NotBlank(message = "Телефон обязателен")
    @Pattern(regexp = "^\\(\\+\\d{3}\\)-\\d{3}-\\d{3}-\\d{3}$",
            message = "Телефон должен быть в формате (+996)-999-999-999")
    private String phone;

    @NotBlank(message = "Адрес доставки обязателен")
    @Size(min = 5, max = 500, message = "Адрес доставки должен быть от 5 до 500 символов")
    private String deliveryAddress;

    private String comment;

    // Данные карты для оплаты
    private String cardNumber;
    private String cvv;
}


