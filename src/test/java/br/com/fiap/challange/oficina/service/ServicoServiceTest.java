package br.com.fiap.challange.oficina.service;

import br.com.fiap.challange.oficina.domain.exception.RecursoNaoEncontradoException;
import br.com.fiap.challange.oficina.domain.model.Servico;
import br.com.fiap.challange.oficina.domain.port.in.command.ServicoCommand;
import br.com.fiap.challange.oficina.domain.port.out.ServicoRepositoryPort;
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
import br.com.fiap.challange.oficina.application.usecase.ServicoUseCase;

@ExtendWith(MockitoExtension.class)
class ServicoServiceTest {

    @Mock
    private ServicoRepositoryPort servicoRepository;

    @InjectMocks
    private ServicoUseCase servicoService;

    @Test
    void deveCriarServico() {
        ServicoCommand command = new ServicoCommand("Troca de óleo", "Troca completa", new BigDecimal("150.00"), 60);
        Servico salvo = servicoComId(1L, "Troca de óleo");
        when(servicoRepository.save(any())).thenReturn(salvo);

        Servico response = servicoService.criar(command);
        assertThat(response.getNome()).isEqualTo("Troca de óleo");
    }

    @Test
    void deveListarServicosAtivos() {
        when(servicoRepository.findByAtivoTrue()).thenReturn(List.of(
                servicoComId(1L, "Troca de óleo"),
                servicoComId(2L, "Alinhamento")
        ));
        assertThat(servicoService.listar()).hasSize(2);
    }

    @Test
    void deveBuscarServicoPorId() {
        when(servicoRepository.findById(1L)).thenReturn(Optional.of(servicoComId(1L, "Troca de óleo")));
        assertThat(servicoService.buscarPorId(1L).getId()).isEqualTo(1L);
    }

    @Test
    void deveLancarExcecaoQuandoServicoNaoEncontrado() {
        when(servicoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicoService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveAtualizarServico() {
        Servico servico = servicoComId(1L, "Troca de óleo");
        when(servicoRepository.findById(1L)).thenReturn(Optional.of(servico));
        when(servicoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Servico response = servicoService.atualizar(1L,
                new ServicoCommand("Troca de óleo 5W30", null, new BigDecimal("180.00"), 90));
        assertThat(response.getNome()).isEqualTo("Troca de óleo 5W30");
    }

    @Test
    void deveDeletarServico() {
        Servico servico = servicoComId(1L, "Alinhamento");
        when(servicoRepository.findById(1L)).thenReturn(Optional.of(servico));
        when(servicoRepository.save(any())).thenReturn(servico);

        servicoService.deletar(1L);
        assertThat(servico.getAtivo()).isFalse();
    }

    @Test
    void deveCriarServicoComTempoNulo() {
        ServicoCommand command = new ServicoCommand("Balanceamento", null, new BigDecimal("80.00"), null);
        Servico salvo = servicoComId(1L, "Balanceamento");
        when(servicoRepository.save(any())).thenReturn(salvo);

        Servico response = servicoService.criar(command);
        assertThat(response.getNome()).isEqualTo("Balanceamento");
    }

    @Test
    void deveAtualizarServicoSemAlterarTempoQuandoNulo() {
        Servico servico = servicoComId(1L, "Alinhamento");
        when(servicoRepository.findById(1L)).thenReturn(Optional.of(servico));
        when(servicoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Servico response = servicoService.atualizar(1L,
                new ServicoCommand("Alinhamento Plus", null, new BigDecimal("200.00"), null));
        assertThat(response.getTempoEstimadoMinutos()).isEqualTo(60);
    }

    private Servico servicoComId(Long id, String nome) {
        return Servico.builder()
                .id(id).nome(nome).preco(new BigDecimal("150.00"))
                .tempoEstimadoMinutos(60).ativo(true).build();
    }
}
