# Bank Payments API — Java 25, DDD e Idempotência

API de referência para transferências internas entre contas, com débito/crédito atômicos, ledger de partidas dobradas, idempotência persistida e evento outbox.

> **Sandbox educacional.** Não conecte contas reais, credenciais bancárias ou dinheiro real. A configuração de autenticação ainda precisa ser integrada a um provedor confiável. Não exponha a aplicação publicamente antes disso.

## Implementado nesta etapa

- POST /api/v1/transfers, com identidade do titular derivada do Principal autenticado, não do JSON enviado pelo cliente.
- Fingerprint SHA-256 canônico para a origem, destino, valor e moeda.
- Reserva concorrente da chave idempotente usando PostgreSQL ON CONFLICT DO NOTHING.
- Bloqueio pessimista das duas contas em ordem determinística.
- Validação de titularidade da origem, status das contas, moeda e saldo disponível.
- Débito e crédito no mesmo commit do PostgreSQL.
- Registro do pagamento e das duas partidas de ledger (DEBIT/CREDIT).
- Persistência da resposta idempotente e evento outbox na mesma transação.
- Testes de integração adicionados com PostgreSQL real via Testcontainers para concorrência, replay idempotente e conflito de payload.
- Workflow GitHub Actions para executar Maven com Java 25.

## Arquitetura

    HTTP controller -> application service -> TransferRepository port
                                                 |
                                                 v
                                      JdbcTransferRepository
                                                 |
                                                 v
                                      PostgreSQL transaction

## Modelo de dados

A migration Flyway V1__banking_ledger.sql define clientes, contas, pagamentos, transações/partidas do ledger, registros idempotentes e outbox.

## Executar testes

Requisitos: JDK 25, Maven e Docker em execução.

    mvn --batch-mode verify

Os testes de transferência sobem um PostgreSQL 16 descartável por meio do Testcontainers. O workflow .github/workflows/build.yml também executa essa verificação em pushes e pull requests. A execução precisa ser confirmada pelo resultado do CI; não marque o projeto como validado até que o workflow fique verde.

## Executar localmente

    docker compose up -d

A configuração local e o datasource precisam ser apontados para o PostgreSQL e o Flyway habilitado conforme .env.example. A configuração padrão ainda usa H2 em memória para o fluxo legado, então a execução da transferência exige configurar explicitamente PostgreSQL/Flyway antes de testar.

## Pendências antes de considerar pronto

- Configurar Spring Security / OAuth2 Resource Server com emissor JWT confiável.
- Garantir configuração local completa para PostgreSQL/Flyway e adicionar dados de sandbox de forma segura.
- Confirmar no CI os testes com PostgreSQL real, incluindo concorrência, idempotência e rollback.
- Implementar publisher da outbox, endpoints de consulta, OpenAPI, rate limiting, métricas e logs estruturados.
- Fazer revisão de segurança independente antes de qualquer uso fora do sandbox.

Consulte docs/api-contract.md, docs/architecture.md e docs/production-readiness.md.


## Projetos Saga Pattern

Também estão disponíveis dois laboratórios separados por diretório:

- [Saga com Temporal](saga-temporal-java/README.md): workflow durável com Temporal Java SDK e compensações.
- [Saga com Camunda 8](saga-camunda-java/README.md): orquestração BPMN 2.0 e workers Java.

Os dois exemplos estão em desenvolvimento e usam operações simuladas. Consulte os respectivos READMEs para requisitos e limitações.
