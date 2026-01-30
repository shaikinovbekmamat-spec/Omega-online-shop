package com.omega.shop.controller;

import com.omega.shop.dto.OrderDto;
import com.omega.shop.dto.ShoppingCart;
import com.omega.shop.entity.Order;
import com.omega.shop.entity.User;
import com.omega.shop.service.CartService;
import com.omega.shop.service.OrderService;
import com.omega.shop.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final CartService cartService;
    private final OrderService orderService;
    private final UserService userService;

    /**
     * Страница оформления заказа
     */
    @GetMapping("/checkout")
    public String checkoutPage(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        ShoppingCart cart = cartService.getCart();

        if (cart.isEmpty()) {
            model.addAttribute("errorMessage", "Ваша корзина пуста. Добавьте товары перед оформлением заказа.");
            return "cart/cart";
        }

        if (!model.containsAttribute("orderDto")) {
            model.addAttribute("orderDto", new OrderDto());
        }
        model.addAttribute("cart", cart);

        return "order/checkout";
    }

    /**
     * Обработка оформления заказа
     */
    @PostMapping("/checkout")
    public String placeOrder(@Valid @ModelAttribute("orderDto") OrderDto orderDto,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        ShoppingCart cart = cartService.getCart();

        if (cart.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ваша корзина пуста. Добавьте товары перед оформлением заказа.");
            return "redirect:/cart";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("cart", cart);
            model.addAttribute("orderDto", orderDto);
            return "order/checkout";
        }

        Optional<User> optionalUser = userService.findByUsername(userDetails.getUsername());
        if (optionalUser.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Пользователь не найден");
            return "redirect:/cart";
        }

        try {
            Order order = orderService.createOrderFromCart(optionalUser.get(), orderDto);
            redirectAttributes.addFlashAttribute("successMessage", "Заказ успешно оформлен");
            return "redirect:/orders/" + order.getId();
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Ошибка оформления заказа: {}", e.getMessage());
            model.addAttribute("cart", cart);
            model.addAttribute("errorMessage", e.getMessage());
            return "order/checkout";
        }
    }

    /**
     * История заказов пользователя
     */
    @GetMapping("/orders")
    public String ordersPage(@AuthenticationPrincipal UserDetails userDetails,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             Model model) {
        try {
            Optional<User> optionalUser = userService.findByUsername(userDetails.getUsername());
            if (optionalUser.isEmpty()) {
                model.addAttribute("errorMessage", "Пользователь не найден");
                model.addAttribute("orders", List.of());
                model.addAttribute("currentPage", 0);
                model.addAttribute("totalPages", 0);
                return "order/orders";
            }

            PageRequest pageable = PageRequest.of(page, size);
            Page<Order> ordersPage = orderService.getUserOrders(optionalUser.get(), pageable);

            model.addAttribute("orders", ordersPage.getContent());
            model.addAttribute("currentPage", ordersPage.getNumber());
            model.addAttribute("totalPages", ordersPage.getTotalPages());

            return "order/orders";
        } catch (Exception e) {
            log.error("Ошибка загрузки заказов: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Произошла ошибка при загрузке заказов: " + e.getMessage());
            model.addAttribute("orders", List.of());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            return "order/orders";
        }
    }

    /**
     * Детали конкретного заказа
     */
    @GetMapping("/orders/{id}")
    public String orderDetails(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model,
                               RedirectAttributes redirectAttributes) {

        Optional<User> optionalUser = userService.findByUsername(userDetails.getUsername());
        if (optionalUser.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Пользователь не найден");
            return "redirect:/orders";
        }

        try {
            Order order = orderService.getUserOrderById(optionalUser.get(), id);
            model.addAttribute("order", order);
            return "order/details";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/orders";
        }
    }

    /**
     * Отмена заказа пользователем
     */
    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {

        Optional<User> optionalUser = userService.findByUsername(userDetails.getUsername());
        if (optionalUser.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Пользователь не найден");
            return "redirect:/orders";
        }

        try {
            orderService.cancelOrder(optionalUser.get(), id);
            redirectAttributes.addFlashAttribute("successMessage", "Заказ успешно отменён");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/orders/" + id;
    }
}


