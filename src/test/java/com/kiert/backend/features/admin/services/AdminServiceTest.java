package com.kiert.backend.features.admin.services;

import com.kiert.backend.entity.Usuario;
import com.kiert.backend.features.admin.dtos.AdminDTO;
import com.kiert.backend.features.admin.dtos.request.AdminLoginRequestDTO;
import com.kiert.backend.features.admin.dtos.response.AdminLoginResponseDTO;
import com.kiert.backend.features.usuario.repository.UsuarioRepository;
import com.kiert.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService - Login de administrador")
class AdminServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AdminService adminService;

    private Usuario adminValido;

    @BeforeEach
    void setUp() {
        adminValido = Usuario.builder()
                .id(1L)
                .email("admin@test.com")
                .nombreUsuario("adminTest")
                .passwordHash("$2a$10$hashedpassword")
                .rol("CEO")
                .activo(true)
                .eliminado(false)
                .bloqueadoHasta(null)
                .build();
    }

    // ============================================================
    // ✅ CASOS DE ÉXITO
    // ============================================================

    @Test
    @DisplayName("✅ Login exitoso con rol CEO devuelve token y admin")
    void login_conAdminValido_devuelveToken() {
        AdminLoginRequestDTO request = new AdminLoginRequestDTO(
                "admin@test.com", "password123"
        );

        when(usuarioRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(adminValido));
        when(passwordEncoder.matches("password123", "$2a$10$hashedpassword"))
                .thenReturn(true);
        when(jwtService.generarTokenConRol(1L, "admin@test.com", "CEO"))
                .thenReturn("token-falso-jwt");

        AdminLoginResponseDTO response = adminService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("token-falso-jwt");
        assertThat(response.admin().email()).isEqualTo("admin@test.com");
        assertThat(response.admin().rol()).isEqualTo("CEO");

        verify(usuarioRepository).findByEmail("admin@test.com");
        verify(passwordEncoder).matches("password123", "$2a$10$hashedpassword");
        verify(jwtService).generarTokenConRol(1L, "admin@test.com", "CEO");
    }

    @Test
    @DisplayName("✅ Login exitoso con rol ADMIN")
    void login_conRolAdmin_funciona() {
        adminValido.setRol("ADMIN");

        AdminLoginRequestDTO request = new AdminLoginRequestDTO(
                "admin@test.com", "password123"
        );

        when(usuarioRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(adminValido));
        when(passwordEncoder.matches("password123", "$2a$10$hashedpassword"))
                .thenReturn(true);
        when(jwtService.generarTokenConRol(1L, "admin@test.com", "ADMIN"))
                .thenReturn("token-admin");

        AdminLoginResponseDTO response = adminService.login(request);

        assertThat(response.admin().rol()).isEqualTo("ADMIN");
        assertThat(response.token()).isEqualTo("token-admin");
    }

    @Test
    @DisplayName("✅ Login exitoso con rol MODERADOR")
    void login_conRolModerador_funciona() {
        adminValido.setRol("MODERADOR");

        AdminLoginRequestDTO request = new AdminLoginRequestDTO(
                "admin@test.com", "password123"
        );

        when(usuarioRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(adminValido));
        when(passwordEncoder.matches("password123", "$2a$10$hashedpassword"))
                .thenReturn(true);
        when(jwtService.generarTokenConRol(1L, "admin@test.com", "MODERADOR"))
                .thenReturn("token-moderador");

        AdminLoginResponseDTO response = adminService.login(request);

        assertThat(response.admin().rol()).isEqualTo("MODERADOR");
        assertThat(response.token()).isEqualTo("token-moderador");
    }

    // ============================================================
    // ❌ CASOS DE FALLO
    // ============================================================

    @Test
    @DisplayName("❌ Email inexistente lanza BadCredentialsException")
    void login_conEmailInexistente_lanzaExcepcion() {
        AdminLoginRequestDTO request = new AdminLoginRequestDTO(
                "noexiste@test.com", "password123"
        );

        when(usuarioRepository.findByEmail("noexiste@test.com"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(false);

        assertThatThrownBy(() -> adminService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales incorrectas");

        verify(jwtService, never()).generarTokenConRol(any(), any(), any());
    }

    @Test
    @DisplayName("❌ Contraseña incorrecta lanza BadCredentialsException")
    void login_conPasswordIncorrecta_lanzaExcepcion() {
        AdminLoginRequestDTO request = new AdminLoginRequestDTO(
                "admin@test.com", "passwordMALA"
        );

        when(usuarioRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(adminValido));
        when(passwordEncoder.matches("passwordMALA", "$2a$10$hashedpassword"))
                .thenReturn(false);

        assertThatThrownBy(() -> adminService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales incorrectas");

        verify(jwtService, never()).generarTokenConRol(any(), any(), any());
    }

    @Test
    @DisplayName("❌ Cuenta inactiva lanza BadCredentialsException")
    void login_conCuentaInactiva_lanzaExcepcion() {
        adminValido.setActivo(false);

        AdminLoginRequestDTO request = new AdminLoginRequestDTO(
                "admin@test.com", "password123"
        );

        when(usuarioRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(adminValido));
        when(passwordEncoder.matches("password123", "$2a$10$hashedpassword"))
                .thenReturn(true);

        assertThatThrownBy(() -> adminService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales incorrectas");
    }

    @Test
    @DisplayName("❌ Cuenta eliminada lanza BadCredentialsException")
    void login_conCuentaEliminada_lanzaExcepcion() {
        adminValido.setEliminado(true);

        AdminLoginRequestDTO request = new AdminLoginRequestDTO(
                "admin@test.com", "password123"
        );

        when(usuarioRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(adminValido));
        when(passwordEncoder.matches("password123", "$2a$10$hashedpassword"))
                .thenReturn(true);

        assertThatThrownBy(() -> adminService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales incorrectas");
    }

    @Test
    @DisplayName("❌ Cuenta bloqueada temporalmente lanza BadCredentialsException")
    void login_conCuentaBloqueada_lanzaExcepcion() {
        adminValido.setBloqueadoHasta(Instant.now().plusSeconds(3600));

        AdminLoginRequestDTO request = new AdminLoginRequestDTO(
                "admin@test.com", "password123"
        );

        when(usuarioRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(adminValido));
        when(passwordEncoder.matches("password123", "$2a$10$hashedpassword"))
                .thenReturn(true);

        assertThatThrownBy(() -> adminService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales incorrectas");
    }

    @Test
    @DisplayName("❌ Usuario con rol USER no puede entrar al panel admin")
    void login_conRolUser_lanzaExcepcion() {
        adminValido.setRol("USER");

        AdminLoginRequestDTO request = new AdminLoginRequestDTO(
                "admin@test.com", "password123"
        );

        when(usuarioRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(adminValido));
        when(passwordEncoder.matches("password123", "$2a$10$hashedpassword"))
                .thenReturn(true);

        assertThatThrownBy(() -> adminService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales incorrectas");

        verify(jwtService, never()).generarTokenConRol(any(), any(), any());
    }
}