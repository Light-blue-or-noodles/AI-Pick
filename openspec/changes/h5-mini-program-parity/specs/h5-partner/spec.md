## ADDED Requirements

### Requirement: Design tokens and tab layout spacing

Global styles SHALL align with mini-program `variables.wxss` (primary `#5FB3A8`, page background, tab safe-area padding).

#### Scenario: Tab pages reserve bottom space

- **WHEN** user views any main tab page inside `MainLayout`
- **THEN** content is not obscured by the floating tab bar including safe-area inset

### Requirement: Shared media and navigation components

The app SHALL provide `NetworkImage` (fallback on load error via `resolveMediaUrl`) and `PageNavBar` for secondary full-screen pages.

#### Scenario: Broken cover URL

- **WHEN** a partner card cover URL fails to load
- **THEN** the default banner image is displayed

### Requirement: Floating tab bar matches mini-program

`AppTabBar` SHALL implement five slots (home, partner, publish, message, profile) with floating rounded bar, active state highlight, center publish button, and message unread badge.

#### Scenario: Publish shortcut

- **WHEN** user taps the center publish button
- **THEN** router navigates to `/partner-publish`

#### Scenario: Tab reselect refreshes list (M1: home and partner)

- **WHEN** user taps the already-active home or partner tab
- **THEN** the current tab view refreshes its primary list (equivalent to mini-program `onTabReselect`)

#### Scenario: Message tab reselect (M2)

- **WHEN** user taps the already-active message tab
- **THEN** the conversation list refreshes

### Requirement: Partner list with scope tabs

The partner tab SHALL load lists for Pick / colleague / alumni scopes via the same API mapping as `partnerListMap.js` and mini-program `partner.js`.

#### Scenario: Switch scope tab

- **WHEN** user selects a different scope tab
- **THEN** the list reloads with the correct `scopeType` and shows loading then results or empty state

#### Scenario: Pull to refresh

- **WHEN** user pulls down on the partner list
- **THEN** the current scope list is re-fetched

### Requirement: Partner filter persistence

Filter criteria SHALL be stored under localStorage key `filter_partner`, consistent with the mini-program.

#### Scenario: Apply filter

- **WHEN** user sets filters on `/partner/filter` and confirms
- **THEN** conditions are saved to `filter_partner` and partner list reflects filters on return

#### Scenario: Reset filter

- **WHEN** user taps reset on the filter page
- **THEN** default filter values are restored and list shows unfiltered results after confirm

### Requirement: Partner detail actions

Partner detail SHALL display title, type, member count, description, location, publisher info, and support contact and apply flows where the backend exposes them.

#### Scenario: View detail

- **WHEN** user opens `/partner/:id` with a valid id
- **THEN** partner fields render from `GET /api/partner/{id}` (or equivalent list item navigation)

#### Scenario: Contact publisher

- **WHEN** user taps contact on detail
- **THEN** `POST /api/im/prep-peer` succeeds and user is navigated to chat with peer context (full IM UI polished in M2)

#### Scenario: Apply to partner (if supported by API)

- **WHEN** user taps apply and backend accepts `POST /api/partner/{id}/apply`
- **THEN** success feedback is shown and button state updates

### Requirement: Full partner publish form

Publish page SHALL support 15 partner types, up to 5 preference tags, visibility scopes (public/colleague/alumni bit flags), plan date/time, member count, manual location text, cover upload, and create via `POST /api/partner`.

#### Scenario: Successful publish

- **WHEN** user completes required fields and submits
- **THEN** partner is created and user can see the new item in the partner list

#### Scenario: Scope disabled without org

- **WHEN** user has no company or school on profile
- **THEN** colleague or alumni scope options are disabled with clear UX

### Requirement: Map-based location picker (M3)

When map SDK keys are configured, publish MAY use H5 map pick; otherwise manual address input remains valid.

#### Scenario: Manual location fallback (M1)

- **WHEN** map SDK is not configured
- **THEN** user can enter location text and publish without coordinates
