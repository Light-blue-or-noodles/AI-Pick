## ADDED Requirements

### Requirement: Message list and IM login (M2)

The message tab SHALL initialize TIM after login, list C2C conversations, and show unread count on the tab badge.

#### Scenario: Conversation list loads

- **WHEN** logged-in user opens `/message`
- **THEN** IM SDK is initialized and conversation list or empty state is shown

#### Scenario: Unread badge updates

- **WHEN** IM reports total unread count changes
- **THEN** `AppTabBar` message tab shows the count capped at `99+`

### Requirement: C2C chat page (M2)

Chat page SHALL load history for `userId` query param, send text messages, and display peer nickname/avatar in the header.

#### Scenario: Send chat message

- **WHEN** user sends text in `/chat` with valid peer context
- **THEN** message appears in the thread and is delivered via TIM

#### Scenario: Chat without IM login

- **WHEN** user opens chat while IM is not logged in
- **THEN** user sees a clear prompt to re-login or retry IM init

### Requirement: navigateToChat helper

A shared `navigateToChat` utility SHALL route to `/chat` for users and `/ai-chat` for AI peers, matching mini-program behavior.

#### Scenario: Navigate from partner detail

- **WHEN** contact flow completes prep-peer
- **THEN** user lands on chat with correct query parameters

### Requirement: Follow graph pages (M2)

The app SHALL provide my partners, following, and followers list pages with API parity to the mini-program.

#### Scenario: Open my partners

- **WHEN** user opens `/my/partners` from profile menu
- **THEN** list of user's published partners loads

#### Scenario: Open following or followers

- **WHEN** user opens `/my/following` or `/my/followers`
- **THEN** respective user lists load with navigation to user profile

### Requirement: Other user profile (M2)

User profile page SHALL show public info and follow/unfollow actions for another user's id.

#### Scenario: View other user

- **WHEN** user navigates to `/user/:id`
- **THEN** nickname, avatar, bio, and follow state are shown
