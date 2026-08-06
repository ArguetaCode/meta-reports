package com.sodi.metareports.role.entity;
import jakarta.persistence.*; import java.util.*;
@Entity @Table(name="role") public class Role {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @Column(nullable=false,unique=true) private String code; @Column(nullable=false) private String name; private String description;
 @Column(name="system_role") private boolean systemRole;
 @ManyToMany(fetch=FetchType.EAGER) @JoinTable(name="role_permission",joinColumns=@JoinColumn(name="role_id"),inverseJoinColumns=@JoinColumn(name="permission_id")) private Set<Permission> permissions=new HashSet<>();
 public UUID getId(){return id;} public String getCode(){return code;} public String getName(){return name;} public Set<Permission> getPermissions(){return permissions;}
}
