# 🎲 Jeu Petit Bac — Multi-Agent System with JADE

A distributed multi-agent implementation of the classic "Petit Bac" (Scattergories) word game, built with the JADE (Java Agent Development Framework). Each player is represented by an autonomous agent that manages game logic, turn coordination, and real-time score validation independently.


 ##📌 Overview

| Property | Details |
| Language | Java |
| Framework | JADE (Java Agent Development Framework) |
| Architecture | Multi-Agent System (MAS) |
| Agent Types | Master Agent, Player Agents, Validator Agent |
| Communication | ACL Messages (FIPA-compliant) |
| Course | Fondements AI — Master AI & Digital Computing |


 ##🧠 Architecture

┌────────────────────────────────────────────┐
│              JADE Main Container           │
│                                            │
│   ┌──────────────┐    ┌────────────────┐  │
│   │ Master Agent │───▶│ Player Agent 1 │  │
│   │  (GameMgr)   │    └────────────────┘  │
│   │              │    ┌────────────────┐  │
│   │  - Sends     │───▶│ Player Agent 2 │  │
│   │    letter    │    └────────────────┘  │
│   │  - Triggers  │    ┌────────────────┐  │
│   │    timer     │───▶│ Player Agent N │  │
│   │  - Collects  │    └────────────────┘  │
│   │    answers   │             │           │
│   └──────┬───────┘             │           │
│          │                     ▼           │
│          │           ┌──────────────────┐  │
│          └──────────▶│ Validator Agent  │  │
│                      │  - Score calc    │  │
│                      │  - Duplicate det │  │
│                      └──────────────────┘  │
└────────────────────────────────────────────┘

**Agent Roles:**

- **Master Agent** — orchestrates game rounds, broadcasts the random letter, manages the countdown timer, and collects answers from all player agents.
- **Player Agents** — autonomous agents representing each player; they receive the letter, submit word answers per category, and wait for score results.
- **Validator Agent** — receives all player answers, detects duplicates, applies scoring rules, and reports final scores back to the Master Agent.



## 🚀 Getting Started

### Prerequisites

- Java JDK 8+
- [JADE Framework](https://jade.tilab.com/) (included in `/lib`)
- Eclipse IDE (`.classpath` and `.project` files included) or any Java IDE

### Installation

```bash
# Clone the repository
git clone https://github.com/Missesqueensy/JeuPetitBac.git
cd JeuPetitBac
```

### Run with JADE GUI

```bash
# Launch JADE main container with GUI
java -cp lib/jade.jar:bin jade.Boot -gui
```

Then in the JADE GUI:
1. Start the **MasterAgent** on the main container
2. Start **PlayerAgent** instances (one per player)
3. Start the **ValidatorAgent**
4. The game will auto-start once all agents register

### Run from CLI (headless)

```bash
java -cp lib/jade.jar:bin jade.Boot -container \
  Master:agents.MasterAgent \
  Player1:agents.PlayerAgent \
  Player2:agents.PlayerAgent \
  Validator:agents.ValidatorAgent
```

---

## 🎮 Game Rules Implemented

- A random letter is broadcast to all player agents simultaneously
- Players must submit a valid word per category (Country, City, Animal, Object, Name...)
- **Scoring:**
  - Unique valid answer → **2 points**
  - Duplicate answer shared with another player → **1 point**
  - No answer or invalid answer → **0 points**
- Rounds continue for a configurable number of letters

---

## 🔑 Key Technical Concepts

- **FIPA ACL Message Passing** — agents communicate via structured, standardized messages
- **Behaviour-based agent design** — each agent uses JADE `Behaviour` classes (`CyclicBehaviour`, `OneShotBehaviour`, `WakerBehaviour`) for non-blocking concurrency
- **Agent lifecycle management** — proper `setup()`, `takeDown()`, and registration with the JADE Directory Facilitator (DF)
- **Synchronization without shared memory** — coordination achieved purely through message-passing, demonstrating distributed systems principles

---

## 📁 Project Structure

```
JeuPetitBac/
├── src/
│   └── agents/
│       ├── MasterAgent.java       # Game orchestration
│       ├── PlayerAgent.java       # Player logic
│       └── ValidatorAgent.java    # Score validation
├── bin/                           # Compiled classes
├── lib/                           # JADE .jar (add manually)
├── APDescription.txt              # Agent Platform description
├── MTPs-Main-Container.txt        # Container config
├── .classpath
└── .project
```

---

## 📚 Concepts Demonstrated

| Concept | Implementation |
|---|---|
| Multi-Agent Systems | JADE agent lifecycle + DF registration |
| Distributed coordination | ACL message-based synchronization |
| Concurrent behaviours | JADE Behaviour classes (non-blocking) |
| Game logic automation | Rule-based scoring in ValidatorAgent |
| Real-time communication | Async message passing between agents |

---

## 👩‍💻 Author

**Ahlame Laouad** — Master's student in AI & Digital Computing, FST Béni Mellal (USMS)

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Ahlame%20Laouad-blue)](https://www.linkedin.com/in/ahlame-laouad-14315a335/)
[![GitHub](https://img.shields.io/badge/GitHub-Missesqueensy-black)](https://github.com/Missesqueensy)

---

## 📄 License

This project is for academic and educational purposes.
