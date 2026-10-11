# Bank Payments API — Java 25, DDD e Idempotência

API de referência para pagamentos entre contas bancárias internas, com débito e crédito atômicos, ledger de partidas dobradas e proteção contra solicitações duplicadas.

> **Sandbox educacional.** Não conecte contas reais, credenciais bancárias ou dinheiro real. Antes de qualquer uso real são necessários revisão independente de segurança, controles antifraude, compliance/regulatório, observabilidade, gestão de chaves e testes de carga/falha.

## Objetivo do domínio

Uma transferência de conta A para conta B deve ser uma única operação atômica:
- debitar A e creditar B na mesma transação PostgreSQL;
- rejeitar saldo insuficiente sem criar lançamentos;
- registrar lançamentos contábeis imutáveis e balanceados;
- impedir que retries com a mesma chave debitem novamente;
- rejeitar a mesma chave quando o payload for diferente;
- serializar operações concorrentes sobre a mesma conta com bloqueio de linha;
- manter trilha de auditoria e permitir conciliação independente.

## Arquitetura desejada

```text
HTTP/API -> application use cases -> domain model
                    |                    |
                    v                    v
             transaction ports     Money / Account / Ledger
                    |
                    v
          PostgreSQL adapters + Flyway
```

## Modelo de dados recomendado

- `customers`: identidade do titular, sem armazenar senhas em texto puro.
- `accounts`: titular, moeda, status e versão/saldo materializado protegido por transação.
- `payments`: comando de pagamento, origem, destino, valor, moeda, estado e timestamps.
- `ledger_transactions`: cabeçalho contábil.
- `ledger_entries`: lançamentos append-only, débito/crédito.
- `idempotency_records`: chave, hash do request, estado e resposta original.
- `outbox_events`: eventos a publicar depois do commit.

Todo pagamento interno deve gerar pelo menos dois lançamentos na mesma moeda. A soma dos débitos deve ser igual à soma dos créditos. Correções futuras devem ser lançadas como estornos compensatórios, nunca editando lançamentos antigos.

## API-alvo

### Criar uma transferência

```http
POST /api/v1/payments
Authorization: Bearer <sandbox-token>
Idempotency-Key: 9c1a6b4f-...
Content-Type: application/json

{
  "sourceAccountId": "uuid-da-conta-origem",
  "destinationAccountId": "uuid-da-conta-destino",
  "amount": "25.90",
  "currency": "BRL",
  "description": "Pagamento de teste"
}
```

Comportamento esperado:
- `201 Created`: pagamento criado e contabilizado.
- `200 OK`: replay de uma resposta previamente concluída para a mesma chave e payload.
- `409 Conflict`: chave reutilizada com payload diferente ou conflito de estado.
- `422 Unprocessable Entity`: saldo insuficiente ou conta indisponível.
- `401/403`: autenticação/autorização ausente ou insuficiente.

### Consultar pagamento

`GET /api/v1/payments/{paymentId}`

### Consultar extrato

`GET /api/v1/accounts/{accountId}/statement?limit=50&cursor=...`

O usuário autenticado só pode acessar contas às quais tem direito.

## Regras financeiras não negociáveis

1. **Dinheiro não usa `double` ou `float`.** Usar `BigDecimal` com escala e arredondamento explícitos; validar moeda e precisão.
2. **Saldo nunca pode ficar negativo** para contas sem limite de crédito.
3. **Uma única transação de banco** engloba idempotência, bloqueio das contas, verificação do saldo, pagamento, lançamentos, atualização dos saldos e evento outbox.
4. **Bloquear contas em ordem determinística** para reduzir deadlocks; revalidar saldo depois do bloqueio.
5. **Chave idempotente tem escopo por titular/cliente**, hash do payload e resposta original persistida.
6. **Ledger é append-only**: não atualizar ou excluir lançamentos contabilizados.
7. **Outbox transacional**: efeitos externos só são publicados após commit; consumidores precisam ser idempotentes.
8. **Autenticação não substitui autorização**: validar titularidade da conta de origem e permissões de cada recurso.
9. **Não guardar dados de cartão ou credenciais bancárias** neste projeto. Integrações externas devem usar provedores sandbox e segredos fora do Git.

## Estado do repositório

A base existente contém um primeiro fluxo de criação de pagamento e testes iniciais de idempotência. A expansão para o modelo bancário acima deve ser implementada e validada incrementalmente; este README descreve a arquitetura e os critérios de aceitação, não afirma que todos os recursos já estão implementados.

## Plano de implementação

- [ ] Migrar de H2 em memória para PostgreSQL e Flyway.
- [ ] Implementar Account, Money, Payment e Ledger como modelo de domínio.
- [ ] Implementar transferência atômica com lock pessimista e saldo insuficiente.
- [ ] Persistir lançamentos balanceados e imutáveis.
- [ ] Tornar idempotência segura sob concorrência e escopada ao titular.
- [ ] Adicionar Spring Security e autorização por conta.
- [ ] Adicionar outbox transacional e processamento retryable.
- [ ] Adicionar testes de integração com PostgreSQL real e testes concorrentes.
- [ ] Adicionar OpenAPI, Docker Compose, health checks, logs estruturados e métricas.
- [ ] Executar build, testes e análise de dependências antes de marcar release.

## Referências de design

A implementação deve ser guiada por propriedades verificáveis de ledger de partidas dobradas, idempotência persistente e concorrência transacional. Uma visão geral técnica útil: [ledger API com PostgreSQL e double-entry](https://github.com/gottostartsomewhere/ledger-api) e [ledger-core com invariantes de contabilidade](https://github.com/BBreadd/ledger-core).

## Executar a versão atual

Requisitos: JDK 25 e Maven.

```bash
mvn test
mvn spring-boot:run
```

A execução dos comandos acima ainda precisa ser feita num ambiente com JDK 25/Maven para confirmar o estado atual do build.
