package br.com.fiap.challange.oficina.domain.model;

/**
 * Comandos enviados pelo orquestrador e eventos recebidos dos participantes.
 * Os mesmos nomes existem no Billing Service e no Execution Service.
 */
public final class TiposMensagem {

    // comandos → Billing
    public static final String GERAR_ORCAMENTO = "GERAR_ORCAMENTO";
    public static final String CANCELAR_ORCAMENTO = "CANCELAR_ORCAMENTO";
    public static final String ESTORNAR_PAGAMENTO = "ESTORNAR_PAGAMENTO";

    // comandos → Execution
    public static final String EXECUTAR_OS = "EXECUTAR_OS";

    // eventos ← Billing
    public static final String ORCAMENTO_GERADO = "ORCAMENTO_GERADO";
    public static final String ORCAMENTO_FALHOU = "ORCAMENTO_FALHOU";
    public static final String ORCAMENTO_APROVADO = "ORCAMENTO_APROVADO";
    public static final String ORCAMENTO_REJEITADO = "ORCAMENTO_REJEITADO";
    public static final String ORCAMENTO_CANCELADO = "ORCAMENTO_CANCELADO";
    public static final String PAGAMENTO_APROVADO = "PAGAMENTO_APROVADO";
    public static final String PAGAMENTO_RECUSADO = "PAGAMENTO_RECUSADO";
    public static final String PAGAMENTO_ESTORNADO = "PAGAMENTO_ESTORNADO";

    // eventos ← Execution
    public static final String EXECUCAO_ATUALIZADA = "EXECUCAO_ATUALIZADA";
    public static final String EXECUCAO_FINALIZADA = "EXECUCAO_FINALIZADA";
    public static final String EXECUCAO_FALHOU = "EXECUCAO_FALHOU";

    private TiposMensagem() {
    }
}
