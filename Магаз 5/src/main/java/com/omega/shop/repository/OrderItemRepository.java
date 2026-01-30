package com.omega.shop.repository;

import com.omega.shop.entity.OrderItem;
import com.omega.shop.entity.Order;
import com.omega.shop.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    boolean existsByProduct(Product product);

    /**
     * Получить статистику продаж товаров за период
     * Возвращает ID товара, название, категорию и общее количество продаж
     * Исключает отмененные заказы
     */
    @Query("SELECT oi.product.id, " +
           "oi.product.name, " +
           "oi.product.category.name, " +
           "SUM(oi.quantity) " +
           "FROM OrderItem oi " +
           "WHERE oi.order.status <> :cancelledStatus " +
           "AND oi.order.createdAt >= :startDate " +
           "AND oi.order.createdAt <= :endDate " +
           "GROUP BY oi.product.id, oi.product.name, oi.product.category.name " +
           "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> getSalesReport(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate,
                                   @Param("cancelledStatus") Order.OrderStatus cancelledStatus);
}


