package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mensajes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emisor_id", nullable = false)
    private Usuario emisor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receptor_id", nullable = false)
    private Usuario receptor;

    @Column(columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "fecha_envio", nullable = false, updatable = false)
    @Builder.Default
    private Instant fechaEnvio = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private boolean leido = false;

    @Column(name = "fecha_leido")
    private Instant fechaLeido;

    @Column(name = "tipo_mensaje", nullable = false, length = 20)
    @Builder.Default
    private String tipoMensaje = "TEXTO";

    @Column(name = "url_archivo", length = 1000)
    private String urlArchivo;

    @Column(name = "nombre_archivo", length = 255)
    private String nombreArchivo;

    @Column(name = "fecha_eliminacion")
    private Instant fechaEliminacion;

    @Column(nullable = false)
    @Builder.Default
    private boolean eliminado = false;

    @OneToMany(
            mappedBy = "mensaje",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("id ASC")
    @Builder.Default
    private List<MensajeArchivo> archivos = new ArrayList<>();

    @PrePersist
    private void prePersist() {
        if (fechaEnvio == null) {
            fechaEnvio = Instant.now();
        }

        if (tipoMensaje == null || tipoMensaje.isBlank()) {
            tipoMensaje = "TEXTO";
        }
    }

    public void agregarArchivo(MensajeArchivo archivo) {
        if (archivo == null) {
            return;
        }

        archivos.add(archivo);
        archivo.setMensaje(this);
    }

    public void quitarArchivo(MensajeArchivo archivo) {
        if (archivo == null) {
            return;
        }

        archivos.remove(archivo);
        archivo.setMensaje(null);
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
