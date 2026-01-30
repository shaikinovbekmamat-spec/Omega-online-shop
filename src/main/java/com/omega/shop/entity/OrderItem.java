package com.omega.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Фиксируем данные товара на момент покупки
    @Column(nullable = false)
    private String productName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @PrePersist
    @PreUpdate
    private void beforeSave() {
        recalculateTotalPrice();
    }

    /**
     * Пересчитать итоговую стоимость позиции
     */
    public void recalculateTotalPrice() {
        if (price != null && quantity != null) {
            this.totalPrice = price.multiply(BigDecimal.valueOf(quantity));
        } else {
            this.totalPrice = BigDecimal.ZERO;
        }
    }

    /**
     * Получить общую стоимость позиции
     */
    public BigDecimal getTotalPrice() {
        if (totalPrice == null) {
            recalculateTotalPrice();
        }
        return totalPrice;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
        recalculateTotalPrice();
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        recalculateTotalPrice();
    }

    /**
     * Создать OrderItem из Product и количества
     */
    public static OrderItem fromProduct(Product product, int quantity) {
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setProductName(product.getName());
        item.setPrice(product.getPrice());
        item.setQuantity(quantity);
        item.setImagePath(product.getImagePath());
        item.recalculateTotalPrice();
        return item;
    }
}