package br.com.fiap.challange.oficina.domain.validator;

import java.util.regex.Pattern;

public final class PlacaValidator {

    private static final Pattern PADRAO_ANTIGO = Pattern.compile("^[A-Z]{3}[0-9]{4}$");
    private static final Pattern PADRAO_MERCOSUL = Pattern.compile("^[A-Z]{3}[0-9][A-Z][0-9]{2}$");

    private PlacaValidator() {
    }

    public static boolean isValida(String placa) {
        if (placa == null) return false;
        String normalizada = normalizar(placa);
        return PADRAO_ANTIGO.matcher(normalizada).matches()
                || PADRAO_MERCOSUL.matcher(normalizada).matches();
    }

    public static String normalizar(String placa) {
        if (placa == null) return null;
        return placa.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }
}
