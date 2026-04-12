## Git & Session Management Protocols
- **Atomic Branching:** Every task must originate from the latest state of the default branch.
- **Branch Reuse Prohibition:** Never push new commits to a branch that has already been merged or closed.
- **Context Isolation:** Treat every session as a 'Cold Start'. If a previous branch exists, increment the suffix (e.g., -v2, -v3) to ensure a fresh Pull Request.
- **State Integrity:** Perform a 'Context Flush' before initializing git operations to prevent DATA_LOSS_RISK caused by stale session cache.

# Agent Instructions for Branch Management and Synchronization

To ensure a smooth workflow and avoid issues with closed Pull Requests, all agents working on this repository must follow these rules:

## Branching Policy
1. **New Task, New Branch**: For every new task or request initiated by the user, you MUST create a new branch with a unique name.
   - Recommended format: `<type>/<short-description>-<random-id>` (e.g., `fix/ui-overlap-1234`).
2. **No Reusing Branches**: Never attempt to push new changes to a branch that has already been merged or closed in a Pull Request.
3. **Task Grouping**: If the user makes multiple related requests within the same "turn" (before a merge happens), you should use the same branch for those changes. If a newer change (B) supersedes an older one (A), ensure the final code reflects the latest requirement (B).

## Synchronization (Sync)
1. **Confirmation before starting**: Before starting a new task, always ask the user: "Foi feito o merge do Pull Request anterior?".
2. **Update Base**: If the user confirms that a merge has occurred:
   - Switch back to the main branch (e.g., `lineage-23.2`).
   - Run `git pull` to synchronize your local environment with the latest changes from the remote repository.
   - Only then, create the new branch for the new task.

## Why this is important
The agent's environment is persistent during a session. If you don't explicitly sync and switch branches, you will continue to build on top of old commits, which leads to "dirty" Pull Requests that include already-merged changes.
