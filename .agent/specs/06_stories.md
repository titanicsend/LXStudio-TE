### Stories Spec

Stories capture the evolving history of Game of Life work. They are the shared memory between S and the agents, forming a knowledge base to understand why things are the way they are.

- **Purpose**: Long-lived record for decisions, experiments, learnings, and context.
- **Representations**: Knowledge graph, short-term/long-term memory, timelines, or other forms suitable for fast/slow retrieval.
- **Privacy warning**: If stories are committed to git, they become public. Do not include confidential or sensitive information.

### Structure

Stories follow a structure similar to projects:

- **Time**: When it happened (timestamps, ranges, or checkpoints)
- **Context**: What was being worked on and why
- **Characters**: S, agents, tools, systems, or other relevant actors
- **Events**: What happened, actions taken, results, and follow-ups
- **Artifacts**: Commands, logs, links, code edits, or references
- **Learnings**: What we discovered or decided

Stories may be kept in this `stories` spec file and/or scattered as short contextual learnings in other spec files as needed. Cross-reference when possible.

### Workflow

- S can instruct an agent: “note down a new story”.
- The story can be about anything: a bug, an experiment, a process, or a meta-reflection.
- Agents should write it using the structure above, keeping it concise and actionable.

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


