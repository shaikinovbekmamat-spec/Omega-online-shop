package com.omega.shop.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Обработка 404 ошибок
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NoHandlerFoundException ex, Model model) {
        log.error("404 ошибка: {}", ex.getMessage());
        model.addAttribute("error", "Страница не найдена");
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }

    /**
     * Обработка отсутствующих статических ресурсов (favicon и т.п.)
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(NoResourceFoundException ex, Model model) {
        log.warn("Ресурс не найден: {}", ex.getResourcePath());
        model.addAttribute("error", "Ресурс не найден");
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }

    /**
     * Обработка 403 ошибок (доступ запрещён)
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        log.error("403 ошибка: {}", ex.getMessage());
        model.addAttribute("error", "Доступ запрещён");
        model.addAttribute("message", "У вас нет прав для доступа к этой странице");
        return "error/403";
    }

    /**
     * Обработка ошибок валидации и некорректных аргументов
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model) {
        log.error("Ошибка валидации: {}", ex.getMessage());
        model.addAttribute("error", "Ошибка валидации");
        model.addAttribute("message", ex.getMessage());
        return "error/500";
    }

    /**
     * Обработка ошибок целостности данных (дубликаты, нарушение ограничений)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleDataIntegrityViolation(DataIntegrityViolationException ex, Model model) {
        log.error("Ошибка целостности данных: {}", ex.getMessage());
        String message = "Ошибка сохранения данных";
        if (ex.getMessage() != null && ex.getMessage().contains("duplicate")) {
            message = "Запись с такими данными уже существует";
        } else if (ex.getMessage() != null && ex.getMessage().contains("constraint")) {
            message = "Нарушение ограничений базы данных";
        }
        model.addAttribute("error", "Ошибка сохранения");
        model.addAttribute("message", message);
        return "error/500";
    }

    /**
     * Обработка ошибок состояния (например, попытка выполнить недопустимое действие)
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalState(IllegalStateException ex, Model model) {
        log.error("Ошибка состояния: {}", ex.getMessage());
        model.addAttribute("error", "Ошибка операции");
        model.addAttribute("message", ex.getMessage());
        return "error/500";
    }

    /**
     * Обработка остальных ошибок
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleException(Exception ex, Model model) {
        log.error("Внутренняя ошибка сервера: ", ex);
        model.addAttribute("error", "Внутренняя ошибка сервера");
        model.addAttribute("message", ex.getMessage() != null ? ex.getMessage() : "Произошла неизвестная ошибка");
        return "error/500";
    }
}