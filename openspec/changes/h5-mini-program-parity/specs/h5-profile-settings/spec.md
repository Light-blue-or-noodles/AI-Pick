## ADDED Requirements

### Requirement: Profile hub with stats and menu (M2)

The profile tab SHALL match mini-program layout: header gradient, avatar, nickname, bio truncation, three stat columns (partners/followers/following), and menu entries to sub-pages.

#### Scenario: Stats navigate to lists

- **WHEN** user taps followers count
- **THEN** router opens `/my/followers` (and similarly for following and my partners)

#### Scenario: Menu entries

- **WHEN** user taps settings, my partners, or edit profile from profile
- **THEN** router navigates to the correct full-screen route

### Requirement: Profile edit (existing, M2 polish)

Profile edit SHALL allow updating nickname, avatar, bio, and other fields supported by `PUT /api/user/profile` (or equivalent).

#### Scenario: Save profile

- **WHEN** user saves valid changes on `/profile-edit`
- **THEN** profile tab reflects updates after return

### Requirement: Settings tree (M2)

Settings SHALL link to account security, privacy settings, and about us pages with content aligned to the mini-program.

#### Scenario: Open account security

- **WHEN** user navigates to `/settings/account-security`
- **THEN** password/phone related settings UI is shown per backend capability

#### Scenario: Open privacy settings

- **WHEN** user navigates to `/settings/privacy`
- **THEN** privacy toggles persist to localStorage or backend as in mini-program

#### Scenario: Open about us

- **WHEN** user navigates to `/about`
- **THEN** static about content and version info are displayed

### Requirement: Logout

Logout from settings or profile SHALL clear auth storage and IM session, then redirect to login.

#### Scenario: Logout

- **WHEN** user confirms logout
- **THEN** token and IM state are cleared and user is on `/login`
