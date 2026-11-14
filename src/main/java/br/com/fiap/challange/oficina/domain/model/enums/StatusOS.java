package br.com.fiap.challange.oficina.domain.model.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida da OS. A partir da Fase 4 parte das transições acontece por
 * eventos de outros serviços (orçamento/pagamento no Billing, execução no
 * Execution Service), conduzidas pela saga — por isso os status de espera
 * (AGUARDANDO_*) e o CANCELADA, usado nas compensações.
 */
public enum StatusOS {
    RECEBIDA("Recebida"),
    EM_DIAGNOSTICO("Em Diagnóstico"),
    AGUARDANDO_ORCAMENTO("Aguardando Orçamento"),
    AGUARDANDO_APROVACAO("Aguardando Aprovação"),
    AGUARDANDO_PAGAMENTO("Aguardando Pagamento"),
    AGUARDANDO_EXECUCAO("Aguardando Execução"),
    EM_EXECUCAO("Em Execução"),
    FINALIZADA("Finalizada"),
    ENTREGUE("Entregue"),
    CANCELADA("Cancelada");

    private final String descricao;

    StatusOS(String descricao) {
        this.descricao = descricao;
    }

    public boolean podeTransicionarPara(StatusOS destino) {
        return destinosPermitidos().contains(destino);
    }

    private Set<StatusOS> destinosPermitidos() {
        return switch (this) {
            case RECEBIDA -> EnumSet.of(EM_DIAGNOSTICO, CANCELADA);
            case EM_DIAGNOSTICO -> EnumSet.of(AGUARDANDO_ORCAMENTO, CANCELADA);
            // sem resposta do Billing ou orçamento rejeitado: volta para o mecânico revisar
            case AGUARDANDO_ORCAMENTO -> EnumSet.of(AGUARDANDO_APROVACAO, EM_DIAGNOSTICO, CANCELADA);
            case AGUARDANDO_APROVACAO -> EnumSet.of(AGUARDANDO_PAGAMENTO, EM_DIAGNOSTICO, CANCELADA);
            case AGUARDANDO_PAGAMENTO -> EnumSet.of(AGUARDANDO_EXECUCAO, CANCELADA);
            case AGUARDANDO_EXECUCAO -> EnumSet.of(EM_EXECUCAO, CANCELADA);
            case EM_EXECUCAO -> EnumSet.of(FINALIZADA, CANCELADA);
            case FINALIZADA -> EnumSet.of(ENTREGUE);
            case ENTREGUE, CANCELADA -> EnumSet.noneOf(StatusOS.class);
        };
    }

    public String getDescricao() {
        return descricao;
    }
}
