# Fork Upstream Merge Workflow

A complete guide for safely merging upstream changes into your fork without losing your work.

## Overview

When you have a fork of a repository and the original (upstream) repo gets new updates, you need a safe way to incorporate those changes without losing your custom work. This workflow ensures you never lose your changes while staying up-to-date with upstream improvements.

## The Problem

You've forked a repo, made custom changes, and now the original repo has new features you want. But git won't let you simply pull because your branches have "diverged" - you both made different commits.

## The Solution: Branch and Merge Strategy

This workflow creates a safe space to merge upstream changes, then brings them into your main branch in a controlled way.

## Step-by-Step Workflow

### 1. Save Your Work First (Force Push)

Always start by ensuring your current work is safely stored on GitHub:

```bash
# Make sure everything is committed locally
git add .
git commit -m "Save my latest work"

# Force push to save your work to GitHub
git push origin main --force
```

**Why force push?** Your fork may have auto-generated commits from GitHub that conflict. Force push ensures your local work (which is most important) is saved.

### 2. Create Upstream Merge Branch

Create a new branch to safely explore what changed upstream:

```bash
# Create and switch to new branch
git checkout -b upstream-merge
```

### 3. Configure Git Merge Strategy

Tell git how to handle conflicts (use merge, not rebase):

```bash
git config pull.rebase false
```

### 4. Pull Upstream Changes

Pull the latest changes from the original repository:

```bash
# Pull from the original repo's main branch
git pull upstream main
```

**What happens:** Git will automatically merge the upstream changes with your work. If there are conflicts, it will ask you to resolve them.

### 5. Review What Changed

See what the upstream team has been working on:

```bash
# See commits they added that you don't have
git log --oneline main..upstream-merge

# See file changes
git diff main..upstream-merge --stat
```

### 6. Test the Merged Version

At this point, you have a branch with:
- ✅ All your custom work
- ✅ All their new features
- ✅ Any conflicts resolved

Test this version to make sure everything works together.

### 7. Create Safety Backup

Before merging, create a backup of your current main branch:

```bash
# Switch back to main branch
git checkout main

# Create safety backup branch (use current date)
git branch my-backup-aug-15
```

### 8. Merge Back to Main

Now safely merge the upstream changes:

```bash
# Merge the upstream changes
git merge upstream-merge

# Push the combined version to your fork
git push origin main
```

### 9. Cleanup

Remove the temporary branch (keep the backup):

```bash
git branch -d upstream-merge
# Keep my-backup-aug-15 for safety - delete later when confident
```

## Example Walkthrough

Here's a real example from updating a Pacman art car fork of LXStudio-TE:

```bash
# 1. Save current work
git add .
git commit -m "pacman 2025 changes"
git push origin main --force
✅ Saved to: github.com/artarazavi/LXStudio-Pacman

# 2. Create merge branch
git checkout -b upstream-merge
✅ Switched to a new branch 'upstream-merge'

# 3. Configure merge
git config pull.rebase false

# 4. Pull upstream
git pull upstream main
✅ Auto-merging te-app/src/main/java/heronarts/lx/studio/TEApp.java
✅ Merge made by the 'ort' strategy.
✅ 52 files changed, 1152 insertions(+), 221 deletions(-)

# 5. Review changes
git log --oneline main..upstream-merge
✅ Shows: datsa_moire.fs, blue_noise.png, PolySpiral.java, DistortEffect.java

# 6. Verify your customizations survived
grep -A 2 "Pacman" te-app/src/main/java/heronarts/lx/studio/TEApp.java
✅ @LXPlugin.Name("Pacman")
✅ flags.windowTitle = "Chromatik — Pacman";

# 7. Create safety backup
git checkout main
git branch my-backup-aug-15
✅ Safety backup created

# 8. Merge to main
git merge upstream-merge
✅ Updating 8acacee4..988333cb
✅ Fast-forward (no conflicts!)

# 9. Push combined version
git push origin main
✅ Your fork now has BOTH your Pacman work AND their new shader patterns!
```

## What You Get

After this workflow:
- 🔒 **Your work is safe** - never lost or overwritten
- 🆕 **Latest features** - you have all upstream improvements  
- 🎯 **Clean history** - proper merge commits show what came from where
- 🔄 **Repeatable** - you can do this anytime they release updates

## When Conflicts Happen

Sometimes git can't auto-merge because you and upstream changed the same lines:

```bash
# When you see conflicts:
git status
# Shows: both modified: some-file.java

# Edit the conflicted files, look for:
<<<<<<< HEAD
Your version
=======
Their version
>>>>>>> upstream/main

# Choose which version to keep or combine them
# Then commit the resolution:
git add some-file.java
git commit -m "Resolved merge conflict in some-file.java"
```

## Advanced: Selective Merging

If you only want specific commits from upstream:

```bash
# See individual commits
git log upstream/main --oneline

# Cherry-pick specific commits
git cherry-pick abc1234  # Just that one commit
git cherry-pick def5678  # And another one
```

## Troubleshooting

### "Divergent branches" error
```bash
# Fix with:
git config pull.rebase false
git pull upstream main
```

### "Non-fast-forward" error
```bash
# You need to pull first:
git pull upstream main
# Then push:
git push origin main
```

### Lost in merge conflicts
```bash
# Abort and start over:
git merge --abort
git checkout main
# Try again with smaller steps
```

## Best Practices

### Before Starting
- ✅ Commit all your work locally
- ✅ Push to your fork as backup  
- ✅ Note what you've customized (e.g., "Pacman branding in TEApp.java")

### During Merge
- 📖 Read their changelog/release notes
- 🧪 Test the merged version thoroughly
- 📝 Document any conflicts you resolved
- 🔍 Verify your customizations survived (check key files)

### After Merge
- 🏷️ Tag the successful merge: `git tag v1.2-merged-aug15`
- 📋 Update your documentation (like this workflow guide!)
- 🔒 Keep safety backup branch for a while
- 🎉 Enjoy the new features!

## Why This Workflow Works

1. **Safety First**: Your work is saved before any risky operations
2. **Isolation**: Upstream changes are tested in a separate branch
3. **Control**: You decide when and how to integrate changes
4. **Transparency**: Clear git history shows what came from where
5. **Repeatability**: Same process works every time upstream updates

This workflow lets you maintain your custom fork while staying current with upstream development. It's the best of both worlds - your customizations plus their improvements!
