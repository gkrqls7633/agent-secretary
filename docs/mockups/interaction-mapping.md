# Interaction & Data Mapping Guide (v1.0)

## 1. Interaction Flow Map
| Trigger Event | Action ID | User Action | Response / Outcome |
| :--- | :--- | :--- | :--- |
| Morning Briefing | `morning_confirm` | Click [OK, I'm leaving] | Message updates to: "Have a safe trip! 🚗" |
| Focus Proposal | `focus_start` | Click [Start Now] | Message updates to: "Focus Mode Active. Slack status changed to DND. 🤫" |
| Focus Proposal | `focus_delay` | Click [Later] | AI schedules a 30-min reminder. |
| Food Suggestion | `food_accept` | Click [Sounds Good] | AI provides directions and records positive preference. |
| Food Suggestion | `food_reject` | Click [Not Feeling It] | AI asks for reason (Fatigue/Taste/Wait) and updates LTM. |

## 2. Data Requirement List (Phase 1 Target)
| Field Name | Type | Description |
| :--- | :--- | :--- |
| `user_focus_score` | Integer | Calculated 0-100 based on sleep and schedule density. |
| `travel_time_buffer` | Minutes | Additional travel time due to weather/traffic. |
| `next_event_title` | String | Title of the closest upcoming calendar event. |
| `is_precipitation` | Boolean | True if rain/snow is expected during commute hours. |
| `rejection_reason` | Enum | [FATIGUE, TASTE, TIME, URGENT_TASK] |
