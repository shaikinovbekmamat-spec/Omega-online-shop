package com.omega.shop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(
            @RequestParam(required = false) String error,
            Model model) {
        model.addAttribute("message", "Добро пожаловать в интернет-магазин OMEGA!");
        
        // Обработка ошибок (если есть)
        if (error != null) {
            switch (error) {
                case "internal":
                    model.addAttribute("errorMessage", "Произошла внутренняя ошибка сервера. Пожалуйста, попробуйте позже.");
                    break;
                case "validation":
                    model.addAttribute("errorMessage", "Ошибка валидации данных.");
                    break;
                case "data":
                    model.addAttribute("errorMessage", "Ошибка сохранения данных.");
                    break;
                case "state":
                    model.addAttribute("errorMessage", "Ошибка выполнения операции.");
                    break;
            }
        }
        
        return "index";
    }
}