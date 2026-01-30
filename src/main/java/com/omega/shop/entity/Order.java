package com.omega.shop.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.NEW;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @NotBlank(message = "Телефон обязателен")
    @Column(nullable = false, length = 20)
    private String phone;

    @NotBlank(message = "Адрес доставки обязателен")
    @Column(nullable = false, length = 500)
    private String deliveryAddress;

    @Column(columnDefinition = "TEXT")
    private String comment;

    // Курьер, назначенный на доставку
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id")
    private User courier;

    // Статус доставки
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", length = 20)
    private DeliveryStatus deliveryStatus = DeliveryStatus.NOT_ASSIGNED;

    // Номер накладной
    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    // Комментарий продавца
    @Column(name = "seller_comment", columnDefinition = "TEXT")
    private String sellerComment;

    // Комментарий курьера
    @Column(name = "courier_comment", columnDefinition = "TEXT")
    private String courierComment;

    // Дата назначения курьера
    @Column(name = "courier_assigned_at")
    private LocalDateTime courierAssignedAt;

    // Дата готовности к отправке
    @Column(name = "ready_for_delivery_at")
    private LocalDateTime readyForDeliveryAt;

    // Дата начала доставки
    @Column(name = "delivery_started_at")
    private LocalDateTime deliveryStartedAt;

    // Дата доставки
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enum для статусов заказа
    public enum OrderStatus {
        NEW("Новый"),
        IN_PROGRESS("В обработке"),
        READY_FOR_DELIVERY("Готов к отправке"),
        DELIVERED("Доставлен"),
        CANCELLED("Отменён");

        private final String displayName;

        OrderStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Enum для статусов доставки
    public enum DeliveryStatus {
        NOT_ASSIGNED("Не назначена"),
        ASSIGNED("Назначена"),
        READY("Готов к отправке"),
        IN_TRANSIT("В пути"),
        DELIVERED("Доставлен"),
        FAILED("Не доставлен"),
        CANCELLED("Отменена");

        private final String displayName;

        DeliveryStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Вспомогательные методы
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }

    public void calculateTotalAmount() {
        this.totalAmount = items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}