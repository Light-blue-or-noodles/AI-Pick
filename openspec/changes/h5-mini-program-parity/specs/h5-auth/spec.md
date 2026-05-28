## ADDED Requirements

### Requirement: Agreement pages are publicly accessible

The H5 app SHALL provide user agreement and privacy policy pages matching mini-program content, without requiring login.

#### Scenario: Open user agreement

- **WHEN** user navigates to `/agreement/user`
- **THEN** the full user agreement text is scrollable and a back navigation control is available

#### Scenario: Open privacy policy

- **WHEN** user navigates to `/agreement/privacy`
- **THEN** the full privacy policy text is scrollable and a back navigation control is available

### Requirement: Login requires explicit protocol consent

The login page SHALL require the user to agree to both agreements before submitting credentials, with links to the agreement routes.

#### Scenario: Login blocked without consent

- **WHEN** user attempts login without checking the protocol checkbox
- **THEN** a toast or inline message is shown and login is not submitted

#### Scenario: Login with consent

- **WHEN** user checks protocol consent and submits valid username/password (or dev test-login when enabled)
- **THEN** JWT and userId are stored and user is redirected to the home tab

### Requirement: Session persistence and invalidation

Authentication state SHALL match mini-program storage keys (`token`, `userId`, `userInfo`, `isLoggedIn`) and behave consistently on refresh and 401.

#### Scenario: Session restore on reload

- **WHEN** user reloads the app with a valid token in localStorage
- **THEN** user remains authenticated without visiting login

#### Scenario: 401 clears session

- **WHEN** any API returns 401 for an authenticated request
- **THEN** client clears session and navigates to login with a user-visible message

### Requirement: WeChat OAuth login (M3)

When WeChat Open Platform web app credentials are configured, the app SHALL support OAuth redirect login as an alternative to username/password.

#### Scenario: OAuth callback success

- **WHEN** user completes WeChat OAuth and backend returns a valid token
- **THEN** user is logged in with the same session storage as password login

#### Scenario: OAuth not configured

- **WHEN** OAuth is not configured in environment
- **THEN** only username/password login is shown without broken OAuth buttons
