package br.com.fiap.challange.oficina.domain.model;

import br.com.fiap.challange.oficina.domain.exception.TransicaoStatusInvalidaException;
import br.com.fiap.challange.oficina.domain.model.enums.StatusOS;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordens_servico")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOS status;

    @Column(name = "descricao_problema", columnDefinition = "TEXT")
    private String descricaoProblema;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Column(name = "data_abertura", nullable = false)
    private LocalDateTime dataAbertura;

    @Column(name = "data_inicio")
    private LocalDateTime dataInicio;

    @Column(name = "data_finalizacao")
    private LocalDateTime dataFinalizacao;

    @Column(name = "data_entrega")
    private LocalDateTime dataEntrega;

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemServicoOS> itensServico = new ArrayList<>();

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemPecaOS> itensPeca = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
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
