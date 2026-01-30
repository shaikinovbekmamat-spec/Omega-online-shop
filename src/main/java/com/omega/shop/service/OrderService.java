package com.omega.shop.service;

import com.omega.shop.dto.CartItem;
import com.omega.shop.dto.OrderDto;
import com.omega.shop.dto.ShoppingCart;
import com.omega.shop.entity.Order;
import com.omega.shop.entity.OrderItem;
import com.omega.shop.entity.Product;
import com.omega.shop.entity.User;
import com.omega.shop.repository.OrderRepository;
import com.omega.shop.repository.UserRepository;
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
    private final UserRepository userRepository;

    @Transactional
    public Order createOrder(User user, OrderDto orderDto) {
        log.info("Создание заказа для пользователя: {}", user.getUsername());

        ShoppingCart cart = cartService.getCart();

        if (cart.isEmpty()) {
            throw new IllegalStateException("Корзина пуста");
        }

        cartService.validateCart();

        // Логируем оплату (данные не сохраняются в БД для безопасности)
        String cardNumber = orderDto.getCardNumber();
        if (cardNumber != null && !cardNumber.trim().isEmpty()) {
            // Удаляем пробелы для логирования
            String cleanCardNumber = cardNumber.replaceAll("\\s+", "").replaceAll("\\D", "");
            if (cleanCardNumber.length() >= 4) {
                String maskedCardNumber = "**** **** **** " + cleanCardNumber.substring(Math.max(0, cleanCardNumber.length() - 4));
                log.info("Оплата картой {} обработана", maskedCardNumber);
            } else {
                log.info("Оплата картой обработана");
            }
        }

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

    // ========== Методы для ПРОДАВЦА ==========

    /**
     * Получить новые заказы для обработки продавцом
     * Показывает все новые заказы (для обработки), фильтрация по продавцу используется только в отчетах и истории
     */
    @Transactional(readOnly = true)
    public Page<Order> getNewOrdersForSeller(User seller, Pageable pageable) {
        // Показываем все новые заказы для обработки продавцом
        // Фильтрация по продавцу применяется только в отчетах и истории продаж
        Page<Order> ordersPage = orderRepository.findByStatusOrderByCreatedAtAsc(
                Order.OrderStatus.NEW, pageable);
        ordersPage.getContent().forEach(order -> {
            order.getItems().size();
            order.getUser().getUsername();
        });
        return ordersPage;
    }

    /**
     * Получить все заказы продавца
     */
    @Transactional(readOnly = true)
    public Page<Order> getSellerOrders(User seller, Pageable pageable) {
        log.debug("Поиск всех заказов для продавца: {} (ID: {})", seller.getUsername(), seller.getId());
        Page<Order> ordersPage = orderRepository.findBySellerOrderByCreatedAtDesc(seller, pageable);
        log.debug("Найдено заказов для продавца {}: {}", seller.getUsername(), ordersPage.getTotalElements());
        
        if (ordersPage.getTotalElements() == 0) {
            log.warn("Не найдено заказов для продавца {} (ID: {}). Проверьте, есть ли товары с этим продавцом в заказах.", 
                    seller.getUsername(), seller.getId());
        }
        
        ordersPage.getContent().forEach(order -> {
            order.getItems().size();
            order.getUser().getUsername();
        });
        return ordersPage;
    }

    /**
     * Получить заказы продавца по статусу
     */
    @Transactional(readOnly = true)
    public Page<Order> getSellerOrdersByStatus(User seller, Order.OrderStatus status, Pageable pageable) {
        log.debug("Поиск заказов со статусом {} для продавца: {} (ID: {})", status, seller.getUsername(), seller.getId());
        Page<Order> ordersPage = orderRepository.findBySellerAndStatusOrderByCreatedAtDesc(seller, status, pageable);
        log.debug("Найдено заказов со статусом {} для продавца {}: {}", status, seller.getUsername(), ordersPage.getTotalElements());
        
        ordersPage.getContent().forEach(order -> {
            order.getItems().size();
            order.getUser().getUsername();
        });
        return ordersPage;
    }

    /**
     * Получить новые заказы для обработки продавцом (старый метод для обратной совместимости)
     * @deprecated Используйте getNewOrdersForSeller(User seller, Pageable pageable)
     */
    @Deprecated
    @Transactional(readOnly = true)
    public Page<Order> getNewOrdersForSeller(Pageable pageable) {
        Page<Order> ordersPage = orderRepository.findByStatusOrderByCreatedAtAsc(
                Order.OrderStatus.NEW, pageable);
        ordersPage.getContent().forEach(order -> {
            order.getItems().size();
            order.getUser().getUsername();
        });
        return ordersPage;
    }

    /**
     * Подтвердить заказ продавцом (перевести в обработку)
     */
    @Transactional
    public Order confirmOrderBySeller(Long orderId, String sellerComment) {
        log.info("Подтверждение заказа #{} продавцом", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Заказ не найден"));

        if (order.getStatus() != Order.OrderStatus.NEW) {
            throw new IllegalStateException("Можно подтверждать только новые заказы");
        }

        // Проверка наличия товаров на складе
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product.getQuantity() < item.getQuantity()) {
                throw new IllegalStateException(
                        "Недостаточно товара " + product.getName() + " на складе. Доступно: " + product.getQuantity());
            }
        }

        order.setStatus(Order.OrderStatus.IN_PROGRESS);
        if (sellerComment != null && !sellerComment.trim().isEmpty()) {
            order.setSellerComment(sellerComment);
        }

        return orderRepository.save(order);
    }

    /**
     * Отклонить заказ продавцом
     */
    @Transactional
    public Order rejectOrderBySeller(Long orderId, String sellerComment) {
        log.info("Отклонение заказа #{} продавцом", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Заказ не найден"));

        if (order.getStatus() != Order.OrderStatus.NEW) {
            throw new IllegalStateException("Можно отклонять только новые заказы");
        }

        // Возвращаем товары на склад
        for (OrderItem item : order.getItems()) {
            productService.increaseQuantity(item.getProduct().getId(), item.getQuantity());
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setDeliveryStatus(Order.DeliveryStatus.CANCELLED);
        if (sellerComment != null && !sellerComment.trim().isEmpty()) {
            order.setSellerComment(sellerComment);
        }

        return orderRepository.save(order);
    }

    /**
     * Подготовить заказ к отправке (готов к отправке)
     */
    @Transactional
    public Order prepareOrderForDelivery(Long orderId, String invoiceNumber, String sellerComment) {
        log.info("Подготовка заказа #{} к отправке", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Заказ не найден"));

        if (order.getStatus() != Order.OrderStatus.IN_PROGRESS) {
            throw new IllegalStateException("Можно подготовить к отправке только заказы в обработке");
        }

        order.setStatus(Order.OrderStatus.READY_FOR_DELIVERY);
        order.setDeliveryStatus(Order.DeliveryStatus.READY);
        order.setReadyForDeliveryAt(java.time.LocalDateTime.now());

        if (invoiceNumber != null && !invoiceNumber.trim().isEmpty()) {
            order.setInvoiceNumber(invoiceNumber);
        }

        if (sellerComment != null && !sellerComment.trim().isEmpty()) {
            order.setSellerComment(sellerComment);
        }

        return orderRepository.save(order);
    }

    /**
     * Назначить курьера на заказ
     */
    @Transactional
    public Order assignCourier(Long orderId, User courier) {
        log.info("Назначение курьера {} на заказ #{}", courier.getUsername(), orderId);

        if (courier.getRole() != User.Role.COURIER) {
            throw new IllegalArgumentException("Пользователь не является курьером");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Заказ не найден"));

        if (order.getStatus() != Order.OrderStatus.READY_FOR_DELIVERY) {
            throw new IllegalStateException("Курьера можно назначить только на заказы готовые к отправке");
        }

        order.setCourier(courier);
        order.setDeliveryStatus(Order.DeliveryStatus.ASSIGNED);
        order.setCourierAssignedAt(java.time.LocalDateTime.now());

        return orderRepository.save(order);
    }

    // ========== Методы для КУРЬЕРА ==========

    /**
     * Получить заказы курьера
     */
    @Transactional(readOnly = true)
    public Page<Order> getCourierOrders(User courier, Pageable pageable) {
        if (courier.getRole() != User.Role.COURIER) {
            throw new IllegalArgumentException("Пользователь не является курьером");
        }

        Page<Order> ordersPage = orderRepository.findByCourierOrderByCreatedAtDesc(courier, pageable);
        ordersPage.getContent().forEach(order -> {
            order.getItems().size();
            order.getUser().getUsername();
        });
        return ordersPage;
    }

    /**
     * Получить заказы курьера по статусу доставки
     */
    @Transactional(readOnly = true)
    public Page<Order> getCourierOrdersByDeliveryStatus(
            User courier,
            Order.DeliveryStatus deliveryStatus,
            Pageable pageable) {
        if (courier.getRole() != User.Role.COURIER) {
            throw new IllegalArgumentException("Пользователь не является курьером");
        }

        Page<Order> ordersPage = orderRepository.findByCourierAndDeliveryStatusOrderByCreatedAtDesc(
                courier, deliveryStatus, pageable);
        ordersPage.getContent().forEach(order -> {
            order.getItems().size();
            order.getUser().getUsername();
        });
        return ordersPage;
    }

    /**
     * Начать доставку (курьер забрал заказ)
     */
    @Transactional
    public Order startDelivery(User courier, Long orderId) {
        log.info("Курьер {} начал доставку заказа #{}", courier.getUsername(), orderId);

        if (courier.getRole() != User.Role.COURIER) {
            throw new IllegalArgumentException("Пользователь не является курьером");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Заказ не найден"));

        if (order.getCourier() == null || !order.getCourier().getId().equals(courier.getId())) {
            throw new IllegalArgumentException("Заказ не назначен вам");
        }

        if (order.getDeliveryStatus() != Order.DeliveryStatus.ASSIGNED &&
                order.getDeliveryStatus() != Order.DeliveryStatus.READY) {
            throw new IllegalStateException("Нельзя начать доставку с текущим статусом");
        }

        order.setDeliveryStatus(Order.DeliveryStatus.IN_TRANSIT);
        order.setDeliveryStartedAt(java.time.LocalDateTime.now());

        return orderRepository.save(order);
    }

    /**
     * Завершить доставку (курьер доставил заказ)
     */
    @Transactional
    public Order completeDelivery(User courier, Long orderId, String courierComment) {
        log.info("Курьер {} завершил доставку заказа #{}", courier.getUsername(), orderId);

        if (courier.getRole() != User.Role.COURIER) {
            throw new IllegalArgumentException("Пользователь не является курьером");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Заказ не найден"));

        if (order.getCourier() == null || !order.getCourier().getId().equals(courier.getId())) {
            throw new IllegalArgumentException("Заказ не назначен вам");
        }

        if (order.getDeliveryStatus() != Order.DeliveryStatus.IN_TRANSIT) {
            throw new IllegalStateException("Заказ должен быть в статусе 'В пути'");
        }

        order.setDeliveryStatus(Order.DeliveryStatus.DELIVERED);
        order.setStatus(Order.OrderStatus.DELIVERED);
        order.setDeliveredAt(java.time.LocalDateTime.now());

        if (courierComment != null && !courierComment.trim().isEmpty()) {
            order.setCourierComment(courierComment);
        }

        return orderRepository.save(order);
    }

    /**
     * Отметить проблему с доставкой
     */
    @Transactional
    public Order markDeliveryProblem(User courier, Long orderId, String problemComment) {
        log.info("Курьер {} отметил проблему с доставкой заказа #{}", courier.getUsername(), orderId);

        if (courier.getRole() != User.Role.COURIER) {
            throw new IllegalArgumentException("Пользователь не является курьером");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Заказ не найден"));

        if (order.getCourier() == null || !order.getCourier().getId().equals(courier.getId())) {
            throw new IllegalArgumentException("Заказ не назначен вам");
        }

        order.setDeliveryStatus(Order.DeliveryStatus.FAILED);
        if (problemComment != null && !problemComment.trim().isEmpty()) {
            order.setCourierComment(problemComment);
        }

        return orderRepository.save(order);
    }

    /**
     * Получить заказ курьера по ID
     */
    @Transactional(readOnly = true)
    public Order getCourierOrderById(User courier, Long orderId) {
        if (courier.getRole() != User.Role.COURIER) {
            throw new IllegalArgumentException("Пользователь не является курьером");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Заказ не найден"));

        order.getItems().size();
        order.getUser().getUsername();

        if (order.getCourier() == null || !order.getCourier().getId().equals(courier.getId())) {
            throw new IllegalArgumentException("Заказ не назначен вам");
        }

        return order;
    }

    /**
     * Получить список всех курьеров
     */
    @Transactional(readOnly = true)
    public java.util.List<User> getAllCouriers() {
        return userRepository.findByRole(User.Role.COURIER);
    }
}