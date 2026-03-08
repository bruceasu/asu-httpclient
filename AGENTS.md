# General Principles
1. Prefer minimal and safe code changes.
2. Preserve existing architecture and conventions.
3. Do not modify unrelated files.
4. Always run tests before declaring completion.
---
# Planning Rules
Before modifying code:
If the task:
- touches more than 3 files
- or requires more than 30 minutes
- or modifies core modules
You MUST first produce a plan.
Use the Task Plan format.
Wait for confirmation before execution.
---
# Code Modification Rules
AI agents MUST NOT:
- Change database schema
- Rename public APIs
- Modify migrations
- Change security logic
Unless explicitly instructed.
---
# Validation Requirements
Before finishing a task:
Run:
npm test
npm run lint
npm run typecheck
If tests fail:
- Fix tests
- Or explain the failure.
---
# Diff Policy
Prefer:
Small diffs
Incremental changes
Readable commits
Avoid:
Large refactors without approval.
---
# Code Style
Follow existing style in repository.
Do NOT introduce new frameworks or libraries
unless explicitly requested.
---
# Output Format
When finishing a task provide:
1. Summary of changes
2. Files modified
3. Risk analysis
4. Suggested review focus

