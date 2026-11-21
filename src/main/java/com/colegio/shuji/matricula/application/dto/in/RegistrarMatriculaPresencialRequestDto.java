package com.colegio.shuji.matricula.application.dto.in;

import com.colegio.shuji.matricula.domain.enums.Genero;
import com.colegio.shuji.matricula.domain.enums.ModalidadIngreso;
import com.colegio.shuji.matricula.domain.enums.Parentesco;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class RegistrarMatriculaPresencialRequestDto {

  @NotNull @Positive
  private Short anioLectivoId;

  @NotNull @Positive
  private Short gradoId;

  @NotNull @Positive
  private Integer seccionId;

  // Datos del Estudiante
  @NotBlank
  @Pattern(regexp = "^[0-9]{8}$", message = "El DNI del estudiante debe tener exactamente 8 dígitos numéricos")
  private String numeroDocumentoEstudiante;

  @NotBlank
  @Size(max = 20)
  @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]+$", message = "Los nombres del estudiante solo deben contener letras y espacios")
  private String nombresEstudiante;

  @NotBlank
  @Size(max = 20)
  @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]+$", message = "El apellido paterno del estudiante solo debe contener letras y espacios")
  private String apellidoPaternoEstudiante;

  @NotBlank
  @Size(max = 20)
  @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]+$", message = "El apellido materno del estudiante solo debe contener letras y espacios")
  private String apellidoMaternoEstudiante;

  @NotNull
  private LocalDate fechaNacimientoEstudiante;

  @NotNull
  private Genero generoEstudiante;

  // Datos del Apoderado
  @NotBlank
  @Pattern(regexp = "^[0-9]{8}$", message = "El DNI del apoderado debe tener exactamente 8 dígitos numéricos")
  private String numeroDocumentoApoderado;

  @NotBlank
  @Size(max = 20)
  @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]+$", message = "Los nombres del apoderado solo deben contener letras y espacios")
  private String nombresApoderado;

  @NotBlank
  @Size(max = 20)
  @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]+$", message = "El apellido paterno del apoderado solo debe contener letras y espacios")
  private String apellidoPaternoApoderado;

  @NotBlank
  @Size(max = 20)
  @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]+$", message = "El apellido materno del apoderado solo debe contener letras y espacios")
  private String apellidoMaternoApoderado;

  @NotBlank
  @Pattern(regexp = "^9[0-9]{8}$", message = "El celular del apoderado debe tener 9 dígitos y empezar por 9")
  private String celularApoderado;

  @NotBlank
  @Email(message = "El correo del apoderado debe tener un formato válido")
  @Size(max = 100)
  private String emailApoderado;

  @NotBlank
  @Size(max = 150)
  @Pattern(regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ\\s#.,\\-_/°º]+$", message = "La dirección no permite caracteres especiales extraños")
  private String direccionApoderado;

  @NotNull
  private Parentesco parentesco;

  // Pago en Efectivo (Caja)
  @jakarta.validation.constraints.Digits(integer = 8, fraction = 2)
  @DecimalMin(value = "0.01", message = "El monto pagado en efectivo debe ser mayor a cero")
  private BigDecimal montoEfectivo;

  private String observaciones;

  // Evidencias Documentales
  private ModalidadIngreso modalidadIngreso = ModalidadIngreso.REGULAR;

  @Size(max = 150)
  private String colegioProcedencia;

  private MultipartFile partida;
  private MultipartFile dni;
  private MultipartFile recibo;
  private MultipartFile foto;
  private MultipartFile certificadoEstudios;
  private MultipartFile constanciaPago;
}
