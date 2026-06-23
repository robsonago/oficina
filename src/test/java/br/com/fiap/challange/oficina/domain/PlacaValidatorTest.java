package br.com.fiap.challange.oficina.domain;

import br.com.fiap.challange.oficina.domain.validator.PlacaValidator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PlacaValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"ABC1234", "ABC-1234", "abc1234", "XYZ9999"})
    void deveValidarPlacasAntigas(String placa) {
        assertThat(PlacaValidator.isValida(placa)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC1D23", "abc1d23", "XYZ0A99"})
    void deveValidarPlacasMercosul(String placa) {
        assertThat(PlacaValidator.isValida(placa)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"AB1234", "ABCD123", "ABC123", "1234ABC", ""})
    void deveRejeitarPlacasInvalidas(String placa) {
        assertThat(PlacaValidator.isValida(placa)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc-1234", "ABC-1234", "abc1234"})
    void deveNormalizarParaMaiusculas(String placa) {
        assertThat(PlacaValidator.normalizar(placa)).matches("^[A-Z0-9]+$");
    }
}
