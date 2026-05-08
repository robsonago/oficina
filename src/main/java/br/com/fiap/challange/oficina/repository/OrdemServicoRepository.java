package br.com.fiap.challange.oficina.repository;

import br.com.fiap.challange.oficina.model.OrdemServico;
import br.com.fiap.challange.oficina.model.enums.StatusOS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {
    Optional<OrdemServico> findByNumero(String numero);

    List<OrdemServico> findByStatus(StatusOS status);

    List<OrdemServico> findByStatusIn(List<StatusOS> statuses);

    List<OrdemServico> findByClienteId(Long clienteId);

    List<OrdemServico> findByVeiculoId(Long veiculoId);

    @Query("SELECT o FROM OrdemServico o WHERE o.status IN ('FINALIZADA', 'ENTREGUE') AND o.dataInicio IS NOT NULL AND o.dataFinalizacao IS NOT NULL")
    List<OrdemServico> findFinalizadasComTempo();
}
