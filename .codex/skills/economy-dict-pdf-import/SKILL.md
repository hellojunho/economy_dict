---
name: economy-dict-pdf-import
description: PDF import workflow guidance for economy_dict. Use when changing admin PDF upload, Spring Batch processing, async task tracking, upload limits, progress bars, OpenAI extraction, or import error handling.
---

# Economy Dict PDF Import

Treat PDF import as an async operational workflow, not a single request.

## Required invariants
- Upload starts asynchronously.
- Task state remains visible to the admin.
- Failures persist error details.
- Large files respect Nginx and Spring multipart limits.
- Progress must be exposed in both API payload and admin UI.

## Check before editing
- Read backend batch/task/import classes.
- Read admin upload UI and progress rendering.
- Read Docker and Nginx config for body size limits.

## State model
- Keep task states explicit: READY, STARTED, PENDING, FINISHED, FAILED.
- Store timestamps and error logs consistently.
- Avoid hiding a failed task behind a generic message.

## UI expectations
- Always show maximum file size in the upload area.
- Show current task progress prominently.
- Show task history in a table.
- When the server returns `413`, surface the configured size limit.
