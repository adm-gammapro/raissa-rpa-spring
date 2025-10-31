package com.raissa.rpa.util;

public abstract class DateUtils {
    public static String parsearFechaBarraToSlash(String fecha) {
        if (fecha == null || fecha.trim().isEmpty()) {
            return fecha;
        }

        return fecha.replace("-", "/");
    }
}
