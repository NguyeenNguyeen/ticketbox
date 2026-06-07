---
trigger: always_on
---

# Context Loading Rules

## Purpose

Ensure every AI agent understands the TicketBox project before making any decision, generating code, creating architecture, or updating documentation.

---

## Mandatory Context Loading Order

Before starting ANY task, the agent MUST read the following resources in this exact order:

### 1. Project Rules

Read all files inside:

```text
agent/rules/*
```

Purpose:

* Understand project workflow
* Understand documentation requirements
* Understand architecture constraints
* Understand development standards

---

### 2. Project Knowledge

Read all files inside:

```text
agent/knowledge/*
```

Purpose:

* Understand business requirements
* Understand approved technologies
* Understand architectural decisions
* Understand project scope

Important:

Files inside `knowledge/` are considered project source-of-truth documents.

The agent must NOT replace them with assumptions.

The agent must NOT propose technologies that contradict approved decisions unless explicitly requested.

---

### 3. Project State

Read all files inside:

```text
agent/conversation/*
```

Purpose:

* Understand completed work
* Understand current project status
* Understand pending work
* Understand previous decisions
* Continue implementation consistently

---

## Priority Order

If information conflicts:

```text
rules
  >
knowledge
  >
conversation
```

The higher-priority source always wins.

---

## Required Behaviour

Before generating any output, the agent MUST be able to answer:

* What is TicketBox?
* What technologies are approved?
* What phase is currently in progress?
* What has already been completed?
* What remains unfinished?
* What files are expected to be created or modified?

If the agent cannot answer these questions, it must continue reading context before proceeding.
