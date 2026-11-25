package cl.duoc.ferreteria.ventas.util;

/**
 * Validación y normalización de RUT chileno (módulo 11), equivalente a la de ms-usuarios §5.1.
 * Acepta "12.345.678-5", "12345678-5", "123456785" y "k" en minúscula;
 * normaliza siempre a "12345678-5" (sin puntos, con guion).
 */
public final class ValidadorRut {

    private ValidadorRut() { }

    /** ¿El RUT cumple el módulo 11? null o vacío → false. */
    public static boolean esValido(String rut) {
        if (rut == null || rut.isBlank()) {
            return false;
        }
        String limpio = soloDigitosYDv(rut);
        if (limpio.length() < 7 || limpio.length() > 9) {   // cuerpo de 6 a 8 dígitos + dígito verificador
            return false;
        }
        String cuerpo = limpio.substring(0, limpio.length() - 1);
        char dv = limpio.charAt(limpio.length() - 1);
        if (!cuerpo.chars().allMatch(Character::isDigit)) {
            return false;
        }
        return dv == dvEsperado(cuerpo);
    }

    /** Devuelve el RUT normalizado ("12345678-5") o null si es inválido. */
    public static String normalizar(String rut) {
        if (!esValido(rut)) {
            return null;
        }
        String limpio = soloDigitosYDv(rut);
        return limpio.substring(0, limpio.length() - 1) + "-" + limpio.charAt(limpio.length() - 1);
    }

    private static String soloDigitosYDv(String rut) {
        return rut.replace(".", "").replace("-", "").replace(" ", "").trim().toUpperCase();
    }

    /** Módulo 11: suma ponderada con el ciclo 2,3,4,5,6,7 desde el último dígito. */
    private static char dvEsperado(String cuerpo) {
        int suma = 0;
        int factor = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += (cuerpo.charAt(i) - '0') * factor;
            factor = factor == 7 ? 2 : factor + 1;
        }
        int resto = 11 - (suma % 11);
        return switch (resto) {
            case 11 -> '0';
            case 10 -> 'K';
            default -> (char) ('0' + resto);
        };
    }
}
