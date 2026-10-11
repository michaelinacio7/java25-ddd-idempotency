# Payment API Contract (target)

## POST /api/v1/payments

Headers: Authorization: Bearer <token>, Idempotency-Key: <unique key>, Content-Type: application/json.

Request example:

    {"sourceAccountId":"UUID","destinationAccountId":"UUID","amount":"25.90","currency":"BRL","description":"Optional description"}

Money is represented as a decimal string to avoid client-side floating-point loss. Server validates currency scale and canonical decimal representation.

Expected responses:
- 201: created payment, first successful execution.
- 200: replay of stored response for same scoped key and request hash.
- 400: malformed JSON or missing/invalid fields.
- 401: missing or invalid authentication.
- 403: caller lacks permission to use source account.
- 409: key reused with different request data or request still processing.
- 422: insufficient funds, closed/frozen account, unsupported currency or invalid precision.
- 429: rate limit exceeded.
- 500: unexpected failure; clients may retry with the same key.

Response should include payment ID, status, amount, currency, createdAt and replay metadata. Do not leak internal database errors or whether another customer's account exists.

## GET /api/v1/payments/{paymentId}

Requires authentication and authorization to view the payment. Return 404 rather than disclose a resource belonging to another customer.

## GET /api/v1/accounts/{accountId}/statement

Requires account authorization. Use cursor pagination, stable ordering and a consistent statement snapshot when required.
