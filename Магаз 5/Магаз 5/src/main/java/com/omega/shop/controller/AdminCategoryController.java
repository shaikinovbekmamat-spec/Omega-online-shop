package com.omega.shop.controller;

import com.omega.shop.entity.Category;
import com.omega.shop.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminCategoryController {

    private final CategoryService categoryService;

    /**
     * Список всех категорий
     */
    @GetMapping
    public String listCategories(Model model) {
        try {
            List<Category> categories = categoryService.getAllCategories();
            // Создаем Map для хранения количества товаров по категориям
            java.util.Map<Long, Long> productCounts = new java.util.HashMap<>();
            categories.forEach(category -> {
                long productCount = categoryService.countProductsInCategory(category.getId());
                productCounts.put(category.getId(), productCount);
            });
            model.addAttribute("categories", categories);
            model.addAttribute("productCounts", productCounts);
            return "admin/categories/list";
        } catch (Exception e) {
            log.error("Ошибка загрузки категорий: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Произошла ошибка при загрузке категорий: " + e.getMessage());
            model.addAttribute("categories", List.of());
            model.addAttribute("productCounts", new java.util.HashMap<>());
            return "admin/categories/list";
        }
    }

    /**
     * Форма создания категории
     */
    @GetMapping("/new")
    public String newCategoryForm(Model model) {
        model.addAttribute("category", new Category());
        return "admin/categories/form";
    }

    /**
     * Создание категории
     */
    @PostMapping
    public String createCategory(
            @Valid @ModelAttribute Category category,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "admin/categories/form";
        }

        try {
            categoryService.createCategory(category);
            redirectAttributes.addFlashAttribute("successMessage", "Категория успешно создана");
            return "redirect:/admin/categories";
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Ошибка создания категории: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/categories";
        } catch (Exception e) {
            log.error("Неожиданная ошибка создания категории: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Произошла ошибка при создании категории: " + e.getMessage());
            return "redirect:/admin/categories";
        }
    }

    /**
     * Форма редактирования категории
     */
    @GetMapping("/{id}/edit")
    public String editCategoryForm(@PathVariable Long id, Model model) {
        Category category = categoryService.getCategoryById(id)
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));

        model.addAttribute("category", category);
        return "admin/categories/form";
    }

    /**
     * Обновление категории
     */
    @PostMapping("/{id}")
    public String updateCategory(
            @PathVariable Long id,
            @Valid @ModelAttribute Category category,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            category.setId(id);
            return "admin/categories/form";
        }

        try {
            categoryService.updateCategory(id, category);
            redirectAttributes.addFlashAttribute("successMessage", "Категория успешно обновлена");
            return "redirect:/admin/categories";
        } catch (IllegalArgumentException e) {
            log.error("Ошибка обновления категории: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            category.setId(id);
            return "admin/categories/form";
        }
    }

    /**
     * Удаление категории
     */
    @PostMapping("/{id}/delete")
    public String deleteCategory(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("successMessage", "Категория удалена");
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.error("Ошибка удаления категории: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/categories";
    }
}