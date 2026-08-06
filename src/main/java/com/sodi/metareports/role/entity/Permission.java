package com.sodi.metareports.role.entity;
import jakarta.persistence.*; import java.util.UUID;
@Entity @Table(name="permission") public class Permission { @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id; @Column(nullable=false,unique=true) private String code; @Column(nullable=false) private String name; private String description; public String getCode(){return code;} }
