package com.sodi.metareports.user.repository;
import com.sodi.metareports.user.entity.AppUser; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface AppUserRepository extends JpaRepository<AppUser,UUID> { Optional<AppUser> findByUsernameIgnoreCase(String username); boolean existsByEmailIgnoreCase(String email); }
