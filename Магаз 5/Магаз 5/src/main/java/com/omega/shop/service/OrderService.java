package com.omega.shop.service;

import com.omega.shop.dto.CartItem;
import com.omega.shop.dto.OrderDto;
import com.omega.shop.dto.ShoppingCart;
import com.omega.shop.entity.Order;
import com.omega.shop.entity.OrderItem;
import com.omega.shop.entity.Product;
import com.omega.shop.entity.User;
import com.omega.shop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final CartService cartService;

    @Transactional
    public Order createOrder(User user, OrderDto orderDto) {
        log.info("Создание заказа для пользователя: {}", user.getUsername());

        ShoppingCart cart = cartService.getCart();

        if (cart.isEmpty()) {
            throw new IllegalStateException("Корзина пуста");
        }

        cartService.validateCart();

        Order order = new Order();
        order.setUser(user);
        order.setPhone(orderDto.getPhone());
        order.setDeliveryAddress(orderDto.getDeliveryAddress());
        order.setComment(orderDto.getComment());
        order.setStatus(Order.OrderStatus.NEW);

        for (CartItem cartItem : cart.getItems()) {
            Product product = productService.getProductById(cartItem.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

            OrderItem orderItem = OrderItem.fromProduct(product, cartItem.getQuantity());
            order.addItem(orderItem);

            productService.decreaseQuantity(product.getId(), cartItem.getQuantity());
        }

        order.calculateTotalAmount();
        Order savedOrder = orderRepository.save(order);
        cartService.clearCart();

        log.info("Заказ #{} успешно создан на сумму {}", savedOrder.getId(), savedOrder.getTotalAmount());

        return savedOrder;
    }

    /**
     * Создать заказ из корзины (алиас для createOrder)
     */
    @Transactional
    public Order createOrderFromCart(User user, OrderDto orderDto) {
        return createOrder(user, orderDto);
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(Long id) {
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            // Загружаем связанные сущности
            order.getItems().size(); // Инициализируем коллекцию
            order.getUser().getUsername(); // Инициализируем user
        }
        return orderOpt;
    }

    @Transactional(readOnly = true)
    public Page<Order> getUserOrders(User user, Pageable pageable) {
        Page<Order> ordersPage = orderRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        // Инициализируем связанные сущности
        ordersPage.getContent().forEach(order -> {
            try {
                order.getItems().size(); // Инициализируем коллекцию items
                if (order.getUser() != null) {
                    order.getUser().getUsername(); // Инициализируем user
                }
            } catch (Exception e) {
                log.warn("Ошибка инициализации связанных сущностей для заказа {}: {}", order.getId(), e.getMessage());
            }
        });
        return ordersPage;
    }

    /**
     * Получить все заказы (для админа) с загрузкой связанных сущностей
     */
    @Transactional(readOnly = true)
    public Page<Order> getAllOrders(Pageable pageable) {
        Page<Order> ordersPage = orderRepository.findAll(pageable);
        // Инициализируем связанные сущности
        ordersPage.getContent().forEach(order -> {
            order.getItems().size(); // Инициализируем коллекцию items
            order.getUser().getUsername(); // Инициализируем user
        });
        return ordersPage;
    }

    @Transactional(readOnly = true)
    public Page<Order> getOrdersByStatus(Order.OrderStatus status, Pageable pageable) {
        Page<Order> ordersPage = orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        // Инициализируем связанные сущности
        ordersPage.getContent().forEach(order -> {
            order.getItems().size(); // Инициализируем коллекцию items
            order.getUser().getUsername(); // Инициализируем user
        });
        return ordersPage;
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, Order.OrderStatus newStatus) {
        log.info("Обновление статуса заказа #{} на {}", orderId, newStatus);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Заказ не найден"));

        Order.OrderStatus oldStatus = order.getStatus();

        if (oldStatus == Order.OrderStatus.CANCELLED) {
            throw new IllegalStateException("Нельзя изменить статус отменённого заказа");
        }

        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(Long orderId) {
        log.info("Отмена заказа #{}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Заказ не найден"));

        if (order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new IllegalStateException("Нельзя отменить доставленный заказ");
        }

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new IllegalStateException("Заказ уже отменён");
        }

        for (OrderItem item : order.getItems()) {
            productService.increaseQuantity(item.getProduct().getId(), item.getQuantity());
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    /**
     * Отменить заказ пользователем (с проверкой владельца)
     */
    @Transactional
    public Order cancelOrder(User user, Long orderId) {
        log.info("Отмена заказа #{} пользователем {}", orderId, user.getUsername());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Заказ не найден"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Вы не можете отменить чужой заказ");
        }

        if (order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new IllegalStateException("Нельзя отменить доставленный заказ");
        }

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new IllegalStateException("Заказ уже отменён");
        }

        for (OrderItem item : order.getItems()) {
            productService.increaseQuantity(item.getProduct().getId(), item.getQuantity());
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    /**
     * Получить заказ пользователя по ID с загрузкой связанных сущностей
     */
    @Transactional(readOnly = true)
    public Order getUserOrderById(User user, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Заказ не найден"));

        // Загружаем связанные сущности
        order.getItems().size(); // Инициализируем коллекцию
        order.getUser().getUsername(); // Инициализируем user

        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Вы не можете просматривать чужой заказ");
        }

        return order;
    }

    public long countUserOrders(User user) {
        return orderRepository.countByUser(user);
    }
}