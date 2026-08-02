package com.reserly.platform.availability.service;

/** Impide eliminar franjas que ya forman parte del historial de alguna reserva. */
public class TimeSlotDeleteConflictException extends RuntimeException {}
