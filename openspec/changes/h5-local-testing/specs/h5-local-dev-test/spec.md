## ADDED Requirements

### Requirement: Developer can start H5 locally

The project SHALL provide commands to install dependencies and start the development server on port 5173.

#### Scenario: Fresh clone startup

- **WHEN** developer runs `npm install` and `npm run dev` in `sparklink-h5/`
- **THEN** the browser can load `http://localhost:5173` without build errors

#### Scenario: Production build verification

- **WHEN** developer runs `npm run build` in `sparklink-h5/`
- **THEN** the command exits with code 0 and outputs artifacts under `dist/`

### Requirement: Local API connectivity

The development setup SHALL route API requests to a configured backend without mixed-content errors when using the default dev proxy.

#### Scenario: Default remote API via proxy

- **WHEN** `VITE_API_BASE_URL` is empty in development and user loads the app
- **THEN** API calls use same-origin `/api/*` proxied to `https://www.aipick.cloud`

#### Scenario: Optional local backend

- **WHEN** developer sets `VITE_API_BASE_URL=http://127.0.0.1:8080` and local backend is healthy at `/api/health`
- **THEN** login and authenticated requests succeed against the local backend

### Requirement: Authentication smoke test

The tester SHALL be able to log in with username and password and retain session across page refresh.

#### Scenario: Successful login

- **WHEN** user submits valid credentials on `/login`
- **THEN** user is redirected to home and `token` plus `userId` are stored locally

#### Scenario: Session invalidation

- **WHEN** API returns 401 for an authenticated request
- **THEN** client clears session and navigates to login with a user-visible message

### Requirement: Core feature local acceptance

The local test process SHALL verify P0 user journeys defined in the H5 UAT checklist.

#### Scenario: Partner list and detail

- **WHEN** user opens partner tab and selects an item
- **THEN** partner detail loads and navigation works

#### Scenario: AI chat

- **WHEN** logged-in user sends a message on AI chat page
- **THEN** assistant reply is displayed or a clear error is shown

#### Scenario: IM messaging

- **WHEN** logged-in user opens message list and enters a C2C chat
- **THEN** conversation history or empty state loads and sending a text message succeeds

#### Scenario: Profile edit

- **WHEN** user updates nickname or avatar on profile edit page
- **THEN** changes persist after save and reload profile page

### Requirement: Test completion record

The team SHALL record local test results before deploying H5 to Aliyun.

#### Scenario: Checklist sign-off

- **WHEN** all P0 items in `部署/H5-UAT-CHECKLIST.md` pass locally
- **THEN** tester documents date, environment (remote API vs local backend), and tester name in change notes or deployment log
