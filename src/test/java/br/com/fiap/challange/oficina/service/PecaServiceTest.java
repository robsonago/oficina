package br.com.fiap.challange.oficina.service;

import br.com.fiap.challange.oficina.dto.request.AtualizarEstoqueRequest;
import br.com.fiap.challange.oficina.dto.request.PecaRequest;
import br.com.fiap.challange.oficina.dto.response.PecaResponse;
import br.com.fiap.challange.oficina.domain.exception.RecursoNaoEncontradoException;
import br.com.fiap.challange.oficina.domain.model.Peca;
import br.com.fiap.challange.oficina.domain.port.out.PecaRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import br.com.fiap.challange.oficina.application.usecase.PecaUseCase;

@ExtendWith(MockitoExtension.class)
class PecaServiceTest {

    @Mock
    private PecaRepositoryPort pecaRepository;

    @InjectMocks
    private PecaUseCase pecaService;

    @Test
    void deveCriarPecaComSucesso() {
        PecaRequest request = new PecaRequest("Filtro de óleo", "Filtro Mann", new BigDecimal("45.90"), 10, "FILT-001");
        Peca pecaSalva = pecaComId(1L, "Filtro de óleo", 10);
        when(pecaRepository.save(any())).thenReturn(pecaSalva);

        PecaResponse response = pecaService.criar(request);
        assertThat(response.nome()).isEqualTo("Filtro de óleo");
        assertThat(response.quantidadeEstoque()).isEqualTo(10);
    }

    @Test
    void deveListarPecas() {
        when(pecaRepository.findByAtivoTrue()).thenReturn(List.of(
                pecaComId(1L, "Filtro", 5),
                pecaComId(2L, "Óleo 5W30", 20)
        ));
        assertThat(pecaService.listar()).hasSize(2);
    }

    @Test
    void deveBuscarPecaPorId() {
        when(pecaRepository.findById(1L)).thenReturn(Optional.of(pecaComId(1L, "Filtro", 5)));
        PecaResponse response = pecaService.buscarPorId(1L);
        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void deveLancarExcecaoQuandoPecaNaoEncontrada() {
        when(pecaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> pecaService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveAtualizarEstoque() {
        Peca peca = pecaComId(1L, "Filtro", 5);
        when(pecaRepository.findById(1L)).thenReturn(Optional.of(peca));
        when(pecaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PecaResponse response = pecaService.atualizarEstoque(1L, new AtualizarEstoqueRequest(50));
        assertThat(response.quantidadeEstoque()).isEqualTo(50);
    }

    @Test
    void deveDeletarPeca() {
        Peca peca = pecaComId(1L, "Filtro", 5);
        when(pecaRepository.findById(1L)).thenReturn(Optional.of(peca));
        when(pecaRepository.save(any())).thenReturn(peca);

        pecaService.deletar(1L);
        assertThat(peca.getAtivo()).isFalse();
    }

    @Test
    void deveCriarPecaComEstoqueNulo() {
        PecaRequest request = new PecaRequest("Filtro", "Desc", new BigDecimal("10.00"), null, "REF-01");
        Peca pecaSalva = pecaComId(1L, "Filtro", 0);
        when(pecaRepository.save(any())).thenReturn(pecaSalva);

        PecaResponse response = pecaService.criar(request);
        assertThat(response.quantidadeEstoque()).isEqualTo(0);
    }

    @Test
    void deveAtualizarPecaSemAlterarEstoqueQuandoQuantidadeNula() {
        Peca peca = pecaComId(1L, "Filtro", 5);
        when(pecaRepository.findById(1L)).thenReturn(Optional.of(peca));
        when(pecaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PecaRequest request = new PecaRequest("Filtro Atualizado", "Desc", new BigDecimal("50.00"), null, "REF-01");
        PecaResponse response = pecaService.atualizar(1L, request);
        assertThat(response.quantidadeEstoque()).isEqualTo(5);
    }

    private Peca pecaComId(Long id, String nome, int estoque) {
        return Peca.builder()
                .id(id).nome(nome).precoUnitario(new BigDecimal("45.90"))
                .quantidadeEstoque(estoque).ativo(true).build();
    }
}
