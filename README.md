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
- Mapeamento inicial de erros de domínio para respostas HTTP.

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

## Execução local

Requisitos: JDK 25, Maven e Docker.

    docker compose up -d

A configuração local e o datasource precisam ser apontados para o PostgreSQL e o Flyway habilitado conforme .env.example. A configuração padrão atual ainda usa H2 em memória para o fluxo legado, então a execução da transferência exige configurar explicitamente PostgreSQL/Flyway antes de testar.

## Pendências antes de considerar pronto

- Configurar Spring Security / OAuth2 Resource Server com emissor JWT confiável.
- Garantir configuração local completa para PostgreSQL/Flyway e adicionar dados de sandbox de forma segura.
- Criar testes de integração com PostgreSQL real para saldo insuficiente, idempotência, concorrência e rollback.
- Executar mvn test com JDK 25 e corrigir qualquer falha de compilação/execução.
- Implementar publisher da outbox, endpoints de consulta, OpenAPI, rate limiting, métricas e logs estruturados.
- Fazer revisão de segurança independente antes de qualquer uso fora do sandbox.

Consulte docs/api-contract.md, docs/architecture.md e docs/production-readiness.md.
