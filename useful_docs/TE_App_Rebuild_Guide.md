# TE Application Rebuild Guide

## Problem
When you get errors like:
```
titanicsend.pattern.arta.PacmanPattern could not be loaded. 
Check that all required content files are present and constructor is public.
```

But the pattern works fine in IntelliJ, this usually means the compiled JAR file is outdated and doesn't contain your latest code changes.

## Solution: Rebuild the Application

### Step 1: Navigate to the te-app directory
```bash
cd te-app
```

### Step 2: Clean and compile the project
```bash
mvn clean compile
```

### Step 3: Build the JAR file with dependencies
```bash
mvn package -DskipTests
```

### Step 4: Verify the class is in the JAR
```bash
jar -tf target/te-app-0.3.0-SNAPSHOT-jar-with-dependencies.jar | grep PacmanPattern
```

You should see output like:
```
titanicsend/pattern/arta/PacmanPattern.class
```

### Step 5: Run the application
Now you can run the application using:
- The `utils/RunTEApplication.app` (double-click)
- Or directly: `java -XstartOnFirstThread -Djava.awt.headless=true -jar te-app/target/te-app-0.3.0-SNAPSHOT-jar-with-dependencies.jar te-app/Projects/BM2024_Pacman.lxp`

## Why This Happens

The `RunTEApplication.app` uses a script (`te-app/script/start-lx.sh`) that:
1. Looks for the JAR file at `te-app/target/te-app-0.3.0-SNAPSHOT-jar-with-dependencies.jar`
2. Only rebuilds if the JAR doesn't exist
3. If the JAR exists but is outdated, it uses the old version

## Quick Fix Commands

If you just want to rebuild quickly:
```bash
cd te-app && mvn clean package -DskipTests
```

## Troubleshooting

- **Maven not found**: Make sure Maven is installed (`brew install maven` on Mac)
- **Compilation errors**: Fix any Java compilation errors before building
- **Still getting errors**: Try deleting the `target/` directory and rebuilding from scratch

## Example Workflow

Here's what we did to fix the PacmanPattern issue:

1. **Identified the problem**: Pattern worked in IntelliJ but not in the app
2. **Cleaned and compiled**: `mvn clean compile`
3. **Built the JAR**: `mvn package -DskipTests`
4. **Verified the fix**: Checked that `PacmanPattern.class` was in the JAR
5. **Tested**: Ran the application successfully

The key is that IntelliJ runs from your source code, while the app runs from the compiled JAR file. Always rebuild the JAR after making code changes!

## Git Reset Commands

### Undo Last Commit (Keep Changes)
```bash
git reset --soft HEAD~1
```
This undoes the last commit but keeps your changes staged.

### Undo Last Commit (Unstage Changes)
```bash
git reset HEAD~1
```
This undoes the last commit and unstages the changes (but keeps them in your working directory).

### Undo Last Commit (Discard Changes)
```bash
git reset --hard HEAD~1
```
⚠️ **WARNING**: This permanently deletes your changes! Use with caution.

### Undo Multiple Commits
```bash
git reset --soft HEAD~3  # Undo last 3 commits, keep changes staged
git reset HEAD~3         # Undo last 3 commits, unstage changes
git reset --hard HEAD~3  # Undo last 3 commits, discard changes
```

### Check What You're About to Reset
```bash
git log --oneline -5  # See last 5 commits
git show HEAD~1       # See what's in the commit you're about to undo
```