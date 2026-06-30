package br.com.fiap.challange.oficina.application.usecase;

import br.com.fiap.challange.oficina.domain.exception.RecursoNaoEncontradoException;
import br.com.fiap.challange.oficina.domain.model.Peca;
import br.com.fiap.challange.oficina.domain.port.in.PecaInputPort;
import br.com.fiap.challange.oficina.domain.port.in.command.AtualizarEstoqueCommand;
import br.com.fiap.challange.oficina.domain.port.in.command.PecaCommand;
import br.com.fiap.challange.oficina.domain.port.out.PecaRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PecaUseCase implements PecaInputPort {

    private final PecaRepositoryPort pecaRepository;

    @Override
    public Peca criar(PecaCommand command) {
        log.info("Criando peça nome={}", command.nome());
        Peca peca = Peca.builder()
                .nome(command.nome())
                .descricao(command.descricao())
                .precoUnitario(command.precoUnitario())
                .quantidadeEstoque(command.quantidadeEstoque() != null ? command.quantidadeEstoque() : 0)
                .codigoReferencia(command.codigoReferencia())
                .build();
        Peca salva = pecaRepository.save(peca);
        log.info("Peça criada id={}", salva.getId());
        return salva;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Peca> listar() {
        log.info("Listando peças");
        return pecaRepository.findByAtivoTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public Peca buscarPorId(Long id) {
        log.info("Buscando peça id={}", id);
        return buscarEntidadePorId(id);
    }

    @Override
    public Peca atualizar(Long id, PecaCommand command) {
        log.info("Atualizando peça id={}", id);
        Peca peca = buscarEntidadePorId(id);
        peca.setNome(command.nome());
        peca.setDescricao(command.descricao());
        peca.setPrecoUnitario(command.precoUnitario());
        peca.setCodigoReferencia(command.codigoReferencia());
        if (command.quantidadeEstoque() != null) {
            peca.setQuantidadeEstoque(command.quantidadeEstoque());
        }
        return pecaRepository.save(peca);
    }

    @Override
    public Peca atualizarEstoque(Long id, AtualizarEstoqueCommand command) {
        log.info("Atualizando estoque peça id={} quantidade={}", id, command.quantidade());
        Peca peca = buscarEntidadePorId(id);
        peca.setQuantidadeEstoque(command.quantidade());
        return pecaRepository.save(peca);
    }

    @Override
    public void deletar(Long id) {
        log.info("Desativando peça id={}", id);
        Peca peca = buscarEntidadePorId(id);
        peca.setAtivo(false);
        pecaRepository.save(peca);
    }

    private Peca buscarEntidadePorId(Long id) {
        return pecaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada: " + id));
    }
}
