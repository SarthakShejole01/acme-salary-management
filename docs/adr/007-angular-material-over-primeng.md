# ADR 007 — Angular Material instead of PrimeNG

**Status:** Accepted

## Context

The plan named PrimeNG as the component library. When the frontend was scaffolded (October 2026), every PrimeNG release on npm — including 19.x and 20.x — was published under the commercial *PrimeUI License*:

- a licence key is required, or the app may show a licence notice;
- the free Community licence only covers individuals and very small organisations, and must be renewed annually;
- an organisation like ACME (~10,000 employees) would need a paid per-developer licence.

## Decision

Use **Angular Material 22** (with `@angular/cdk`), MIT-licensed and released in lock-step with Angular.

- Employee list: `mat-table` + `mat-paginator` + `mat-sort` driven by server-side paging, sorting and filtering.
- Add/edit: `mat-dialog` with reactive forms and `mat-form-field` error messages.
- Charts are not part of Material. The salary-distribution histogram (plan item 23) will use a small MIT chart library or plain SVG — decided at that item, with the dependency justified then.

## Consequences

- No licence key, no annual renewal, no commercial exposure for the ACME scenario.
- Material has no built-in "lazy table" component; wiring paginator/sort events to the HTTP service is a few lines of our own code (which is also easier to test).
- Fewer ready-made widgets than PrimeNG (no chart, no built-in filter row); the UI stays deliberately simple.
- Rejected: PrimeNG under a Community licence — workable for the assessment, but it misrepresents what ACME could actually run, and adds a key to manage.
