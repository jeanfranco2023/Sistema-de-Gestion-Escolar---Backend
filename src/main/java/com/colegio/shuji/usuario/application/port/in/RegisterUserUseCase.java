package com.colegio.shuji.usuario.application.port.in;

import com.colegio.shuji.usuario.application.dto.in.RegisterUserRequestDto;
import com.colegio.shuji.usuario.application.dto.out.UserResponseDto;

/** Caso de uso: Registro y alta de un nuevo usuario en la plataforma. */
public interface RegisterUserUseCase {

  UserResponseDto register(RegisterUserRequestDto registerRequest);
}
