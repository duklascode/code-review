# PR #482 — Add refund endpoint to payment gateway

**Author:** payments-agent (AI-generated, requested by product)
**Target branch:** `main`
**Status:** CI green (all tests pass), awaiting human review

## Description

Adds `POST /payments/{id}/refunds` so merchants can refund a captured payment.

- Supports idempotent retries via the `Idempotency-Key` header
- Deducts a 1.5% processing fee from the refunded amount
- Rejects refunds larger than the captured amount
- Marks the payment as `REFUNDED`
- Publishes a `refund-events` message so downstream services (ledger, notifications) can react

## Context

- Stack: Java 21, Spring Boot, Spring WebFlux, Spring Data R2DBC (PostgreSQL), Google Pub/Sub
- Runs on Cloud Run, typically 20–50 instances at peak
- `refundRepo` and `paymentRepo` are reactive Spring Data repositories
- The change is in `RefundController.java` in this folder

## Your task

Review this PR as if it were going to production tomorrow.

1. List the problems you find, **most serious first**.
2. For the top issues, explain what would happen in production and how you'd fix it.
3. Say whether you would approve, request changes, or block.

You may use any AI tools you normally work with. If you do, tell us what the tool found and whether you agree with it.

Time: about 15 minutes to review, then we discuss.
