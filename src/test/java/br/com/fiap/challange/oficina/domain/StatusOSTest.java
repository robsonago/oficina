package br.com.fiap.challange.oficina.domain;

import br.com.fiap.challange.oficina.domain.model.enums.StatusOS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class StatusOSTest {

    @ParameterizedTest
    @CsvSource({
            "RECEBIDA,EM_DIAGNOSTICO,true",
            "RECEBIDA,CANCELADA,true",
            "RECEBIDA,AGUARDANDO_APROVACAO,false",
            "RECEBIDA,EM_EXECUCAO,false",
            "EM_DIAGNOSTICO,AGUARDANDO_ORCAMENTO,true",
            "EM_DIAGNOSTICO,AGUARDANDO_APROVACAO,false",
            "EM_DIAGNOSTICO,RECEBIDA,false",
            "AGUARDANDO_ORCAMENTO,AGUARDANDO_APROVACAO,true",
            "AGUARDANDO_ORCAMENTO,EM_DIAGNOSTICO,true",
            "AGUARDANDO_APROVACAO,AGUARDANDO_PAGAMENTO,true",
            "AGUARDANDO_APROVACAO,EM_DIAGNOSTICO,true",
            "AGUARDANDO_APROVACAO,CANCELADA,true",
            "AGUARDANDO_APROVACAO,EM_EXECUCAO,false",
            "AGUARDANDO_APROVACAO,FINALIZADA,false",
            "AGUARDANDO_PAGAMENTO,AGUARDANDO_EXECUCAO,true",
            "AGUARDANDO_PAGAMENTO,CANCELADA,true",
            "AGUARDANDO_PAGAMENTO,EM_EXECUCAO,false",
            "AGUARDANDO_EXECUCAO,EM_EXECUCAO,true",
            "AGUARDANDO_EXECUCAO,CANCELADA,true",
            "EM_EXECUCAO,FINALIZADA,true",
            "EM_EXECUCAO,CANCELADA,true",
            "EM_EXECUCAO,ENTREGUE,false",
            "FINALIZADA,ENTREGUE,true",
            "FINALIZADA,CANCELADA,false",
            "FINALIZADA,EM_EXECUCAO,false",
            "ENTREGUE,RECEBIDA,false",
            "CANCELADA,RECEBIDA,false"
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
        assertThat(StatusOS.AGUARDANDO_ORCAMENTO.getDescricao()).isEqualTo("Aguardando Orçamento");
        assertThat(StatusOS.AGUARDANDO_PAGAMENTO.getDescricao()).isEqualTo("Aguardando Pagamento");
        assertThat(StatusOS.AGUARDANDO_EXECUCAO.getDescricao()).isEqualTo("Aguardando Execução");
        assertThat(StatusOS.CANCELADA.getDescricao()).isEqualTo("Cancelada");
    }
}
