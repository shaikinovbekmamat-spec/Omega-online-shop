package com.omega.shop.controller;

import com.omega.shop.entity.Order;
import com.omega.shop.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * Список всех заказов
     */
    @GetMapping
    public String listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> ordersPage;

        if (status != null && !status.trim().isEmpty()) {
            try {
                Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
                ordersPage = orderService.getOrdersByStatus(orderStatus, pageable);
                model.addAttribute("selectedStatus", orderStatus);
            } catch (IllegalArgumentException e) {
                ordersPage = orderService.getAllOrders(pageable);
            }
        } else {
            ordersPage = orderService.getAllOrders(pageable);
        }

        model.addAttribute("orders", ordersPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ordersPage.getTotalPages());
        model.addAttribute("totalItems", ordersPage.getTotalElements());
        model.addAttribute("statuses", Order.OrderStatus.values());

        return "admin/orders/list";
    }

    /**
     * Детали заказа
     */
    @GetMapping("/{id}")
    public String orderDetails(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Order order = orderService.getOrderById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Заказ не найден"));

            model.addAttribute("order", order);
            model.addAttribute("statuses", Order.OrderStatus.values());

            return "admin/orders/details";
        } catch (IllegalArgumentException e) {
            log.error("Ошибка получения заказа: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/orders";
        }
    }

    /**
     * Изменение статуса заказа
     */
    @PostMapping("/{id}/status")
    public String updateOrderStatus(
            @PathVariable Long id,
            @RequestParam Order.OrderStatus status,
            RedirectAttributes redirectAttributes
    ) {
        try {
            orderService.updateOrderStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Статус заказа успешно изменён на: " + status.getDisplayName());
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Ошибка изменения статуса заказа: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/orders/" + id;
    }

    /**
     * Отмена заказа администратором
     */
    @PostMapping("/{id}/cancel")
    public String cancelOrder(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            orderService.cancelOrder(id);
            redirectAttributes.addFlashAttribute("successMessage", "Заказ успешно отменён");
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Ошибка отмены заказа: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/orders/" + id;
    }
}

