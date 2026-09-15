# RFC-003 — Estratégia de autenticação de clientes por CPF

**Status:** Aceito · **Fase:** 3

## Contexto

O sistema já tinha autenticação de **funcionários** via usuário/senha + JWT (Fase 1/2,
`AuthUseCase`/`JwtService`). O enunciado da Fase 3 pede especificamente uma **rota de autenticação
para clientes usando apenas o CPF** (sem senha), servida por uma **function serverless** dedicada —
diferente do login de funcionário, que continua na aplicação principal.

## Opções consideradas

1. **Novo endpoint na aplicação principal** (`POST /api/auth/login-cliente`), reaproveitando o
   `AuthUseCase`/`JwtService` já existentes.
   - Prós: menos código novo, um único lugar para toda a lógica de autenticação.
   - Contras: não atende ao requisito explícito de que essa autenticação seja uma function serverless
     separada; acopla o ciclo de deploy da autenticação de cliente ao da aplicação principal.

2. **Identity Platform / Firebase Auth** com um provedor customizado.
   - Prós: solução gerenciada, com refresh token, revogação, etc.
   - Contras: over-engineering para "autenticar só com CPF" — não existe fluxo nativo de "documento
     sem senha"; exigiria uma Cloud Function de qualquer forma para validar o CPF e trocar por um
     token do Identity Platform, e depois **outra** tradução para o formato de JWT que a aplicação já
     valida. Dobra a complexidade sem resolver o problema mais rápido.

3. **Cloud Function dedicada** (`oficina-auth-function`, escolhida) que:
   - valida o formato/dígito verificador do CPF;
   - consulta diretamente o Cloud SQL para confirmar que existe um cliente ativo com aquele documento;
   - emite um JWT **compatível** com o `JwtService` da aplicação principal (mesma chave HMAC, mesmo
     algoritmo), com a claim `role=CLIENTE`.

## Decisão

Opção 3. A aplicação principal só precisou de um ajuste pontual: `JwtAuthenticationFilter` passou a
aceitar um token com `role=CLIENTE` sem tentar resolvê-lo contra o `UserDetailsService` de
funcionários (que não teria esse "usuário" cadastrado). Fora isso, o restante da autorização
(`SecurityConfig`, os endpoints protegidos) não muda — o token do cliente é um JWT como qualquer
outro, só que emitido por um serviço diferente.

Não usar senha para o cliente é uma decisão de produto do enunciado, não desta RFC — o CPF já é a
identificação única do cliente no domínio (`Cliente.documento`), então exigir uma segunda credencial
adicionaria fricção sem ganho de segurança proporcional para este caso de uso (consulta e
acompanhamento de OS, não operações administrativas).

## Consequências

- Dois emissores de JWT compartilham a mesma chave secreta (`JWT_SECRET`, no Secret Manager) — se essa
  chave precisar rotacionar, os dois lados (`oficina` e `oficina-auth-function`) precisam ser
  atualizados e reimplantados juntos.
- A function acessa o Cloud SQL diretamente via JDBC cru (sem passar pela aplicação principal nem por
  um port/adapter da Arquitetura Hexagonal) — é um trade-off aceito para manter a function pequena e
  sem depender do `oficina-app` estar no ar para autenticar; o preço é ter uma segunda implementação
  (fora deste repositório) que sabe o schema da tabela `clientes`.
- Não há refresh token nem revogação — o token do cliente expira e ele precisa se autenticar de novo
  com o CPF; suficiente para o caso de uso de consulta pública de status.
