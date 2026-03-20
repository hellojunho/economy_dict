---
name: economy-dict-glossary-prompting
description: Prompt engineering guidance for economy_dict glossary extraction and term explanation. Use when revising OpenAI prompts for economics term extraction, JSON schema control, DB insertion shape, or answer reliability.
---

# Economy Dict Glossary Prompting

Optimize prompts for structured, DB-safe outputs.

## Requirements
- Make the output schema explicit.
- State which fields may be null.
- Tell the model not to invent unstated meanings when extraction requires grounding in source text.
- Distinguish extraction from explanation mode.

## Extraction mode
- Mention the target DB fields: `word`, `meaning`, `englishWord`, `englishMeaning`.
- Exclude auto-generated DB fields such as numeric IDs.
- Require valid JSON only when the downstream parser expects JSON.
- Require duplicate removal and concise terms.

## Explanation mode
- For term search, ask for a direct definition first, then brief context.
- Keep answers concise enough to store and display in the current UI.
- If the project stores responses, keep field names and shape stable.

## Validation
- If a prompt is meant for parsing, include one compact example output.
- If accuracy matters, add an internal self-check instruction but do not ask the model to expose hidden reasoning.
