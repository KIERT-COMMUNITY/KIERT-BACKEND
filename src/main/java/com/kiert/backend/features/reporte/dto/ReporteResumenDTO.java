package com.kiert.backend.features.reporte.dto;

public record ReporteResumenDTO(
        Long totalPendientes,
        Long totalRevisando,
        Long totalResueltos,
        Long totalRechazados
) {}