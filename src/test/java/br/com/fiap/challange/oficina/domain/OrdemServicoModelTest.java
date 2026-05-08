package br.com.fiap.challange.oficina.domain;

import br.com.fiap.challange.oficina.exception.TransicaoStatusInvalidaException;
import br.com.fiap.challange.oficina.model.*;
import br.com.fiap.challange.oficina.model.enums.StatusOS;
import br.com.fiap.challange.oficina.model.enums.TipoDocumento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrdemServicoModelTest {

    @Test
    void deveRecalcularTotalComServicosEPecas() {
        OrdemServico os = osPadrao();

        Servico servico = Servico.builder().id(1L).nome("Troca de óleo")
                .preco(new BigDecimal("150.00")).ativo(true).build();
        ItemServicoOS itemServico = ItemServicoOS.builder()
                .ordemServico(os).servico(servico)
                .quantidade(2).precoUnitario(new BigDecimal("150.00")).build();
        os.getItensServico().add(itemServico);

        Peca peca = Peca.builder().id(1L).nome("Filtro").ativo(true)
                .precoUnitario(new BigDecimal("45.90")).quantidadeEstoque(10).build();
        ItemPecaOS itemPeca = ItemPecaOS.builder()
                .ordemServico(os).peca(peca)
                .quantidade(3).precoUnitario(new BigDecimal("45.90")).build();
        os.getItensPeca().add(itemPeca);

        os.recalcularTotal();

        BigDecimal esperado = new BigDecimal("300.00").add(new BigDecimal("137.70"));
        assertThat(os.getValorTotal()).isEqualByComparingTo(esperado);
    }

    @Test
    void deveTransicionarStatusCorretamente() {
        OrdemServico os = osPadrao();
        assertThat(os.getStatus()).isEqualTo(StatusOS.RECEBIDA);

        os.transicionarStatus(StatusOS.EM_DIAGNOSTICO);
        assertThat(os.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
        assertThat(os.getDataInicio()).isNull();

        os.transicionarStatus(StatusOS.AGUARDANDO_APROVACAO);
        assertThat(os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);

        os.transicionarStatus(StatusOS.EM_EXECUCAO);
        assertThat(os.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
        assertThat(os.getDataInicio()).isNotNull();

        os.transicionarStatus(StatusOS.FINALIZADA);
        assertThat(os.getStatus()).isEqualTo(StatusOS.FINALIZADA);
        assertThat(os.getDataFinalizacao()).isNotNull();

        os.transicionarStatus(StatusOS.ENTREGUE);
        assertThat(os.getStatus()).isEqualTo(StatusOS.ENTREGUE);
        assertThat(os.getDataEntrega()).isNotNull();
    }

    @Test
    void deveLancarExcecaoEmTransicaoInvalida() {
        OrdemServico os = osPadrao();
        assertThatThrownBy(() -> os.transicionarStatus(StatusOS.FINALIZADA))
                .isInstanceOf(TransicaoStatusInvalidaException.class)
                .hasMessageContaining("inválida");
    }

    @Test
    void deveCalcularSubtotalDoItemServico() {
        Servico servico = Servico.builder().id(1L).nome("Alinhamento")
                .preco(new BigDecimal("80.00")).ativo(true).build();
        ItemServicoOS item = ItemServicoOS.builder()
                .servico(servico).quantidade(3).precoUnitario(new BigDecimal("80.00")).build();

        assertThat(item.getSubtotal()).isEqualByComparingTo(new BigDecimal("240.00"));
    }

    @Test
    void deveCalcularSubtotalDoItemPeca() {
        Peca peca = Peca.builder().id(1L).nome("Filtro de óleo").ativo(true)
                .precoUnitario(new BigDecimal("45.90")).quantidadeEstoque(5).build();
        ItemPecaOS item = ItemPecaOS.builder()
                .peca(peca).quantidade(2).precoUnitario(new BigDecimal("45.90")).build();

        assertThat(item.getSubtotal()).isEqualByComparingTo(new BigDecimal("91.80"));
    }

    @Test
    void deveRetornarTotalZeroSemItens() {
        OrdemServico os = osPadrao();
        os.recalcularTotal();
        assertThat(os.getValorTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private OrdemServico osPadrao() {
        Cliente cliente = Cliente.builder().id(1L).nome("João").documento("52998224725")
                .tipoDocumento(TipoDocumento.CPF).ativo(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        Veiculo veiculo = Veiculo.builder().id(1L).placa("ABC1234").marca("Toyota")
                .modelo("Corolla").ano(2020).cliente(cliente).ativo(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        return OrdemServico.builder()
                .id(1L).numero("OS-2024-TEST001")
                .cliente(cliente).veiculo(veiculo)
                .status(StatusOS.RECEBIDA)
                .dataAbertura(LocalDateTime.now())
                .valorTotal(BigDecimal.ZERO)
                .itensServico(new ArrayList<>()).itensPeca(new ArrayList<>())
                .build();
    }
}
