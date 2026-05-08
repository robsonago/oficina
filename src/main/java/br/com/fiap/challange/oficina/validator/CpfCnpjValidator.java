package br.com.fiap.challange.oficina.validator;

import br.com.fiap.challange.oficina.model.enums.TipoDocumento;

public final class CpfCnpjValidator {

    private CpfCnpjValidator() {
    }

    public static boolean isValido(String documento) {
        if (documento == null) return false;
        String digits = documento.replaceAll("[^0-9]", "");
        return switch (digits.length()) {
            case 11 -> isCpfValido(digits);
            case 14 -> isCnpjValido(digits);
            default -> false;
        };
    }

    public static TipoDocumento detectarTipo(String documento) {
        if (documento == null) return null;
        String digits = documento.replaceAll("[^0-9]", "");
        return switch (digits.length()) {
            case 11 -> TipoDocumento.CPF;
            case 14 -> TipoDocumento.CNPJ;
            default -> null;
        };
    }

    public static String normalizar(String documento) {
        if (documento == null) return null;
        return documento.replaceAll("[^0-9]", "");
    }

    private static boolean isCpfValido(String cpf) {
        if (cpf.chars().distinct().count() == 1) return false;

        int soma = 0;
        for (int i = 0; i < 9; i++) soma += Character.getNumericValue(cpf.charAt(i)) * (10 - i);
        int r1 = (soma * 10) % 11;
        if (r1 == 10 || r1 == 11) r1 = 0;
        if (r1 != Character.getNumericValue(cpf.charAt(9))) return false;

        soma = 0;
        for (int i = 0; i < 10; i++) soma += Character.getNumericValue(cpf.charAt(i)) * (11 - i);
        int r2 = (soma * 10) % 11;
        if (r2 == 10 || r2 == 11) r2 = 0;
        return r2 == Character.getNumericValue(cpf.charAt(10));
    }

    private static boolean isCnpjValido(String cnpj) {
        if (cnpj.chars().distinct().count() == 1) return false;

        int[] weights1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int soma = 0;
        for (int i = 0; i < 12; i++) soma += Character.getNumericValue(cnpj.charAt(i)) * weights1[i];
        int r1 = soma % 11 < 2 ? 0 : 11 - (soma % 11);
        if (r1 != Character.getNumericValue(cnpj.charAt(12))) return false;

        int[] weights2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        soma = 0;
        for (int i = 0; i < 13; i++) soma += Character.getNumericValue(cnpj.charAt(i)) * weights2[i];
        int r2 = soma % 11 < 2 ? 0 : 11 - (soma % 11);
        return r2 == Character.getNumericValue(cnpj.charAt(13));
    }
}
