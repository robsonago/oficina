#!/usr/bin/env bash
set -euo pipefail

IMAGE="ghcr.io/corpp00429/oficina-app:latest"
CLUSTER_NAME="oficina"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

check_command() {
  if ! command -v "$1" &>/dev/null; then
    echo "  '$1' não encontrado. Instale antes de continuar."
    echo "   Docker : https://docs.docker.com/get-docker/"
    echo "   kind   : https://kind.sigs.k8s.io/docs/user/quick-start/#installation"
    echo "   kubectl: https://kubernetes.io/docs/tasks/tools/"
    exit 1
  fi
}

echo "=== Verificando dependências ==="
check_command docker
check_command kind
check_command kubectl
echo "Todas as dependências encontradas"

echo ""
echo "=== Cluster kind ==="
if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
  echo "Cluster '${CLUSTER_NAME}' já existe"
else
  echo "🔧 Criando cluster '${CLUSTER_NAME}'..."
  kind create cluster --name "${CLUSTER_NAME}" --config "${SCRIPT_DIR}/kind-config.yaml"
fi

KUBE_CTX="kind-${CLUSTER_NAME}"

echo ""
echo "=== Instalando metrics-server (necessário para HPA) ==="
kubectl --context "${KUBE_CTX}" apply -f \
  https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
kubectl --context "${KUBE_CTX}" patch deployment metrics-server \
  -n kube-system \
  --type='json' \
  -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'

echo ""
echo "=== Build da imagem Docker ==="
docker build -t "${IMAGE}" "${ROOT_DIR}"

echo ""
echo "=== Carregando imagem no cluster kind ==="
kind load docker-image "${IMAGE}" --name "${CLUSTER_NAME}"

echo ""
echo "=== Aplicando manifestos Kubernetes ==="
kubectl --context "${KUBE_CTX}" apply -f "${ROOT_DIR}/k8s/namespace.yaml"
kubectl --context "${KUBE_CTX}" apply -f "${ROOT_DIR}/k8s/configmap.yaml"
kubectl --context "${KUBE_CTX}" apply -f "${ROOT_DIR}/k8s/secret.yaml"
kubectl --context "${KUBE_CTX}" apply -f "${ROOT_DIR}/k8s/deployment-postgres.yaml"
kubectl --context "${KUBE_CTX}" apply -f "${ROOT_DIR}/k8s/service-postgres.yaml"
kubectl --context "${KUBE_CTX}" apply -f "${ROOT_DIR}/k8s/deployment-mailhog.yaml"
kubectl --context "${KUBE_CTX}" apply -f "${ROOT_DIR}/k8s/service-mailhog.yaml"
kubectl --context "${KUBE_CTX}" apply -f "${ROOT_DIR}/k8s/deployment-app.yaml"
kubectl --context "${KUBE_CTX}" apply -f "${ROOT_DIR}/k8s/service-app.yaml"
kubectl --context "${KUBE_CTX}" apply -f "${ROOT_DIR}/k8s/hpa.yaml"

echo ""
echo "=== Aguardando pods ficarem prontos ==="
echo "(pode levar até 3 minutos na primeira execução)"
kubectl --context "${KUBE_CTX}" -n oficina wait \
  --for=condition=ready pod -l app=postgres \
  --timeout=120s
kubectl --context "${KUBE_CTX}" -n oficina wait \
  --for=condition=ready pod -l app=oficina-app \
  --timeout=180s

echo ""
echo "=== Status dos pods ==="
kubectl --context "${KUBE_CTX}" -n oficina get pods

echo ""
echo "=== Status do HPA ==="
kubectl --context "${KUBE_CTX}" -n oficina get hpa

echo ""
echo " Ambiente pronto!"
echo ""
echo "   API:          http://localhost:30080"
echo "   Swagger UI:   http://localhost:30080/swagger-ui.html"
echo "   Health:       http://localhost:30080/actuator/health"
echo "   Mailhog UI:   http://localhost:30825"
echo ""
echo "Para derrubar o cluster:"
echo "   kind delete cluster --name ${CLUSTER_NAME}"
