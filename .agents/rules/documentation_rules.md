---
trigger: always_on
---

# Documentation Rules

## Purpose

Ensure every project decision is documented and can be continued by another AI agent.

---

## Mandatory Documentation Updates

After completing a task, the agent MUST create or update documents inside:

```text
agent/conversation/
```

---

## Required Files

### progress_tracker.md

Tracks:

* Completed tasks
* Current task
* Pending tasks
* Blocked tasks

Use checklist format:

```markdown
- [x] Completed
- [ ] Pending
```

---

### current_project_state.md

Must always contain:

* Current project phase
* Existing architecture status
* Existing implementations
* Missing implementations
* Recommended next step

---

## Task Documents

Each major task should create a dedicated document.

Examples:

```text
00_project_context.md
01_api_protection_design.md
02_api_protection_implementation.md
03_api_protection_review.md
04_rabbitmq_design.md
05_worker_implementation.md
```

---

## Required Sections

Every task document should contain:

### Objective

What the task was supposed to achieve.

### Decisions Made

Architectural or technical decisions.

### Files Created

List of created files.

### Files Modified

List of modified files.

### Dependencies

Related modules or team members.

### Remaining Work

What still needs to be done.

### Open Questions

Unknown items requiring clarification.

---

## Single Source of Truth

The folder:

```text
agent/conversation/
```

must always contain enough information for a new AI agent to continue the project without reading previous chat history.

If a decision exists only in conversation and not in documentation, it is considered undocumented and may be lost.
