package cl.duoc.ferreteria.ventas.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reglas de transición de estados (C9.6): solo al siguiente, o a ANULADA desde uno no final. */
class EstadoBoletaTest {

    @Test
    void recibidaPuedePasarASiguienteEstado() {
        assertTrue(EstadoBoleta.RECIBIDA.puedeCambiarA(EstadoBoleta.EN_PREPARACION));
    }

    @Test
    void noSePuedeSaltarEstados() {
        assertFalse(EstadoBoleta.RECIBIDA.puedeCambiarA(EstadoBoleta.ENTREGADA));
    }

    @Test
    void unEstadoNoFinalPuedeAnularse() {
        assertTrue(EstadoBoleta.LISTA_PARA_RETIRO.puedeCambiarA(EstadoBoleta.ANULADA));
    }

    @Test
    void entregadaEsFinalYNoCambia() {
        assertFalse(EstadoBoleta.ENTREGADA.puedeCambiarA(EstadoBoleta.ANULADA));
    }

    @Test
    void anuladaEsFinalYNoCambia() {
        assertFalse(EstadoBoleta.ANULADA.puedeCambiarA(EstadoBoleta.RECIBIDA));
        assertFalse(EstadoBoleta.ANULADA.puedeCambiarA(EstadoBoleta.ANULADA));
    }

    @Test
    void elMismoEstadoNoEsUnCambioValido() {
        assertFalse(EstadoBoleta.RECIBIDA.puedeCambiarA(EstadoBoleta.RECIBIDA));
    }

    @Test
    void noSePuedeRetroceder() {
        assertFalse(EstadoBoleta.EN_PREPARACION.puedeCambiarA(EstadoBoleta.RECIBIDA));
    }
}
