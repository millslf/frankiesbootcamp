# ADR 0001: Admin maintenance refresh uses competition context

## Status
Accepted

## Context

Persistent activity mode stores derived stats per `competition_athlete`. A refresh that skips competition context can no longer rebuild all athletes safely.

## Decision

Expose admin maintenance refreshes as competition-aware operations. A full refresh iterates active competitions and rebuilds each competition summary in turn.

## Consequences

- Admins can still trigger a full rebuild from one endpoint.
- Persistent reads stay aligned with the competition-scoped storage model.
- Maintenance work may take longer because each active competition is rebuilt explicitly.
