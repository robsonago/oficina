package br.com.fiap.challange.oficina.service;

import br.com.fiap.challange.oficina.dto.request.ClienteRequest;
import br.com.fiap.challange.oficina.dto.response.ClienteResponse;
import br.com.fiap.challange.oficina.exception.DocumentoInvalidoException;
import br.com.fiap.challange.oficina.exception.RecursoNaoEncontradoException;
import br.com.fiap.challange.oficina.exception.RegraDeNegocioException;
import br.com.fiap.challange.oficina.model.Cliente;
import br.com.fiap.challange.oficina.repository.ClienteRepository;
import br.com.fiap.challange.oficina.validator.CpfCnpjValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteResponse criar(ClienteRequest request) {
        log.info("Criando cliente nome={} documento={}", request.nome(), request.documento());
        validarDocumento(request.documento());
        String docNormalizado = CpfCnpjValidator.normalizar(request.documento());

        if (clienteRepository.existsByDocumento(docNormalizado)) {
            throw new RegraDeNegocioException("Já existe um cliente com o documento: " + docNormalizado);
        }

        Cliente cliente = Cliente.builder()
                .nome(request.nome())
                .documento(docNormalizado)
                .tipoDocumento(CpfCnpjValidator.detectarTipo(docNormalizado))
                .email(request.email())
                .telefone(request.telefone())
                .endereco(request.endereco())
                .build();

        ClienteResponse response = ClienteResponse.from(clienteRepository.save(cliente));
        log.info("Cliente criado id={}", response.id());
        return response;
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        log.info("Listando clientes");
        return clienteRepository.findAll().stream()
                .map(ClienteResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(Long id) {
        log.info("Buscando cliente id={}", id);
        return ClienteResponse.from(buscarEntidadePorId(id));
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorDocumento(String documento) {
        log.info("Buscando cliente documento={}", documento);
        String docNormalizado = CpfCnpjValidator.normalizar(documento);
        return clienteRepository.findByDocumento(docNormalizado)
                .map(ClienteResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado com documento: " + documento));
    }

    public ClienteResponse atualizar(Long id, ClienteRequest request) {
        log.info("Atualizando cliente id={}", id);
        Cliente cliente = buscarEntidadePorId(id);

        String docNormalizado = CpfCnpjValidator.normalizar(request.documento());
        if (!cliente.getDocumento().equals(docNormalizado)) {
            validarDocumento(request.documento());
            if (clienteRepository.existsByDocumento(docNormalizado)) {
                throw new RegraDeNegocioException("Já existe um cliente com o documento: " + docNormalizado);
            }
            cliente.setDocumento(docNormalizado);
            cliente.setTipoDocumento(CpfCnpjValidator.detectarTipo(docNormalizado));
        }

        cliente.setNome(request.nome());
        cliente.setEmail(request.email());
        cliente.setTelefone(request.telefone());
        cliente.setEndereco(request.endereco());

        return ClienteResponse.from(clienteRepository.save(cliente));
    }

    public void deletar(Long id) {
        log.info("Desativando cliente id={}", id);
        Cliente cliente = buscarEntidadePorId(id);
        cliente.setAtivo(false);
        clienteRepository.save(cliente);
    }

    public Cliente buscarEntidadePorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + id));
    }

    private void validarDocumento(String documento) {
        if (!CpfCnpjValidator.isValido(documento)) {
            throw new DocumentoInvalidoException("CPF/CNPJ inválido: " + documento);
        }
    }
}
