package br.com.fiap.challange.oficina.domain;

import br.com.fiap.challange.oficina.model.enums.TipoDocumento;
import br.com.fiap.challange.oficina.validator.CpfCnpjValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CpfCnpjValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-25", "52998224725", "111.444.777-35", "11144477735"})
    void deveValidarCpfsValidos(String cpf) {
        assertThat(CpfCnpjValidator.isValido(cpf)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"111.111.111-11", "000.000.000-00", "12345678900", "123.456.789-00"})
    void deveRejeitarCpfsInvalidos(String cpf) {
        assertThat(CpfCnpjValidator.isValido(cpf)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"11.222.333/0001-81", "11222333000181", "60.701.190/0001-04"})
    void deveValidarCnpjsValidos(String cnpj) {
        assertThat(CpfCnpjValidator.isValido(cnpj)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"11.111.111/1111-11", "00.000.000/0000-00", "12345678000100"})
    void deveRejeitarCnpjsInvalidos(String cnpj) {
        assertThat(CpfCnpjValidator.isValido(cnpj)).isFalse();
    }

    @Test
    void deveRetornarFalsoParaNulo() {
        assertThat(CpfCnpjValidator.isValido(null)).isFalse();
    }

    @Test
    void deveDetectarTipoCPF() {
        assertThat(CpfCnpjValidator.detectarTipo("529.982.247-25")).isEqualTo(TipoDocumento.CPF);
    }

    @Test
    void deveDetectarTipoCNPJ() {
        assertThat(CpfCnpjValidator.detectarTipo("11.222.333/0001-81")).isEqualTo(TipoDocumento.CNPJ);
    }

    @Test
    void deveNormalizarDocumento() {
        assertThat(CpfCnpjValidator.normalizar("529.982.247-25")).isEqualTo("52998224725");
        assertThat(CpfCnpjValidator.normalizar("11.222.333/0001-81")).isEqualTo("11222333000181");
    }
}
