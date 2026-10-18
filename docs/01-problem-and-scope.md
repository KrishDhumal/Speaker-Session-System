# Task 1 — Problem Definition and Scope

## Problem statement

Conferences and college technical events collect talk proposals from speakers,
have them reviewed, and then publish a schedule. Most small events still do this
with a mix of email, spreadsheets and chat messages. The result is predictable:

- proposals are lost in inboxes, and nobody can say how many were received;
- two people review the same proposal while a third is never looked at;
- the schedule exists in one person's spreadsheet, so a room change does not
  reach everyone;
- there is no record of *why* a talk was rejected.

The Speaker Session Management System replaces that with one place where a
proposal is created, reviewed, approved and scheduled, with the current state of
every proposal visible to everyone who needs it.

## Target users

| User | What they need |
|---|---|
| **Speaker** | Submit a proposal, correct it after feedback, see its status |
| **Reviewer** | See everything waiting for review, approve or reject with a reason |
| **Organizer** | Turn approved talks into a schedule, assign rooms, cancel if needed |

## Stakeholders

| Stakeholder | Interest in the system |
|---|---|
| Speakers | Fair, visible handling of their proposal |
| Review committee | A queue that is shared, not duplicated |
| Event organizers | An accurate schedule and a count of what is confirmed |
| Event attendees | A published programme that does not change silently |
| Department / college | A record of the event that can be reported on |

## Existing pain points

1. No single list of proposals — the count differs depending on who you ask.
2. No defined states, so "approved" means different things to different people.
3. Anyone can edit the schedule; there is no notion of who is allowed to do what.
4. Rejection reasons are not recorded, so the same weak proposal returns next year.
5. No summary view — organizers cannot answer "how many talks per track?".

## Objectives

| # | Objective | How it is measured |
|---|---|---|
| O1 | One authoritative record per proposal | Every proposal has exactly one row with one status |
| O2 | A status cannot be changed by the wrong role | The server rejects the move, not just the UI |
| O3 | Any proposal can be found in a few seconds | Search by keyword, status and track |
| O4 | Organizers can see the shape of the event at a glance | Dashboard with counts per status and per track |
| O5 | Every change is attributable | Each record stores who created it and the review note |

## Constraints

- **Time**: built within a single-semester lab schedule, 13 tasks.
- **Skills**: Java and Spring are known; the rest is being learned alongside.
- **Infrastructure**: must run on one laptop — no cloud account, no paid service.
- **Data**: an embedded database is acceptable; no database server to administer.
- **Scope**: this is an MVP. Email notification, file uploads for slides,
  attendee-facing pages and multi-event support are explicitly *out of scope*.

## Measurable success criteria

| Criterion | Target |
|---|---|
| Core journeys automated end to end | 5 Selenium journeys passing |
| Build reproducibility | `mvn verify` green from a clean clone |
| Pipeline | A commit reaches a running container with no manual step |
| Role enforcement | A forged request from the wrong role is refused by the server |
| Recovery | A bad release can be rolled back with one command |

## Approved MVP scope — 15 items

| # | Item | Status |
|---|---|---|
| 1 | Create a session proposal | Done |
| 2 | View a single session in full | Done |
| 3 | Edit a session's details | Done |
| 4 | Delete a session (organizer only) | Done |
| 5 | List all sessions | Done |
| 6 | Search by keyword (title or speaker) | Done |
| 7 | Filter by status | Done |
| 8 | Filter by track | Done |
| 9 | Submit a draft for review (speaker) | Done |
| 10 | Approve or reject with a note (reviewer) | Done |
| 11 | Send a proposal back for changes (reviewer) | Done |
| 12 | Schedule or cancel (organizer) | Done |
| 13 | Role-based sign-in with three roles | Done |
| 14 | Summary dashboard (counts per status and track) | Done |
| 15 | Upcoming scheduled sessions list | Done |

**Out of scope for the MVP:** email notifications, slide uploads, speaker
self-registration, multiple events, attendee voting, calendar export.
