package com.omega.shop.controller;

import com.omega.shop.dto.ShoppingCart;
import com.omega.shop.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;

    /**
     * Страница корзины
     */
    @GetMapping("/cart")
    public String viewCart(Model model) {
        ShoppingCart cart = cartService.getCart();
        model.addAttribute("cart", cart);
        return "cart/cart";
    }

    /**
     * Добавить товар в корзину (AJAX)
     */
    @PostMapping("/cart/add")
    @ResponseBody
    public ResponseEntity<?> addToCart(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") int quantity
    ) {
        try {
            cartService.addToCart(productId, quantity);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Товар добавлен в корзину");
            response.put("cartItemsCount", cartService.getCart().getTotalItems());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Ошибка добавления в корзину: {}", e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Обновить количество товара (AJAX)
     */
    @PostMapping("/cart/update")
    @ResponseBody
    public ResponseEntity<?> updateQuantity(
            @RequestParam Long productId,
            @RequestParam int quantity
    ) {
        try {
            cartService.updateQuantity(productId, quantity);

            ShoppingCart cart = cartService.getCart();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("cartItemsCount", cart.getTotalItems());
            response.put("totalPrice", cart.getTotalPrice());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Ошибка обновления корзины: {}", e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Удалить товар из корзины
     */
    @PostMapping("/cart/remove")
    public String removeFromCart(
            @RequestParam Long productId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            cartService.removeFromCart(productId);
            redirectAttributes.addFlashAttribute("successMessage", "Товар удалён из корзины");
        } catch (Exception e) {
            log.error("Ошибка удаления из корзины: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/cart";
    }

    /**
     * Очистить корзину
     */
    @PostMapping("/cart/clear")
    public String clearCart(RedirectAttributes redirectAttributes) {
        cartService.clearCart();
        redirectAttributes.addFlashAttribute("successMessage", "Корзина очищена");
        return "redirect:/cart";
    }

    /**
     * Получить количество товаров в корзине (AJAX)
     */
    @GetMapping("/cart/count")
    @ResponseBody
    public ResponseEntity<Map<String, Integer>> getCartCount() {
        int count = cartService.getCart().getTotalItems();
        return ResponseEntity.ok(Map.of("count", count));
    }
}