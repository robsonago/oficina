# language: pt
Funcionalidade: Saga da ordem de serviço entre OS Service, Billing e Execution
  Para manter os três serviços consistentes sem uma transação única
  Como oficina
  Quero que a OS só avance quando cada serviço confirmar a sua parte
  E que tudo o que já foi feito seja desfeito quando alguma etapa falhar

  Contexto:
    Dado um cliente com o veículo "BRA2E19" cadastrado
    E uma OS aberta com o serviço "Troca de pastilhas" de R$ "180,00" e 2 peças "Pastilha de freio" de R$ "90,00" com 10 em estoque
    E o mecânico iniciou o diagnóstico

  Cenário: OS percorre todos os serviços até ser entregue
    Quando o mecânico pede o orçamento
    Então a OS fica "AGUARDANDO_ORCAMENTO"
    E o comando "GERAR_ORCAMENTO" é enviado para o Billing com valor total de R$ "360,00"
    Quando o Billing responde "ORCAMENTO_GERADO"
    Então a OS fica "AGUARDANDO_APROVACAO"
    Quando o Billing responde "ORCAMENTO_APROVADO"
    Então a OS fica "AGUARDANDO_PAGAMENTO"
    E o estoque da peça fica em 8
    Quando o Billing responde "PAGAMENTO_APROVADO"
    Então a OS fica "AGUARDANDO_EXECUCAO"
    E o comando "EXECUTAR_OS" é enviado para o Execution Service
    Quando o Execution Service responde "EXECUCAO_ATUALIZADA"
    Então a OS fica "EM_EXECUCAO"
    Quando o Execution Service responde "EXECUCAO_FINALIZADA"
    Então a OS fica "FINALIZADA"
    E a saga fica na etapa "CONCLUIDA"
    Quando o atendente registra a entrega do veículo
    Então a OS fica "ENTREGUE"

  Cenário: Pagamento recusado devolve as peças e cancela o orçamento
    Dado que o cliente já aprovou o orçamento
    Quando o Billing responde "PAGAMENTO_RECUSADO"
    Então a OS fica "CANCELADA"
    E o estoque da peça fica em 10
    E o comando "CANCELAR_ORCAMENTO" é enviado para o Billing
    E a saga fica na etapa "COMPENSANDO"
    Quando o Billing responde "ORCAMENTO_CANCELADO"
    Então a saga fica na etapa "COMPENSADA"

  Cenário: Falha na execução estorna o pagamento no Mercado Pago
    Dado que o pagamento já foi confirmado
    Quando o Execution Service responde "EXECUCAO_FALHOU"
    Então a OS fica "CANCELADA"
    E o estoque da peça fica em 10
    E o comando "ESTORNAR_PAGAMENTO" é enviado para o Billing
    Quando o Billing responde "PAGAMENTO_ESTORNADO"
    Então a saga fica na etapa "COMPENSADA"

  Cenário: Peça acabou no estoque antes de o cliente aprovar
    Dado que o orçamento foi gerado
    E o estoque da peça zerou
    Quando o Billing responde "ORCAMENTO_APROVADO"
    Então a OS fica "CANCELADA"
    E o comando "CANCELAR_ORCAMENTO" é enviado para o Billing
    E a saga fica na etapa "COMPENSANDO"

  Cenário: Cliente rejeita o orçamento e a OS volta para o mecânico
    Dado que o orçamento foi gerado
    Quando o Billing responde "ORCAMENTO_REJEITADO"
    Então a OS fica "EM_DIAGNOSTICO"
    E a saga fica na etapa "COMPENSADA"

  Cenário: Cliente não paga dentro do prazo
    Dado que o cliente já aprovou o orçamento
    E o prazo da saga venceu
    Quando o verificador de prazos roda
    Então a OS fica "CANCELADA"
    E o estoque da peça fica em 10
    E o comando "CANCELAR_ORCAMENTO" é enviado para o Billing

  Cenário: Mensagem repetida pelo Pub/Sub não avança a saga duas vezes
    Dado que o orçamento foi gerado
    Quando o Billing reenvia a última mensagem
    E o Execution Service responde "EXECUCAO_FINALIZADA"
    Então a OS fica "AGUARDANDO_APROVACAO"
    E o histórico da OS mostra "AGUARDANDO_APROVACAO" uma única vez
