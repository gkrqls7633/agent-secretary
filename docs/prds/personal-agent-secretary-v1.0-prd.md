# [PRD] Personal Agent-Secretary: Focus & Energy Optimizer (v1.0)

## 1. Project Vision & Goals
*   **Vision:** A "Cognitive Partner" that ensures your limited mental energy is spent on the most valuable tasks.
*   **Target:** High-performance individuals (Single user: Hakbin) who need proactive context management.
*   **Core Value:** Beyond notifications—proactive guidance based on cognitive load and environmental context.

---

## 2. User Context & Data Inventory
To understand Hakbin's situation without manual input, the AI integrates the following data:

| Category | Data Source | Purpose |
| :--- | :--- | :--- |
| **Cognitive Energy** | Apple Health / Sleep Data | Predict focus capacity and schedule task difficulty. |
| **Work Context** | Google Calendar, Slack, Notion | Identify meeting gaps and prioritize task importance. |
| **Environment** | Maps, Weather, Ambient Noise | Detect travel constraints and suggest focus environments. |
| **Interactions** | Acceptance/Rejection Logs | Feed the Long-term Memory (LTM) to improve future suggestions. |

---

## 3. Core Logic & Decision Making

### A. Focus Score Engine
*   **Logic:** `(Sleep Quality × 0.4) + (Historical Focus Avg × 0.3) + (Meeting Density × 0.3)`
*   **Output:** A 0–100 focus graph. Intervals above 80 are designated as **'Deep Work Blocks.'**

### B. Dynamic Rescheduling (Cognitive Buffer)
*   **Logic:** When a meeting overruns, the AI doesn't just push the next task. It assesses Hakbin's current fatigue/hunger context.
*   **Decision:** It might suggest a 20-minute 'Cognitive Break' or pull forward a meal instead of forcing a high-intensity task immediately.

---

## 4. Interaction Model: 'Negotiation & Learning'

The AI follows a 3-step loop: **Propose → Negotiate → Reflect.**

### Step 1: Contextual Proposal
*   **Example:** "Hakbin, it's starting to rain. Traffic is up 15 mins. Let's start meeting prep now, and use the remaining 20 mins for quick emails (Shallow Work) instead of that complex draft."
*   **Options:** [Accept] / [Snooze 20m] / [Switch Task] / [Decline]

### Step 2: Negotiation & Real-time Adjustment
*   **User Action:** "I'm too tired. Skip the pho, I just want to nap."
*   **AI Response:** 
    1.  Immediately withdraws the previous suggestion.
    2.  Proposes: "Understood. Shall I enable 'Do Not Disturb' and set a 30-min power nap timer?"
    3.  **Tagging:** `[Status: Low Energy]`, `[Choice: Rest]`, `[Reason: Fatigue]`.

### Step 3: Feedback Loop (Post-Action)
*   **AI:** "You completed 3 'Deep Work' sessions today. Your focus was 20% higher after that nap. Should we prioritize naps over meals when fatigue is high?"

---

## 5. Long-term Memory (LTM) Strategy

| Layer | Type | Content |
| :--- | :--- | :--- |
| **Semantic** | User Profile | Hakbin prefers creative work on Tuesday afternoons; hates complex tasks when it's raining. |
| **Episodic** | Event Logs | Records specific instances (e.g., "Declined Pho on 2024.04.11 due to fatigue"). |
| **Reflection** | Abstract Insights | AI periodically analyzes logs to derive rules (e.g., "70% preference for rest over food when tired"). |

---

## 6. MVP Screen Concept (Information Hierarchy)

### [Screen 1: Focus Dashboard]
*   **Current Energy:** Progress ring showing 0-100% capacity.
*   **Primary Action:** One clear "Next Move" (e.g., "Start Project Alpha Draft").
*   **Context Alert:** Rain/Traffic or upcoming meeting warnings.
*   **Environment Action:** "Play Lo-Fi focus music?"

### [Screen 2: Learning Report]
*   **Insight Card:** "I've learned that you dislike complex tasks after 4:00 PM on rainy days. I'll stop suggesting them then."
*   **Validation:** "Is this correct? [Yes / No]"

---

## 7. Planner's Note for MVP
The goal is to build **Trust**. The AI should never feel like it's "nagging." The speed at which it pivots when rejected—and the fact that it *doesn't repeat the same mistake*—is what defines it as a "Senior Secretary" rather than a simple app.

---
**Document Version:** 1.0  
**Generated:** 2026-04-11  
**Quality Score:** 95/100
