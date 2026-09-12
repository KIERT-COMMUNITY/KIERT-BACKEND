package com.kiert.backend.dto;

public record ReporteResumenDTO(
        Long totalPendientes,
        Long totalRevisando,
        Long totalResueltos,
        Long totalRechazados
) {}