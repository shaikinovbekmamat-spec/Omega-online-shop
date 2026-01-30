package com.omega.shop.repository;

import com.omega.shop.entity.Order;
import com.omega.shop.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Найти заказы пользователя
     */
    Page<Order> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * Найти заказы по статусу
     */
    Page<Order> findByStatusOrderByCreatedAtDesc(Order.OrderStatus status, Pageable pageable);

    /**
     * Найти заказы за период
     */
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    List<Order> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Подсчитать количество заказов пользователя
     */
    long countByUser(User user);

    /**
     * Найти последние заказы пользователя
     */
    List<Order> findTop10ByUserOrderByCreatedAtDesc(User user);

    /**
     * Найти заказы по курьеру
     */
    Page<Order> findByCourierOrderByCreatedAtDesc(User courier, Pageable pageable);

    /**
     * Найти заказы по статусу доставки
     */
    Page<Order> findByDeliveryStatusOrderByCreatedAtDesc(Order.DeliveryStatus deliveryStatus, Pageable pageable);

    /**
     * Найти заказы по курьеру и статусу доставки
     */
    Page<Order> findByCourierAndDeliveryStatusOrderByCreatedAtDesc(
            User courier,
            Order.DeliveryStatus deliveryStatus,
            Pageable pageable
    );

    /**
     * Найти новые заказы (для продавца)
     */
    Page<Order> findByStatusOrderByCreatedAtAsc(Order.OrderStatus status, Pageable pageable);

    /**
     * Найти заказы готовые к отправке
     */
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.deliveryStatus = :deliveryStatus ORDER BY o.readyForDeliveryAt ASC")
    Page<Order> findReadyForDelivery(
            @Param("status") Order.OrderStatus status,
            @Param("deliveryStatus") Order.DeliveryStatus deliveryStatus,
            Pageable pageable
    );

    /**
     * Подсчитать количество заказов курьера
     */
    long countByCourier(User courier);

    /**
     * Подсчитать количество заказов курьера по статусу доставки
     */
    long countByCourierAndDeliveryStatus(User courier, Order.DeliveryStatus deliveryStatus);

    /**
     * Найти заказы по продавцу (заказы, содержащие товары этого продавца)
     * Использует EXISTS для поиска заказов, содержащих хотя бы один товар с указанным продавцом
     */
    @Query("SELECT o FROM Order o " +
           "WHERE EXISTS (SELECT 1 FROM OrderItem oi " +
           "            JOIN oi.product p " +
           "            WHERE oi.order = o AND p.seller = :seller) " +
           "ORDER BY o.createdAt DESC")
    Page<Order> findBySellerOrderByCreatedAtDesc(@Param("seller") User seller, Pageable pageable);

    /**
     * Найти заказы по продавцу и статусу
     * Использует EXISTS для поиска заказов, содержащих хотя бы один товар с указанным продавцом
     */
    @Query("SELECT o FROM Order o " +
           "WHERE o.status = :status " +
           "AND EXISTS (SELECT 1 FROM OrderItem oi " +
           "            JOIN oi.product p " +
           "            WHERE oi.order = o AND p.seller = :seller) " +
           "ORDER BY o.createdAt DESC")
    Page<Order> findBySellerAndStatusOrderByCreatedAtDesc(
            @Param("seller") User seller,
            @Param("status") Order.OrderStatus status,
            Pageable pageable);

    /**
     * Найти заказы по продавцу и статусу (по возрастанию даты для новых заказов)
     * Использует EXISTS для поиска заказов, содержащих хотя бы один товар с указанным продавцом
     */
    @Query("SELECT o FROM Order o " +
           "WHERE o.status = :status " +
           "AND EXISTS (SELECT 1 FROM OrderItem oi " +
           "            JOIN oi.product p " +
           "            WHERE oi.order = o AND p.seller = :seller) " +
           "ORDER BY o.createdAt ASC")
    Page<Order> findBySellerAndStatusOrderByCreatedAtAsc(
            @Param("seller") User seller,
            @Param("status") Order.OrderStatus status,
            Pageable pageable);
}