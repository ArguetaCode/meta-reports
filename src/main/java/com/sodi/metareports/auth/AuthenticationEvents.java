package com.sodi.metareports.auth;
import com.sodi.metareports.user.repository.AppUserRepository; import java.time.*; import org.springframework.beans.factory.annotation.Value; import org.springframework.context.event.EventListener; import org.springframework.security.authentication.event.*; import org.springframework.stereotype.Component; import org.springframework.transaction.annotation.Transactional;
@Component public class AuthenticationEvents {
 private final AppUserRepository users; private final int maxAttempts; private final long lockMinutes;
 public AuthenticationEvents(AppUserRepository users,@Value("${app.security.max-failed-attempts:5}") int m,@Value("${app.security.lock-duration-minutes:15}") long l){this.users=users;maxAttempts=m;lockMinutes=l;}
 @EventListener @Transactional public void success(AuthenticationSuccessEvent e){users.findByUsernameIgnoreCase(e.getAuthentication().getName()).ifPresent(u->{u.setFailedLoginAttempts(0);u.setAccountLocked(false);u.setLockedUntil(null);u.setLastLoginAt(Instant.now());users.save(u);});}
 @EventListener @Transactional public void failure(AbstractAuthenticationFailureEvent e){String n=e.getAuthentication().getName();users.findByUsernameIgnoreCase(n).ifPresent(u->{int attempts=u.getFailedLoginAttempts()+1;u.setFailedLoginAttempts(attempts);if(attempts>=maxAttempts){u.setAccountLocked(true);u.setLockedUntil(Instant.now().plus(Duration.ofMinutes(lockMinutes)));}users.save(u);});}
}
