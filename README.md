# PetitBac Multi-agent JADE Project

This project implements a Petit Bac game with two AI agents and a referee using JADE. Each agent picks an algorithm randomly each round from: Random, BFS, DFS, UCS, A*.

Prerequisites
- Java JDK 11 or newer (javac and java on PATH)
- PowerShell (on Windows)
- JADE library (jade.jar) — place it into `lib/` (see below)

How to prepare and run (Windows / PowerShell)

1. Open PowerShell in the project folder (where `run.ps1` is located).

2. Ensure you have a JDK installed and `javac` is on your PATH. If not, install from AdoptOpenJDK/Temurin or Oracle and set `JAVA_HOME`.

3. Run the helper script which:
   - creates `lib/` if missing,
   - downloads `gson-2.10.1.jar` automatically,
   - asks you to place `jade.jar` into `lib/` (download JADE binary distribution from http://jade.tilab.com/download/),
   - compiles the project into `out/`,
   - launches JADE and starts three agents: two `AgentJoueur` and one `AgentArbitre`.

Run:
```powershell
.\run.ps1
```

Notes
- If automatic download of Gson fails (no network), download `gson-2.10.1.jar` from Maven Central and put it into `lib/`.
- Download JADE from the official site, extract, and copy `jade.jar` into `lib/`.
- `dictionary.json` must be present at project root (it's loaded from classpath). Ensure it's in the working directory or bundled in the classpath.

CSV Benchmark
- The referee writes `petitbac_benchmark.csv` into the project folder, containing: `round,player,algorithm,nodesExpanded,timeMs,score,chosenWords`.

Next steps (optional)
- Make `maxRounds` configurable via agent arguments.
- Add more sophisticated SearchProblem modeling (trie/prefix) to better demonstrate UCS/A*.
