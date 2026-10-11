# API contract

## POST /api/v1/transfers

Implemented sandbox endpoint for internal account-to-account transfers.

Headers:
- Authorization: Bearer <token> (the hosting environment must authenticate the request and populate the Java Principal; this repository does not yet configure a JWT issuer/resource server).
- Idempotency-Key: <unique key>
- Content-Type: application/json

Request:

    {"sourceAccountId":"uuid-da-conta-origem","destinationAccountId":"uuid-da-conta-destino","amount":25.90,"currency":"BRL"}

The authenticated principal name is mapped to customers.external_subject. The API does not accept a caller-supplied customer ID. The source account must belong to that customer. The destination account may belong to another customer, but must be active and use the same currency.

Responses:
- 201 Created: transfer committed.
- 400 Bad Request: invalid fields, currency or idempotency key.
- 401 Unauthorized: no principal or no registered customer identity.
- 409 Conflict: the same idempotency key was reused with a different request fingerprint.
- 422 Unprocessable Entity: insufficient funds or account not active.

The current adapter returns the original transfer for a matching idempotency key. A future API refinement should persist and replay the exact original HTTP status/body and explicitly mark replay metadata.

## Transaction guarantees implemented

The PostgreSQL adapter reserves the scoped idempotency key, locks both accounts in UUID order, checks ownership/status/currency/balance, updates both balances, inserts the payment, inserts matching debit/credit ledger entries, stores the idempotency response and inserts an outbox event in a single database transaction. Any runtime failure rolls back the transaction.

## Not yet production-ready

- Authentication provider/JWT resource-server configuration and account-level authorization policies.
- PostgreSQL integration and concurrency tests, including crash/retry scenarios.
- A background outbox publisher with retries and operational visibility.
- Read endpoints for payment status and account statements.
- Rate limiting, OpenAPI, metrics, structured audit logging and independent security review.
