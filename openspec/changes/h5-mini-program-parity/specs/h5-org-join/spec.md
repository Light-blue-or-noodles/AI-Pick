## ADDED Requirements

### Requirement: Join company flow (M3)

The app SHALL provide `/join/company` to search or apply for company affiliation, matching mini-program `company-join` behavior and backend APIs.

#### Scenario: Successful company join request

- **WHEN** user submits valid company join form
- **THEN** backend accepts the request and user's `companyName` reflects after refresh

### Requirement: Join school flow (M3)

The app SHALL provide `/join/school` for school affiliation with parity to mini-program `school-join`.

#### Scenario: Successful school join request

- **WHEN** user submits valid school join form
- **THEN** backend accepts the request and user's school field reflects after refresh

### Requirement: Post-M3 full UAT and deployment

After M3, the team SHALL run full `部署/H5-UAT-CHECKLIST.md`, production build, and `scripts/deploy-h5.sh` to Aliyun.

#### Scenario: Production deploy

- **WHEN** all milestone checklists pass on staging/UAT
- **THEN** `dist/` is deployed behind Nginx with HTTPS and `/api` reverse proxy
