# Project Documentation and Management Framework

## Project Documentation Purpose

Project documentation serves as the **primary knowledge repository** for development initiatives. These documents maintain context across agent sessions and ensure systematic progress tracking throughout project lifecycles.

## Document Organization Structure

### Standard Organization

```markdown
# Project Implementation Plan

## Background and References

1. Links to specifications and guidelines
2. References to related initiatives or documentation

### [STATUS] Phase X

Description of phase goals and deliverables

Phase X Status: STATUS

## Implementation Notes and Progress

Comprehensive notes on development, modifications, and current system state
```

### Status Indicators

- ✅ **COMPLETED** - Task finished successfully
- 🔄 **IN PROGRESS** - Currently being worked on
- ⏸️ **PAUSED** - Temporarily halted, will resume later
- ❌ **CANCELLED** - No longer needed or blocking issue
- 🔄 **NEXT** - Ready to begin, dependency resolved

### Progress Documentation Standards

#### Task Updates Format

```markdown
### Task X.Y - [STATUS] - Brief descriptive title

- **Result:** What was accomplished
- **Issues:** Any problems encountered
- **Next Steps:** What should happen next
- **Files Modified:** List of changed files
```

#### Decision Documentation

```markdown
**Decision:** Brief summary
**Rationale:** Why this approach was chosen
**Alternatives Considered:** Other options evaluated
**Impact:** Effects on system/workflow
```

## Project File Lifecycle

### Creation Guidelines

1. **Use Template**: Start from `.agent/projects/template/00_PHASED_PROJECT.md`
2. **Include Context**: Reference all relevant specifications
3. **Define Phases**: Break work into logical, manageable phases
4. **Set Success Criteria**: Clear definitions of completion

### Maintenance Standards

1. **Real-time Updates**: Update progress as work proceeds
2. **Preserve History**: Keep completed task records for reference
3. **Link Dependencies**: Reference related files and external resources
4. **Document Decisions**: Capture reasoning behind significant choices

### Review and Closure

1. **Final Status Review**: Ensure all tasks marked appropriately
2. **Lessons Learned**: Document insights for future projects
3. **Handoff Notes**: Provide context for future maintainers
4. **Archive Decision**: Determine if project should remain active

## File Naming Conventions

### Project Files

Format: `YYYYMMDD_initiator_brief_description.md`

- Example: `20250724_sinas_make_ndi_work_with_gpu_mixer.md`

### Specification Files

Format: `NN_topic.md` where NN is sequence number

- Example: `03_mvn.md`, `02_project_files.md`

### Template Files

Prefix with sequence number for ordering

- Example: `00_PHASED_PROJECT.md`

## Quality Standards

### Writing Requirements

- **Clarity**: Write for future readers unfamiliar with the project
- **Completeness**: Include sufficient context for understanding decisions
- **Accuracy**: Keep information current and factual
- **Organization**: Use consistent structure and formatting

### Technical Documentation

- **Code References**: Use file paths and line numbers where relevant
- **Commands**: Include exact commands used with explanations
- **Configuration**: Document settings and their purposes
- **Dependencies**: List requirements and installation instructions

## Integration with Development Workflow

### Pre-work Phase

1. Create project file from template
2. Link relevant specifications and context
3. Define clear success criteria and phases
4. Set up task tracking structure

### During Development

1. Update task status in real-time
2. Document decisions and rationale immediately
3. Note any deviations from original plan
4. Capture useful commands and configurations

### Post-completion

1. Mark all tasks with final status
2. Document lessons learned
3. Update related specifications if needed
4. Ensure handoff clarity for future work

This framework ensures that project knowledge is preserved, decisions are documented, and future work can build effectively on past efforts.
