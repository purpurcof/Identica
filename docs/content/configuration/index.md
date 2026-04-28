---
title: Configuration
description: Conceptual guide to Identica configuration areas and responsibilities.
---

This section explains Identica configuration as a set of concepts, not as one specific file syntax.
That is intentional: Identica can choose its config representation at runtime, so the important thing is understanding what each config area means first.

## Format-agnostic by design

Identica does not have to be documented as "the YAML plugin" or "the JSON plugin."
The runtime selects a configuration format from a marker file in the data directory, with `type=YAML` as the default behavior in the current codebase.

Because of that, this documentation describes:

- what each configuration area controls
- how the pieces relate to each other
- when you should edit one config area instead of another
