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
     * Проверить существование категории по имени и родителю
     */
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.name = :name AND " +
           "(COALESCE(:parentId, NULL) IS NULL AND c.parent IS NULL OR c.parent.id = :parentId)")
    boolean existsByNameAndParent(@Param("name") String name, @Param("parentId") Long parentId);

    /**
     * Найти все корневые категории (без родителя)
     */
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL ORDER BY c.name")
    java.util.List<Category> findRootCategories();

    /**
     * Найти все дочерние категории для родителя
     */
    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId ORDER BY c.name")
    java.util.List<Category> findByParentId(@Param("parentId") Long parentId);

    /**
     * Подсчитать количество товаров в категории
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
    long countProductsByCategoryId(@Param("categoryId") Long categoryId);
}
