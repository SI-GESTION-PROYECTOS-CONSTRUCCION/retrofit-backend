package com.retrofit.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project_item_resources")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectItemResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1. LA PARTIDA (El "Muro de Ladrillo")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_item_id", nullable = false)
    private ProjectItem projectItem;

    // 2. EL RECURSO (El "Peón", el "Cemento" o la "Mezcladora")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    // 3. LA CUADRILLA (Ej: 0.1 Capataz, 1.0 Peón). Para los materiales esto será null o 0.
    @Column(name = "squad")
    private Double squad = 0.0;

    // 4. LA CANTIDAD EXACTA que entra en 1 unidad de la partida.
    // - En Materiales: El usuario la digita (Ej: 1.5 bolsas).
    // - En MO/Equipos: El backend la calculará con la fórmula del rendimiento.
    @Column(nullable = false)
    private Double quantity = 0.0;

    // 5. EL PRECIO PARCIAL (Cantidad * Precio Base del Recurso)
    // Esto lo calculará el backend automáticamente.
    @Column(name = "partial_price", nullable = false)
    private Double partialPrice = 0.0;
}