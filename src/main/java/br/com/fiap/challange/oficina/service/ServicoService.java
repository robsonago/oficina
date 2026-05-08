package br.com.fiap.challange.oficina.service;

import br.com.fiap.challange.oficina.dto.request.ServicoRequest;
import br.com.fiap.challange.oficina.dto.response.ServicoResponse;
import br.com.fiap.challange.oficina.exception.RecursoNaoEncontradoException;
import br.com.fiap.challange.oficina.model.Servico;
import br.com.fiap.challange.oficina.repository.ServicoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ServicoService {

    private final ServicoRepository servicoRepository;

    public ServicoResponse criar(ServicoRequest request) {
        log.info("Criando serviço nome={}", request.nome());
        Servico servico = Servico.builder()
                .nome(request.nome())
                .descricao(request.descricao())
                .preco(request.preco())
                .tempoEstimadoMinutos(request.tempoEstimadoMinutos() != null ? request.tempoEstimadoMinutos() : 60)
                .build();
        ServicoResponse response = ServicoResponse.from(servicoRepository.save(servico));
        log.info("Serviço criado id={}", response.id());
        return response;
    }

    @Transactional(readOnly = true)
    public List<ServicoResponse> listar() {
        log.info("Listando serviços");
        return servicoRepository.findByAtivoTrue().stream().map(ServicoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ServicoResponse buscarPorId(Long id) {
        log.info("Buscando serviço id={}", id);
        return ServicoResponse.from(buscarEntidadePorId(id));
    }

    public ServicoResponse atualizar(Long id, ServicoRequest request) {
        log.info("Atualizando serviço id={}", id);
        Servico servico = buscarEntidadePorId(id);
        servico.setNome(request.nome());
        servico.setDescricao(request.descricao());
        servico.setPreco(request.preco());
        if (request.tempoEstimadoMinutos() != null) {
            servico.setTempoEstimadoMinutos(request.tempoEstimadoMinutos());
        }
        return ServicoResponse.from(servicoRepository.save(servico));
    }

    public void deletar(Long id) {
        log.info("Desativando serviço id={}", id);
        Servico servico = buscarEntidadePorId(id);
        servico.setAtivo(false);
        servicoRepository.save(servico);
    }

    public Servico buscarEntidadePorId(Long id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado: " + id));
    }
}
