package com.omega.shop.dto;

import com.omega.shop.entity.Product;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItem {

    private Long productId;
    private String productName;
    private BigDecimal price;
    private int quantity;
    private String imagePath;

    public CartItem() {
    }

    public CartItem(Product product, int quantity) {
        this.productId = product.getId();
        this.productName = product.getName();
        this.price = product.getPrice();
        this.quantity = quantity;
        this.imagePath = product.getImagePath();
    }

    /**
     * Получить общую стоимость этого элемента
     */
    public BigDecimal getTotalPrice() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Увеличить количество
     */
    public void increaseQuantity(int amount) {
        this.quantity += amount;
    }

    /**
     * Уменьшить количество
     */
    public void decreaseQuantity(int amount) {
        this.quantity -= amount;
        if (this.quantity < 0) {
            this.quantity = 0;
        }
    }

    /**
     * Установить количество
     */
    public void setQuantity(int quantity) {
        if (quantity < 0) {
            this.quantity = 0;
        } else {
            this.quantity = quantity;
        }
    }
}