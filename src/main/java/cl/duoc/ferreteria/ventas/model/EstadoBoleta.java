package cl.duoc.ferreteria.ventas.model;

/**
 * Estados de la boleta. EL ORDEN IMPORTA: solo se avanza al siguiente de la
 * secuencia, o se anula desde cualquiera no final (C9.6).
 */
public enum EstadoBoleta {
    RECIBIDA, EN_PREPARACION, LISTA_PARA_RETIRO, ENTREGADA, ANULADA;

    public boolean esFinal() {
        return this == ENTREGADA || this == ANULADA;
    }

    /** Solo al estado siguiente de la secuencia, o a ANULADA desde uno no final. */
    public boolean puedeCambiarA(EstadoBoleta nuevo) {
        if (esFinal() || nuevo == null) {
            return false;
        }
        if (nuevo == ANULADA) {
            return true;
        }
        return nuevo.ordinal() == this.ordinal() + 1;
    }
}
