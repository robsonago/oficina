package br.com.fiap.challange.oficina.domain;

import br.com.fiap.challange.oficina.model.enums.StatusOS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class StatusOSTest {

    @ParameterizedTest
    @CsvSource({
            "RECEBIDA,EM_DIAGNOSTICO,true",
            "RECEBIDA,AGUARDANDO_APROVACAO,false",
            "RECEBIDA,EM_EXECUCAO,false",
            "EM_DIAGNOSTICO,AGUARDANDO_APROVACAO,true",
            "EM_DIAGNOSTICO,RECEBIDA,false",
            "AGUARDANDO_APROVACAO,EM_EXECUCAO,true",
            "AGUARDANDO_APROVACAO,EM_DIAGNOSTICO,true",
            "AGUARDANDO_APROVACAO,FINALIZADA,false",
            "EM_EXECUCAO,FINALIZADA,true",
            "EM_EXECUCAO,ENTREGUE,false",
            "FINALIZADA,ENTREGUE,true",
            "FINALIZADA,EM_EXECUCAO,false",
            "ENTREGUE,RECEBIDA,false"
    })
    void deveValidarTransicoesDeStatus(String origem, String destino, boolean esperado) {
        StatusOS statusOrigem = StatusOS.valueOf(origem);
        StatusOS statusDestino = StatusOS.valueOf(destino);
        assertThat(statusOrigem.podeTransicionarPara(statusDestino)).isEqualTo(esperado);
    }

    @Test
    void deveRetornarDescricaoCorreta() {
        assertThat(StatusOS.RECEBIDA.getDescricao()).isEqualTo("Recebida");
        assertThat(StatusOS.EM_DIAGNOSTICO.getDescricao()).isEqualTo("Em Diagnóstico");
        assertThat(StatusOS.AGUARDANDO_APROVACAO.getDescricao()).isEqualTo("Aguardando Aprovação");
        assertThat(StatusOS.EM_EXECUCAO.getDescricao()).isEqualTo("Em Execução");
        assertThat(StatusOS.FINALIZADA.getDescricao()).isEqualTo("Finalizada");
        assertThat(StatusOS.ENTREGUE.getDescricao()).isEqualTo("Entregue");
    }
}
