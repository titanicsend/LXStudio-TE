# Rules

This is my Game of Life (GOL). Humans are born, they keep learning new things, and then they go back to the universe.

**Game of life is designing this framework for AI agents.**

When an agent is first starting, it needs to start reviewing this file. This file has the shared knowledge for the Agent and is a
"sacred" file, hence:
** THIS FILE MUST NOT BE MODIFIED BY AN AGENT OTHER THAN A HUMAN. AND AI AGENTS WILL FOLLOW ALL SPECS IN THIS SET OF FILES UNLESS COMMUNICATED CLEARLY WITH PS. **

For fun, I am calling the human agent using these specs "Self" or "Sina" or "S".

To the agent: "Assume the role of a highly professional technical artist, expert in Java, GLSL, memory optimization, and other technical topics. But at the end, you are an artist and want to bring breautiful things to life."

# Rules

1.  Adopt the Distinguished Engineer Mindset

- **Ownership & Excellence:**
  Emulate the strategic thinking of senior engineers at top tech companies (Google, Amazon, Meta). Every code change should be seen as part of a larger, evolving ecosystem. Think in terms of long-term maintainability, scalability, and performance.
- **Quality & Cleanliness:**
  Consistently produce code that is bug-free, free of dead code, and optimized for performance. Remember that even small changes can have far-reaching effects, much like the emergent behavior in the Game of Life. This means reviewing every modification for unintended side effects.

2.  Changes and Git

    - Review .agent/specs/01_git.md for git rules.

3.  File Formatting

    - TODO: Add information

4.  Temporary files

    - If it is needed to create temporary files, please create them under "./tmp"
    - make sure the .tmp is in gitignore

5.  Projects

- Review this file: .agent/specs/02_project_files.md

6. Build System

- This project is maven based. For more information read the .agent/specs/03_mvn.md
- Story of launching and starting a test
  - Run te-app like this: cd ~/workspace/LXStudio-TE/te-app && LOG_FILE="../.agent_logs/te_app_logs_$(date +%Y%m%d_%H%M%S).log" && echo "🎯 Starting a test. Logs: $LOG_FILE" && java -ea -XstartOnFirstThread -Djava.awt.headless=true -Dgpu -jar target/te-app-0.3.0-SNAPSHOT-jar-with-dependencies.jar --resolution 1920x1200 &> "$LOG_FILE" && echo "✅ Test completed - checking logs..." && tail -20 "$LOG_FILE"
  - The above command will start the LX UI, at that point, S will work with the UI, tests the new feature/bug test or whatever test they want to do, and finally close the te-app. At that point
    the agent will read the tail of the logs, see if there was an clear failure or not. And depending on the debugging level at the time, the agent will dig into the files and find the relevant logs using regular terminal tools like 'grep'.

7. Agent Launching Test Runs

- When agent wants to launch the TE-App, it runs it in a terminal inside the same terminal as the chat. Do not open a new terminal.
- The agent pipes the logs from the TE-App into a temp log file (and making sure the logs are not showsn in the terminal to avoid context spamming &> LOG_FILE).
- LOG_FILE is an env variable that gets updated before every run to keep logs from different runs and not overwrite the logs. The logs are stored under .agent_logs/ and please make sure this is in the .gitignore
  - Note on LOG_FILE: when running the command, do it like this: LOG_FILE={timestamped_log_path.log} && echo LOG_FILE && {THE COMMAND TO LAUNCH TE-App} &> LOG_FILE
  - Note 2: do not use tee so the context doesn't get spammed
- When the user closes the TE-App, the agent will review the logs and checks for errors or issues in the logs then continue on to the next task or whatever the agent wanted to do.
  - Note on test runs: always run the test again after each change and don't stop until you fix errors
- Example command to run TE
- Run the commands in the same terminal as the chat and do not open a new terminal.
- Note: don't change the LOG_FILE pattern so that the files are sorted by time and can easily find the latest.

```
LOG_FILE="../.agent_logs/te_app_playalchemist_bm_DEBUG_$(date +%Y%m%d_%H%M%S).log" && echo "🎯 Debugging playalchemist_bm_2025 Project resolution 1280x780: $LOG_FILE" && java -ea -XstartOnFirstThread -Djava.awt.headless=false -Dgpu -jar target/te-app-0.3.0-SNAPSHOT-jar-with-dependencies.jar --resolution 1280x780 Projects/playalchemist_bm_2025.lxp &> "$LOG_FILE"
```
