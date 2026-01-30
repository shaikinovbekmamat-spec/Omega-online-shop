package com.omega.shop.controller;

import com.omega.shop.entity.Category;
import com.omega.shop.entity.Product;
import com.omega.shop.service.CategoryService;
import com.omega.shop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CatalogController {

    private final ProductService productService;
    private final CategoryService categoryService;

    /**
     * Главная страница каталога
     */
    @GetMapping("/catalog")
    public String catalog(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            Model model
    ) {
        // Создание объекта пагинации
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // Получение товаров с учётом фильтров
        Page<Product> productsPage;

        if (search != null && !search.trim().isEmpty()) {
            // Поиск по ключевому слову
            productsPage = productService.searchProducts(search, pageable);
            model.addAttribute("search", search);
        } else if (categoryId != null) {
            Category category = categoryService.getCategoryById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));

            if (minPrice != null && maxPrice != null) {
                // Фильтр по категории и цене
                productsPage = productService.getProductsByCategoryAndPriceRange(
                        category, minPrice, maxPrice, pageable);
            } else {
                // Только по категории
                productsPage = productService.getProductsByCategory(category, pageable);
            }
            model.addAttribute("selectedCategory", category);
        } else if (minPrice != null && maxPrice != null) {
            // Только по цене
            productsPage = productService.getProductsByPriceRange(minPrice, maxPrice, pageable);
        } else {
            // Все товары
            productsPage = productService.getAllActiveProducts(pageable);
        }

        // Получение всех категорий для фильтра
        List<Category> categories = categoryService.getAllCategories();

        // Добавление данных в модель
        model.addAttribute("products", productsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productsPage.getTotalPages());
        model.addAttribute("totalItems", productsPage.getTotalElements());
        model.addAttribute("categories", categories);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        return "catalog/catalog";
    }

    /**
     * Детальная страница товара
     */
    @GetMapping("/product/{id}")
    public String productDetails(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        model.addAttribute("product", product);
        return "catalog/details";
    }
}