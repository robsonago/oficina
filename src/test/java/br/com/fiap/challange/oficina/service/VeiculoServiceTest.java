package br.com.fiap.challange.oficina.service;

import br.com.fiap.challange.oficina.dto.request.VeiculoRequest;
import br.com.fiap.challange.oficina.dto.response.VeiculoResponse;
import br.com.fiap.challange.oficina.domain.exception.RecursoNaoEncontradoException;
import br.com.fiap.challange.oficina.domain.exception.RegraDeNegocioException;
import br.com.fiap.challange.oficina.domain.model.Cliente;
import br.com.fiap.challange.oficina.domain.model.Veiculo;
import br.com.fiap.challange.oficina.domain.model.enums.TipoDocumento;
import br.com.fiap.challange.oficina.domain.port.out.ClienteRepositoryPort;
import br.com.fiap.challange.oficina.domain.port.out.VeiculoRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import br.com.fiap.challange.oficina.application.usecase.VeiculoUseCase;

@ExtendWith(MockitoExtension.class)
class VeiculoServiceTest {

    @Mock
    VeiculoRepositoryPort veiculoRepository;
    @Mock
    ClienteRepositoryPort clienteRepository;

    @InjectMocks
    VeiculoUseCase veiculoService;

    @Test
    void deveCriarVeiculoComSucesso() {
        VeiculoRequest request = new VeiculoRequest("ABC-1234", "Toyota", "Corolla", 2020, 1L);
        when(veiculoRepository.existsByPlaca("ABC1234")).thenReturn(false);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clientePadrao()));
        when(veiculoRepository.save(any())).thenAnswer(inv -> {
            Veiculo v = Veiculo.builder().id(1L).placa("ABC1234").marca("Toyota").modelo("Corolla")
                    .ano(2020).cliente(clientePadrao()).ativo(true)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
            return v;
        });

        VeiculoResponse response = veiculoService.criar(request);
        assertThat(response.placa()).isEqualTo("ABC1234");
        assertThat(response.marca()).isEqualTo("Toyota");
    }

    @Test
    void deveRejeitarPlacaInvalida() {
        VeiculoRequest request = new VeiculoRequest("AB123", "Toyota", "Corolla", 2020, 1L);
        assertThatThrownBy(() -> veiculoService.criar(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("inválida");
    }

    @Test
    void deveRejeitarPlacaDuplicada() {
        VeiculoRequest request = new VeiculoRequest("ABC1234", "Toyota", "Corolla", 2020, 1L);
        when(veiculoRepository.existsByPlaca("ABC1234")).thenReturn(true);
        assertThatThrownBy(() -> veiculoService.criar(request))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void deveListarVeiculos() {
        when(veiculoRepository.findAll()).thenReturn(List.of(veiculoPadrao()));
        assertThat(veiculoService.listar()).hasSize(1);
    }

    @Test
    void deveBuscarPorPlaca() {
        when(veiculoRepository.findByPlaca("ABC1234")).thenReturn(Optional.of(veiculoPadrao()));
        VeiculoResponse response = veiculoService.buscarPorPlaca("abc-1234");
        assertThat(response.placa()).isEqualTo("ABC1234");
    }

    @Test
    void deveLancarExcecaoVeiculoNaoEncontrado() {
        when(veiculoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> veiculoService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveListarPorCliente() {
        when(veiculoRepository.findByClienteId(1L)).thenReturn(List.of(veiculoPadrao()));
        assertThat(veiculoService.listarPorCliente(1L)).hasSize(1);
    }

    @Test
    void deveAtualizarVeiculo() {
        Veiculo veiculo = veiculoPadrao();
        when(veiculoRepository.findById(1L)).thenReturn(Optional.of(veiculo));
        when(veiculoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VeiculoResponse response = veiculoService.atualizar(1L,
                new VeiculoRequest("ABC1234", "Honda", "Civic", 2022, 1L));
        assertThat(response.marca()).isEqualTo("Honda");
        assertThat(response.ano()).isEqualTo(2022);
    }

    @Test
    void deveDeletarVeiculo() {
        Veiculo veiculo = veiculoPadrao();
        when(veiculoRepository.findById(1L)).thenReturn(Optional.of(veiculo));
        when(veiculoRepository.save(any())).thenReturn(veiculo);

        veiculoService.deletar(1L);
        assertThat(veiculo.getAtivo()).isFalse();
    }

    @Test
    void deveLancarExcecaoClienteNaoEncontradoNoCadastroVeiculo() {
        VeiculoRequest request = new VeiculoRequest("ABC1234", "Toyota", "Corolla", 2020, 99L);
        when(veiculoRepository.existsByPlaca("ABC1234")).thenReturn(false);
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> veiculoService.criar(request))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveAtualizarVeiculoComPlacaDiferenteValida() {
        Veiculo veiculo = veiculoPadrao(); // placa ABC1234
        when(veiculoRepository.findById(1L)).thenReturn(Optional.of(veiculo));
        when(veiculoRepository.existsByPlaca("DEF5678")).thenReturn(false);
        when(veiculoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VeiculoResponse response = veiculoService.atualizar(1L,
                new VeiculoRequest("DEF-5678", "Honda", "Civic", 2022, 1L));
        assertThat(response.placa()).isEqualTo("DEF5678");
    }

    @Test
    void deveRejeitarAtualizacaoComPlacaInvalidaQuandoAlterada() {
        Veiculo veiculo = veiculoPadrao();
        when(veiculoRepository.findById(1L)).thenReturn(Optional.of(veiculo));

        assertThatThrownBy(() -> veiculoService.atualizar(1L,
                new VeiculoRequest("INVALIDA", "Honda", "Civic", 2022, 1L)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("inválida");
    }

    @Test
    void deveRejeitarAtualizacaoComPlacaDuplicadaQuandoAlterada() {
        Veiculo veiculo = veiculoPadrao();
        when(veiculoRepository.findById(1L)).thenReturn(Optional.of(veiculo));
        when(veiculoRepository.existsByPlaca("DEF5678")).thenReturn(true);

        assertThatThrownBy(() -> veiculoService.atualizar(1L,
                new VeiculoRequest("DEF-5678", "Honda", "Civic", 2022, 1L)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("DEF5678");
    }

    @Test
    void deveAtualizarVeiculoComClienteDiferente() {
        Veiculo veiculo = veiculoPadrao(); // cliente id=1
        Cliente novoCliente = Cliente.builder().id(2L).nome("Carlos").documento("11144477735")
                .tipoDocumento(br.com.fiap.challange.oficina.domain.model.enums.TipoDocumento.CPF)
                .ativo(true).createdAt(java.time.LocalDateTime.now()).updatedAt(java.time.LocalDateTime.now()).build();
        when(veiculoRepository.findById(1L)).thenReturn(Optional.of(veiculo));
        when(clienteRepository.findById(2L)).thenReturn(Optional.of(novoCliente));
        when(veiculoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VeiculoResponse response = veiculoService.atualizar(1L,
                new VeiculoRequest("ABC1234", "Toyota", "Corolla", 2020, 2L));
        assertThat(response).isNotNull();
    }

    @Test
    void deveRejeitarAtualizacaoComNovoClienteNaoEncontrado() {
        Veiculo veiculo = veiculoPadrao();
        when(veiculoRepository.findById(1L)).thenReturn(Optional.of(veiculo));
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> veiculoService.atualizar(1L,
                new VeiculoRequest("ABC1234", "Toyota", "Corolla", 2020, 99L)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveRejeitarBuscaPorPlacaNaoEncontrada() {
        when(veiculoRepository.findByPlaca("XYZ9999")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> veiculoService.buscarPorPlaca("XYZ-9999"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private Cliente clientePadrao() {
        return Cliente.builder().id(1L).nome("Ana Silva").documento("52998224725")
                .tipoDocumento(TipoDocumento.CPF).ativo(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private Veiculo veiculoPadrao() {
        return Veiculo.builder().id(1L).placa("ABC1234").marca("Toyota").modelo("Corolla")
                .ano(2020).cliente(clientePadrao()).ativo(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }
}
