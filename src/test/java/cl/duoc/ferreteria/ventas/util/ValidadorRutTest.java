package cl.duoc.ferreteria.ventas.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorRutTest {

    @Test
    void rutValidoConPuntosYGuion() {
        assertTrue(ValidadorRut.esValido("12.345.678-5"));
    }

    @Test
    void rutValidoSinPuntosNiGuion() {
        assertTrue(ValidadorRut.esValido("123456785"));
    }

    @Test
    void digitoVerificadorConKMinuscula() {
        assertTrue(ValidadorRut.esValido("15.236.783-k"));
        assertEquals("15236783-K", ValidadorRut.normalizar("15.236.783-k"));
    }

    @Test
    void normalizaASinPuntosYConGuion() {
        assertEquals("12345678-5", ValidadorRut.normalizar("12.345.678-5"));
    }

    @Test
    void rutConDigitoVerificadorIncorrecto() {
        assertFalse(ValidadorRut.esValido("12345678-9"));
        assertNull(ValidadorRut.normalizar("12345678-9"));
    }

    @Test
    void rutNuloOVacioOBasura() {
        assertFalse(ValidadorRut.esValido(null));
        assertFalse(ValidadorRut.esValido("  "));
        assertFalse(ValidadorRut.esValido("abc-def"));
    }
}
