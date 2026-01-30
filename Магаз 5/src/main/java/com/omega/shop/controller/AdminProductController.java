package com.omega.shop.controller;

import com.omega.shop.entity.Category;
import com.omega.shop.entity.Product;
import com.omega.shop.service.CategoryService;
import com.omega.shop.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    /**
     * Список всех товаров
     */
    @GetMapping
    public String listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productsPage = productService.getAllActiveProducts(pageable);

        model.addAttribute("products", productsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productsPage.getTotalPages());
        model.addAttribute("totalItems", productsPage.getTotalElements());

        return "admin/products/list";
    }

    /**
     * Форма создания товара
     */
    @GetMapping("/new")
    public String newProductForm(Model model) {
        List<Category> categories = categoryService.getAllCategories();

        model.addAttribute("product", new Product());
        model.addAttribute("categories", categories);

        return "admin/products/form";
    }

    /**
     * Создание товара
     */
    @PostMapping
    public String createProduct(
            @Valid @ModelAttribute Product product,
            BindingResult bindingResult,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllCategories());
            return "admin/products/form";
        }

        try {
            productService.createProduct(product, imageFile);
            redirectAttributes.addFlashAttribute("successMessage", "Товар успешно создан");
            return "redirect:/admin/products";
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Ошибка создания товара: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/products";
        } catch (Exception e) {
            log.error("Неожиданная ошибка создания товара: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Произошла ошибка при создании товара: " + e.getMessage());
            return "redirect:/admin/products";
        }
    }

    /**
     * Форма редактирования товара
     */
    @GetMapping("/{id}/edit")
    public String editProductForm(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        List<Category> categories = categoryService.getAllCategories();

        model.addAttribute("product", product);
        model.addAttribute("categories", categories);

        return "admin/products/form";
    }

    /**
     * Обновление товара
     */
    @PostMapping("/{id}")
    public String updateProduct(
            @PathVariable Long id,
            @Valid @ModelAttribute Product product,
            BindingResult bindingResult,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            product.setId(id);
            model.addAttribute("categories", categoryService.getAllCategories());
            return "admin/products/form";
        }

        try {
            productService.updateProduct(id, product, imageFile);
            redirectAttributes.addFlashAttribute("successMessage", "Товар успешно обновлён");
            return "redirect:/admin/products";
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Ошибка обновления товара: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("categories", categoryService.getAllCategories());
            product.setId(id);
            return "admin/products/form";
        } catch (Exception e) {
            log.error("Неожиданная ошибка обновления товара: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Произошла ошибка при обновлении товара: " + e.getMessage());
            model.addAttribute("categories", categoryService.getAllCategories());
            product.setId(id);
            return "admin/products/form";
        }
    }

    /**
     * Удаление товара
     */
    @PostMapping("/{id}/delete")
    public String deleteProduct(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("successMessage", "Товар удалён");
        } catch (Exception e) {
            log.error("Ошибка удаления товара: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/products";
    }
}