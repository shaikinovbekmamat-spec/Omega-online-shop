package com.omega.shop.controller;

import com.omega.shop.entity.User;
import com.omega.shop.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final UserService userService;

    /**
     * Страница профиля пользователя
     */
    @GetMapping
    public String profilePage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        try {
            Optional<User> optionalUser = userService.findByUsername(userDetails.getUsername());
            
            if (optionalUser.isEmpty()) {
                model.addAttribute("errorMessage", "Пользователь не найден");
                return "profile/index";
            }

            User user = optionalUser.get();
            model.addAttribute("user", user);
            
            return "profile/index";
        } catch (Exception e) {
            log.error("Ошибка загрузки профиля: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Произошла ошибка при загрузке профиля: " + e.getMessage());
            return "profile/index";
        }
    }
}

