package com.toeflsimulator.identity;
import java.util.UUID;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
public record StudentPrincipal(UUID id, String email, String passwordHash) implements UserDetails {
    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return passwordHash; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return java.util.List.of(new SimpleGrantedAuthority("ROLE_STUDENT")); }
}
