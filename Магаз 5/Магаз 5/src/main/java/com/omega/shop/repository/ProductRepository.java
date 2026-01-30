package com.omega.shop.repository;

import com.omega.shop.entity.Category;
import com.omega.shop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Найти все активные товары
     */
    Page<Product> findByIsActiveTrue(Pageable pageable);

    /**
     * Найти товары по категории
     */
    Page<Product> findByCategoryAndIsActiveTrue(Category category, Pageable pageable);

    /**
     * Поиск товаров по имени (регистронезависимый)
     */
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND p.isActive = true")
    Page<Product> searchByName(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Фильтрация по категории и диапазону цен
     */
    @Query("SELECT p FROM Product p WHERE p.category = :category " +
            "AND p.price BETWEEN :minPrice AND :maxPrice " +
            "AND p.isActive = true")
    Page<Product> findByCategoryAndPriceRange(
            @Param("category") Category category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    /**
     * Фильтрация только по диапазону цен
     */
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice AND p.isActive = true")
    Page<Product> findByPriceRange(
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    /**
     * Получить товары с количеством меньше указанного (для уведомлений о низком остатке)
     */
    List<Product> findByQuantityLessThan(Integer quantity);

    /**
     * Подсчитать количество товаров с низким остатком
     */
    long countByQuantityLessThan(Integer quantity);

    /**
     * Последние добавленные товары
     */
    @EntityGraph(attributePaths = {"category"})
    List<Product> findTop5ByOrderByCreatedAtDesc();
}
