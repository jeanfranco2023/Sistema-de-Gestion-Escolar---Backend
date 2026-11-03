package com.colegio.shuji.usuario.infrastructure.controller.rest;

import com.colegio.shuji.config.security.UserPrincipal;
import com.colegio.shuji.shared.application.dto.ApiResponse;
import com.colegio.shuji.usuario.application.dto.in.CambiarEstadoRequestDto;
import com.colegio.shuji.usuario.application.dto.in.CambiarPasswordRequestDto;
import com.colegio.shuji.usuario.application.dto.in.CorregirDniRequestDto;
import com.colegio.shuji.usuario.application.dto.in.RegisterUserRequestDto;
import com.colegio.shuji.usuario.application.dto.in.UpdateUsuarioRequestDto;
import com.colegio.shuji.usuario.application.dto.out.ExpedienteEmpleadoDocumentoResponseDto;
import com.colegio.shuji.usuario.application.dto.out.UserResponseDto;
import com.colegio.shuji.usuario.application.port.in.ExpedienteEmpleadoUseCase;
import com.colegio.shuji.usuario.application.port.in.GestionUsuarioUseCase;
import com.colegio.shuji.usuario.domain.enums.TipoDocumentoEmpleado;
import com.colegio.shuji.usuario.domain.exception.ReglaNegocioException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Controlador REST para la gestión integral de cuentas de usuarios institucionales y expedientes. */
@Slf4j
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Tag(
    name = "Usuarios",
    description =
        "Endpoints de administración, perfiles, control de acceso y expedientes de personal")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

  private static final long MAX_EXPEDIENTE_FILE_SIZE_BYTES = 10L * 1024 * 1024;

  private final GestionUsuarioUseCase gestionUsuarioUseCase;
  private final ExpedienteEmpleadoUseCase expedienteEmpleadoUseCase;

  @Operation(
      summary = "Crear nuevo usuario institucional",
      description = "Permite a Dirección dar de alta usuarios institucionales asignando roles específicos")
  @PostMapping
  @PreAuthorize("hasRole('DIRECCION')")
  public ResponseEntity<ApiResponse<UserResponseDto>> crearUsuario(
      @Valid @RequestBody RegisterUserRequestDto requestDto) {
    UserResponseDto usuario = gestionUsuarioUseCase.registrarPorDireccion(requestDto);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.created(usuario, "Usuario creado exitosamente"));
  }

  @Operation(
      summary = "Obtener perfil propio",
      description = "Retorna la información del usuario actualmente autenticado")
  @GetMapping("/me")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE','AUXILIAR','TUTOR','APODERADO')")
  public ResponseEntity<ApiResponse<UserResponseDto>> obtenerMiPerfil(
      @AuthenticationPrincipal UserPrincipal principal) {

    UserResponseDto perfil = gestionUsuarioUseCase.obtenerPerfil(principal.getId());
    return ResponseEntity.ok(ApiResponse.ok(perfil, "Perfil obtenido exitosamente"));
  }

  @Operation(
      summary = "Actualizar perfil propio",
      description = "Actualiza los datos básicos del usuario autenticado")
  @PutMapping("/me")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE','AUXILIAR','TUTOR','APODERADO')")
  public ResponseEntity<ApiResponse<UserResponseDto>> actualizarMiPerfil(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody UpdateUsuarioRequestDto requestDto) {

    UserResponseDto actualizado =
        gestionUsuarioUseCase.actualizarPerfil(principal.getId(), requestDto);
    return ResponseEntity.ok(ApiResponse.ok(actualizado, "Perfil actualizado exitosamente"));
  }

  @Operation(
      summary = "Listar o filtrar usuarios por rol",
      description =
          "Lista todos los usuarios o filtra según el código de rol institucional (ej: DOCENTE,"
              + " SECRETARIA)")
  @GetMapping
  @PreAuthorize("hasRole('DIRECCION')")
  public ResponseEntity<ApiResponse<List<UserResponseDto>>> listarUsuarios(
      @Parameter(description = "Código del rol (opcional)", example = "DOCENTE")
          @RequestParam(name = "rol", required = false)
          String rol) {

    List<UserResponseDto> usuarios = gestionUsuarioUseCase.listarPorRol(rol);
    return ResponseEntity.ok(ApiResponse.ok(usuarios, "Usuarios obtenidos exitosamente"));
  }

  @Operation(
      summary = "Cambiar estado activo/inactivo de un usuario (borrado lógico)",
      description = "Permite a Dirección activar o suspender una cuenta institucional")
  @PatchMapping("/{id}/estado")
  @PreAuthorize("hasRole('DIRECCION')")
  public ResponseEntity<ApiResponse<UserResponseDto>> cambiarEstado(
      @Parameter(description = "ID del usuario a modificar", required = true) @PathVariable("id")
          Long id,
      @Valid @RequestBody(required = false) CambiarEstadoRequestDto body,
      @RequestParam(name = "activo", required = false) Boolean activoParam) {

    Boolean nuevoEstado =
        (body != null && body.getActivo() != null) ? body.getActivo() : activoParam;
    if (nuevoEstado == null) {
      throw new IllegalArgumentException(
          "Debe indicar el estado 'activo' en el cuerpo o como parámetro de consulta.");
    }

    UserResponseDto usuario = gestionUsuarioUseCase.cambiarEstado(id, nuevoEstado);
    return ResponseEntity.ok(
        ApiResponse.ok(usuario, "Estado del usuario actualizado exitosamente"));
  }

  @Operation(
      summary = "Corregir DNI de un usuario institucional",
      description = "Acción transaccional restringida a Dirección para corregir el DNI con registro de auditoría")
  @PatchMapping("/{id}/dni")
  @PreAuthorize("hasRole('DIRECCION')")
  public ResponseEntity<ApiResponse<UserResponseDto>> corregirDni(
      @Parameter(description = "ID del usuario a modificar", required = true) @PathVariable("id")
          Long id,
      @Valid @RequestBody CorregirDniRequestDto requestDto) {

    UserResponseDto usuario = gestionUsuarioUseCase.corregirDni(id, requestDto);
    return ResponseEntity.ok(ApiResponse.ok(usuario, "DNI corregido exitosamente"));
  }

  @Operation(
      summary = "Cambiar contraseña del usuario autenticado",
      description = "Permite al usuario actual modificar su contraseña validando la anterior")
  @PutMapping("/password")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE','AUXILIAR','TUTOR','APODERADO')")
  public ResponseEntity<ApiResponse<Void>> cambiarPassword(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody CambiarPasswordRequestDto requestDto) {

    gestionUsuarioUseCase.cambiarPassword(principal.getId(), requestDto);
    return ResponseEntity.ok(ApiResponse.ok(null, "Contraseña actualizada exitosamente"));
  }

  @Operation(
      summary = "Subir documento del expediente de empleado",
      description = "Carga un PDF privado. Solo devuelve VALIDADO_IA cuando el motor configurado confirma tipo, legibilidad e identidad; sin IA requiere revisión manual.")
  @PostMapping(value = "/{id}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  public ResponseEntity<ApiResponse<ExpedienteEmpleadoDocumentoResponseDto>> subirDocumento(
      @Parameter(description = "ID del usuario empleado", required = true) @PathVariable("id")
          Long id,
      @Parameter(description = "Tipo de documento laboral", required = true) @RequestParam("tipoDocumento")
          TipoDocumentoEmpleado tipoDocumento,
      @Parameter(description = "Archivo PDF del documento", required = true) @RequestParam("archivo")
          MultipartFile archivo) {

    if (archivo.getSize() > MAX_EXPEDIENTE_FILE_SIZE_BYTES) {
      throw new ReglaNegocioException("El tamaño del archivo no puede exceder los 10 MB", "TAMANO_EXCEDIDO");
    }
    byte[] contenido;
    try {
      contenido = archivo.getBytes();
    } catch (IOException ex) {
      throw new ReglaNegocioException("Error al leer los bytes del archivo cargado", "ERROR_LECTURA_ARCHIVO");
    }
    ExpedienteEmpleadoDocumentoResponseDto response = expedienteEmpleadoUseCase.subirYValidarDocumento(
        id, tipoDocumento, archivo.getOriginalFilename(), archivo.getContentType(), contenido);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.created(response, "Documento recibido; el resultado de validación está disponible en el expediente"));
  }

  @Operation(
      summary = "Listar documentos del expediente de un empleado",
      description = "Obtiene los documentos cargados para el empleado junto con sus dictámenes de validación IA")
  @GetMapping("/{id}/documentos")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  public ResponseEntity<ApiResponse<List<ExpedienteEmpleadoDocumentoResponseDto>>> listarDocumentos(
      @Parameter(description = "ID del usuario empleado", required = true) @PathVariable("id")
          Long id) {

    List<ExpedienteEmpleadoDocumentoResponseDto> documentos =
        expedienteEmpleadoUseCase.listarDocumentos(id);
    return ResponseEntity.ok(ApiResponse.ok(documentos, "Documentos del expediente obtenidos exitosamente"));
  }

  @Operation(
      summary = "Obtener un enlace temporal de descarga del expediente de empleado",
      description = "Solo Dirección puede generar el enlace firmado de corta duración para un documento del empleado indicado.")
  @GetMapping("/{id}/documentos/{documentoId}/descarga")
  @PreAuthorize("hasRole('DIRECCION')")
  public ResponseEntity<ApiResponse<String>> obtenerEnlaceDescargaDocumento(
      @Parameter(description = "ID del usuario empleado", required = true) @PathVariable("id") Long id,
      @Parameter(description = "ID del documento", required = true) @PathVariable("documentoId") Long documentoId) {
    String url = expedienteEmpleadoUseCase.urlDescargaDocumento(id, documentoId);
    return ResponseEntity.ok(ApiResponse.ok(url, "Enlace temporal de descarga generado"));
  }

  @Operation(
      summary = "Revocar rol de un usuario institucional",
      description = "Revoca un rol institucional garantizando que quede al menos un administrador y bloqueando autorevocación")
  @DeleteMapping("/{id}/roles/{rolId}")
  @PreAuthorize("hasRole('DIRECCION')")
  public ResponseEntity<ApiResponse<UserResponseDto>> revocarRol(
      @Parameter(description = "ID del usuario", required = true) @PathVariable("id")
          Long id,
      @Parameter(description = "ID del rol a revocar", required = true) @PathVariable("rolId")
          Short rolId) {

    UserResponseDto actualizado = gestionUsuarioUseCase.revocarRol(id, rolId);
    return ResponseEntity.ok(ApiResponse.ok(actualizado, "Rol revocado exitosamente"));
  }
}
