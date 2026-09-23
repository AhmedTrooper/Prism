## Rules Before Commit

### Step 0: No Shortcuts
- Production-grade only. Never take the cheap or "minimal" path to ship faster[cite: 1].
- When fixing bugs, audits, or regressions, write the full, robust fix even if it is more code or more time[cite: 1].
- Do not suggest "this is fine for now, ship it", do not paper over an issue, do not leave a TODO for a known gap unless the user accepts it explicitly[cite: 1].

### Step 1: Write and Pass Tests[cite: 1]
- Write automated tests for every new feature or bug fix in Rust or TypeScript[cite: 1].
- Run and pass tests before any linting or compiler checks[cite: 1]:
  ```bash
  cargo test --manifest-path api/Cargo.toml
  bun --cwd web test
  ```[cite: 1]

### Step 2: Language-Specific Checks[cite: 1]
- **For Rust**[cite: 1]:
  ```bash
  cargo fmt --manifest-path api/Cargo.toml --check
  cargo clippy --manifest-path api/Cargo.toml -- -D warnings
  cargo check --manifest-path api/Cargo.toml
  ```[cite: 1]
  *(Run `cargo fmt --manifest-path api/Cargo.toml` to format if needed)*[cite: 1].
- **For TypeScript (Strictly Bun Runtime)**[cite: 1]:
  ```bash
  bun --cwd web run build
  bun --cwd web run lint
  ```[cite: 1]
  *(Never use `npm`, `pnpm`, or `yarn`)*[cite: 1].

### Step 3: Update README.md[cite: 1]
- `README.md` must be updated instantly before making the commit[cite: 1].
- `README.md` must NEVER describe the repository as a "template"; it must always present the system as a real, live, production-grade project[cite: 1].

### Step 4: Mandatory Git Inspection[cite: 1]
- Run and review all staged and unstaged changes line-by-line[cite: 1]:
  ```bash
  git status
  git diff
  ```[cite: 1]

---

## Commit Message Rules & Standards

- **Single Line Title / Header**: The main title must be a single line without bullet points, line breaks, or paragraphs[cite: 1].
- **Extra Context in Body Only**: Put detailed descriptions in the commit body, separated by an empty line from the title[cite: 1].
- **Human-like Tone (B1 Level English)**: Use simple, natural everyday English (B1 level). Avoid robotic phrasing, marketing buzzwords, and complex C1 vocabulary (e.g., "delve", "orchestrate", "plethora", "revolutionize", "seamless")[cite: 1].
- **Strictly No AI Attribution**: Never include `Co-authored-by:` lines for AI tools. Never mention `Claude`, `Codex`, `Puku`, `Copilot`, `ChatGPT`, or any AI tool name in commit messages or git metadata[cite: 1].

---

## Rules at / After Commit

### Step 5: Commit Instantly[cite: 1]
- Create the commit immediately once all previous steps have succeeded[cite: 1].
