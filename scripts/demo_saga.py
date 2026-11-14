#!/usr/bin/env python3
"""
Percorre a saga da OS de ponta a ponta usando só as APIs REST dos três
microsserviços — o mesmo caminho que um usuário faria pelo Postman.

Uso:
  python3 scripts/demo_saga.py                       # caminho feliz, ambiente local (docker compose)
  python3 scripts/demo_saga.py --cenario falha-execucao
  python3 scripts/demo_saga.py --cenario pagamento-recusado
  python3 scripts/demo_saga.py --base https://<gateway>   # tudo pelo API Gateway no GCP

Cenários:
  feliz               orçamento → aprovação → pagamento → execução → entrega
  pagamento-recusado  compensação: peças voltam ao estoque e o orçamento é cancelado
  falha-execucao      compensação: peças voltam ao estoque e o pagamento é estornado
"""
import argparse
import json
import random
import sys
import time
import urllib.error
import urllib.request

PASSO = 0


def titulo(texto):
    global PASSO
    PASSO += 1
    print(f"\n\033[1m{PASSO:>2}. {texto}\033[0m")


def chamar(metodo, url, token=None, corpo=None, esperado=(200, 201, 202, 204)):
    dados = json.dumps(corpo).encode() if corpo is not None else None
    req = urllib.request.Request(url, data=dados, method=metodo)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            texto = resp.read().decode()
            status = resp.status
    except urllib.error.HTTPError as e:
        texto = e.read().decode()
        status = e.code
    if status not in esperado:
        print(f"   ✗ {metodo} {url} -> {status}: {texto}")
        sys.exit(1)
    return json.loads(texto) if texto else None


def cpf_valido():
    n = [random.randint(0, 9) for _ in range(9)]
    for _ in range(2):
        s = sum(v * p for v, p in zip(n, range(len(n) + 1, 1, -1)))
        d = 11 - s % 11
        n.append(0 if d >= 10 else d)
    return "".join(map(str, n))


def placa_mercosul():
    letras = "ABCDEFGHJKLMNPRSTUVWXYZ"
    return (random.choice(letras) + random.choice(letras) + random.choice(letras)
            + str(random.randint(0, 9)) + random.choice(letras) + f"{random.randint(0, 99):02d}")


def aguardar_status(api, token, os_id, esperado, limite=60):
    inicio = time.time()
    while time.time() - inicio < limite:
        status = chamar("GET", f"{api['os']}/api/ordens-servico/{os_id}", token)["status"]
        if status == esperado:
            print(f"   ✓ OS {os_id} agora está {status} ({time.time() - inicio:.1f}s)")
            return
        time.sleep(1)
    print(f"   ✗ OS {os_id} não chegou em {esperado} (está {status})")
    sys.exit(1)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--cenario", default="feliz", choices=["feliz", "pagamento-recusado", "falha-execucao"])
    parser.add_argument("--base", help="URL única (API Gateway). Sem isso usa as portas do docker compose")
    parser.add_argument("--usuario", default="admin")
    parser.add_argument("--senha", default="admin123")
    args = parser.parse_args()

    if args.base:
        api = {"os": args.base, "billing": args.base, "execucao": args.base}
    else:
        api = {"os": "http://localhost:8080", "billing": "http://localhost:8081", "execucao": "http://localhost:8082"}

    titulo("Login do funcionário (OS Service)")
    token = chamar("POST", f"{api['os']}/api/auth/login", corpo={"username": args.usuario, "password": args.senha})["token"]
    print("   ✓ token JWT obtido — vale para os três serviços")

    titulo("Cadastros: cliente, veículo, serviço e peça (OS Service)")
    cpf, placa = cpf_valido(), placa_mercosul()
    cliente = chamar("POST", f"{api['os']}/api/clientes", token,
                     {"nome": "Cliente Demonstração", "documento": cpf, "email": "cliente.demo@exemplo.com"})
    chamar("POST", f"{api['os']}/api/veiculos", token,
           {"placa": placa, "marca": "Volkswagen", "modelo": "Polo", "ano": 2023, "clienteId": cliente["id"]})
    servico = chamar("POST", f"{api['os']}/api/servicos", token,
                     {"nome": "Troca de pastilhas de freio", "preco": 180.00, "tempoEstimadoMinutos": 90})
    peca = chamar("POST", f"{api['os']}/api/pecas", token,
                  {"nome": "Pastilha de freio dianteira", "precoUnitario": 95.50, "quantidadeEstoque": 10})
    print(f"   ✓ cliente {cpf}, veículo {placa}, peça {peca['id']} com 10 em estoque")

    titulo("Abertura da OS e diagnóstico (OS Service)")
    os_ = chamar("POST", f"{api['os']}/api/ordens-servico", token, {
        "documentoCliente": cpf, "placaVeiculo": placa, "descricaoProblema": "Barulho ao frear",
        "servicos": [{"servicoId": servico["id"], "quantidade": 1}],
        "pecas": [{"pecaId": peca["id"], "quantidade": 2}]})
    os_id = os_["id"]
    chamar("POST", f"{api['os']}/api/ordens-servico/{os_id}/iniciar-diagnostico", token)
    print(f"   ✓ OS {os_['numero']} (id {os_id}) em diagnóstico, total R$ {os_['valorTotal']}")

    titulo("Mecânico pede o orçamento → início da saga")
    resposta = chamar("POST", f"{api['os']}/api/ordens-servico/{os_id}/gerar-orcamento", token)
    print(f"   ✓ HTTP 202, OS {resposta['status']} — comando GERAR_ORCAMENTO saiu pela outbox")
    aguardar_status(api, token, os_id, "AGUARDANDO_APROVACAO")

    titulo("Cliente consulta e aprova o orçamento (Billing Service)")
    orcamento = chamar("GET", f"{api['billing']}/api/orcamentos/ordem-servico/{os_id}", token)[0]
    print(f"   ✓ orçamento {orcamento['id']} de R$ {orcamento['valorTotal']} ({len(orcamento['itens'])} itens)")
    aprovado = chamar("POST", f"{api['billing']}/api/orcamentos/{orcamento['id']}/aprovacao", token,
                      {"aprovado": True, "observacao": "Pode fazer"})
    print(f"   ✓ aprovado — link de pagamento: {aprovado['pagamento']['linkPagamento']}")
    aguardar_status(api, token, os_id, "AGUARDANDO_PAGAMENTO")
    estoque = chamar("GET", f"{api['os']}/api/pecas/{peca['id']}", token)["quantidadeEstoque"]
    print(f"   ✓ peças reservadas: estoque agora {estoque}")

    titulo("Pagamento (Mercado Pago / simulação do webhook)")
    pago = chamar("POST", f"{api['billing']}/api/pagamentos/simulacao/{orcamento['id']}", token,
                  {"aprovado": args.cenario != "pagamento-recusado"})
    print(f"   ✓ orçamento {pago['status']}")

    if args.cenario == "pagamento-recusado":
        aguardar_status(api, token, os_id, "CANCELADA")
        mostrar_compensacao(api, token, os_id, peca["id"], orcamento["id"])
        return

    aguardar_status(api, token, os_id, "AGUARDANDO_EXECUCAO")

    titulo("Fila de execução (Execution Service)")
    execucao = None
    for _ in range(30):
        lista = chamar("GET", f"{api['execucao']}/api/execucoes/ordem-servico/{os_id}", token)
        if lista:
            execucao = lista[0]
            break
        time.sleep(1)
    print(f"   ✓ execução {execucao['id']} {execucao['status']}")
    chamar("POST", f"{api['execucao']}/api/execucoes/{execucao['id']}/diagnostico", token)
    aguardar_status(api, token, os_id, "EM_EXECUCAO")

    if args.cenario == "falha-execucao":
        titulo("Mecânico registra que não foi possível executar")
        chamar("POST", f"{api['execucao']}/api/execucoes/{execucao['id']}/falha", token,
               {"motivo": "Disco de freio empenado, precisa de peça que não temos"})
        aguardar_status(api, token, os_id, "CANCELADA")
        mostrar_compensacao(api, token, os_id, peca["id"], orcamento["id"])
        return

    chamar("POST", f"{api['execucao']}/api/execucoes/{execucao['id']}/reparo", token)
    chamar("POST", f"{api['execucao']}/api/execucoes/{execucao['id']}/finalizar", token,
           {"observacoes": "Pastilhas trocadas e freio testado"})
    aguardar_status(api, token, os_id, "FINALIZADA")

    titulo("Entrega do veículo (OS Service)")
    chamar("POST", f"{api['os']}/api/ordens-servico/{os_id}/entregar", token)
    aguardar_status(api, token, os_id, "ENTREGUE")
    mostrar_resumo(api, token, os_id)


def mostrar_compensacao(api, token, os_id, peca_id, orcamento_id):
    titulo("Compensação da saga")
    for _ in range(30):
        saga = chamar("GET", f"{api['os']}/api/ordens-servico/{os_id}/saga", token)[0]
        if saga["etapa"] == "COMPENSADA":
            break
        time.sleep(1)
    estoque = chamar("GET", f"{api['os']}/api/pecas/{peca_id}", token)["quantidadeEstoque"]
    orcamento = chamar("GET", f"{api['billing']}/api/orcamentos/{orcamento_id}", token)
    print(f"   ✓ saga {saga['etapa']}: {saga['motivoCompensacao']}")
    print(f"   ✓ estoque devolvido: {estoque}")
    print(f"   ✓ orçamento no Billing: {orcamento['status']}")
    mostrar_resumo(api, token, os_id)


def mostrar_resumo(api, token, os_id):
    titulo("Histórico da OS e passos da saga")
    for h in chamar("GET", f"{api['os']}/api/ordens-servico/{os_id}/historico", token):
        print(f"   {h['ocorridoEm'][11:19]}  {h['statusNovo']:<22} {h['origem'] or '':<18} {h['observacao'] or ''}")
    saga = chamar("GET", f"{api['os']}/api/ordens-servico/{os_id}/saga", token)[0]
    print(f"\n   saga {saga['id']} — etapa final {saga['etapa']}")
    for p in saga["passos"]:
        print(f"   {p['ocorridoEm'][11:19]}  {p['evento']:<22} {p['descricao']}")


if __name__ == "__main__":
    main()
