package com.colegio.shuji.shared.infrastructure.audit;

import java.util.UUID;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

@Service
@RequiredArgsConstructor
public class AuditoriaAccesoService {

  private final AuditoriaAccesoRepository repository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void registrar(
      String metodo,
      String ruta,
      int estadoHttp,
      String direccionIp,
      String agenteUsuario,
      long duracionMs,
      Long usuarioId,
      Authentication authentication) {
    boolean autenticado =
        authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);

    var evento = new AuditoriaAccesoEntity();
    evento.setSolicitudId(UUID.randomUUID().toString());
    evento.setUsuarioNombre(autenticado ? limitar(authentication.getName(), 100) : "ANONIMO");
    evento.setRoles(
        autenticado
            ? authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(rol -> rol.startsWith("ROLE_") ? rol.substring(5) : rol)
                .sorted()
                .reduce((primero, segundo) -> primero + "," + segundo)
                .map(roles -> limitar(roles, 300))
                .orElse("SIN_ROL")
            : "ANONIMO");
    evento.setUsuarioId(usuarioId);
    evento.setMetodo(limitar(metodo, 10));
    evento.setRuta(limitar(rutaAuditable(ruta), 500));
    evento.setEstadoHttp(estadoHttp);
    evento.setDireccionIp(limitar(direccionIp, 45));
    evento.setAgenteUsuario(limitar(agenteUsuario, 300));
    evento.setDuracionMs(Math.max(duracionMs, 0));
    repository.save(evento);
  }

  @Transactional(readOnly = true)
  public org.springframework.data.domain.Page<AuditoriaAccesoEntity> listar(
      org.springframework.data.domain.Pageable pageable) {
    return repository.findAllByOrderByFechaEventoDesc(pageable);
  }

  @Transactional(readOnly = true)
  public org.springframework.data.domain.Page<AuditoriaAccesoEntity> listar(
      String usuarioNombre,
      String metodo,
      String ruta,
      org.springframework.data.domain.Pageable pageable) {
    String u = StringUtils.hasText(usuarioNombre) ? usuarioNombre.trim() : null;
    String m = StringUtils.hasText(metodo) ? metodo.trim() : null;
    String r = StringUtils.hasText(ruta) ? ruta.trim() : null;
    if (u == null && m == null && r == null) {
      return repository.findAllByOrderByFechaEventoDesc(pageable);
    }
    return repository.filtrar(u, m, r, pageable);
  }

  private static String rutaAuditable(String ruta) {
    if (ruta == null) return null;
    try {
      String decoded = UriUtils.decode(ruta, StandardCharsets.UTF_8);
      String seguimiento = "/api/v1/solicitudes-matricula/public/por-token/";
      return decoded.startsWith(seguimiento) ? seguimiento + "{token}" : ruta;
    } catch (IllegalArgumentException ex) {
      return "{ruta-no-valida}";
    }
  }

  private static String limitar(String valor, int maximo) {
    if (!StringUtils.hasText(valor)) return null;
    return valor.length() <= maximo ? valor : valor.substring(0, maximo);
  }

}
