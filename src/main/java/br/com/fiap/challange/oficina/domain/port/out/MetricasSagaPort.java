package br.com.fiap.challange.oficina.domain.port.out;

public interface MetricasSagaPort {
    void sagaIniciada();
    void sagaConcluida();
    void sagaCompensada(String motivo);
}
