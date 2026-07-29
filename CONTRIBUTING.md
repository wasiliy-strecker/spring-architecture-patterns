# Contributing

Thank you for considering a contribution.

## Development workflow

1. Create a focused branch from `main`.
2. Keep changes inside the owning application module.
3. Run `./mvnw clean verify`.
4. Explain the business behavior and architectural impact in the pull request.

Pull requests should include tests for changed behavior and must not introduce
dependencies on another module's internal packages. Generated build output,
credentials, and production data must never be committed.
