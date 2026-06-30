package br.com.fiap.challange.oficina.application.usecase;

import br.com.fiap.challange.oficina.domain.exception.RecursoNaoEncontradoException;
import br.com.fiap.challange.oficina.domain.model.Servico;
import br.com.fiap.challange.oficina.domain.port.in.ServicoInputPort;
import br.com.fiap.challange.oficina.domain.port.in.command.ServicoCommand;
import br.com.fiap.challange.oficina.domain.port.out.ServicoRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ServicoUseCase implements ServicoInputPort {

    private final ServicoRepositoryPort servicoRepository;

    @Override
    public Servico criar(ServicoCommand command) {
        log.info("Criando serviço nome={}", command.nome());
        Servico servico = Servico.builder()
                .nome(command.nome())
                .descricao(command.descricao())
                .preco(command.preco())
                .tempoEstimadoMinutos(command.tempoEstimadoMinutos() != null ? command.tempoEstimadoMinutos() : 60)
                .build();
        Servico salvo = servicoRepository.save(servico);
        log.info("Serviço criado id={}", salvo.getId());
        return salvo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Servico> listar() {
        log.info("Listando serviços");
        return servicoRepository.findByAtivoTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public Servico buscarPorId(Long id) {
        log.info("Buscando serviço id={}", id);
        return buscarEntidadePorId(id);
    }

    @Override
    public Servico atualizar(Long id, ServicoCommand command) {
        log.info("Atualizando serviço id={}", id);
        Servico servico = buscarEntidadePorId(id);
        servico.setNome(command.nome());
        servico.setDescricao(command.descricao());
        servico.setPreco(command.preco());
        if (command.tempoEstimadoMinutos() != null) {
            servico.setTempoEstimadoMinutos(command.tempoEstimadoMinutos());
        }
        return servicoRepository.save(servico);
    }

    @Override
    public void deletar(Long id) {
        log.info("Desativando serviço id={}", id);
        Servico servico = buscarEntidadePorId(id);
        servico.setAtivo(false);
        servicoRepository.save(servico);
    }

    private Servico buscarEntidadePorId(Long id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado: " + id));
    }
}
