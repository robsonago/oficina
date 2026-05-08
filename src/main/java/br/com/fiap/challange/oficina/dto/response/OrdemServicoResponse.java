package br.com.fiap.challange.oficina.dto.response;

import br.com.fiap.challange.oficina.model.OrdemServico;
import br.com.fiap.challange.oficina.model.enums.StatusOS;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrdemServicoResponse(
        Long id,
        String numero,
        Long clienteId,
        String nomeCliente,
        String documentoCliente,
        Long veiculoId,
        String placaVeiculo,
        String modeloVeiculo,
        StatusOS status,
        String statusDescricao,
        String descricaoProblema,
        String observacoes,
        BigDecimal valorTotal,
        LocalDateTime dataAbertura,
        LocalDateTime dataInicio,
        LocalDateTime dataFinalizacao,
        LocalDateTime dataEntrega,
        List<ItemServicoOSResponse> itensServico,
        List<ItemPecaOSResponse> itensPeca
) {
    public static OrdemServicoResponse from(OrdemServico os) {
        return new OrdemServicoResponse(
                os.getId(),
                os.getNumero(),
                os.getCliente().getId(),
                os.getCliente().getNome(),
                os.getCliente().getDocumento(),
                os.getVeiculo().getId(),
                os.getVeiculo().getPlaca(),
                os.getVeiculo().getMarca() + " " + os.getVeiculo().getModelo(),
                os.getStatus(),
                os.getStatus().getDescricao(),
                os.getDescricaoProblema(),
                os.getObservacoes(),
                os.getValorTotal(),
                os.getDataAbertura(),
                os.getDataInicio(),
                os.getDataFinalizacao(),
                os.getDataEntrega(),
                os.getItensServico().stream().map(ItemServicoOSResponse::from).toList(),
                os.getItensPeca().stream().map(ItemPecaOSResponse::from).toList()
        );
    }
}
