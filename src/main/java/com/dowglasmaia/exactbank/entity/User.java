package com.dowglasmaia.exactbank.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuario")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(length = 100)
    private String name;

    @Column(length = 11, unique = true)
    private String CPF;

    @Column(length = 100, unique = true)
    private String email;

    @Column(length = 9)
    private Integer phoneNumber;

    @Column(length = 2)
    private Integer areaCode;

}
