package com.omega.shop.repository;

import com.omega.shop.entity.OrderItem;
import com.omega.shop.entity.Order;
import com.omega.shop.entity.Product;
import com.omega.shop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    boolean existsByProduct(Product product);

    /**
     * Получить статистику продаж по продавцам за период
     * Возвращает информацию о продавце, общее количество проданных товаров и сумму продаж
     * Использует только завершенные заказы (DELIVERED)
     * Группирует строго по продавцам
     */
    @Query("SELECT " +
           "CASE WHEN seller.id IS NOT NULL THEN seller.id ELSE 0L END, " +
           "CASE WHEN seller.username IS NOT NULL THEN seller.username ELSE 'Без продавца' END, " +
           "CASE WHEN seller.email IS NOT NULL THEN seller.email ELSE '' END, " +
           "SUM(oi.quantity), " +
           "SUM(oi.totalPrice) " +
           "FROM OrderItem oi " +
           "LEFT JOIN oi.product.seller seller " +
           "WHERE oi.order.status = :deliveredStatus " +
           "AND oi.order.createdAt >= :startDate " +
           "AND oi.order.createdAt <= :endDate " +
           "GROUP BY seller.id, seller.username, seller.email " +
           "ORDER BY CASE WHEN seller.username IS NOT NULL THEN seller.username ELSE 'Без продавца' END")
    List<Object[]> getSalesReportBySeller(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          @Param("deliveredStatus") Order.OrderStatus deliveredStatus);

    /**
     * Получить статистику продаж конкретного продавца за период
     * Возвращает информацию о продавце, общее количество проданных товаров и сумму продаж
     * Использует только завершенные заказы (DELIVERED)
     */
    @Query("SELECT " +
           "seller.id, " +
           "seller.username, " +
           "seller.email, " +
           "SUM(oi.quantity), " +
           "SUM(oi.totalPrice) " +
           "FROM OrderItem oi " +
           "JOIN oi.product.seller seller " +
           "WHERE seller = :seller " +
           "AND oi.order.status = :deliveredStatus " +
           "AND oi.order.createdAt >= :startDate " +
           "AND oi.order.createdAt <= :endDate " +
           "GROUP BY seller.id, seller.username, seller.email")
    List<Object[]> getSalesReportForSeller(@Param("seller") User seller,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate,
                                           @Param("deliveredStatus") Order.OrderStatus deliveredStatus);

    /**
     * Получить статистику продаж конкретного продавца за период (все статусы кроме CANCELLED)
     * Используется для показа истории продаж, когда нет завершенных заказов
     */
    @Query("SELECT " +
           "seller.id, " +
           "seller.username, " +
           "seller.email, " +
           "SUM(oi.quantity), " +
           "SUM(oi.totalPrice) " +
           "FROM OrderItem oi " +
           "JOIN oi.product.seller seller " +
           "WHERE seller = :seller " +
           "AND oi.order.status != :cancelledStatus " +
           "AND oi.order.createdAt >= :startDate " +
           "AND oi.order.createdAt <= :endDate " +
           "GROUP BY seller.id, seller.username, seller.email")
    List<Object[]> getSalesReportForSellerAllStatuses(@Param("seller") User seller,
                                                      @Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate,
                                                      @Param("cancelledStatus") Order.OrderStatus cancelledStatus);
}


