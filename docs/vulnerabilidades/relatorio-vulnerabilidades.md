# Relatório de Análise de Vulnerabilidades

## Sistema: Oficina Mecânica — Back-end (relatório vivo, reexecutado a cada fase)

**Histórico de reanálises deste documento:**

| Fase | Data | Versão da aplicação | O que mudou |
|---|---|---|---|
| Fase 1 (original) | 2026-05-04 | `0.0.1-SNAPSHOT` | Análise inicial — seções 1 a 8 abaixo |
| Fase 2 (reanálise) | 2026-07-03 | `2.0.0-SNAPSHOT` | Ver [seção 0](#0-reanálise--fase-2-2026-07-03) |

**Metodologia (ambas as reanálises):** Análise estática de código + auditoria de dependências (Maven
`dependency:list`)  
**Ferramenta complementar recomendada:** OWASP Dependency-Check, Snyk, Trivy

---

## 0. Reanálise — Fase 2 (2026-07-03)

> Esta seção documenta o que mudou desde a análise original da Fase 1 (seções 1-8 abaixo, mantidas
> intactas para rastreabilidade). Metodologia idêntica à da Fase 1: `./mvnw dependency:list` executado
> de verdade + releitura do código-fonte atual. Nenhum item foi corrigido a partir desta reanálise —
> assim como o relatório original, este documento só reporta achados.

### 0.1 O que mudou nas dependências

`./mvnw dependency:list` foi reexecutado no código atual (128 artefatos resolvidos, ante 115 na Fase 1
— parte da diferença de contagem também vem de uma versão mais nova do plugin
`maven-dependency-plugin`, que expande mais transitivos no relatório; não é indicativo de 13
dependências de risco novas). Mudanças reais identificadas:

| Artefato | Fase 1 | Fase 2 | O que significa |
|---|---|---|---|
| `org.springdoc:springdoc-openapi-starter-webmvc-ui` | 2.3.0 ⚠️ Desatualizado | **2.8.3** | Resolve o alerta de "versão desatualizada" da Fase 1 (recomendação P4 da seção 7) |
| `io.swagger.core.v3:swagger-*-jakarta` | 2.2.19 | 2.2.27 | Trazido junto pelo bump do springdoc acima |
| `org.webjars:swagger-ui` | 5.10.3 | 5.18.2 | Idem |
| `org.mapstruct:mapstruct` (+ `mapstruct-processor`) | — (não existia) | **1.6.3** (novo) | Adicionado para a conversão domínio ↔ entidade JPA no refactor hexagonal desta fase. `mapstruct-processor` é *annotation processor* (só gera código em tempo de compilação) — não entra no `.jar`/imagem Docker final, sem exposição em runtime |

Nenhum CVE crítico ou alto identificado nas versões novas (mesma metodologia da seção 2 abaixo).

### 0.2 Status de cada vulnerabilidade da Fase 1 — todas continuam ABERTAS

Nenhuma das 7 vulnerabilidades originais foi corrigida como efeito colateral do trabalho desta fase
(o foco da Fase 2 foi arquitetura/infraestrutura, não hardening de segurança). Confirmado lendo o
código atual, arquivo por arquivo:

| ID | Descrição | Status na Fase 2 |
|---|---|---|
| VUL-001 | Senha padrão `admin123` hardcoded no `DataInitializer` | 🔴 **Aberto** — e com um agravante novo, ver 0.3 abaixo |
| VUL-002 | Valor padrão do `JWT_SECRET` exposto em `application.yaml` | 🔴 **Aberto** — `application.yaml:37` inalterado |
| VUL-003 | Sem rate limiting no `/api/auth/login` | 🔴 **Aberto** — nenhuma dependência de rate limiting (`resilience4j`/`bucket4j`) foi adicionada |
| VUL-004 | CORS não configurado explicitamente | 🔴 **Aberto** — `SecurityConfig.java` continua sem bean de CORS |
| VUL-005 | Exceptions do JWT suprimidas silenciosamente | 🔴 **Aberto** — `JwtAuthenticationFilter.java:49-50` ainda é `catch (Exception ignored) {}` |
| VUL-006 | Swagger UI público sem controle por ambiente | 🔴 **Aberto** — `SecurityConfig.java:41` continua com `permitAll()` incondicional |
| VUL-007 | `open-in-view` habilitado (padrão) | 🔴 **Aberto** — `application.yaml` não define `spring.jpa.open-in-view: false` |

### 0.3 Achado novo — agravante do VUL-001: senha em texto puro no log

**Localização:** `infrastructure/config/DataInitializer.java:29`

```java
log.info("Usuário admin criado com sucesso. Senha padrão: admin123");
```

**Descrição:** além da senha padrão hardcoded (já era o VUL-001 original), a linha de log imprime o
valor da senha em texto puro no log da aplicação toda vez que o admin é criado. Isso soma um segundo
vetor de exposição: quem tiver acesso aos logs (inclusive agregadores de log em produção, se
configurados sem redação) vê a senha, mesmo sem acesso ao código-fonte.

**OWASP:** A09:2021 – Security Logging and Monitoring Failures (armazenamento de dado sensível em log)
combinado com A07:2021 (já cobria o VUL-001 original).

**Mitigação recomendada:** junto com a mitigação já sugerida no VUL-001 (externalizar a senha via
variável de ambiente obrigatória), remover o valor da senha da mensagem de log — logar só
`"Usuário admin criado com sucesso."`, sem o valor.

### 0.4 Achado da Fase 1 que foi corrigido nesta fase (fora do escopo deste relatório de segurança, mas vale registrar)

Durante a auditoria de qualidade geral desta fase (`atividades/validacao_completa_aplicacao.md`,
Correção 1), foi encontrado e corrigido um vazamento de informação interna independente das 7
vulnerabilidades acima: `GlobalExceptionHandler.handleGeneric` devolvia `ex.getMessage()` no corpo de
qualquer resposta HTTP 500 não mapeada — mensagens de exceção do Hibernate/JDBC frequentemente incluem
nomes de tabela/coluna e detalhes de schema (CWE-209 / OWASP A09:2021). Já corrigido: a resposta agora
é a mensagem genérica `"Erro interno no servidor"`, com o detalhe real só no log. Não é um item aberto
— citado aqui só para manter este relatório de segurança como o índice completo do que foi encontrado
e resolvido nesta fase.

### 0.5 Nota sobre segredos de infraestrutura (Kubernetes/Terraform)

Fora do escopo de "dependências e código Java", mas relevante para uma visão de segurança completa: os
manifestos `k8s/secret.yaml` e `infra/variables.tf` têm senha do banco (`oficina123`) e `JWT_SECRET`
versionados em texto puro no Git. Isso já foi analisado e documentado como decisão intencional (valores
só para demonstração local, nunca produção) em `docs/arquitetura/infraestrutura.md` (seção 3.3) e no
`README.md` (seção 14). Não é um achado novo desta reanálise, só um cross-reference para quem estiver
lendo este relatório isoladamente.

### 0.6 Resumo executivo da Fase 2

| Severidade | Fase 1 | Fase 2 | Delta |
|---|---|---|---|
| 🔴 Crítica | 0 | 0 | — |
| 🟠 Alta | 0 | 0 | — |
| 🟡 Média | 3 | 3 | — (todas ainda abertas) |
| 🔵 Baixa | 4 | 4 | — (todas ainda abertas) |
| ℹ️ Informativa | 3 | 2 | -1 (springdoc desatualizado foi resolvido) |
| Novo (0.3) | — | 1 | Agravante do VUL-001 (senha em log) |

**Conclusão da Fase 2:** o hardening de segurança em si não avançou nesta fase — o esforço foi
concentrado em arquitetura, infraestrutura e qualidade de código (ver `validacao_completa_aplicacao.md`
e `_v2.md`). As 7 vulnerabilidades da Fase 1 continuam válidas e as recomendações da seção 7 abaixo
permanecem como próximos passos recomendados antes de um ambiente de produção real.

---

## 1. Resumo Executivo

> *(Conteúdo original da análise da Fase 1, mantido intacto abaixo para rastreabilidade histórica —
> ver seção 0 acima para o que mudou na Fase 2.)*

| Severidade     | Quantidade |
|----------------|------------|
| 🔴 Crítica     | 0          |
| 🟠 Alta        | 0          |
| 🟡 Média       | 3          |
| 🔵 Baixa       | 4          |
| ℹ️ Informativa | 3          |

**Conclusão:** O projeto não apresenta vulnerabilidades críticas ou altas nas dependências auditadas. Todas as
bibliotecas de produção estão em versões recentes e mantidas ativamente. As vulnerabilidades identificadas são de nível
médio e baixo, concentradas em práticas de configuração e segurança defensiva, e podem ser mitigadas conforme detalhado
neste relatório.

---

## 2. Auditoria de Dependências

### 2.1 Dependências de Produção (compile/runtime)

| Artefato                                            | Versão      | Status           | CVEs conhecidos                                                 |
|-----------------------------------------------------|-------------|------------------|-----------------------------------------------------------------|
| `org.springframework.boot`                          | 3.4.1       | ✅ Seguro         | Nenhum crítico                                                  |
| `org.springframework.security`                      | 6.4.2       | ✅ Seguro         | Nenhum crítico                                                  |
| `org.springframework` (core/web)                    | 6.2.1       | ✅ Seguro         | Nenhum crítico                                                  |
| `org.apache.tomcat.embed`                           | 10.1.34     | ✅ Seguro         | Nenhum crítico                                                  |
| `org.hibernate.orm:hibernate-core`                  | 6.6.4.Final | ✅ Seguro         | Nenhum crítico                                                  |
| `org.hibernate.validator`                           | 8.0.2.Final | ✅ Seguro         | Nenhum crítico                                                  |
| `org.postgresql:postgresql`                         | 42.7.4      | ✅ Seguro         | Nenhum crítico                                                  |
| `com.fasterxml.jackson.core:jackson-databind`       | 2.18.2      | ✅ Seguro         | Nenhum crítico                                                  |
| `io.jsonwebtoken:jjwt-api`                          | 0.12.3      | ✅ Seguro         | Nenhum crítico                                                  |
| `org.flywaydb:flyway-core`                          | 10.20.1     | ✅ Seguro         | Nenhum crítico                                                  |
| `ch.qos.logback:logback-classic`                    | 1.5.12      | ✅ Seguro         | Nenhum crítico                                                  |
| `org.apache.logging.log4j:log4j-api`                | 2.24.3      | ✅ Seguro         | Apenas `log4j-api` (sem `log4j-core`) — imune a Log4Shell       |
| `com.zaxxer:HikariCP`                               | 5.1.0       | ✅ Seguro         | Nenhum crítico                                                  |
| `org.springdoc:springdoc-openapi-starter-webmvc-ui` | 2.3.0       | ⚠️ Desatualizado | Sem CVEs críticos; versão atual é 2.7.x                         |
| `org.yaml:snakeyaml`                                | 2.3         | ✅ Seguro         | Versões < 1.31 foram vulneráveis (CVE-2022-25857); 2.3 é seguro |
| `org.apache.commons:commons-lang3`                  | 3.17.0      | ✅ Seguro         | Nenhum crítico                                                  |

### 2.2 Dependências de Teste (test scope)

> Dependências de teste não compõem o artefato final (`jar`/`Docker image`) e não representam risco em produção.

| Artefato                   | Versão  | Observação                                              |
|----------------------------|---------|---------------------------------------------------------|
| `com.h2database:h2`        | 2.3.232 | Não exposto em produção                                 |
| `net.minidev:json-smart`   | 2.5.1   | CVE-2023-1370 afetou versões < 2.4.9; 2.5.1 está seguro |
| `org.mockito:mockito-core` | 5.14.2  | Apenas em testes                                        |
| `org.junit.jupiter`        | 5.11.4  | Apenas em testes                                        |

---

## 3. Análise de Código — Vulnerabilidades Identificadas

### 🟡 MÉDIA — VUL-001: Credenciais Padrão na Inicialização

**Localização:** `src/main/java/br/com/fiap/challange/oficina/infrastructure/config/DataInitializer.java`

**Descrição:**  
Um usuário administrador com senha `admin123` é criado automaticamente na primeira execução, caso não exista. Em
ambientes de produção, isso representa um risco real se o operador não trocar a senha após o primeiro deploy.

**OWASP:** A07:2021 – Identification and Authentication Failures

**Risco:** Um atacante que identifique o sistema como baseado neste projeto poderia tentar as credenciais padrão.

**Mitigação recomendada:**

```yaml
# application.yaml — adicionar variáveis de ambiente para credenciais iniciais
app:
  admin:
    username: ${ADMIN_USERNAME:admin}
    password: ${ADMIN_PASSWORD:}  # sem default — forçar configuração
```

E no `DataInitializer`, lançar exceção se `ADMIN_PASSWORD` não for definido.

---

### 🟡 MÉDIA — VUL-002: Valor Padrão do Segredo JWT Exposto

**Localização:** `src/main/resources/application.yaml`

```yaml
jwt:
  secret: ${JWT_SECRET:bXlTdXBlclNlY3JldEtleUZvckpXVFN5c3RlbU9maWNpbmFNZWNhbmljYTIwMjQ=}
```

**Descrição:**  
O segredo JWT possui um valor padrão em Base64 hardcoded no arquivo de configuração. Se o repositório for público ou
o `.jar` for descompilado, o segredo pode ser extraído, permitindo a forja de tokens JWT válidos.

**OWASP:** A02:2021 – Cryptographic Failures

**Risco:** Forja de tokens JWT com qualquer `username` e `role`, comprometendo todo o sistema de autenticação.

**Mitigação recomendada:**

- Remover o valor padrão; tornar `JWT_SECRET` obrigatório em produção.
- Documentar no `README.md` que a variável é mandatória.
- Usar segredo com pelo menos 256 bits (32 bytes) de entropia aleatória:

```bash
openssl rand -base64 32
```

---

### 🟡 MÉDIA — VUL-003: Ausência de Rate Limiting no Endpoint de Login

**Localização:** `POST /api/auth/login`

**Descrição:**  
O endpoint de autenticação não possui limitação de tentativas. Um atacante pode realizar ataques de força bruta ou
credential stuffing sem qualquer bloqueio.

**OWASP:** A07:2021 – Identification and Authentication Failures

**Risco:** Comprometimento de contas via força bruta; impacto em disponibilidade (DoS).

**Mitigação recomendada:**

- Adicionar `spring-boot-starter-actuator` + `resilience4j` para rate limiting.
- Ou configurar via nginx/API Gateway na camada de infraestrutura.
- Implementar bloqueio temporário após N tentativas falhas consecutivas.

---

### 🔵 BAIXA — VUL-004: CORS Não Configurado Explicitamente

**Localização:** `src/main/java/br/com/fiap/challange/oficina/infrastructure/config/SecurityConfig.java`

**Descrição:**  
Não há configuração de CORS (Cross-Origin Resource Sharing). O comportamento padrão do Spring Boot rejeita requisições
cross-origin, mas pode ser configurado inadvertidamente por outros beans.

**OWASP:** A05:2021 – Security Misconfiguration

**Mitigação recomendada:**

```java
// Adicionar ao SecurityConfig.java
http.cors(cors->cors.configurationSource(request->{
        CorsConfiguration config=new CorsConfiguration();
        config.setAllowedOrigins(List.of(System.getenv().getOrDefault("ALLOWED_ORIGINS","http://localhost:3000")));
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE"));
        config.setAllowedHeaders(List.of("Authorization","Content-Type"));
        return config;
        }));
```

---

### 🔵 BAIXA — VUL-005: Exceptions JWT Suprimidas Silenciosamente

**Localização:** `src/main/java/br/com/fiap/challange/oficina/infrastructure/security/JwtAuthenticationFilter.java`

```java
}catch(Exception ignored){
        }
```

**Descrição:**  
Qualquer erro no parse do JWT (token expirado, assinatura inválida, formato malformado) é silenciado. Isso dificulta a
detecção de tentativas de ataque e oculta erros legítimos de operação.

**OWASP:** A09:2021 – Security Logging and Monitoring Failures

**Mitigação recomendada:**

```java
}catch(ExpiredJwtException e){
        log.debug("Token JWT expirado: {}",e.getMessage());
        }catch(JwtException e){
        log.warn("Token JWT inválido: {}",e.getMessage());
        }
```

---

### 🔵 BAIXA — VUL-006: Swagger UI Disponível em Produção Sem Autenticação de Acesso

**Localização:** `SecurityConfig.java` — regras de acesso público

```java
.requestMatchers("/swagger-ui/**","/swagger-ui.html","/api-docs/**","/v3/api-docs/**").permitAll()
```

**Descrição:**  
A documentação da API (Swagger UI) é acessível publicamente, expondo todos os endpoints, seus parâmetros e modelos de
dados.

**OWASP:** A05:2021 – Security Misconfiguration

**Mitigação recomendada:**

```yaml
# Desabilitar em produção via perfil
springdoc:
  swagger-ui:
    enabled: ${SWAGGER_ENABLED:false}
  api-docs:
    enabled: ${SWAGGER_ENABLED:false}
```

---

### 🔵 BAIXA — VUL-007: Open-In-View Habilitado por Padrão

**Localização:** `src/main/resources/application.yaml`

**Descrição:**  
`spring.jpa.open-in-view=true` (padrão) mantém a sessão JPA aberta durante toda a requisição HTTP, podendo executar
queries durante a serialização de resposta, fora do contexto transacional. Isso aumenta a superfície de ataque
a `LazyInitializationException` encadeada.

**Risco:** Baixo, mas pode levar a vazamento de dados em respostas aninhadas não esperadas.

**Mitigação recomendada:**

Na Fase 2, com a migração para Arquitetura Hexagonal, identificou-se que habilitar
`open-in-view: false` exige que o mapeamento entidade → DTO ocorra **dentro** da transação
(no use case), e não no controller/adapter de entrada:

```yaml
spring:
  jpa:
    open-in-view: false
```

Se `open-in-view: false` for habilitado, garantir que toda associação lazy (`LAZY FetchType`)
seja carregada dentro do `@Transactional` do use case antes de retornar a entidade ao controller.
Alternativa: mapear entidade → DTO dentro da transação.

---

### Nota — Caminhos de Arquivo Atualizados na Fase 2

Com a migração para Arquitetura Hexagonal, as classes de configuração e segurança foram
movidas para dentro do pacote `infrastructure/`:

| Classe                     | Caminho Fase 1                       | Caminho Fase 2                                      |
|----------------------------|----------------------------------------|-------------------------------------------------------|
| `SecurityConfig`          | `config/SecurityConfig.java`          | `infrastructure/config/SecurityConfig.java`          |
| `JwtService`               | `security/JwtService.java`            | `infrastructure/security/JwtService.java`            |
| `JwtAuthenticationFilter` | `security/JwtAuthenticationFilter.java` | `infrastructure/security/JwtAuthenticationFilter.java` |
| `DataInitializer`         | `config/DataInitializer.java`         | `infrastructure/config/DataInitializer.java`         |

---

## 4. Análise de Segurança da Implementação JWT

| Aspecto                | Implementação                                  | Status                       |
|------------------------|------------------------------------------------|------------------------------|
| Algoritmo              | HS256                                          | ✅ Aceito — mínimo 256 bits   |
| Tamanho do segredo     | ≥ 256 bits (base64 decodificado)               | ✅                            |
| Expiração              | Configurável via `JWT_EXPIRATION` (padrão 24h) | ✅                            |
| Validação de expiração | Sim, via `isTokenExpired()`                    | ✅                            |
| Armazenamento de senha | BCrypt (fator 10)                              | ✅ Adequado                   |
| Stateless              | Sim — sem sessão no servidor                   | ✅                            |
| HTTPS                  | Responsabilidade do proxy reverso / infra      | ⚠️ Documentar                |
| Claims validados       | `subject` + `expiration`                       | ✅                            |
| `iss`/`aud` claims     | Não implementado                               | ℹ️ Recomendado para produção |
| Refresh token          | Não implementado                               | ℹ️ Recomendado para produção |

---

## 5. Análise de Proteção contra OWASP Top 10 (2021)

| Categoria OWASP                 | Status              | Detalhe                                                               |
|---------------------------------|---------------------|-----------------------------------------------------------------------|
| A01 – Broken Access Control     | ✅ Protegido         | Spring Security + JWT + roles (`@PreAuthorize`)                       |
| A02 – Cryptographic Failures    | ⚠️ Parcial          | BCrypt para senhas ✅; segredo JWT com default ⚠️ (VUL-002)            |
| A03 – Injection                 | ✅ Protegido         | JPA/Hibernate + queries parametrizadas; sem SQL nativo raw            |
| A04 – Insecure Design           | ✅ Adequado para MVP | Fluxo de OS com transições validadas no domínio                       |
| A05 – Security Misconfiguration | ⚠️ Parcial          | CORS indefinido (VUL-004); Swagger público (VUL-006)                  |
| A06 – Vulnerable Components     | ✅ Protegido         | Todas as deps em versões recentes e sem CVEs críticos                 |
| A07 – Auth Failures             | ⚠️ Parcial          | JWT implementado ✅; sem rate limiting no login ⚠️ (VUL-003)           |
| A08 – Software & Data Integrity | ✅ Protegido         | Flyway para migrações controladas; imagem Docker com usuário não-root |
| A09 – Security Logging          | ⚠️ Parcial          | Logging básico presente; JWT errors suprimidos (VUL-005)              |
| A10 – SSRF                      | ✅ N/A               | Aplicação não realiza requisições a URLs externas                     |

---

## 6. Boas Práticas de Segurança Implementadas

| Prática                                             | Status                                      |
|-----------------------------------------------------|---------------------------------------------|
| Senhas com BCrypt (fator 10)                        | ✅                                           |
| Autenticação Stateless (JWT)                        | ✅                                           |
| Retorno HTTP 401 para não autenticados              | ✅ (corrigido no `AuthenticationEntryPoint`) |
| Soft delete (preservação de histórico)              | ✅                                           |
| Validação de entrada com Bean Validation            | ✅                                           |
| Validação de CPF/CNPJ com algoritmo oficial         | ✅                                           |
| Validação de placa de veículo                       | ✅                                           |
| CSRF desabilitado (correto para API REST stateless) | ✅                                           |
| Sessão STATELESS (sem cookies de sessão)            | ✅                                           |
| Dockerfile com usuário não-root (`spring:spring`)   | ✅                                           |
| Multi-stage build no Dockerfile (imagem enxuta)     | ✅                                           |
| Sem SQL nativo (ORM Hibernate)                      | ✅                                           |
| Variáveis de ambiente para configurações sensíveis  | ✅ (parcial — ver VUL-002)                   |
| Endpoint público de status da OS (mínimo acesso)    | ✅                                           |

---

## 7. Recomendações Prioritizadas

| Prioridade | Ação                                                                              |
|------------|-----------------------------------------------------------------------------------|
| 🔴 P1      | Remover valor padrão do `JWT_SECRET` e torná-lo obrigatório em produção (VUL-002) |
| 🔴 P1      | Externalizar senha do admin inicial via variável de ambiente (VUL-001)            |
| 🟡 P2      | Implementar rate limiting no endpoint `/api/auth/login` (VUL-003)                 |
| 🟡 P2      | Desabilitar Swagger UI em produção via perfil Spring (VUL-006)                    |
| 🟡 P2      | Configurar CORS explicitamente com origens permitidas (VUL-004)                   |
| 🔵 P3      | Registrar exceções JWT com `log.warn` em vez de ignorar (VUL-005)                 |
| 🔵 P3      | Definir `spring.jpa.open-in-view=false` em `application.yaml` (VUL-007)           |
| ℹ️ P4      | Atualizar `springdoc-openapi` para versão 2.7.x                                   |
| ℹ️ P4      | Implementar `iss`/`aud` claims no JWT para produção                               |
| ℹ️ P4      | Implementar refresh token para maior segurança de sessão                          |

---

## 8. Dependências — Lista Completa (115 artefatos resolvidos)

```
ch.qos.logback:logback-classic:1.5.12
ch.qos.logback:logback-core:1.5.12
com.fasterxml.jackson.core:jackson-annotations:2.18.2
com.fasterxml.jackson.core:jackson-core:2.18.2
com.fasterxml.jackson.core:jackson-databind:2.18.2
com.fasterxml.jackson.dataformat:jackson-dataformat-toml:2.18.2
com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.2
com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.18.2
com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2
com.fasterxml.jackson.module:jackson-module-parameter-names:2.18.2
com.fasterxml:classmate:1.7.0
com.h2database:h2:2.3.232 [test]
com.jayway.jsonpath:json-path:2.9.0 [test]
com.zaxxer:HikariCP:5.1.0
io.jsonwebtoken:jjwt-api:0.12.3
io.jsonwebtoken:jjwt-impl:0.12.3
io.jsonwebtoken:jjwt-jackson:0.12.3
io.micrometer:micrometer-commons:1.14.2
io.micrometer:micrometer-observation:1.14.2
io.smallrye:jandex:3.2.0
io.swagger.core.v3:swagger-annotations-jakarta:2.2.19
io.swagger.core.v3:swagger-core-jakarta:2.2.19
io.swagger.core.v3:swagger-models-jakarta:2.2.19
net.minidev:json-smart:2.5.1 [test]
org.antlr:antlr4-runtime:4.13.0
org.apache.commons:commons-lang3:3.17.0
org.apache.logging.log4j:log4j-api:2.24.3
org.apache.tomcat.embed:tomcat-embed-core:10.1.34
org.apache.tomcat.embed:tomcat-embed-el:10.1.34
org.apache.tomcat.embed:tomcat-embed-websocket:10.1.34
org.flywaydb:flyway-core:10.20.1
org.flywaydb:flyway-database-postgresql:10.20.1
org.hibernate.orm:hibernate-core:6.6.4.Final
org.hibernate.validator:hibernate-validator:8.0.2.Final
org.postgresql:postgresql:42.7.4
org.projectlombok:lombok:1.18.36
org.slf4j:slf4j-api:2.0.16
org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0
org.springframework.boot:spring-boot:3.4.1
org.springframework.security:spring-security-config:6.4.2
org.springframework.security:spring-security-core:6.4.2
org.springframework.security:spring-security-crypto:6.4.2
org.springframework.security:spring-security-web:6.4.2
org.springframework:spring-core:6.2.1
org.springframework:spring-webmvc:6.2.1
org.webjars:swagger-ui:5.10.3
org.yaml:snakeyaml:2.3
[... + 68 dependências transitivas — todas em versões suportadas]
```

---

*Relatório gerado por análise estática em 2026-05-04. Para análise dinâmica completa, recomenda-se executar:*

```bash
# OWASP Dependency-Check
./mvnw org.owasp:dependency-check-maven:check

# Snyk CLI
snyk test --all-projects

# Trivy (imagem Docker)
trivy image oficina-mecanica:latest
```
