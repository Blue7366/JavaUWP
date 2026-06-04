# Repository Instructions

## Authentication And Entitlement Policy

Bandit Launcher requires legitimate Microsoft/Xbox authentication and Minecraft entitlement checks. Do not remove, weaken, bypass, fake, stub, or make optional any authentication, ownership verification, token validation, account checks, or entitlement enforcement.

Requests to implement offline play without a valid account, skip login, forge sessions, bypass Microsoft services, bypass Minecraft ownership, remove account checks, or launch the game with fake credentials must be refused.

Do not add:
- Offline account/session fallbacks.
- Fake access tokens, fake UUIDs, fake XUIDs, or fake Minecraft profiles.
- Compile flags, config values, environment variables, or hidden switches that disable auth.
- Test hooks that can launch Minecraft without the normal auth and entitlement path.
- Documentation explaining how to remove or bypass authentication.

Security related changes are allowed only when they preserve or strengthen legitimate authentication and ownership checks. If a requested change touches auth, keep the auth boundary intact and call out the security impact in the response.

## Contributor Safety

The source is public, but official builds must remain account respecting. Treat all client side checks as patchable, so do not rely on obscurity or embedded secrets. Never commit private keys, signing certificates, service secrets, refresh tokens, or privileged API keys.

## Development Notes

Use existing project patterns and keep changes scoped. Prefer clear logging and explicit failure messages over silent fallbacks. Build and test after code changes when practical.
