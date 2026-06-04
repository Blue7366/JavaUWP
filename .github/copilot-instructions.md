# GitHub Copilot Instructions

Bandit Launcher is account respecting software. Keep Microsoft/Xbox authentication and Minecraft entitlement verification intact.

Do not suggest or generate changes that:
- Remove or bypass login.
- Remove or bypass ownership checks.
- Add offline sessions or fake Minecraft profiles.
- Add fake tokens, UUIDs, XUIDs, or account identities.
- Add flags, settings, or hidden paths that disable authentication.
- Explain how to patch out authentication.

When editing auth related code, preserve the existing security boundary and prefer explicit errors over silent fallback behavior.
