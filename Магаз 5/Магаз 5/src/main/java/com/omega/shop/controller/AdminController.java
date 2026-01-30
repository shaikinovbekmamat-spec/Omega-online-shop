package com.omega.shop.controller;

import com.omega.shop.service.CategoryService;
import com.omega.shop.service.OrderService;
import com.omega.shop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final OrderService orderService;

    /**
     * Главная страница админки (дашборд)
     */
    @GetMapping
    public String dashboard(Model model) {
        // Статистика
        long totalProducts = productService.getAllActiveProducts(PageRequest.of(0, 1)).getTotalElements();
        long totalCategories = categoryService.getAllCategories().size();
        long totalOrders = orderService.getAllOrders(PageRequest.of(0, 1)).getTotalElements();

        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("totalCategories", totalCategories);
        model.addAttribute("totalOrders", totalOrders);

        return "admin/dashboard";
    }
}