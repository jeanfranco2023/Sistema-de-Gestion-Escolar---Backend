package com.colegio.shuji.usuario.application.port.in;

import com.colegio.shuji.usuario.application.dto.in.ActualizarUsuarioAdminRequestDto;
import com.colegio.shuji.usuario.application.dto.in.CambiarPasswordRequestDto;
import com.colegio.shuji.usuario.application.dto.in.CorregirDniRequestDto;
import com.colegio.shuji.usuario.application.dto.in.RegisterUserRequestDto;
import com.colegio.shuji.usuario.application.dto.in.UpdateUsuarioRequestDto;
import com.colegio.shuji.usuario.application.dto.out.UserResponseDto;
import java.util.List;

/** Caso de uso: Gestión integral del ciclo de vida y administración de usuarios institucionales. */
public interface GestionUsuarioUseCase {

  UserResponseDto obtenerPerfil(Long usuarioId);

  UserResponseDto actualizarPerfil(Long usuarioId, UpdateUsuarioRequestDto updateUsuarioDto);

  List<UserResponseDto> listarPorRol(String codigoRol);

  List<UserResponseDto> listarTodos();

  UserResponseDto cambiarEstado(Long usuarioId, Boolean activo);

  void cambiarPassword(Long usuarioId, CambiarPasswordRequestDto cambiarPasswordDto);

  UserResponseDto registrarPorDireccion(RegisterUserRequestDto request);

  UserResponseDto corregirDni(Long usuarioId, CorregirDniRequestDto request);

  UserResponseDto revocarRol(Long usuarioId, Short rolId);

  default UserResponseDto actualizarUsuario(
      Long usuarioId, ActualizarUsuarioAdminRequestDto request) {
    throw new UnsupportedOperationException("Operación no soportada");
  }
}
