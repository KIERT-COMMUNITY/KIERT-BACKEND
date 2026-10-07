package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mensajes_grupo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeGrupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grupo_id", nullable = false)
    private GrupoChat grupo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emisor_id", nullable = false)
    private Usuario emisor;

    @Column(columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "tipo_mensaje", nullable = false, length = 20)
    @Builder.Default
    private String tipoMensaje = "TEXTO";

    @Column(name = "url_archivo", length = 1000)
    private String urlArchivo;

    @Column(name = "nombre_archivo", length = 255)
    private String nombreArchivo;

    @Column(name = "fecha_envio", nullable = false, updatable = false)
    @Builder.Default
    private Instant fechaEnvio = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private boolean eliminado = false;

    @OneToMany(
            mappedBy = "mensajeGrupo",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("id ASC")
    @Builder.Default
    private List<MensajeArchivoGrupo> archivos = new ArrayList<>();

    @PrePersist
    private void prePersist() {
        if (fechaEnvio == null) {
            fechaEnvio = Instant.now();
        }

        if (tipoMensaje == null || tipoMensaje.isBlank()) {
            tipoMensaje = "TEXTO";
        }
    }

    public void agregarArchivo(MensajeArchivoGrupo archivo) {
        if (archivo == null) {
            return;
        }

        archivos.add(archivo);
        archivo.setMensajeGrupo(this);
    }

    public void quitarArchivo(MensajeArchivoGrupo archivo) {
        if (archivo == null) {
            return;
        }

        archivos.remove(archivo);
        archivo.setMensajeGrupo(null);
    }

    public boolean tieneContenido() {
        return contenido != null && !contenido.isBlank();
    }

    public boolean tieneArchivos() {
        return archivos != null && !archivos.isEmpty();
    }

    public boolean tieneContenidoValido() {
        return tieneContenido() || tieneArchivos();
    }
}
