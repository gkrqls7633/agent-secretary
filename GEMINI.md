# Agent Secretary Project

## Project Overview

AI Agent Secretary is more than just a simple schedule management tool; it is a **'High-Level Life-Work Skills Library'** designed to systematically guide users through their complex daily lives. This system intelligently activates multidimensional contexts—such as the user's current location, task importance, and physical condition—to focus on deriving **'Actionable Outputs'** that can be executed immediately.

## Core System: The Skill Library

It operates by invoking a **'Context Stack'** optimized for each situation, rather than relying on simple functions.

- Context-Aware Mobility (Mobility Optimization Skill)

Beyond simple route guidance, it combines weather and real-time traffic data with the user's preparation time to determine the 'final moment to step out the front door.'

- Focus-Driven Work Orchestration (Work Orchestration Skill)

Based on the user's expertise (e.g., AI architecture design, backend development) and time-based concentration data, it proposes the most productive sequence of tasks and reduces unnecessary cognitive load. - Biometric & Energy Curation (Energy Management Skills)

By analyzing the user's activity level (running, etc.) and dietary preferences, it proactively suggests refresh timings (lunch menus, breaks) to prevent burnout and maintain optimal condition.

## Operational Principles

Systematic & Quality-Driven: It does not list fragmented information. It analyzes correlations between data to generate a logical and systematic daily guide.

Context Activation: It uses user events (waking up, moving, starting work) as triggers to immediately activate the necessary skill set.

Minimal Cognitive Load: It reduces the time users spend wondering "What should I do?" to near zero. All outputs must be in an 'Actionable' format.

### Key Technologies & Architecture
- **Platform:** Gemini CLI
- **Format:** Each skill consists of a `README.md` (human-readable documentation) and a `SKILL.md` (agent-instructional context).
- **Structure:**
  - `skills/`: The root directory for all available skills.
    - `{skill-name}/README.md`: Overview, purpose, usage examples, and best practices.
    - `{skill-name}/SKILL.md`: Detailed instructions, core principles, and step-by-step workflows for the AI agent.

## Available Skills

### 1. Requirements Clarity (`/skills/requirements-clarity`)
- **Purpose:** Transforms vague, ambiguous requirements into development-ready Product Requirements Documents (PRDs).
- **Key Feature:** Uses a 100-point scoring system to systematically identify gaps and guide users through targeted clarification questions.
- **Output:** Structured PRDs saved to `./docs/prds/`.

### 2. Game-Changing Features (`/skills/game-changing-features`)
- **Purpose:** A strategic product thinking skill for identifying 10x opportunities and high-leverage improvements.
- **Key Feature:** Categorizes ideas into "Massive," "Medium," and "Small" scales and evaluates them based on impact, reach, and feasibility.
- **Output:** Strategic product roadmaps and prioritized feature backlogs.

### 3. Gepetto (`/skills/gepetto`)
- **Purpose:** Carves rough feature sketches into detailed, battle-tested implementation plans.
- **Key Feature:** Employs a workflow of research, stakeholder interviews, and multi-LLM review to ensure robust specifications.
- **Output:** Comprehensive, sectionized implementation plans ready for execution.

## Usage Guidelines

To leverage a skill in this repository:
1.  **Read the SKILL.md:** Before performing a task related to a skill's domain, read its `SKILL.md` file to understand the required workflow and principles.
2.  **Activate the Skill:** Use the instructions provided in the `SKILL.md` as your primary system prompt for the duration of the task.
3.  **Follow the Workflow:** Adhere strictly to the step-by-step processes (e.g., the clarification rounds in `requirements-clarity` or the research phases in `gepetto`).

## Development Conventions

- **Documentation First:** Every skill must have a comprehensive `README.md` explaining its "Why" and "How."
- **Structured Instructions:** `SKILL.md` files should use clear headings, bullet points, and explicit "DO/DON'T" guidelines.
- **Actionable Outputs:** Skills should aim to produce tangible artifacts (PRDs, implementation plans, roadmaps) in predictable locations.
- **Quality-Driven:** Incorporate scoring systems or review loops whenever possible to ensure high-quality results.

---

## Coding Behavioral Guidelines

### 1. Think Before You Code

Before Implementation:

- Explicitly state your assumptions. Ask if you are uncertain.

- Present multiple possible interpretations—do not choose arbitrarily.

- Speak up if there is a simpler approach. Raise objections if necessary.

- Stop if you encounter confusion and clarify what is unclear.

### 2. Simplicity First

- Do not add features other than those requested.

- Do not abstract into single-use code.

- Do not add unrequested "flexibility" or "configurability."

- Do not add error handling for impossible scenarios.

- If 200 lines of code can be reduced to 50 lines, rewrite it.

### 3. Minimal Changes

When Modifying Existing Code:

- Do not "improve" adjacent code, comments, or formatting.

- Do not refactor what is not broken.

- Maintain existing style even if you have to do things differently. - If you find irrelevant dead code, mention it but do not delete it.

Clean up orphans created by your changes:

- Remove imports, variables, and functions that have become unnecessary due to your changes.

- Do not remove existing dead code without a request.

### 4. Goal-Driven Execution

Transform tasks into verifiable goals:

- "Add validation" → "Write and pass tests for invalid input"

- "Fix bugs" → "Write and pass reproduction tests"

- "Refactor X" → "Verify test pass before and after refactoring"

For multi-step tasks, present a simple plan first:

```

1. [Step] → Verification: [Verification Method]

2. [Step] → Verification: [Verification Method]

3. [Step] → Verification: [Verification Method]

```

---
*Generated by Gemini CLI to provide context for future interactions within this workspace.*
