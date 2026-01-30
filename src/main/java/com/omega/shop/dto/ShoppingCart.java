package com.omega.shop.dto;

import lombok.Data;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
@Data
public class ShoppingCart implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<Long, CartItem> items = new HashMap<>();

    /**
     * Добавить товар в корзину
     */
    public void addItem(CartItem item) {
        Long productId = item.getProductId();

        if (items.containsKey(productId)) {
            // Если товар уже в корзине - увеличиваем количество
            CartItem existingItem = items.get(productId);
            existingItem.increaseQuantity(item.getQuantity());
        } else {
            // Добавляем новый товар
            items.put(productId, item);
        }
    }

    /**
     * Удалить товар из корзины
     */
    public void removeItem(Long productId) {
        items.remove(productId);
    }

    /**
     * Обновить количество товара
     */
    public void updateQuantity(Long productId, int quantity) {
        if (items.containsKey(productId)) {
            if (quantity <= 0) {
                items.remove(productId);
            } else {
                items.get(productId).setQuantity(quantity);
            }
        }
    }

    /**
     * Получить товар из корзины
     */
    public CartItem getItem(Long productId) {
        return items.get(productId);
    }

    /**
     * Получить все товары
     */
    public List<CartItem> getItems() {
        return List.copyOf(items.values());
    }

    /**
     * Очистить корзину
     */
    public void clear() {
        items.clear();
    }

    /**
     * Проверить, пуста ли корзина
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Получить количество товаров в корзине
     */
    public int getTotalItems() {
        return items.values().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    /**
     * Получить общую стоимость корзины
     */
    public BigDecimal getTotalPrice() {
        return items.values().stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Проверить, содержится ли товар в корзине
     */
    public boolean containsProduct(Long productId) {
        return items.containsKey(productId);
    }
}