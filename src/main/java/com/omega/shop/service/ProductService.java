package com.omega.shop.service;

import com.omega.shop.entity.Category;
import com.omega.shop.entity.Product;
import com.omega.shop.repository.OrderItemRepository;
import com.omega.shop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;
    private final OrderItemRepository orderItemRepository;

    /**
     * Получить все активные товары с пагинацией
     */
    public Page<Product> getAllActiveProducts(Pageable pageable) {
        return productRepository.findByIsActiveTrue(pageable);
    }

    /**
     * Получить все товары (для админки)
     */
    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    /**
     * Получить товар по ID
     */
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    /**
     * Поиск товаров по ключевому слову
     */
    public Page<Product> searchProducts(String keyword, Pageable pageable) {
        log.info("Поиск товаров по запросу: {}", keyword);
        return productRepository.searchByName(keyword, pageable);
    }

    /**
     * Фильтрация по категории
     */
    public Page<Product> getProductsByCategory(Category category, Pageable pageable) {
        return productRepository.findByCategoryAndIsActiveTrue(category, pageable);
    }

    /**
     * Фильтрация по категории и диапазону цен
     */
    public Page<Product> getProductsByCategoryAndPriceRange(
            Category category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {
        return productRepository.findByCategoryAndPriceRange(category, minPrice, maxPrice, pageable);
    }

    /**
     * Фильтрация только по диапазону цен
     */
    public Page<Product> getProductsByPriceRange(
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {
        return productRepository.findByPriceRange(minPrice, maxPrice, pageable);
    }

    /**
     * Создать новый товар
     */
    @Transactional
    public Product createProduct(Product product, MultipartFile imageFile) {
        log.info("Создание нового товара: {}", product.getName());

        try {
            // Загрузка изображения (если есть)
            if (imageFile != null && !imageFile.isEmpty()) {
                String imagePath = fileStorageService.saveFile(imageFile);
                product.setImagePath(imagePath);
            }

            return productRepository.save(product);
        } catch (IllegalArgumentException e) {
            log.error("Ошибка валидации при создании товара: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Неожиданная ошибка при создании товара: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось создать товар: " + e.getMessage(), e);
        }
    }

    /**
     * Обновить товар
     */
    @Transactional
    public Product updateProduct(Long id, Product productDetails, MultipartFile imageFile) {
        log.info("Обновление товара с ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        // Обновление полей
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setQuantity(productDetails.getQuantity());
        product.setSpecifications(productDetails.getSpecifications());
        product.setCategory(productDetails.getCategory());
        product.setActive(productDetails.isActive());

        // Обновление изображения (если загружено новое)
        if (imageFile != null && !imageFile.isEmpty()) {
            // Удаление старого изображения
            if (product.getImagePath() != null) {
                fileStorageService.deleteFile(product.getImagePath());
            }
            // Сохранение нового
            String imagePath = fileStorageService.saveFile(imageFile);
            product.setImagePath(imagePath);
        }

        return productRepository.save(product);
    }

    /**
     * Удалить товар
     */
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Удаление товара с ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        if (orderItemRepository.existsByProduct(product)) {
            throw new IllegalStateException("Товар нельзя удалить, так как он уже есть в заказах");
        }

        // Удаление изображения
        if (product.getImagePath() != null) {
            fileStorageService.deleteFile(product.getImagePath());
        }

        productRepository.delete(product);
    }

    /**
     * Уменьшить количество товара
     */
    @Transactional
    public void decreaseQuantity(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        product.decreaseQuantity(quantity);
        productRepository.save(product);
    }

    /**
     * Увеличить количество товара
     */
    @Transactional
    public void increaseQuantity(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        product.increaseQuantity(quantity);
        productRepository.save(product);
    }
}