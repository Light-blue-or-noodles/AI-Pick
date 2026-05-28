## ADDED Requirements

### Requirement: Home AI entry matches mini-program index

The home tab SHALL show the AI mascot, welcome copy, and quick chips that deep-link into AI chat with preset prompts.

#### Scenario: Quick chip opens AI chat

- **WHEN** user taps a quick chip such as「游戏搭子」or「AI 对话」
- **THEN** router opens `/ai-chat` with a `quick` query and the first user message is sent automatically when applicable

### Requirement: AI chat conversation UI

The AI chat page SHALL support sending text, show user messages right-aligned and assistant messages left-aligned, and display loading state while waiting for a reply.

#### Scenario: Send message and receive reply

- **WHEN** logged-in user sends a message on `/ai-chat`
- **THEN** assistant reply appears in the thread or a clear error toast is shown

#### Scenario: Session continuity

- **WHEN** user returns to AI chat in the same browser
- **THEN** `sessionId` is reused from localStorage when the backend supports it

### Requirement: AI recommended partner cards

When the API returns recommended partners in the AI response, the UI SHALL render tappable cards that navigate to partner detail.

#### Scenario: Tap recommendation

- **WHEN** user taps a recommended partner card
- **THEN** router navigates to `/partner/:id` for that partner
