package com.sodi.metareports.user.entity;

import com.sodi.metareports.role.entity.Role;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity @Table(name = "app_user")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable=false, unique=true, length=80) private String username;
    @Column(nullable=false, unique=true, length=254) private String email;
    @Column(name="password_hash", nullable=false, length=100) private String passwordHash;
    @Column(name="first_name", nullable=false) private String firstName;
    @Column(name="last_name", nullable=false) private String lastName;
    private boolean enabled = true;
    @Column(name="account_locked") private boolean accountLocked;
    @Column(name="failed_login_attempts") private int failedLoginAttempts;
    @Column(name="locked_until") private Instant lockedUntil;
    @Column(name="last_login_at") private Instant lastLoginAt;
    @Column(name="password_changed_at") private Instant passwordChangedAt = Instant.now();
    @Column(name="created_at", insertable=false, updatable=false) private Instant createdAt;
    @Column(name="updated_at") private Instant updatedAt = Instant.now();
    @Version private long version;
    @ManyToMany(fetch=FetchType.EAGER)
    @JoinTable(name="user_role", joinColumns=@JoinColumn(name="user_id"), inverseJoinColumns=@JoinColumn(name="role_id"))
    private Set<Role> roles = new HashSet<>();
    @PreUpdate void touch(){ updatedAt=Instant.now(); }
    public UUID getId(){return id;} public String getUsername(){return username;} public void setUsername(String v){username=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;} public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;}
    public String getFirstName(){return firstName;} public void setFirstName(String v){firstName=v;} public String getLastName(){return lastName;} public void setLastName(String v){lastName=v;}
    public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;} public boolean isAccountLocked(){return accountLocked;} public void setAccountLocked(boolean v){accountLocked=v;}
    public int getFailedLoginAttempts(){return failedLoginAttempts;} public void setFailedLoginAttempts(int v){failedLoginAttempts=v;} public Instant getLockedUntil(){return lockedUntil;} public void setLockedUntil(Instant v){lockedUntil=v;}
    public Instant getLastLoginAt(){return lastLoginAt;} public void setLastLoginAt(Instant v){lastLoginAt=v;} public Instant getPasswordChangedAt(){return passwordChangedAt;} public void setPasswordChangedAt(Instant v){passwordChangedAt=v;}
    public Set<Role> getRoles(){return roles;} public void setRoles(Set<Role> v){roles=v;}
}
