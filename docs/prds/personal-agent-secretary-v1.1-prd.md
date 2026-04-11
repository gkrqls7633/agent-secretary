# [PRD] Personal Agent-Secretary: Focus & Energy Optimizer (v1.1)

## 1. Requirements Description

### Background
- **Business Problem**: Modern professionals suffer from cognitive overload and decision fatigue due to fragmented schedules and unpredictable energy levels.
- **Target Users**: A single high-performance user (Hakbin) leading a busy life.
- **Value Proposition**: Proactive, context-aware guidance that optimizes "Deep Work" and manages energy transitions.

### Feature Overview
- **Core Features**: Focus score prediction, proactive Slack briefings, emergency energy boosts, and local feedback learning.
- **Feature Boundaries**: 
    - **In-scope**: Reading Calendar/Health/Context data, Slack-based notifications, Local-first memory storage.
    - **Out-of-scope (MVP)**: Writing/modifying events (Write permissions), Multi-user support, Cloud-based syncing.
- **User Scenarios**: Morning briefing, post-meeting energy check, meal/rest suggestions based on weather and fatigue.

### Detailed Requirements
- **Input/Output**: 
    - **Input**: Google Calendar (Read), Apple Health (Read), Weather API, User feedback via Slack buttons.
    - **Output**: Structured Slack blocks with actionable insights and interactive buttons.
- **User Interaction**: Propose (AI) → Negotiate (User via Slack buttons) → Reflect (AI Learning).
- **Data Requirements**: High-frequency context sampling (every 30-60 mins) for real-time relevance.
- **Edge Cases**: 
    - **Data Conflict**: If a meeting exists but Focus Score is low, trigger "Emergency Boost" (e.g., Caffeine/Quick Nap/Stretch).
    - **Data Silence**: If APIs fail, fallback to a generic time-based briefing.

## 2. Design Decisions

### Technical Approach
- **Architecture Choice**: Event-driven agent using Slack Bolt API as the primary interface.
- **Key Components**: Context Collector (API Integrations), Logic Engine (Focus Scoring), Response Generator (Slack Formatting), Memory Manager (Local Storage).
- **Data Storage**: Local-first storage (e.g., SQLite or local Vector DB) for LTM to ensure maximum privacy.
- **Interface Design**: Slack Slash commands for manual status checks, Interactive Buttons for proposal feedback.

### Constraints
- **Performance**: Notifications must arrive within 1 minute of a significant context change (e.g., meeting delay).
- **Compatibility**: Primary access via Slack (Desktop & Mobile).
- **Security**: **Local-first Data Policy**. Sensitive health/work data never leaves the local environment for learning purposes.
- **Scalability**: Designed for single-user performance; vertical scaling of local processing if needed.

### Risk Assessment
- **Technical Risks**: API rate limits or authentication timeouts for external services.
- **Dependency Risks**: Changes in Slack API or Google OAuth policies.
- **Schedule Risks**: Over-tuning the "Focus Score" algorithm delaying the MVP launch.

## 3. Acceptance Criteria

### Functional Acceptance
- [ ] AI correctly identifies a 30-minute "Deep Work" gap in the calendar.
- [ ] Slack notification is triggered 5 minutes before a scheduled "Leave Home" time.
- [ ] AI correctly processes a "Decline" response and tags the reason (e.g., Fatigue).

### Quality Standards
- [ ] Data Privacy: No personal schedule or health data is sent to external servers for long-term storage.
- [ ] Interaction: Slack responses handle user clicks within 3 seconds.

### User Acceptance
- [ ] User (Hakbin) confirms the "Emergency Boost" prompts are helpful during high-fatigue periods.
- [ ] User confirms the AI stops suggesting "Pho" after it was rejected once due to specific reasons.

## 4. Execution Phases (Updated)
### Phase 0: Mock-up Design : Slack Block Kit Builder
**Goal**: Design Slack UI for key scenarios and finalize the data schema to be delivered from the backend to validate the project's visual vision.

- [ ] Task 1: Designing Slack Block Kit Messages by Scenario
Configuring message layouts for the three MoTs (Morning, Focus, Restoration) defined in PRD v1.1.
Arranging information hierarchy to minimize user cognitive load.
Deliverable: Block Kit JSON files for each scenario.

- [ ] Task 2: Prototyping Interaction Flow (Propose-Negotiate)
Defining the action_id and value system generated when a Slack button is clicked.
Designing scenarios where messages 'update' after a button click (e.g., messages disappear or status changes upon clicking the 'OK' button).
Deliverable: Interaction Flow Map (describes which buttons trigger which events).

- [ ] Task 3: Define UI-Backend Data Mapping
Extract variable data (e.g., departure time, focus score, restaurant name) for the mock-up UI and map it to the API data to be integrated in Phase 1.
Deliveable: Mock-up-based Data Requirement List (List of required API fields).

### Phase 1: Preparation (Environment & Auth)
**Goal**: Set up local environment and secure Read-only API access.
- [ ] Task 1: Initialize Slack App and Bolt API environment.
- [ ] Task 2: Implement OAuth for Google Calendar and connection to Health data.
- **Deliverables**: Connected Data Hub (Read-only).
- **Time**: 3 days.

### Phase 2: Core Engine (Logic & Memory)
**Goal**: Build the Focus Score engine and Local Memory manager.
- [ ] Task 1: Develop Focus Score algorithm (Sleep + Schedule).
- [ ] Task 2: Implement Local Storage for Interaction Logs (Rejection tagging).
- **Deliverables**: Focus Engine & Local Memory System.
- **Time**: 5 days.

### Phase 3: Interaction (Slack & Feedback)
**Goal**: Implement the Propose-Negotiate-Reflect loop.
- [ ] Task 1: Design and implement Slack Block Kit messages for briefings.
- [ ] Task 2: Build the feedback handling logic (Button click → Memory update).
- **Deliverables**: Fully functional Slack Agent.
- **Time**: 4 days.

### Phase 4: Validation & Tuning
**Goal**: Real-world testing and algorithm refinement.
- [ ] Task 1: 3-day trial run with live data.
- [ ] Task 2: Fine-tune "Emergency Boost" triggers based on actual fatigue logs.
- **Deliverables**: Final MVP ready for daily use.
- **Time**: 3 days.

---

**Document Version**: 1.1
**Created**: 2026-04-11
**Clarification Rounds**: 2
**Quality Score**: 94/100
