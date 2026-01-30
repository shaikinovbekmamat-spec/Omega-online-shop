package com.omega.shop.controller;

import com.omega.shop.dto.SalesReportDto;
import com.omega.shop.entity.Order;
import com.omega.shop.entity.User;
import com.omega.shop.service.OrderService;
import com.omega.shop.service.ProductService;
import com.omega.shop.service.ReportService;
import com.omega.shop.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/seller")
@PreAuthorize("hasRole('SELLER')")
@RequiredArgsConstructor
@Slf4j
public class SellerOrderController {

    private final OrderService orderService;
    private final ProductService productService;
    private final UserService userService;
    private final ReportService reportService;

    /**
     * Главная страница продавца (дашборд)
     */
    @GetMapping
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User seller = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Продавец не найден"));

        // Статистика заказов (новые заказы - все, остальные - только этого продавца)
        long newOrdersCount = orderService.getNewOrdersForSeller(seller, PageRequest.of(0, 1)).getTotalElements();
        long inProgressCount = orderService.getSellerOrdersByStatus(
                seller, Order.OrderStatus.IN_PROGRESS, PageRequest.of(0, 1)).getTotalElements();
        long readyForDeliveryCount = orderService.getSellerOrdersByStatus(
                seller, Order.OrderStatus.READY_FOR_DELIVERY, PageRequest.of(0, 1)).getTotalElements();

        model.addAttribute("newOrdersCount", newOrdersCount);
        model.addAttribute("inProgressCount", inProgressCount);
        model.addAttribute("readyForDeliveryCount", readyForDeliveryCount);

        return "seller/dashboard";
    }

    /**
     * Список новых заказов для обработки
     */
    @GetMapping("/orders/new")
    public String newOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model
    ) {
        User seller = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Продавец не найден"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Order> ordersPage = orderService.getNewOrdersForSeller(seller, pageable);

        model.addAttribute("orders", ordersPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ordersPage.getTotalPages());
        model.addAttribute("totalItems", ordersPage.getTotalElements());

        return "seller/orders/new";
    }

    /**
     * Детали заказа для продавца
     */
    @GetMapping("/orders/{id}")
    public String orderDetails(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Order order = orderService.getOrderById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Заказ не найден"));

            // Загружаем связанные сущности
            order.getItems().size();
            order.getUser().getUsername();

            model.addAttribute("order", order);
            model.addAttribute("couriers", orderService.getAllCouriers());

            return "seller/orders/details";
        } catch (IllegalArgumentException e) {
            log.error("Ошибка получения заказа: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/seller/orders/new";
        }
    }

    /**
     * Подтвердить заказ (перевести в обработку)
     */
    @PostMapping("/orders/{id}/confirm")
    public String confirmOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String sellerComment,
            RedirectAttributes redirectAttributes
    ) {
        try {
            orderService.confirmOrderBySeller(id, sellerComment);
            redirectAttributes.addFlashAttribute("successMessage", "Заказ успешно подтверждён");
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Ошибка подтверждения заказа: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/seller/orders/" + id;
    }

    /**
     * Отклонить заказ
     */
    @PostMapping("/orders/{id}/reject")
    public String rejectOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String sellerComment,
            RedirectAttributes redirectAttributes
    ) {
        try {
            orderService.rejectOrderBySeller(id, sellerComment);
            redirectAttributes.addFlashAttribute("successMessage", "Заказ отклонён");
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Ошибка отклонения заказа: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/seller/orders/" + id;
    }

    /**
     * Подготовить заказ к отправке
     */
    @PostMapping("/orders/{id}/prepare")
    public String prepareOrderForDelivery(
            @PathVariable Long id,
            @RequestParam(required = false) String invoiceNumber,
            @RequestParam(required = false) String sellerComment,
            RedirectAttributes redirectAttributes
    ) {
        try {
            orderService.prepareOrderForDelivery(id, invoiceNumber, sellerComment);
            redirectAttributes.addFlashAttribute("successMessage", "Заказ подготовлен к отправке");
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Ошибка подготовки заказа: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/seller/orders/" + id;
    }

    /**
     * Назначить курьера на заказ
     */
    @PostMapping("/orders/{id}/assign-courier")
    public String assignCourier(
            @PathVariable Long id,
            @RequestParam Long courierId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            // Проверяем существование заказа
            orderService.getOrderById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Заказ не найден"));

            // Получаем курьера из списка всех курьеров
            User courier = orderService.getAllCouriers().stream()
                    .filter(c -> c.getId().equals(courierId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Курьер не найден"));

            orderService.assignCourier(id, courier);
            redirectAttributes.addFlashAttribute("successMessage", "Курьер успешно назначен");
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Ошибка назначения курьера: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/seller/orders/" + id;
    }

    /**
     * Список заказов в обработке
     */
    @GetMapping("/orders/in-progress")
    public String inProgressOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model
    ) {
        User seller = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Продавец не найден"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Order> ordersPage = orderService.getSellerOrdersByStatus(
                seller, Order.OrderStatus.IN_PROGRESS, pageable);

        model.addAttribute("orders", ordersPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ordersPage.getTotalPages());
        model.addAttribute("totalItems", ordersPage.getTotalElements());

        return "seller/orders/in-progress";
    }

    /**
     * Список заказов готовых к отправке
     */
    @GetMapping("/orders/ready")
    public String readyOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model
    ) {
        User seller = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Продавец не найден"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Order> ordersPage = orderService.getSellerOrdersByStatus(
                seller, Order.OrderStatus.READY_FOR_DELIVERY, pageable);

        model.addAttribute("orders", ordersPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ordersPage.getTotalPages());
        model.addAttribute("totalItems", ordersPage.getTotalElements());
        model.addAttribute("couriers", orderService.getAllCouriers());

        return "seller/orders/ready";
    }

    /**
     * История обработанных заказов
     */
    @GetMapping("/orders/history")
    public String orderHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model
    ) {
        User seller = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Продавец не найден"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Order> ordersPage;

        if (status != null && !status.trim().isEmpty()) {
            try {
                Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
                ordersPage = orderService.getSellerOrdersByStatus(seller, orderStatus, pageable);
                model.addAttribute("selectedStatus", orderStatus);
            } catch (IllegalArgumentException e) {
                ordersPage = orderService.getSellerOrders(seller, pageable);
            }
        } else {
            ordersPage = orderService.getSellerOrders(seller, pageable);
        }

        model.addAttribute("orders", ordersPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ordersPage.getTotalPages());
        model.addAttribute("totalItems", ordersPage.getTotalElements());
        model.addAttribute("statuses", Order.OrderStatus.values());

        return "seller/orders/history";
    }

    /**
     * История продаж продавца с статистикой и зарплатой
     */
    @GetMapping("/sales")
    public String salesHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Integer days,
            Model model
    ) {
        try {
            User seller = userService.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalStateException("Продавец не найден"));

            SalesReportDto salesReport;

            if (startDate != null && endDate != null) {
                salesReport = reportService.getSellerSalesReport(seller, startDate, endDate);
                model.addAttribute("startDate", startDate);
                model.addAttribute("endDate", endDate);
            } else if (days != null && days > 0) {
                salesReport = reportService.getSellerSalesReportLastDays(seller, days);
                model.addAttribute("days", days);
            } else {
                salesReport = reportService.getSellerSalesReportCurrentMonth(seller);
                LocalDateTime now = LocalDateTime.now();
                model.addAttribute("startDate", now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0));
                model.addAttribute("endDate", now);
            }

            model.addAttribute("salesReport", salesReport);
            model.addAttribute("seller", seller);

            return "seller/sales";
        } catch (Exception e) {
            log.error("Ошибка генерации отчета о продажах: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Произошла ошибка при генерации отчета: " + e.getMessage());
            return "seller/sales";
        }
    }
}

