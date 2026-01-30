package com.omega.shop.controller;

import com.omega.shop.entity.Order;
import com.omega.shop.entity.User;
import com.omega.shop.repository.UserRepository;
import com.omega.shop.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/courier")
@PreAuthorize("hasRole('COURIER')")
@RequiredArgsConstructor
@Slf4j
public class CourierController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    /**
     * Получить текущего курьера из аутентификации
     */
    private User getCurrentCourier(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Курьер не найден"));
    }

    /**
     * Главная страница курьера (дашборд)
     */
    @GetMapping
    public String dashboard(Authentication authentication, Model model) {
        User courier = getCurrentCourier(authentication);

        // Статистика
        long assignedCount = orderService.getCourierOrdersByDeliveryStatus(
                courier, Order.DeliveryStatus.ASSIGNED, PageRequest.of(0, 1)).getTotalElements();
        long inTransitCount = orderService.getCourierOrdersByDeliveryStatus(
                courier, Order.DeliveryStatus.IN_TRANSIT, PageRequest.of(0, 1)).getTotalElements();
        long deliveredCount = orderService.getCourierOrdersByDeliveryStatus(
                courier, Order.DeliveryStatus.DELIVERED, PageRequest.of(0, 1)).getTotalElements();

        model.addAttribute("assignedCount", assignedCount);
        model.addAttribute("inTransitCount", inTransitCount);
        model.addAttribute("deliveredCount", deliveredCount);

        return "courier/dashboard";
    }

    /**
     * Список назначенных доставок
     */
    @GetMapping("/deliveries")
    public String deliveries(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model
    ) {
        User courier = getCurrentCourier(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> ordersPage;

        if (status != null && !status.trim().isEmpty()) {
            try {
                Order.DeliveryStatus deliveryStatus = Order.DeliveryStatus.valueOf(status.toUpperCase());
                ordersPage = orderService.getCourierOrdersByDeliveryStatus(courier, deliveryStatus, pageable);
                model.addAttribute("selectedStatus", deliveryStatus);
            } catch (IllegalArgumentException e) {
                ordersPage = orderService.getCourierOrders(courier, pageable);
            }
        } else {
            ordersPage = orderService.getCourierOrders(courier, pageable);
        }

        model.addAttribute("orders", ordersPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ordersPage.getTotalPages());
        model.addAttribute("totalItems", ordersPage.getTotalElements());
        model.addAttribute("statuses", Order.DeliveryStatus.values());

        return "courier/deliveries";
    }

    /**
     * Детали заказа для курьера
     */
    @GetMapping("/deliveries/{id}")
    public String deliveryDetails(
            Authentication authentication,
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User courier = getCurrentCourier(authentication);
            Order order = orderService.getCourierOrderById(courier, id);

            // Загружаем связанные сущности
            order.getItems().size();
            order.getUser().getUsername();

            model.addAttribute("order", order);

            return "courier/delivery-details";
        } catch (IllegalArgumentException e) {
            log.error("Ошибка получения заказа: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/courier/deliveries";
        }
    }

    /**
     * Начать доставку (курьер забрал заказ)
     */
    @PostMapping("/deliveries/{id}/start")
    public String startDelivery(
            Authentication authentication,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User courier = getCurrentCourier(authentication);
            orderService.startDelivery(courier, id);
            redirectAttributes.addFlashAttribute("successMessage", "Доставка начата");
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Ошибка начала доставки: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/courier/deliveries/" + id;
    }

    /**
     * Завершить доставку (курьер доставил заказ)
     */
    @PostMapping("/deliveries/{id}/complete")
    public String completeDelivery(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam(required = false) String courierComment,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User courier = getCurrentCourier(authentication);
            orderService.completeDelivery(courier, id, courierComment);
            redirectAttributes.addFlashAttribute("successMessage", "Доставка завершена");
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Ошибка завершения доставки: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/courier/deliveries/" + id;
    }

    /**
     * Отметить проблему с доставкой
     */
    @PostMapping("/deliveries/{id}/problem")
    public String markDeliveryProblem(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam String problemComment,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User courier = getCurrentCourier(authentication);
            orderService.markDeliveryProblem(courier, id, problemComment);
            redirectAttributes.addFlashAttribute("successMessage", "Проблема с доставкой отмечена");
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Ошибка отметки проблемы: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/courier/deliveries/" + id;
    }

    /**
     * История выполненных доставок
     */
    @GetMapping("/deliveries/history")
    public String deliveryHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model
    ) {
        User courier = getCurrentCourier(authentication);
        Pageable pageable = PageRequest.of(page, size);

        Page<Order> ordersPage = orderService.getCourierOrdersByDeliveryStatus(
                courier, Order.DeliveryStatus.DELIVERED, pageable);

        model.addAttribute("orders", ordersPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ordersPage.getTotalPages());
        model.addAttribute("totalItems", ordersPage.getTotalElements());

        return "courier/history";
    }
}




