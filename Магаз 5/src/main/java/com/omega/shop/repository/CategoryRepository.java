package com.omega.shop.repository;

import com.omega.shop.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Найти категорию по имени
     */
    Optional<Category> findByName(String name);

    /**
     * Проверить существование категории по имени
     */
    boolean existsByName(String name);

    /**
     * Подсчитать количество товаров в категории
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
    long countProductsByCategoryId(@Param("categoryId") Long categoryId);
}
