package br.com.fiap.challange.oficina.service;

import br.com.fiap.challange.oficina.dto.request.AtualizarEstoqueRequest;
import br.com.fiap.challange.oficina.dto.request.PecaRequest;
import br.com.fiap.challange.oficina.dto.response.PecaResponse;
import br.com.fiap.challange.oficina.exception.RecursoNaoEncontradoException;
import br.com.fiap.challange.oficina.model.Peca;
import br.com.fiap.challange.oficina.repository.PecaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PecaService {

    private final PecaRepository pecaRepository;

    public PecaResponse criar(PecaRequest request) {
        log.info("Criando peça nome={}", request.nome());
        Peca peca = Peca.builder()
                .nome(request.nome())
                .descricao(request.descricao())
                .precoUnitario(request.precoUnitario())
                .quantidadeEstoque(request.quantidadeEstoque() != null ? request.quantidadeEstoque() : 0)
                .codigoReferencia(request.codigoReferencia())
                .build();
        PecaResponse response = PecaResponse.from(pecaRepository.save(peca));
        log.info("Peça criada id={}", response.id());
        return response;
    }

    @Transactional(readOnly = true)
    public List<PecaResponse> listar() {
        log.info("Listando peças");
        return pecaRepository.findByAtivoTrue().stream().map(PecaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PecaResponse buscarPorId(Long id) {
        log.info("Buscando peça id={}", id);
        return PecaResponse.from(buscarEntidadePorId(id));
    }

    public PecaResponse atualizar(Long id, PecaRequest request) {
        log.info("Atualizando peça id={}", id);
        Peca peca = buscarEntidadePorId(id);
        peca.setNome(request.nome());
        peca.setDescricao(request.descricao());
        peca.setPrecoUnitario(request.precoUnitario());
        peca.setCodigoReferencia(request.codigoReferencia());
        if (request.quantidadeEstoque() != null) {
            peca.setQuantidadeEstoque(request.quantidadeEstoque());
        }
        return PecaResponse.from(pecaRepository.save(peca));
    }

    public PecaResponse atualizarEstoque(Long id, AtualizarEstoqueRequest request) {
        log.info("Atualizando estoque peça id={} quantidade={}", id, request.quantidade());
        Peca peca = buscarEntidadePorId(id);
        peca.setQuantidadeEstoque(request.quantidade());
        return PecaResponse.from(pecaRepository.save(peca));
    }

    public void deletar(Long id) {
        log.info("Desativando peça id={}", id);
        Peca peca = buscarEntidadePorId(id);
        peca.setAtivo(false);
        pecaRepository.save(peca);
    }

    public Peca buscarEntidadePorId(Long id) {
        return pecaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada: " + id));
    }
}
