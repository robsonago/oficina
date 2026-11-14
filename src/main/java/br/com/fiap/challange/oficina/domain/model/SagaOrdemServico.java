package br.com.fiap.challange.oficina.domain.model;

import br.com.fiap.challange.oficina.domain.model.enums.EtapaSaga;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Estado da saga de uma OS: orçamento → aprovação → pagamento → execução.
 * O OS Service é o orquestrador; esta entidade guarda em que passo a saga
 * está e o que já foi feito, para saber o que desfazer se algo der errado.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaOrdemServico {

    private String id;
    private Long ordemServicoId;
    private EtapaSaga etapa;
    private String orcamentoId;
    private String pagamentoId;
    private String linkPagamento;
    private boolean estoqueReservado;
    private String motivoCompensacao;
    private LocalDateTime prazo;
    private LocalDateTime criadaEm;
    private LocalDateTime atualizadaEm;
    private Long versao;

    @Builder.Default
    private List<PassoSaga> passos = new ArrayList<>();

    public static SagaOrdemServico iniciar(Long ordemServicoId, LocalDateTime prazo) {
        LocalDateTime agora = LocalDateTime.now();
        SagaOrdemServico saga = SagaOrdemServico.builder()
                .id(UUID.randomUUID().toString())
                .ordemServicoId(ordemServicoId)
                .etapa(EtapaSaga.GERANDO_ORCAMENTO)
                .prazo(prazo)
                .criadaEm(agora)
                .atualizadaEm(agora)
                .build();
        saga.registrar("SAGA_INICIADA", "Pedido de orçamento enviado ao Billing");
        return saga;
    }

    public boolean estaEm(EtapaSaga... etapas) {
        for (EtapaSaga e : etapas) {
            if (etapa == e) {
                return true;
            }
        }
        return false;
    }

    public void avancar(EtapaSaga nova, String evento, String descricao, LocalDateTime novoPrazo) {
        this.etapa = nova;
        this.prazo = novoPrazo;
        registrar(evento, descricao);
    }

    /** Começa a desfazer: fica COMPENSANDO até o participante confirmar. */
    public void compensar(String evento, String motivo) {
        this.etapa = EtapaSaga.COMPENSANDO;
        this.motivoCompensacao = motivo;
        this.prazo = null;
        registrar(evento, "Compensação iniciada: " + motivo);
    }

    /** Compensação que não depende de ninguém (nada foi cobrado nem reservado). */
    public void encerrarCompensada(String evento, String motivo) {
        this.etapa = EtapaSaga.COMPENSADA;
        this.motivoCompensacao = motivo;
        this.prazo = null;
        registrar(evento, motivo);
    }

    public void registrar(String evento, String descricao) {
        this.atualizadaEm = LocalDateTime.now();
        passos.add(new PassoSaga(etapa, evento, descricao, atualizadaEm));
    }
}
