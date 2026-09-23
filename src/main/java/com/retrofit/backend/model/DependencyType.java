package com.retrofit.backend.model;

/** Relaciones lógicas entre la actividad predecesora y la sucesora. */
public enum DependencyType {
    FINISH_TO_START,
    START_TO_START,
    FINISH_TO_FINISH,
    START_TO_FINISH
}
