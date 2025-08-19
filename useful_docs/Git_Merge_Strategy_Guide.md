# Git Merge Strategy Guide

## Preserving Local LXP Files When Pulling from Main

When working with LX Studio project files (`.lxp`) and you want to keep your local version as the source of truth while pulling other changes from main, use the "ours" merge strategy.

### Scenario
You have local modifications to `BM2024_Pacman.lxp` (like copied patterns, audio stems, etc.) and want to:
- Keep your LXP file exactly as it is
- Get other updates from main (new patterns, code changes, etc.)
- Avoid manual conflict resolution

### Solution: Use "Ours" Merge Strategy

```bash
git pull origin main --no-rebase -X ours
```

### What This Does

- **`--no-rebase`**: Uses merge strategy instead of rebase
- **`-X ours`**: When conflicts occur, automatically chooses your local version ("ours")

### Result
- ✅ Your `BM2024_Pacman.lxp` file is preserved unchanged
- ✅ Other files from main are merged normally
- ✅ New files from main are added
- ✅ Deleted files from main are removed
- ✅ Creates a merge commit

### Example Output
```
Auto-merging te-app/Projects/BM2024_Pacman.lxp
Merge made by the 'ort' strategy.
 te-app/Projects/BM2024_Pacman.lxp                                | 234 +++++++++++++++++++++++++++++++++++-
---------
 te-app/src/main/java/heronarts/lx/studio/TEApp.java              |   3 +
 te-app/src/main/java/titanicsend/pattern/arta/PacmanPattern.java | 271 ++++++++++++++++++++++++++++++++++++
+++++++++++++++++
 useful_docs/How_to_Run_TE_App.md                                 | 107 ---------------------
 4 files changed, 461 insertions(+), 154 deletions(-)
 create mode 100644 te-app/src/main/java/titanicsend/pattern/arta/PacmanPattern.java
 delete mode 100644 useful_docs/How_to_Run_TE_App.md
```

### Alternative Strategies

#### Manual Merge (Default)
```bash
git pull origin main --no-rebase
```
- Pauses on conflicts and asks you to resolve manually
- Good when you want to review each conflict

#### Rebase
```bash
git pull origin main --rebase
```
- Replays your commits on top of main
- Creates linear history but may conflict with LXP changes

#### Fast-forward Only
```bash
git pull origin main --ff-only
```
- Only works if branches can be fast-forwarded
- Fails if there are divergent changes

### When to Use "Ours" Strategy

✅ **Use when:**
- You have local LXP modifications you want to keep
- You want to get code updates from main
- You want to avoid manual conflict resolution
- Your LXP file is the source of truth

❌ **Don't use when:**
- You want to review what changed in the LXP file
- You want to merge specific changes from main's LXP file
- You need to resolve conflicts manually

### Best Practices

1. **Backup your work** before pulling
2. **Commit your changes** before pulling
3. **Review the merge** after completion
4. **Test your LXP file** to ensure it still works

### Troubleshooting

If you accidentally used the wrong strategy:
```bash
# Reset to before the pull
git reset --hard HEAD~1

# Then use the correct strategy
git pull origin main --no-rebase -X ours
```

### Related Commands

```bash
# Check what files would be affected
git status

# See what changed in the merge
git log --oneline -5

# View the merge commit
git show HEAD
```
