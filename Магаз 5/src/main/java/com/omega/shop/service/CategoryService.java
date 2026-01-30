package com.omega.shop.service;

import com.omega.shop.entity.Category;
import com.omega.shop.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Получить все категории
     */
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        // Инициализируем коллекцию products для каждой категории
        categories.forEach(category -> category.getProducts().size());
        return categories;
    }

    /**
     * Найти категорию по ID
     */
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    /**
     * Найти категорию по имени
     */
    public Optional<Category> getCategoryByName(String name) {
        return categoryRepository.findByName(name);
    }

    /**
     * Создать новую категорию
     */
    @Transactional
    public Category createCategory(Category category) {
        log.info("Создание новой категории: {}", category.getName());

        if (categoryRepository.existsByName(category.getName())) {
            throw new IllegalArgumentException("Категория с таким именем уже существует");
        }

        return categoryRepository.save(category);
    }

    /**
     * Обновить категорию
     */
    @Transactional
    public Category updateCategory(Long id, Category categoryDetails) {
        log.info("Обновление категории с ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));

        // Проверка уникальности имени (если изменилось)
        if (!category.getName().equals(categoryDetails.getName())
                && categoryRepository.existsByName(categoryDetails.getName())) {
            throw new IllegalArgumentException("Категория с таким именем уже существует");
        }

        category.setName(categoryDetails.getName());
        category.setDescription(categoryDetails.getDescription());

        return categoryRepository.save(category);
    }

    /**
     * Удалить категорию
     */
    @Transactional
    public void deleteCategory(Long id) {
        log.info("Удаление категории с ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));

        // Проверка на наличие товаров
        if (categoryRepository.countProductsByCategoryId(id) > 0) {
            throw new IllegalStateException(
                    "Нельзя удалить категорию, в которой есть товары. " +
                            "Сначала удалите или переместите товары."
            );
        }

        categoryRepository.delete(category);
    }

    /**
     * Подсчёт товаров в категории
     */
    public long countProductsInCategory(Long categoryId) {
        return categoryRepository.countProductsByCategoryId(categoryId);
    }
}
