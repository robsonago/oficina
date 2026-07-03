package br.com.fiap.challange.oficina.domain.model;

import br.com.fiap.challange.oficina.domain.exception.TransicaoStatusInvalidaException;
import br.com.fiap.challange.oficina.domain.model.enums.StatusOS;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(exclude = {"itensServico", "itensPeca"})
@EqualsAndHashCode(of = "id")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdemServico {

    private Long id;
    private String numero;
    private Cliente cliente;
    private Veiculo veiculo;
    private StatusOS status;
    private String descricaoProblema;
    private String observacoes;

    @Builder.Default
    private BigDecimal valorTotal = BigDecimal.ZERO;

    private LocalDateTime dataAbertura;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFinalizacao;
    private LocalDateTime dataEntrega;

    @Builder.Default
    private List<ItemServicoOS> itensServico = new ArrayList<>();

    @Builder.Default
    private List<ItemPecaOS> itensPeca = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void recalcularTotal() {
        BigDecimal totalServicos = itensServico.stream()
                .map(ItemServicoOS::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPecas = itensPeca.stream()
                .map(ItemPecaOS::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.valorTotal = totalServicos.add(totalPecas);
    }

    public void transicionarStatus(StatusOS novoStatus) {
        if (!this.status.podeTransicionarPara(novoStatus)) {
            throw new TransicaoStatusInvalidaException(
                    String.format("Transição inválida: %s → %s", this.status.getDescricao(), novoStatus.getDescricao())
            );
        }
        this.status = novoStatus;

        if (novoStatus == StatusOS.EM_EXECUCAO) {
            this.dataInicio = LocalDateTime.now();
        } else if (novoStatus == StatusOS.FINALIZADA) {
            this.dataFinalizacao = LocalDateTime.now();
        } else if (novoStatus == StatusOS.ENTREGUE) {
            this.dataEntrega = LocalDateTime.now();
        }
    }
}
