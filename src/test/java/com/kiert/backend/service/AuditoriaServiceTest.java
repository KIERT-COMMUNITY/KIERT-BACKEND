package com.kiert.backend.service;

import com.kiert.backend.entity.AuditoriaUsuario;
import com.kiert.backend.entity.EventoAuditoria;
import com.kiert.backend.repository.AuditoriaUsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditoriaService - Pruebas unitarias")
class AuditoriaServiceTest {

    @Mock
    private AuditoriaUsuarioRepository repository;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuditoriaService auditoriaService;

    private static final Long USUARIO_ID = 1L;
    private static final String EMAIL = "adrian@kiert.com";
    private static final String DESCRIPCION = "Inicio de sesión correcto";
    private static final String IP = "192.168.1.100";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0)";

    // ============================================================
    // TEST 1: registrar() SIN datos extra
    // ============================================================
    @Test
    @DisplayName("Debe registrar auditoría correctamente sin datos extra")
    void registrar_sinDatosExtra_guardaAuditoriaCorrectamente() {
        // ===== Arrange =====
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(IP);
        when(request.getHeader("User-Agent")).thenReturn(USER_AGENT);
        when(repository.save(any(AuditoriaUsuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // ===== Act =====
        auditoriaService.registrar(
                USUARIO_ID,
                EMAIL,
                EventoAuditoria.LOGIN,
                DESCRIPCION,
                true
        );

        // ===== Assert =====
        ArgumentCaptor<AuditoriaUsuario> captor =
                ArgumentCaptor.forClass(AuditoriaUsuario.class);

        verify(repository, times(1)).save(captor.capture());

        AuditoriaUsuario guardada = captor.getValue();

        assertEquals(USUARIO_ID, guardada.getUsuarioId());
        assertEquals(EMAIL, guardada.getEmail());
        assertEquals(EventoAuditoria.LOGIN.name(), guardada.getEvento());
        assertEquals(DESCRIPCION, guardada.getDescripcion());
        assertTrue(guardada.getExito());
        assertEquals(IP, guardada.getIp());
        assertEquals(USER_AGENT, guardada.getUserAgent());
        assertNull(guardada.getDatosExtra());
    }

    // ============================================================
    // TEST 2: registrar() CON datos extra
    // ============================================================
    @Test
    @DisplayName("Debe registrar auditoría correctamente con datos extra")
    void registrar_conDatosExtra_guardaAuditoriaCorrectamente() {
        // ===== Arrange =====
        String datosExtra = "{\"motivo\":\"test\",\"origen\":\"junit\"}";

        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(IP);
        when(request.getHeader("User-Agent")).thenReturn(USER_AGENT);
        when(repository.save(any(AuditoriaUsuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // ===== Act =====
        auditoriaService.registrar(
                USUARIO_ID,
                EMAIL,
                EventoAuditoria.LOGIN,
                DESCRIPCION,
                true,
                datosExtra
        );

        // ===== Assert =====
        ArgumentCaptor<AuditoriaUsuario> captor =
                ArgumentCaptor.forClass(AuditoriaUsuario.class);

        verify(repository, times(1)).save(captor.capture());

        AuditoriaUsuario guardada = captor.getValue();

        assertEquals(datosExtra, guardada.getDatosExtra());
        assertEquals(USUARIO_ID, guardada.getUsuarioId());
        assertTrue(guardada.getExito());
    }

    // ============================================================
    // TEST 3: registrar() con X-Forwarded-For (proxy)
    // ============================================================
    @Test
    @DisplayName("Debe usar X-Forwarded-For cuando está presente")
    void registrar_conXForwardedFor_usaPrimeraIp() {
        // ===== Arrange =====
        String xForwardedFor = "203.0.113.5, 198.51.100.10, 192.168.1.1";

        when(request.getHeader("X-Forwarded-For")).thenReturn(xForwardedFor);
        when(request.getHeader("User-Agent")).thenReturn(USER_AGENT);
        when(repository.save(any(AuditoriaUsuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // ===== Act =====
        auditoriaService.registrar(
                USUARIO_ID,
                EMAIL,
                EventoAuditoria.LOGIN,
                DESCRIPCION,
                true
        );

        // ===== Assert =====
        ArgumentCaptor<AuditoriaUsuario> captor =
                ArgumentCaptor.forClass(AuditoriaUsuario.class);

        verify(repository).save(captor.capture());

        // Debe tomar solo la PRIMERA IP del header
        assertEquals("203.0.113.5", captor.getValue().getIp());
    }

    // ============================================================
    // TEST 4: registrar() cuando falla (exito = false)
    // ============================================================
    @Test
    @DisplayName("Debe registrar auditoría con exito=false sin problemas")
    void registrar_conExitoFalse_guardaCorrectamente() {
        // ===== Arrange =====
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(IP);
        when(request.getHeader("User-Agent")).thenReturn(USER_AGENT);
        when(repository.save(any(AuditoriaUsuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // ===== Act =====
        auditoriaService.registrar(
                USUARIO_ID,
                EMAIL,
                EventoAuditoria.LOGIN_FALLIDO,
                "Credenciales incorrectas",
                false
        );

        // ===== Assert =====
        ArgumentCaptor<AuditoriaUsuario> captor =
                ArgumentCaptor.forClass(AuditoriaUsuario.class);

        verify(repository).save(captor.capture());

        AuditoriaUsuario guardada = captor.getValue();

        assertFalse(guardada.getExito());
        assertEquals(EventoAuditoria.LOGIN_FALLIDO.name(), guardada.getEvento());
        assertEquals("Credenciales incorrectas", guardada.getDescripcion());
    }

    // ============================================================
    // TEST 5: registrar() con usuarioId null (evento anónimo)
    // ============================================================
    @Test
    @DisplayName("Debe permitir usuarioId null (eventos anónimos)")
    void registrar_usuarioIdNull_guardaCorrectamente() {
        // ===== Arrange =====
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(IP);
        when(request.getHeader("User-Agent")).thenReturn(USER_AGENT);
        when(repository.save(any(AuditoriaUsuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // ===== Act =====
        auditoriaService.registrar(
                null,
                "intruso@hacker.com",
                EventoAuditoria.LOGIN_FALLIDO,
                "Intento con email no registrado",
                false
        );

        // ===== Assert =====
        ArgumentCaptor<AuditoriaUsuario> captor =
                ArgumentCaptor.forClass(AuditoriaUsuario.class);

        verify(repository).save(captor.capture());

        assertNull(captor.getValue().getUsuarioId());
        assertEquals("intruso@hacker.com", captor.getValue().getEmail());
    }

    // ============================================================
    // TEST 6: registrar() cuando el repositorio LANZA excepción
    // (el servicio debe capturarla y NO propagarla)
    // ============================================================
    @Test
    @DisplayName("No debe propagar excepción si el repository falla")
    void registrar_repositoryFalla_noPropagaExcepcion() {
        // ===== Arrange =====
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(IP);
        when(request.getHeader("User-Agent")).thenReturn(USER_AGENT);
        when(repository.save(any(AuditoriaUsuario.class)))
                .thenThrow(new RuntimeException("Error de conexión a BD"));

        // ===== Act + Assert =====
        // No debe lanzar excepción gracias al try/catch interno
        assertDoesNotThrow(() ->
                auditoriaService.registrar(
                        USUARIO_ID,
                        EMAIL,
                        EventoAuditoria.LOGIN,
                        DESCRIPCION,
                        true
                )
        );

        verify(repository, times(1)).save(any(AuditoriaUsuario.class));
    }

    // ============================================================
    // TEST 7: registrar() cuando el request falla
    // (obtenerIp y obtenerUserAgent devuelven null)
    // ============================================================
    @Test
    @DisplayName("Debe guardar con IP y UserAgent null si request falla")
    void registrar_requestFalla_guardaConIpNull() {
        // ===== Arrange =====
        // X-Forwarded-For lanza excepción → obtenerIp() devuelve null
        when(request.getHeader("X-Forwarded-For"))
                .thenThrow(new RuntimeException("Request destruido"));

        // User-Agent lanza excepción → obtenerUserAgent() devuelve null
        when(request.getHeader("User-Agent"))
                .thenThrow(new RuntimeException("Request destruido"));

        // ⚠️ IMPORTANTE: NO mockear request.getRemoteAddr() porque NUNCA se llama
        // (obtenerIp captura la excepción del getHeader antes de llegar a getRemoteAddr)

        when(repository.save(any(AuditoriaUsuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // ===== Act =====
        auditoriaService.registrar(
                USUARIO_ID,
                EMAIL,
                EventoAuditoria.LOGIN,
                DESCRIPCION,
                true
        );

        // ===== Assert =====
        ArgumentCaptor<AuditoriaUsuario> captor =
                ArgumentCaptor.forClass(AuditoriaUsuario.class);

        verify(repository).save(captor.capture());

        assertNull(captor.getValue().getIp());
        assertNull(captor.getValue().getUserAgent());
    }

    // ============================================================
    // TEST 8: verificar que se llama al repository exactamente 1 vez
    // ============================================================
    @Test
    @DisplayName("Debe llamar al repository solo UNA vez")
    void registrar_llamaRepositoryUnaVez() {
        // ===== Arrange =====
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(IP);
        when(request.getHeader("User-Agent")).thenReturn(USER_AGENT);
        when(repository.save(any(AuditoriaUsuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // ===== Act =====
        auditoriaService.registrar(
                USUARIO_ID,
                EMAIL,
                EventoAuditoria.LOGIN,
                DESCRIPCION,
                true
        );

        // ===== Assert =====
        verify(repository, times(1)).save(any(AuditoriaUsuario.class));
        verifyNoMoreInteractions(repository);
    }
}