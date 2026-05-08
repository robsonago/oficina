package br.com.fiap.challange.oficina.model.enums;

public enum StatusOS {
    RECEBIDA,
    EM_DIAGNOSTICO,
    AGUARDANDO_APROVACAO,
    EM_EXECUCAO,
    FINALIZADA,
    ENTREGUE;

    public boolean podeTransicionarPara(StatusOS destino) {
        return switch (this) {
            case RECEBIDA -> destino == EM_DIAGNOSTICO;
            case EM_DIAGNOSTICO -> destino == AGUARDANDO_APROVACAO;
            case AGUARDANDO_APROVACAO -> destino == EM_EXECUCAO || destino == EM_DIAGNOSTICO;
            case EM_EXECUCAO -> destino == FINALIZADA;
            case FINALIZADA -> destino == ENTREGUE;
            case ENTREGUE -> false;
        };
    }

    public String getDescricao() {
        return switch (this) {
            case RECEBIDA -> "Recebida";
            case EM_DIAGNOSTICO -> "Em Diagnóstico";
            case AGUARDANDO_APROVACAO -> "Aguardando Aprovação";
            case EM_EXECUCAO -> "Em Execução";
            case FINALIZADA -> "Finalizada";
            case ENTREGUE -> "Entregue";
        };
    }
}
