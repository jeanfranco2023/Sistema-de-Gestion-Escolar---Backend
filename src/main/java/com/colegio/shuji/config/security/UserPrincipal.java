package com.colegio.shuji.config.security;

import com.colegio.shuji.config.persistence.SupabaseAuditInterceptor;
import com.colegio.shuji.usuario.domain.model.Usuario;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Representación del usuario autenticado en el contexto de seguridad de Spring Security. */
@Getter
@Builder
@AllArgsConstructor
public class UserPrincipal implements UserDetails, SupabaseAuditInterceptor.UserPrincipalHolder {

  private final Long id;
  private final UUID uuid;
  private final String username;
  private final String email;
  private final String password;
  private final boolean active;
  private final Collection<? extends GrantedAuthority> authorities;

  public static UserPrincipal fromDomain(Usuario usuario) {
    List<SimpleGrantedAuthority> authorities =
        usuario.getRoles().stream()
            .map(
                r ->
                    new SimpleGrantedAuthority(
                        r.getCodigo().startsWith("ROLE_")
                            ? r.getCodigo()
                            : "ROLE_" + r.getCodigo()))
            .toList();

    return UserPrincipal.builder()
        .id(usuario.getId())
        .uuid(usuario.getUuid())
        .username(usuario.getUsername())
        .email(usuario.getEmail())
        .password(usuario.getPasswordHash())
        .active(usuario.estaActivo())
        .authorities(authorities)
        .build();
  }

  @Override
  public Long getUserId() {
    return id;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return active;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return active;
  }
}
