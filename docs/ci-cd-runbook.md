# CI/CD Runbook

## Pipeline responsibilities

The `CI` workflow runs for pull requests, non-`main` branch pushes, and manual dispatches. It performs:

1. `Maven verification`
2. `Container build and vulnerability scan`

The container check runs only after Maven verification succeeds. Trivy blocks fixable `HIGH` or `CRITICAL` operating-system and library vulnerabilities.

The `Delivery` workflow runs for `main`, semantic version tags matching `v*.*.*`, and manual dispatches. It repeats Maven verification, builds and scans an image, and only then publishes to GitHub Container Registry (GHCR). Published images include provenance and an SBOM.

## Repository configuration

Configure branch protection or a ruleset for `main` with these exact required check names:

- `Maven verification`
- `Container build and vulnerability scan`

Require pull requests and prevent required-check bypass except for a documented emergency role. GitHub Actions must have permission to publish packages. Delivery grants `packages: write`, `attestations: write`, and `id-token: write` only to its publish job and authenticates with the scoped `GITHUB_TOKEN`.

## Image names and tags

Images are published to `ghcr.io/<owner>/<repository>` using a lower-case repository name. Expected tags are:

- `latest` for the default branch
- branch name where applicable
- `sha-<commit>` for immutable source traceability
- semantic version and major/minor variants for `v*.*.*` tags

Deployments should pin the published digest or `sha-<commit>` tag. Do not use `latest` as a production rollback reference.

## Creating a release

1. Confirm `main` is protected and green.
2. Choose a semantic version and review user-visible changes.
3. Create and push an annotated `vMAJOR.MINOR.PATCH` tag from the intended commit.
4. Monitor `Maven verification`, security scan, GHCR publication, provenance, and SBOM generation.
5. Record the image digest in the release or deployment change record.

The workflow publishes an artifact; it does not deploy it to a runtime environment.

## Maven verification failures

1. Open the `Maven verification` job and identify compilation, unit-test, integration-test, or Testcontainers failure.
2. Reproduce locally with `./mvnw -B -ntp clean verify` on Linux or `.\mvnw.cmd -B -ntp clean verify` on Windows.
3. Do not rerun repeatedly to hide an intermittent test. Capture the failing order and shared external state, especially Redis or PostgreSQL cleanup.
4. Push a fix and require a fresh green run.

## Vulnerability scan failures

1. Identify whether the finding belongs to the runtime OS, JRE, or an application dependency.
2. Prefer upgrading the direct dependency, Spring Boot patch line, or base image rather than adding an ignore.
3. If no fix exists, the current workflow ignores it through `ignore-unfixed`; do not introduce an additional suppression without a time-bounded security exception.
4. Rebuild from scratch when validating a base-image remediation so stale cache layers do not mask it.

## GHCR publication failures

Check, in order:

- The repository permits GitHub Actions to write packages.
- The publish job received `packages: write`.
- The normalized GHCR image name is valid.
- The tag or branch triggered `Delivery`, not only `CI`.
- GHCR or GitHub Actions is not experiencing an incident.

Never replace the scoped token with a long-lived registry password unless an external registry explicitly requires it and the credential is stored in a protected environment secret.

## Rollback

No automated runtime deployment or rollback exists yet. For a future deployment pipeline:

1. Record the previously healthy image digest before deployment.
2. Deploy immutable digests and gate promotion on readiness plus a functional redirect check.
3. Roll back by redeploying the recorded prior digest.
4. Treat Flyway migrations as forward-only. Add a compatible corrective migration instead of editing or deleting an applied migration.

Because the current migrations are additive, application rollback still requires checking schema compatibility. A published older image is not proof that it is safe against the current database schema.

## Dependency automation

Dependabot checks GitHub Actions, Maven dependencies, and Docker images weekly. Review grouped upgrades for compatibility, run the complete verification gate, and retain the same vulnerability scan before merging.
