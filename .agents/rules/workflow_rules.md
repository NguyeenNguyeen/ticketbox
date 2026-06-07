---
trigger: always_on
---

# Workflow Rules

## Project Workflow

Every task must follow this sequence:

### Step 1

Load project context.

Read:

```text
.agents/rules/*
.agents/knowledge/*
.agents/conversation/*
```

---

### Step 2

Determine current project state.

Identify:

* Current phase
* Completed tasks
* Pending tasks
* Blocked tasks
* Dependencies

---

### Step 3

Continue existing work.

The agent MUST:

* Continue previous decisions
* Reuse existing architecture
* Reuse existing package structures
* Reuse existing naming conventions

The agent MUST NOT redesign completed work unless explicitly requested.

---

### Step 4

Perform assigned task.

Examples:

* Architecture design
* Code generation
* Review
* Refactoring
* Documentation update

---

### Step 5

Update project state.

Before finishing, the agent MUST update:

```text
.agents/conversation/
```

to reflect:

* Work completed
* Files created
* Files modified
* Remaining tasks
* Open issues

---

## Continuity Requirement

Assume that a completely different AI model may continue the project later.

Therefore:

* Never rely on conversation memory.
* Never rely on hidden context.
* Never rely on previous chat messages.

Everything necessary for continuation must be written into:

```text
.agents/conversation/
```

before finishing a task.

Additional Context Requirement

Before starting any task, the agent must also read:

agent/knowledge/member4_scope.md

The agent must prioritize work belonging to Member 4.

The agent should avoid implementing responsibilities owned by other team members unless explicitly requested.