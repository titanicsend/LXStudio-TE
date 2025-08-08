### Stories Spec

Stories capture the evolving history of Game of Life - TE work. They are the shared memory between S and the agents, forming a collective knowledge base to understand why things are the way they are.

- **Purpose**: Long-lived record for decisions, experiments, learnings, and context.
- **Representations**: Knowledge graph, short-term/long-term memory, timelines, or other forms suitable for fast/slow retrieval.
- **Privacy warning**: If stories are committed to git, they become public. Do not include confidential or sensitive information.

### Format and storage

Stories are unstructured and can be in any form. Use whatever conveys the story best:
- prose, bullet points, checklists
- code/command/log blocks
- diagrams, timelines, snapshots

Common optional prompts when writing a story:

- **Time**: When it happened (timestamps, ranges, or checkpoints, time stores were generated)
- **Context**: What was going on at the time
- **Characters**: S, agents, tools, systems, or other relevant actors
- **Events**: What happened, actions taken, results, and follow-ups
- **Artifacts**: Commands, logs, links, code edits, or references (in short, story lines)
- **Learnings**: What we discovered or decided

Stories may be kept in this `stories` spec file and/or scattered as short contextual learnings in other spec files as needed. Cross-reference when possible.

Storage convention for individual story files:
- Path: `stories/MONTH-YEAR/DAYMONTHYEAR_story_name.md` (e.g., `stories/2025-08/08082025_launching_test.md`).
- Use lowercase, hyphenated names; keep file names concise.
- Short stories may remain inline in this spec, but prefer file-per-story for longevity.

Authoring guidance:
- Be concise; capture what matters and why.
- Include exact commands/logs in fenced blocks where useful.
- Add explicit “Learnings” when there’s a takeaway.
- Optionally add tags at the end, e.g., `tags: osc, logging, remapper`.
- Link PRs, issues, files, and commits where relevant.

Privacy reminder: stories committed to git are public; omit sensitive data.

### Workflow

- S can instruct an agent: “note down a new story”.
- The story can be about anything: a bug, an experiment, a process, or a meta-reflection.
- Agents should write it, while keeping it concise.

### First Story

- **Title**: Story of launching and starting a test
- **Time**: When initiating an interactive test session
- **Context**: Run te-app in the foreground so S can drive the UI while logs are captured
- **Characters**: S (operator), agent (observer), te-app (system under test)
- **Action**: Start te-app with logs, no timeout, in the foreground

```bash
cd ~/workspace/LXStudio-TE/te-app && \
LOG_FILE="../.agent_logs/te_app_logs_$(date +%Y%m%d_%H%M%S).log" && \
echo "🎯 Starting a test. Logs: $LOG_FILE" && \
java -ea -XstartOnFirstThread -Djava.awt.headless=true -Dgpu -jar target/te-app-0.3.0-SNAPSHOT-jar-with-dependencies.jar --resolution 1920x1200 &> "$LOG_FILE" && \
echo "✅ Test completed - checking logs..." && tail -20 "$LOG_FILE"
```

- **Procedure**:
  - The command starts the LX UI. S interacts with the UI, tests features/bugs.
  - When S closes te-app, the final two commands run: printing completion and tailing the logs.
  - The agent then inspects the log tail. If deeper diagnosis is needed, use terminal tools (e.g., `grep`) to search logs for relevant lines.

- **Learnings**:
  - Foreground execution ensures the UI is interactive; S controls lifecycle and ends the session when done.
  - Centralized log location enables quick triage and deeper grep-driven analysis when necessary.

File it under `stories/MONTH-YEAR/DAYMONTHYEAR_story_name.md` when saving as a standalone story.