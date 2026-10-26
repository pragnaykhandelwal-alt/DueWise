# STUDENT-WORK.md — my design notes and explanations

This file is mine. I write the notes and explanations here, in my own words.
AI tools don't write in this file. Claude Code only created these empty headings on 2026-10-04.
Only real facts go here: no invented contributions, test results, or user feedback.

## The problem and who it's for

## DatePolicy: my date rules
If you have granola bars in your earthquake readiness bag and also in your pantry, and if granola bars are expiring in 7 days or fewer, you should move them to the pantry. For example, you look at today's date and you notice that the item is expriring within a 7 days window  generate a notification. 
Rotation planner is a functionality for when a similar item exists in a more active area. For example, a granola bar can exist in a pantry as well as in an earthquake bag. If they are expiring in the earthquake bag, the notification should be to move them to an active area like a pantry for consumption. 

Date planner and rotation planner go hand in hand. The app should be able to check whether a similar item exists in multiple locations (for example, an earthquake bag or a pantry). If they are, it should rotate them to the active area (for example, the pantry), where it could be consumed sooner to reduce food waste. 
If the bar is only in one of the locations, not in the pantry, and the rotation policy does not apply, you should still generate a notification so that it could be consumed. 
The notification should appear when the app opens and also on the home screen as a push notification. 

### Statuses
| Status | A batch gets it when... |
|---|---|
| NO_DATE     | when an item is entered without any date |
| NOT_DUE     | when the expiration date is more than 14 days away|
| REVIEW_SOON | When the expiration date is between 7 and 13 days|
| DATE_PASSED | when the expiration date > today's date|

### Worked examples (they become my tests)
Today: ?   Settings numbers: ?
| Batch | Location type | Label and date | Status I expect | needsReplacement? |
|---|---|---|---|---|
| Food   | Earthquake kit | Best by 30 Oct 2025 | NOT_DUE | Non |
| Food   | Earthquake kit | Best by 5 Oct 2025 | Review_soon | yes |
| Food   | Pantry | Use by 15 Oct 2025 | Review_soon     | no |
| Medicine |FIrst aid supplies | Use by 15 Nov 2025 | Not_due     | no |
| Food   | Pantry | Expires 01 Oct 2025 | Date_passed     | Yes |



### Edge decisions
- With a threshold of 7, a batch dated exactly 7 days from today is  REVIEW_SOON
- Date_Passed when a batch get on its date itself (date = today)
- When there are multiple dates go by the earliest of (Best by, Use by, Expires) 

## RotationPlanner: my rotation rules
If you have granola bars in your earthquake readiness bag and also in your pantry, and if granola bars are expiring in 7 days or fewer, you should move them to the pantry.
## Explanations in my own words

## Decisions and trade-offs

## Tests I ran and what happened

## Real feedback from my household
