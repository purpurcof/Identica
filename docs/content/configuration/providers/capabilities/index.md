---
title: Capabilities
description: Shared and provider-scoped configuration for built-in provider capabilities.
---

Capabilities are optional behavior modules that providers can advertise and install.
They are not automatic core behavior for every provider.

This section documents the built-in/default capabilities that ship with Identica.

Capabilities commonly surface configuration in two operator-facing places:

- shared capability files under `providers/capabilities/<capability-id>/...`
- provider-scoped fields under `providers[].capabilities.<capability-id>...`

Only providers that advertise a capability can use that capability's provider-scoped fields.

## Documented capabilities

- [Authoritative Username](./authoritative-username/index.mdx)
- [Restriction](./restriction/index.mdx)
- [Recognition](./recognition/index.mdx)
