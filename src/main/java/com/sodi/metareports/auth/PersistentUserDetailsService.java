package com.sodi.metareports.auth;
import com.sodi.metareports.user.entity.AppUser; import com.sodi.metareports.user.repository.AppUserRepository;
import java.time.Instant; import org.springframework.security.core.userdetails.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class PersistentUserDetailsService implements UserDetailsService {
 private final AppUserRepository users; public PersistentUserDetailsService(AppUserRepository users){this.users=users;}
 @Override @Transactional(readOnly=true) public UserDetails loadUserByUsername(String username){ AppUser u=users.findByUsernameIgnoreCase(username).orElseThrow(()->new UsernameNotFoundException("Invalid credentials"));
  boolean locked=u.isAccountLocked() && (u.getLockedUntil()==null || u.getLockedUntil().isAfter(Instant.now()));
  var authorities=u.getRoles().stream().flatMap(r->java.util.stream.Stream.concat(java.util.stream.Stream.of("ROLE_"+r.getCode()),r.getPermissions().stream().map(p->p.getCode()))).distinct().toArray(String[]::new);
  return User.withUsername(u.getUsername()).password(u.getPasswordHash()).disabled(!u.isEnabled()).accountLocked(locked).authorities(authorities).build(); }
}
