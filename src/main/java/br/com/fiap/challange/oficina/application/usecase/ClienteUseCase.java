package br.com.fiap.challange.oficina.application.usecase;

import br.com.fiap.challange.oficina.domain.exception.DocumentoInvalidoException;
import br.com.fiap.challange.oficina.domain.exception.RecursoNaoEncontradoException;
import br.com.fiap.challange.oficina.domain.exception.RegraDeNegocioException;
import br.com.fiap.challange.oficina.domain.model.Cliente;
import br.com.fiap.challange.oficina.domain.port.in.ClienteInputPort;
import br.com.fiap.challange.oficina.domain.port.in.command.ClienteCommand;
import br.com.fiap.challange.oficina.domain.port.out.ClienteRepositoryPort;
import br.com.fiap.challange.oficina.domain.validator.CpfCnpjValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ClienteUseCase implements ClienteInputPort {

    private final ClienteRepositoryPort clienteRepository;

    @Override
    public Cliente criar(ClienteCommand command) {
        log.info("Criando cliente nome={} documento={}", command.nome(), command.documento());
        validarDocumento(command.documento());
        String docNormalizado = CpfCnpjValidator.normalizar(command.documento());

        if (clienteRepository.existsByDocumento(docNormalizado)) {
            throw new RegraDeNegocioException("Já existe um cliente com o documento: " + docNormalizado);
        }

        Cliente cliente = Cliente.builder()
                .nome(command.nome())
                .documento(docNormalizado)
                .tipoDocumento(CpfCnpjValidator.detectarTipo(docNormalizado))
                .email(command.email())
                .telefone(command.telefone())
                .endereco(command.endereco())
                .build();

        Cliente salvo = clienteRepository.save(cliente);
        log.info("Cliente criado id={}", salvo.getId());
        return salvo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cliente> listar() {
        log.info("Listando clientes");
        return clienteRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Cliente buscarPorId(Long id) {
        log.info("Buscando cliente id={}", id);
        return buscarEntidadePorId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Cliente buscarPorDocumento(String documento) {
        log.info("Buscando cliente documento={}", documento);
        String docNormalizado = CpfCnpjValidator.normalizar(documento);
        return clienteRepository.findByDocumento(docNormalizado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado com documento: " + documento));
    }

    @Override
    public Cliente atualizar(Long id, ClienteCommand command) {
        log.info("Atualizando cliente id={}", id);
        Cliente cliente = buscarEntidadePorId(id);

        String docNormalizado = CpfCnpjValidator.normalizar(command.documento());
        if (!cliente.getDocumento().equals(docNormalizado)) {
            validarDocumento(command.documento());
            if (clienteRepository.existsByDocumento(docNormalizado)) {
                throw new RegraDeNegocioException("Já existe um cliente com o documento: " + docNormalizado);
            }
            cliente.setDocumento(docNormalizado);
            cliente.setTipoDocumento(CpfCnpjValidator.detectarTipo(docNormalizado));
        }

        cliente.setNome(command.nome());
        cliente.setEmail(command.email());
        cliente.setTelefone(command.telefone());
        cliente.setEndereco(command.endereco());

        return clienteRepository.save(cliente);
    }

    @Override
    public void deletar(Long id) {
        log.info("Desativando cliente id={}", id);
        Cliente cliente = buscarEntidadePorId(id);
        cliente.setAtivo(false);
        clienteRepository.save(cliente);
    }

    private Cliente buscarEntidadePorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + id));
    }

    private void validarDocumento(String documento) {
        if (!CpfCnpjValidator.isValido(documento)) {
            throw new DocumentoInvalidoException("CPF/CNPJ inválido: " + documento);
        }
    }
}
