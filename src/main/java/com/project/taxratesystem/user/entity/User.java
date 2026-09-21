package com.project.taxratesystem.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Table(name = "user")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
}
