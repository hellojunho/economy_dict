---
name: economy-dict-ui-minimal
description: Minimal white UI guidance for the economy_dict frontend. Use when redesigning or extending React pages, forms, tables, navigation, or admin screens while keeping the project formal and restrained.
---

# Economy Dict UI Minimal

Preserve a white, minimal, formal interface.

## Visual direction
- Prefer white backgrounds and subtle gray separation.
- Keep typography clean and compact.
- Avoid heavy gradients, oversized hero effects, and decorative noise.
- Use emphasis through spacing, borders, and weight rather than bright color blocks.

## Layout rules
- Auth pages: one core card, one form, minimal footer links.
- Admin pages: sidebar + table workspace.
- Search/chat pages: clear primary action and readable results area.
- Keep top navigation sparse and predictable.

## Component rules
- Buttons must look distinct by hierarchy, not by visual clutter.
- Tables must remain readable first; avoid dense ornamentation.
- Empty states must explain the next action.
- Every visible button must trigger a real request or route transition.

## Implementation targets
- Update `/Users/chojunho/project/economy_dict/frontend/src/styles.css` first when the change is systemic.
- Update page-level markup only after the styling direction is clear.
