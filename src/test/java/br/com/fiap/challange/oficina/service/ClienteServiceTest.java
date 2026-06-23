package br.com.fiap.challange.oficina.service;

import br.com.fiap.challange.oficina.dto.request.ClienteRequest;
import br.com.fiap.challange.oficina.dto.response.ClienteResponse;
import br.com.fiap.challange.oficina.domain.exception.DocumentoInvalidoException;
import br.com.fiap.challange.oficina.domain.exception.RecursoNaoEncontradoException;
import br.com.fiap.challange.oficina.domain.exception.RegraDeNegocioException;
import br.com.fiap.challange.oficina.domain.model.Cliente;
import br.com.fiap.challange.oficina.domain.model.enums.TipoDocumento;
import br.com.fiap.challange.oficina.domain.port.out.ClienteRepositoryPort;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import br.com.fiap.challange.oficina.application.usecase.ClienteUseCase;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    private static final String CPF_VALIDO = "52998224725";
    private static final String CPF_INVALIDO = "11111111111";
    @Mock
    private ClienteRepositoryPort clienteRepository;
    @InjectMocks
    private ClienteUseCase clienteService;

    @Test
    void deveCriarClienteComSucesso() {
        ClienteRequest request = new ClienteRequest("João Silva", "529.982.247-25",
                "joao@email.com", "11999999999", "Rua A, 123");
        Cliente clienteSalvo = clienteComId(1L, "João Silva", CPF_VALIDO);

        when(clienteRepository.existsByDocumento(CPF_VALIDO)).thenReturn(false);
        when(clienteRepository.save(any())).thenReturn(clienteSalvo);

        ClienteResponse response = clienteService.criar(request);

        assertThat(response.nome()).isEqualTo("João Silva");
        assertThat(response.documento()).isEqualTo(CPF_VALIDO);
        verify(clienteRepository).save(any());
    }

    @Test
    void deveRejeitarCPFInvalido() {
        ClienteRequest request = new ClienteRequest("João", CPF_INVALIDO, null, null, null);
        assertThatThrownBy(() -> clienteService.criar(request))
                .isInstanceOf(DocumentoInvalidoException.class);
    }

    @Test
    void deveRejeitarDocumentoDuplicado() {
        ClienteRequest request = new ClienteRequest("João", "529.982.247-25", null, null, null);
        when(clienteRepository.existsByDocumento(CPF_VALIDO)).thenReturn(true);

        assertThatThrownBy(() -> clienteService.criar(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining(CPF_VALIDO);
    }

    @Test
    void deveListarClientes() {
        when(clienteRepository.findAll()).thenReturn(List.of(
                clienteComId(1L, "João", CPF_VALIDO),
                clienteComId(2L, "Maria", "11144477735")
        ));
        List<ClienteResponse> lista = clienteService.listar();
        assertThat(lista).hasSize(2);
    }

    @Test
    void deveBuscarClientePorId() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteComId(1L, "João", CPF_VALIDO)));
        ClienteResponse response = clienteService.buscarPorId(1L);
        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void deveLancarExcecaoQuandoClienteNaoEncontrado() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> clienteService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveBuscarClientePorDocumento() {
        when(clienteRepository.findByDocumento(CPF_VALIDO))
                .thenReturn(Optional.of(clienteComId(1L, "João", CPF_VALIDO)));
        ClienteResponse response = clienteService.buscarPorDocumento("529.982.247-25");
        assertThat(response.documento()).isEqualTo(CPF_VALIDO);
    }

    @Test
    void deveAtualizarClienteComSucesso() {
        Cliente cliente = clienteComId(1L, "João", CPF_VALIDO);
        ClienteRequest request = new ClienteRequest("João Atualizado", "529.982.247-25", null, null, null);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ClienteResponse response = clienteService.atualizar(1L, request);
        assertThat(response.nome()).isEqualTo("João Atualizado");
    }

    @Test
    void deveDeletarCliente() {
        Cliente cliente = clienteComId(1L, "João", CPF_VALIDO);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any())).thenReturn(cliente);

        clienteService.deletar(1L);

        assertThat(cliente.getAtivo()).isFalse();
        verify(clienteRepository).save(cliente);
    }

    @Test
    void deveAtualizarClienteComDocumentoAlterado() {
        String novoDoc = "11144477735";
        Cliente cliente = clienteComId(1L, "João", CPF_VALIDO);
        ClienteRequest request = new ClienteRequest("João", "111.444.777-35", null, null, null);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.existsByDocumento(novoDoc)).thenReturn(false);
        when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ClienteResponse response = clienteService.atualizar(1L, request);
        assertThat(response.documento()).isEqualTo(novoDoc);
    }

    @Test
    void deveRejeitarAtualizacaoComDocumentoAlteradoDuplicado() {
        String novoDoc = "11144477735";
        Cliente cliente = clienteComId(1L, "João", CPF_VALIDO);
        ClienteRequest request = new ClienteRequest("João", "111.444.777-35", null, null, null);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.existsByDocumento(novoDoc)).thenReturn(true);

        assertThatThrownBy(() -> clienteService.atualizar(1L, request))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void deveRejeitarAtualizacaoComDocumentoAlteradoInvalido() {
        Cliente cliente = clienteComId(1L, "João", CPF_VALIDO);
        ClienteRequest request = new ClienteRequest("João", "111.111.111-11", null, null, null);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> clienteService.atualizar(1L, request))
                .isInstanceOf(DocumentoInvalidoException.class);
    }

    @Test
    void deveRejeitarBuscaPorDocumentoNaoEncontrado() {
        when(clienteRepository.findByDocumento(CPF_VALIDO)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> clienteService.buscarPorDocumento("529.982.247-25"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private Cliente clienteComId(Long id, String nome, String doc) {
        return Cliente.builder()
                .id(id).nome(nome).documento(doc)
                .tipoDocumento(TipoDocumento.CPF).ativo(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }
}
