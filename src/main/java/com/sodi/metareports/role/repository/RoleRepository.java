package com.sodi.metareports.role.repository;
import com.sodi.metareports.role.entity.Role; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface RoleRepository extends JpaRepository<Role,UUID> { Optional<Role> findByCode(String code); }
