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
     * Получить все категории (плоский список)
     */
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        // Инициализируем коллекции для каждой категории
        categories.forEach(category -> {
            category.getProducts().size();
            category.getChildren().size(); // Инициализируем children
            if (category.getParent() != null) {
                category.getParent().getName(); // Инициализируем parent
            }
        });
        return categories;
    }

    /**
     * Получить все корневые категории (для построения дерева)
     */
    @Transactional(readOnly = true)
    public List<Category> getRootCategories() {
        return categoryRepository.findRootCategories();
    }

    /**
     * Получить дочерние категории для родителя
     */
    @Transactional(readOnly = true)
    public List<Category> getChildCategories(Long parentId) {
        return categoryRepository.findByParentId(parentId);
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

        Long parentId = category.getParent() != null ? category.getParent().getId() : null;
        
        // Проверка уникальности имени в рамках родителя
        if (categoryRepository.existsByNameAndParent(category.getName(), parentId)) {
            throw new IllegalArgumentException("Категория с таким именем уже существует в этой родительской категории");
        }

        // Проверка, что родитель не является самой категорией (защита от циклических ссылок)
        if (category.getId() != null && parentId != null && category.getId().equals(parentId)) {
            throw new IllegalArgumentException("Категория не может быть родителем самой себя");
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

        Long newParentId = categoryDetails.getParent() != null ? categoryDetails.getParent().getId() : null;
        Long oldParentId = category.getParent() != null ? category.getParent().getId() : null;

        // Проверка уникальности имени в рамках родителя (если изменилось имя или родитель)
        if ((!category.getName().equals(categoryDetails.getName()) || 
             (newParentId != null && !newParentId.equals(oldParentId)) ||
             (newParentId == null && oldParentId != null)) &&
            categoryRepository.existsByNameAndParent(categoryDetails.getName(), newParentId)) {
            throw new IllegalArgumentException("Категория с таким именем уже существует в этой родительской категории");
        }

        // Проверка, что родитель не является самой категорией или её потомком (защита от циклических ссылок)
        if (newParentId != null) {
            if (newParentId.equals(id)) {
                throw new IllegalArgumentException("Категория не может быть родителем самой себя");
            }
            // Проверка, что новый родитель не является потомком этой категории
            if (isDescendant(newParentId, id)) {
                throw new IllegalArgumentException("Категория не может быть родителем своей дочерней категории");
            }
        }

        category.setName(categoryDetails.getName());
        category.setDescription(categoryDetails.getDescription());
        category.setParent(categoryDetails.getParent());

        return categoryRepository.save(category);
    }

    /**
     * Проверить, является ли categoryId потомком ancestorId
     */
    private boolean isDescendant(Long categoryId, Long ancestorId) {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category == null || category.getParent() == null) {
            return false;
        }
        if (category.getParent().getId().equals(ancestorId)) {
            return true;
        }
        return isDescendant(category.getParent().getId(), ancestorId);
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

        // Проверка на наличие подкатегорий
        if (!category.getChildren().isEmpty()) {
            throw new IllegalStateException(
                    "Нельзя удалить категорию, у которой есть подкатегории. " +
                            "Сначала удалите или переместите подкатегории."
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
