# Banking Payments Architecture

## Payment flow

1. Authenticate caller and resolve customer identity.
2. Validate idempotency key and canonical request hash.
3. Start a database transaction.
4. Reserve idempotency record under unique (customer_id, key).
5. Lock source and destination account rows in ascending ID order using SELECT FOR UPDATE.
6. Verify ownership, account status, currency, amount precision and sufficient funds.
7. Create payment and ledger transaction.
8. Insert balanced immutable ledger entries and update materialized balances in the same transaction.
9. Persist exact response and append outbox event.
10. Commit. Only after commit may an asynchronous publisher deliver the event.

Any failure rolls back the complete transaction. A retry must not create a second debit.

## Ledger invariants

- Amount is strictly positive.
- Every posted internal transfer has a source debit and destination credit for the same amount and currency.
- Total debits equal total credits.
- Posted entries cannot be edited or deleted.
- A reversal is a new compensating transaction linked to the original.
- Source balance cannot become negative unless a separately modeled credit facility permits it.

## Concurrency

Lock both account rows in deterministic order. Re-read and validate source balance after acquiring locks. Use database uniqueness constraints as the final defense for idempotency. Handle uniqueness races by rolling back and loading the winning record in a fresh transaction. Never continue using a transaction marked rollback-only.

## Idempotency

Persist customer scope, key, canonical request hash, state (IN_PROGRESS/COMPLETED/FAILED_RETRYABLE), response status/body, payment ID and timestamps. Same key plus same hash returns the original response. Same key plus different hash returns 409. Define a documented policy for in-progress requests and retain keys longer than the caller retry window.

## External payment rails

A call to an external processor cannot be atomic with a PostgreSQL transaction. Use an outbox and a state machine such as RECEIVED -> PROCESSING -> SUCCEEDED/FAILED/UNKNOWN. Use the processor's idempotency key. Reconcile UNKNOWN outcomes instead of blindly retrying a debit. Internal account transfers and external rail payments are different use cases.

## Security baseline

- Validate OAuth2/JWT issuer and audience.
- Enforce account-level authorization on every read and write.
- Use rate limits, abuse protection, TLS, redacted structured logs and correlation IDs.
- Store secrets in a secret manager, never in Git.
- Do not store card data or online banking credentials.
- Complete threat modeling, penetration testing and regulatory review before production use.
