package br.com.fiap.challange.oficina.application.usecase;

import br.com.fiap.challange.oficina.domain.exception.EstoqueInsuficienteException;
import br.com.fiap.challange.oficina.domain.model.ItemPecaOS;
import br.com.fiap.challange.oficina.domain.model.OrdemServico;
import br.com.fiap.challange.oficina.domain.model.Peca;
import br.com.fiap.challange.oficina.domain.port.out.PecaRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Reserva (baixa) e devolução de peças da OS. A reserva é um passo da saga
 * (feito quando o cliente aprova o orçamento); a devolução é a compensação.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EstoqueService {

    private final PecaRepositoryPort pecaRepository;

    public void validar(Peca peca, int quantidade) {
        if (peca.getQuantidadeEstoque() < quantidade) {
            throw new EstoqueInsuficienteException(String.format(
                    "Estoque insuficiente para '%s'. Disponível: %d, Solicitado: %d",
                    peca.getNome(), peca.getQuantidadeEstoque(), quantidade));
        }
    }

    /** Tudo ou nada: confere todas as peças antes de baixar qualquer uma. */
    public void reservar(OrdemServico os) {
        for (ItemPecaOS item : os.getItensPeca()) {
            validar(atual(item), item.getQuantidade());
        }
        for (ItemPecaOS item : os.getItensPeca()) {
            Peca peca = atual(item);
            peca.setQuantidadeEstoque(peca.getQuantidadeEstoque() - item.getQuantidade());
            pecaRepository.save(peca);
            log.info("Estoque reservado OS={} peça={} qtd={}", os.getNumero(), peca.getNome(), item.getQuantidade());
        }
    }

    public void devolver(OrdemServico os) {
        for (ItemPecaOS item : os.getItensPeca()) {
            Peca peca = atual(item);
            peca.setQuantidadeEstoque(peca.getQuantidadeEstoque() + item.getQuantidade());
            pecaRepository.save(peca);
            log.info("Estoque devolvido OS={} peça={} qtd={}", os.getNumero(), peca.getNome(), item.getQuantidade());
        }
    }

    private Peca atual(ItemPecaOS item) {
        // relê do banco: o objeto dentro da OS pode estar desatualizado
        return pecaRepository.findById(item.getPeca().getId()).orElse(item.getPeca());
    }
}
