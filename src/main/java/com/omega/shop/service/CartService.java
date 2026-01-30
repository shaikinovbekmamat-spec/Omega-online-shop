package com.omega.shop.service;

import com.omega.shop.dto.CartItem;
import com.omega.shop.dto.ShoppingCart;
import com.omega.shop.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final ShoppingCart shoppingCart;
    private final ProductService productService;

    /**
     * Добавить товар в корзину Бека топ
     */
    public void addToCart(Long productId, int quantity) {
        log.info("Добавление товара {} в корзину, количество: {}", productId, quantity);

        // Проверка количества
        if (quantity <= 0) {

            throw new IllegalArgumentException("Количество должно быть   больше 0");


        }

        // Получение товара
        Product product = productService.getProductById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        // Проверка наличия на складе
        if (!product.isInStock()) {
            throw new IllegalStateException("Товар отсутствует на складе");
        }

        // Проверка достаточного количества
        int currentQuantityInCart = shoppingCart.containsProduct(productId)
                ? shoppingCart.getItem(productId).getQuantity()
                : 0;

        if (currentQuantityInCart + quantity > product.getQuantity()) {
            throw new IllegalStateException(
                    "Недостаточно товара на складе. Доступно: " + product.getQuantity()
            );
        }

        // Добавление в корзину
        CartItem cartItem = new CartItem(product, quantity);
        shoppingCart.addItem(cartItem);

        log.info("Товар {} успешно добавлен в корзину", product.getName());
    }

    /**
     * Удалить товар из корзины
     */
    public void removeFromCart(Long productId) {
        log.info("Удаление товара {} из корзины", productId);
        shoppingCart.removeItem(productId);
    }

    /**
     * Обновить количество товара в корзине
     */
    public void updateQuantity(Long productId, int quantity) {
        log.info("Обновление количества товара {} до {}", productId, quantity);

        if (quantity <= 0) {
            removeFromCart(productId);
            return;
        }

        // Проверка наличия товара
        Product product = productService.getProductById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        // Проверка достаточного количества на складе
        if (quantity > product.getQuantity()) {
            throw new IllegalStateException(
                    "Недостаточно товара на складе. Доступно: " + product.getQuantity()
            );
        }

        shoppingCart.updateQuantity(productId, quantity);
    }

    /**
     * Очистить корзину
     */
    public void clearCart() {
        log.info("Очистка корзины");
        shoppingCart.clear();
    }

    /**
     * Получить корзину
     */
    public ShoppingCart getCart() {
        return shoppingCart;
    }

    /**
     * Проверить доступность всех товаров в корзине
     */
    public void validateCart() {
        for (CartItem item : shoppingCart.getItems()) {
            Product product = productService.getProductById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Товар " + item.getProductName() + " не найден"
                    ));

            if (!product.isInStock()) {
                throw new IllegalStateException(
                        "Товар " + product.getName() + " больше не доступен"
                );
            }

            if (item.getQuantity() > product.getQuantity()) {
                throw new IllegalStateException(
                        "Для товара " + product.getName() +
                                " доступно только " + product.getQuantity() + " шт."
                );
            }
        }
    }
}