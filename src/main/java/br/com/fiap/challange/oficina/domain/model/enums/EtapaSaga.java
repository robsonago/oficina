package br.com.fiap.challange.oficina.domain.model.enums;

public enum EtapaSaga {
    GERANDO_ORCAMENTO,
    AGUARDANDO_APROVACAO,
    AGUARDANDO_PAGAMENTO,
    AGUARDANDO_EXECUCAO,
    EM_EXECUCAO,
    CONCLUIDA,
    COMPENSANDO,
    COMPENSADA;

    public boolean ativa() {
        return this != CONCLUIDA && this != COMPENSADA;
    }
}
