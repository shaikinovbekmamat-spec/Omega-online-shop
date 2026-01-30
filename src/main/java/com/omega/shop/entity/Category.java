package com.omega.shop.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Название категории обязательно")
    @Size(min = 2, max = 100, message = "Название должно быть от 2 до 100 символов")
    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Родительская категория (для создания иерархии)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    // Дочерние категории (подкатегории)
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Category> children = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Связь с товарами (один ко многим)
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Product> products = new ArrayList<>();

    /**
     * Проверить, является ли категория корневой (без родителя)
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * Получить полный путь категории (например: "Электроника > Телефоны > Смартфоны")
     */
    public String getFullPath() {
        return getFullPath(new java.util.HashSet<>());
    }

    /**
     * Внутренний метод для получения пути с защитой от циклических ссылок
     */
    private String getFullPath(java.util.Set<Long> visited) {
        if (visited.contains(id)) {
            return name; // Защита от циклических ссылок
        }
        visited.add(id);
        
        if (parent == null) {
            return name;
        }
        return parent.getFullPath(visited) + " > " + name;
    }
}