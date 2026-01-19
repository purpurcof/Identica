# Identica Plan (Velocity-first)

## 0) Reference check (done)
I reviewed the existing Socialismus module loader and module.json format, plus Bubbler/Chirper multi-module layout and bootstrap shading setup. This plan mirrors those patterns where appropriate.

## 1) Goals and constraints
- Velocity implementation first, but keep platform-agnostic core and API to add more platforms later.
- Provider-based auth: providers are separate jars dropped into `providers/`, each with its own config/working folder.
- Support CRACKED + PREMIUM providers initially; allow more providers later.
- Support future external identity providers (e.g., Discord bot, website) so users can link multiple login methods.
- Custom Identica UUIDs to avoid overlap between account types.
- SQL storage (SQLite default) in a separate Gradle module; MySQL + PostgreSQL supported.
- Toggleable Redis sync in a separate Gradle module.
- Java 25, Lombok, Guice, Configura, Dialectica, Attache, Keystone, Commandant.

## 2) Proposed repository layout (mirrors Socialismus/Bubbler/Chirper)
- `identica-api`
  - Public API, models, provider SPI, events, types (account, auth result, provider type, conflicts).
- `identica-common`
  - Core logic: provider registry, auth pipeline, UUID strategy, conflict resolver, session service, cache, rate limiter.
- `identica-adapter-command`
  - Core admin commands via Commandant (reload, provider management).
  - Provides **Identica custom command annotations** + annotation parser (mirrors Oraylen) so modules don’t depend on Cloud annotations directly.
  - Provider-specific commands live in provider modules.
- `identica-platform:platform-velocity` (bootstrap)
  - Velocity plugin entrypoint; wires DI, loads config, registers listeners/commands.
  - Builds the shaded plugin jar similar to `bubbler-bootstrap`/`chirper-bootstrap`.
- `identica-provider:provider-cracked` (provider id: `Cracked`)
  - Provider jar with its own `provider.json` and config defaults.
- `identica-provider:provider-premium` (provider id: `Premium`)
  - Provider jar with its own `provider.json` and config defaults.
- `identica-adapter-database`
  - Dialectica-backed storage (SQLite/MySQL/Postgres) + migrations.
- `identica-adapter-synchronization`
  - Redis pub/sub + cache/session replication.

## 3) Provider system (modeled after Socialismus modules)
### 3.1 Provider descriptor
A `provider.json` inside each provider jar (similar to module.json):
- `id`, `name`, `version`, `main`
- `authors`
- `supportedPlatforms`
- `priorityDefault`
- **No dependencies declared in the provider descriptor.**

### 3.2 Provider lifecycle
`IdenticaProvider` (abstract) with lifecycle hooks:
- `onLoad()`, `onEnable()`, `onDisable()`, `onUnload()`
- Injected via Guice, gets `workingPath` set to `providers/<id>/`.

### 3.3 Provider loading
`ProviderManager`:
- Scans `providers/` for jars, reads `provider.json`.
- Loads providers with isolated `URLClassLoader` (same as Socialismus ModuleManager).
- Applies priority + enabled flags from `providers/providers.yml`.
- Maintains a registry for the auth pipeline to query.

## 4) Configuration layout (Configura)
- Default format: **YAML**, optional **JSON** (same as Socialismus).
- `settings.yml` (main settings)
  - provider-agnostic settings: routing + optional platform-specific options.
- `commands.yml` (core commands)
  - command definitions for core/admin commands (aliases, permissions, cooldowns, etc.).
- `synchronization.yml` (synchronization-only)
  - Redis connection + channel settings.
  - Toggle + server identity for sync.
- `database.yml` (database-only)
  - SQLite/MySQL/PostgreSQL settings.
- `providers/providers.yml`
  - list of providers with `id`, `enabled`, `priority`.
  - conflict keys + actions (by provider id).
- `providers/<id>/config.yml` (e.g., `providers/Cracked/config.yml`)
  - provider-specific settings (e.g., cracked credential rules + login/session, premium verification + Mojang API URLs, fallback password).
- `providers/<id>/messages.yml` (optional, e.g., `providers/Cracked/messages.yml`)
  - provider-specific messages only (if needed).
- `providers/<id>/commands.yml` (optional, e.g., `providers/Cracked/commands.yml`)
  - provider-specific command definitions (same schema as core commands).

### 4.2 Config provider pattern (same as Socialismus)
- Use `ConfigProvider<T>` + `DefaultConfigProvider<T>` adapters with Configura templates.
- All config providers implement `Reloadable` and register into a reloadable registry.
- `identica-adapter-command` reload command iterates the registry (same pattern as Socialismus).
- Each provider ships:
  - **Config provider** (loads `providers/<id>/config.yml`)
  - **Template provider** (default config generation)
  - Optional **Messages provider + template** (loads `providers/<id>/messages.yml`)
  - Optional **Commands provider + template** (loads `providers/<id>/commands.yml`)

### 4.1 Example configs (YAML)
Main `settings.yml` (base + platform overrides):
```yaml
routing:
  default: "lobby"
  fallback: "auth"
  steps:
    preLogin: "auth"
    auth: "auth"
    postAuth: "lobby"
# Destination interpretation is handled by the active platform.

platform: {}
```

Database `database.yml` (polymorphic Configura; `type` controls fields):
```yaml
enabled: true
type: SQLITE # SQLITE | MYSQL | POSTGRES

sqlite:
  file: identica.db

sql:
  host: localhost
  port: 3306
  database: identica
  username: identica
  password: ""

hikari:
  poolName: "Identica"
  maximumPoolSize: 10
  minimumIdle: 2
  connectionTimeout: 30000
  idleTimeout: 600000
  maxLifetime: 1800000
```

Synchronization `synchronization.yml`:
```yaml
enabled: true
serverId: "" # empty -> auto-generate UUID, or set a custom string
redis:
  host: localhost
  port: 6379
  password: ""
  channels:
    accountUpdates: "identica:accounts"
    sessions: "identica:sessions"
    conflicts: "identica:conflicts"
```

Providers list `providers/providers.yml`:
```yaml
conflicts:
  keys: ["username"]
  defaultActions: ["allow"]
  policies:
    - when: ["Premium", "Cracked"]
      key: "username"
      actions: ["allow", "kick_existing", "rename_existing"]
    - when: ["Cracked", "Cracked"]
      key: "username"
      actions: ["deny"]

providers:
  - id: Cracked
    enabled: true
    priority: 50
  - id: Premium
    enabled: true
    priority: 100
```

Cracked provider `providers/Cracked/config.yml`:
```yaml
register:
  enabled: true
  requireRepeat: true

login:
  maxAttempts: 5
  lockSeconds: 300
  session:
    autoLoginSeconds: 1209600
    
credentials:
  username:
    minLength: 3
    maxLength: 16
    allowedPattern: "^[a-zA-Z0-9_]+$"
  password:
    requirements:
      minLength: 6
      maxLength: 64
      minUpper: 0
      minLower: 1
      minNumber: 1
      minSpecial: 0
    hashing:
      algorithm: argon2 # or bcrypt
      rehashOnLogin: true

conflict:
  prefix: "CR_"
  suffix: ""
  applyOnConflict: true
```

Premium provider `providers/Premium/config.yml`:
```yaml
mode: verify_only # verify_only | prompt
method: profile_lookup # profile_lookup | session_server
fallback:
  allowLocalPassword: true
  denyIfUnavailable: true
api:
  endpoints:
    - kind: profile
      url: "https://api.mojang.com/users/profiles/minecraft/%s"
    - kind: session
      url: "https://sessionserver.mojang.com/session/minecraft/hasJoined"
  failover:
    mode: sequential # sequential | random
    rotateOn: ["timeout", "rate_limit", "5xx"]
  timeoutMs: 3000
  retries: 1
  cache:
    positiveTtlSeconds: 3600   # cache premium/verified results
    negativeTtlSeconds: 600    # cache "not premium/not found" results to reduce API calls
```

Provider messages (examples, optional):
- `providers/Cracked/messages.yml`: register/login prompts, errors, lockouts, password rules.
- `providers/Premium/messages.yml`: verification failed/unavailable, fallback available, rate-limit notices.

## 5) UUID strategy (no overlap + account conversion)
- **Identica UUID** is **internal-only** and generated once on account creation (UUIDv7 or v4) and stored in DB.
- Provider identities map to the same Identica UUID (so users can convert between account types).
- This guarantees **no overlap** with Mojang/offline UUIDs, and **persistence across provider changes**.
- Provider identity fields:
  - PREMIUM providerSubject = Mojang UUID (stable across name changes).
  - CRACKED providerSubject = normalized username at registration time.
- Account conversion updates identity mappings without changing the Identica UUID.

## 6) Data model (SQL)
### 6.1 Core tables
- `identica_accounts`
  - `uuid` (PK)
  - `username` (current)
  - `created_at`, `last_seen`
- `identica_identities`
  - `uuid` (FK)
  - `provider_id`
  - `provider_subject` (e.g., mojang uuid, cracked name, discord id, website id)
  - `primary` (optional)
  - `created_at`, `last_used_at`
- `identica_sessions`
  - `uuid`, `session_id`, `expires_at`, `ip`
## Core tables are minimal; provider-specific data lives in provider tables.

### 6.2 Storage module
`identica-adapter-database` exposes **small services**, not repositories (Intercept pattern):
- `DatabaseService` (init + health)
- `AccountPersistenceService`
- `IdentityPersistenceService`
- `SessionPersistenceService`
Repositories stay internal to the adapter module.

### 6.3 Provider storage extensions
- Providers must register their own migrations and create **provider-owned tables**
  (e.g., `identica_provider_<id>_*`) through a migration hook in the provider SPI.
- Core only reads base tables; provider data is isolated and managed by that provider.
- Providers can implement their own repositories/services inside their module; adapter-database does not expose raw repositories.

### 6.4 Provider tables (planned defaults)
- **Cracked provider**:
  - `identica_provider_cracked_credentials`
    - `uuid` (FK)
    - `password`, `algorithm_type`
    - `created_at`, `updated_at`
- login attempt tracking uses in-memory + Redis (no dedicated table).
- **Premium provider**:
  - No required tables by default; add provider-owned tables via migrations if needed later.

## 7) Auth pipeline (Velocity-first, extensible)
Default pipeline steps (extensible via API):
1. **PreLogin/Login**: gather username, IP, requested profile.
2. **Premium check mode** (configurable):
   - **Verify-only mode (default)**: run verification automatically (no prompt).
   - **Prompt mode (optional)**: send a message offering **premium verify** or **cracked register/login** (only if cracked provider is enabled).
3. **License check** (async with cache + rate limits) if premium verification is chosen/required.
4. **Provider selection**: sort enabled providers by priority; first `supports(context)` wins.
5. **Auth decision**:
   - PREMIUM: auto-login when verified; allow optional local password if configured.
   - CRACKED: require register/login; allow session auto-login.
6. **Conflict resolver** (per session):
   - Uses **provider ids** (strings) and **policy registry** (no core enums).
   - Default: premium wins; cracked gets renamed (prefix/suffix around username) for that session.
   - If premium joins and conflicts with a cracked session, kick the cracked session so they rejoin with the updated display name.
   - Alternate policy: deny cracked if conflict.
7. **Post-auth**: update DB; sync across Redis.

**Extensibility**:
- API allows registration of new pipeline steps **before/after** default steps or at start/end.
- Each step can optionally define a **target server**; if the next step has no target, the player stays on the previous server.

## 8) License detection (no rejoin)
- Support both **profile existence** and **session-server verification**; configurable.
- In prompt mode, show a message offering premium verification or cracked flow.
- Use caching + rate limits; allow **custom Mojang API URLs** (proxy/alternate endpoints).
- If API fails or rate limited:
  - Allow **premium fallback password** (if configured) so premium users can still join.
  - Otherwise **deny join** (default).

## 9) Redis sync (toggleable)
- Enabled via `identica-adapter-synchronization` and `synchronization.yml`.
- Broadcast account updates, session logins/logouts, name conflict resolutions.
- Use pub/sub channels plus Redis-backed shared caches (rate limits, attempts, premium verification cache, session tokens).
- Keep a short-lived local in-memory cache as read-through for hot lookups.
- When sync is disabled, fall back to in-memory only.

## 10) Commands (Commandant)
- `identica-adapter-command` provides **core admin commands only**:
  - `identica reload`
  - `identica providers list|enable|disable|reload`
- **Cracked provider only**:
  - `register`, `login`, `logout`, `changepassword`
  - admin: delete account, force register cracked, drop sessions, change password for specific user

### 10.2 Command definitions (Socialismus pattern)
- Use `commands.yml` with `CommandDefinition` objects (aliases/permissions/cooldowns/etc.).
- Bridge definitions into Commandant via `DefinitionAdapter` (same pattern as Socialismus).
- Provider commands can have their own `providers/<id>/commands.yml`.

### 10.1 Command annotations (hide Commandant/Cloud)
- Follow Oraylen’s approach: define Identica-owned annotations in `identica-api` (e.g., `@Command`, `@Argument`, `@Definition`, `@Permission`, `@Description`, `@Suggestions`, `@Range`, `@Default`).
- Implement a custom annotation parser in `identica-adapter-command` (similar to `OraylenAnnotationParser`) that:
  - Replaces Cloud’s command/argument extractors with Identica-aware ones.
  - Registers builder modifiers for `@Definition`, `@Description`, `@Permission`.
  - Registers annotation mapper for `@Range`.
- This keeps provider modules and other consumers free from direct Cloud annotations.

## 11) Extensibility notes (provider-driven identifiers)
- Providers expose their **id** (string) and optional **conflict group** for rules.
- Conflict policies are registered via API (string keys), not hardcoded enums in core.
- `settings.yml` conflict rules reference provider ids (or groups) supplied by installed providers.

## 12) Recommendations (based on your clarifications)
- **Store the real Mojang UUID** for premium accounts (stable identity across name changes).
- **Hashing**: modular hasher registry with Argon2 + BCrypt by default.
- **Mojang API**: cache + backoff, support alternate URLs, and allow premium fallback passwords.
- **Cracked rename**: apply configured prefix/suffix around the username, and enforce per-session conflict checks.
