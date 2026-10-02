# Battleship: Naval Command

A modern Java 21 + JavaFX naval warfare game built with strict object-oriented design and clean architecture.

---

## Features

### Game Modes
- **Single Player (vs AI):** Battle against three distinct AI difficulty tiers: Easy (Ensign), Normal (Lieutenant), and Hard (Admiral).
- **Pass & Play (Hotseat):** Local two-player mode with a private handoff pass screen (`PassScreen`) between turns to maintain fleet secrecy.
- **LAN Multiplayer ("Play with a Friend"):** Direct peer-to-peer TCP socket connection across local networks:
  - Scannable QR code (via ZXing) or shareable join string (`BATTLESHIP:<ip>:<port>:<code>` or `<ip>:<port>:<code>`).
  - Resilient lobby join: Flexible input parsing (with or without protocol prefix, whitespace-tolerant), port range validation (1-65535), and `Enter` key shortcut submission.
  - Strict Fog-of-War: Real ship placements are never transmitted over the wire; each client is authoritative only over its own board and reports shot outcomes (`MISS`, `HIT`, `SUNK`).
  - Robust session lifecycle: Graceful disconnect detection, state-aware modal cleanup preventing false disconnect warnings upon normal game completion, and synchronized network firing audio.

### Battlefields & Theaters
- **Quick Match (5×5):** Fast skirmish with 2× Patrol Boat (size 2) and 1× Submarine (size 3) - 3 ships total. Total 7 hits to win.
- **Standard (8×8):** Tactical engagement with 2× Destroyer (size 2), 2× Submarine (size 3), and 1× Battleship (size 4) - 5 ships total. Total 14 hits to win.
- **Classic (10×10):** Full fleet action with 2× Destroyer (size 2), 2× Submarine (size 3), 1× Cruiser (size 3), 1× Battleship (size 4), and 1× Carrier (size 5) - 7 ships total. Total 22 hits to win.

### Advanced Weaponry & Arsenal System
- **Standard Shell:** Precise 1×1 shot with infinite ammo.
- **Salvo Barrage ("Level 2"):** 1×3 line barrage available on larger grids (8×8 and 10×10).
- **Tactical Nuclear Warhead:** 2×3 area devastation available across all theaters.
  - **Launch Code Protocol:** Firing requires answering naval trivia questions to authorize detonation.
  - **Auto-Resupply:** Initiates a resupply drill to restock warheads after usage.
- **Orientation & Target Preview:** Press **`R`** or **Right-Click** to rotate weapon trajectory with real-time ghost overlay.

### AI Strategies (Strategy & Composition Pattern)
- **Ensign (Easy):** Uniform random targeting across unshot coordinates.
- **Lieutenant (Normal):** Checkerboard parity hunt exploiting minimum ship size (length 2), transitioning to targeted neighbor pursuit upon impact.
- **Admiral (Hard):** Probability density mapping that computes valid remaining ship configurations for every cell, paired with opportunistic area-weapon bombardment and line-following targeting.

### Audio & Visuals
- Procedural canvas-rendered ocean waves, flowing ribbon accents, radar sweep, and compass watermark animations (dedicated `decor/` renderer classes).
- Fleet health monitor with live per-ship damage progress bars and attack logs.
- Dual-mode audio system: high-fidelity sound clips with a procedural sound generator fallback, delivered through the swappable `GameAudio` abstraction.

---

## Architecture & Package Layout

Strict separation of concerns is enforced: `model` and `ai` contain pure Java domain logic and have **zero** JavaFX or UI dependencies.

```
com.battleship
├── model           # Pure domain entities, aggregate roots, and value objects
│   ├── AiPlayer.java          # Machine participant entity
│   ├── AmmoReadout.java       # Read-only ammo query interface
│   ├── Arsenal.java           # Centralized launcher ammo tracking
│   ├── CellStatus.java        # Cell state enum (EMPTY, SHIP, HIT, MISS, SUNK)
│   ├── Coordinate.java        # Immutable board coordinate (row, col)
│   ├── FleetDeployment.java   # Fleet placement state mutator interface
│   ├── FleetReadout.java      # Read-only fleet query interface
│   ├── GameMode.java          # Mode selector (AI_EASY, AI_NORMAL, AI_HARD, HOTSEAT, ONLINE)
│   ├── GameState.java         # Game state machine (MAIN_MENU, SHIP_PLACEMENT, PASS_SCREEN, BATTLE, GAME_OVER)
│   ├── HumanPlayer.java       # Human participant entity
│   ├── MatchStatistics.java   # Domain record computing shots, hits, misses, accuracy, and sunk ships
│   ├── Orientation.java       # Ship and weapon direction enum (HORIZONTAL, VERTICAL)
│   ├── Player.java            # Abstract participant base class
│   ├── PrimaryGrid.java       # Own board state (secret fleet placement and damage)
│   ├── Ship.java              # Placed ship with idempotent hit tracking
│   ├── ShipType.java          # Fleet classes (size, asset metadata, traits)
│   ├── ShotOrder.java         # Shot command record
│   ├── ShotResult.java        # Outcome record (coordinate, outcome, shipSunk)
│   ├── ShotTarget.java        # Target coordinate query abstraction
│   ├── Theater.java           # Battlefield configuration presets (SKIRMISH, ENGAGEMENT, FLEET_ACTION)
│   ├── Turn.java              # Turn ownership enum (PLAYER_1, PLAYER_2)
│   │
│   ├── fog/                   # Strict Fog-of-War tracking
│   │   ├── MarkerStatus.java  # Known enemy cell status (UNKNOWN, MISS, HIT, SUNK)
│   │   └── TrackingGrid.java  # Admiral's private observation grid of enemy waters
│   │
│   ├── projection/            # Read-only domain projections
│   │   └── ShipSnapshot.java  # Immutable snapshot of placed ship state
│   │
│   └── weapon/                # Polymorphic weapon hierarchy (OCP / LSP)
│       ├── BlastPattern.java  # Area-of-effect offsets with orientation rotation
│       ├── NuclearWarhead.java# 2×3 blast weapon with authorization & trivia gating
│       ├── SalvoBarrage.java  # 1×3 line barrage weapon ("Level 2" in UI)
│       ├── StandardShell.java # Standard 1×1 single shell weapon
│       ├── Weapon.java        # Core weapon abstraction (sound, authorization, blast)
│       └── WeaponCatalog.java # Preconfigured weapon instances
│
├── ai              # AI strategy implementations (pure Java domain logic)
│   ├── AIFactory.java         # Strategy factory with null-safe mode resolution
│   ├── AIStrategy.java        # Strategy interface
│   ├── Difficulty.java        # Difficulty enum (Ensign, Lieutenant, Admiral)
│   ├── HuntTargetAI.java      # Normal difficulty: parity hunt + target state machine
│   ├── ParityHunter.java      # Shared stateless checkerboard parity hunt component
│   ├── RandomAI.java          # Easy difficulty: random unshot targeting
│   ├── SmartAI.java           # Hard difficulty: probability density mapping
│   └── TargetingQueue.java    # Reusable target queue component (composition)
│
├── controller      # Application services & orchestration
│   ├── BattleService.java     # Turn management, launcher selection & firing pipeline
│   ├── GameController.java    # Thin mediator between views and domain services
│   ├── LauncherFireResult.java# Multi-cell shot result and sunk ship container
│   ├── NetworkFireService.java# Domain mutations of a network shot (ammo, resupply)
│   ├── PlacementService.java  # Fleet placement legality & auto-deployment
│   ├── ShotResolution.java    # Functional strategy interface for resolving shots (DIP)
│   └── ShotResolver.java      # Applies launcher blast patterns to target grids
│
├── net             # LAN multiplayer networking (TCP sockets)
│   ├── NetMessage.java        # Sealed JSON protocol message hierarchy
│   ├── NetMessageCodec.java   # Gson wire codec with type discriminator
│   ├── NetUtil.java           # LAN IP and ephemeral port discovery utilities
│   ├── NetworkBattleMediator.java # Controller-level mediation for network battles
│   ├── NetworkGameSession.java# Encapsulated networking context and turn state
│   ├── NetworkSession.java    # Socket listener/sender with async callbacks
│   └── Role.java              # HOST/CLIENT role enum
│
├── persistence     # Save and load game state (JSON via Gson)
│   ├── GameSaveDTO.java       # Serializable transfer object
│   ├── GameSaveMapper.java    # SRP domain-to-DTO and DTO-to-domain mapper
│   └── SaveGameService.java   # File storage service
│
└── view            # JavaFX presentation layer
    ├── Launcher.java          # Fat JAR bootstrap entry point bypassing JavaFX module restrictions
    ├── MainApp.java           # JavaFX Application entry point
    ├── ScreenNavigator.java   # Focused screen routing role interface (ISP)
    ├── ViewNavigator.java     # Composite navigator interface (ScreenNavigator, AudioProvider, WindowProvider)
    ├── AudioProvider.java     # Audio accessor interface
    ├── WindowProvider.java    # Stage accessor interface
    ├── MainMenuView.java      # Title screen with centered navigation
    ├── MenuOverlays.java      # In-game modal overlay sheets (Settings and Tactical Manual)
    ├── GameModeSelectView.java# Mode selector with card-wide click handlers
    ├── BoardSelectView.java   # Theater selector with interactive preview cards
    ├── AbstractShipPlaceView.java # Shared deployment logic and UI
    ├── ShipPlaceView.java     # Drag-and-drop & auto fleet deployment (local modes)
    ├── NetworkShipPlaceView.java # Network deployment synchronization
    ├── ShipDockPane.java      # Dock tray for unplaced ships
    ├── BoardGridPane.java     # Interactive grid component
    ├── AbstractBattleView.java# Shared combat command center logic and UI
    ├── LocalBattleView.java   # Main combat view (AI & Hotseat)
    ├── NetworkBattleView.java # Authoritative peer combat view
    ├── GameOverView.java      # Victory/Defeat screen with MatchStatistics
    ├── NetworkGameOverView.java  # Network match outcome screen
    ├── PassScreen.java        # Hotseat turn privacy screen
    ├── MultiplayerLobbyView.java # Host / Join router
    ├── HostLobbyView.java     # QR invite and hosting listener
    ├── JoinLobbyView.java     # Join code input screen
    ├── GameAudio.java         # Audio abstraction (composes SfxAudio, MusicAudio, AudioSettings)
    ├── SfxAudio.java          # Sound effects role interface
    ├── MusicAudio.java        # Background music role interface
    ├── AudioSettings.java     # Volume/mute settings role interface
    ├── SilentAudio.java       # No-op audio stub for unit tests
    ├── SoundManager.java      # BGM and SFX player (GameAudio implementation)
    ├── SoundGenerator.java    # Procedural audio synthesizer fallback
    ├── DecorUtil.java         # Facade over the canvas decor renderers
    ├── AlertUtil.java         # Reusable modal dialog utilities (confirmation, alerts, disconnects)
    ├── CssClasses.java        # Centralized CSS style class constants
    ├── ImageResources.java    # Asset cache
    ├── QrCodeUtil.java        # ZXing QR code generator
    │
    ├── battle/                # Combat view UI components & viewport strategies
    │   ├── AlternatingPerspective.java # Hotseat alternating viewport strategy (OCP)
    │   ├── BattleLog.java     # Reusable combat message log component
    │   ├── BattlePerspective.java # Viewport perspective strategy interface
    │   ├── FixedPerspective.java # Single-player pinned viewport strategy (OCP)
    │   └── WeaponConsole.java # Weapon selection and ammo UI bar
    │
    ├── decor/                 # Procedural canvas animation renderers
    │   ├── CompassWatermark.java   # Decorative compass overlay
    │   ├── OceanRibbonRenderer.java# Flowing ribbon wave accents
    │   ├── OceanSceneRenderer.java # Layered ocean waves and sky gradients
    │   └── RadarSweepRenderer.java # Rotating radar sweep animation
    │
    └── quiz/                  # Nuclear authorization & resupply minigames
        ├── NuclearLaunchDialog.java
        ├── NuclearResupplyDialog.java
        ├── QuizBank.java
        └── QuizQuestion.java
```

---

## Controls & Shortcuts

### Play together on the same network

1. Connect both computers to the same Wi-Fi or wired LAN and open the game on both.
2. On the first computer, host a multiplayer game. Choose the Wi-Fi/Ethernet adapter from the list; virtual/VPN adapters are labeled. Wait for **Lobby ready**.
3. Select **Copy invite** and send the text to the other player.
4. On the second computer, open **Join a game** and paste the invite. The host IP, port, and four-digit code fill automatically. Alternatively, enter those three fields manually.
5. Select **Connect to game**. Both players enter a shared lobby showing the host, guest, and each player's Ready status.
6. Both players press **Ready** in the lobby to begin fleet deployment. After both fleets are placed and confirmed, the battle starts automatically.

Keep the host lobby open while joining. If the host cannot be reached, check the selected adapter and allow Java through the host's firewall on the private network. Guest Wi-Fi may prevent devices from reaching each other. The QR code contains the same invite text; scanning it is optional.

| Action | Control |
|:---|:---|
| **Place Ship** | Drag from Dock onto Grid or Click dock then target cell |
| **Rotate Ship / Weapon** | **`R`** key or **Right-Click**; **`R`** also rotates a ship while dragging |
| **Remove Placed Ship** | Click a placed ship or drag it back to the ship dock during deployment |
| **Move Placed Ship** | Drag a placed ship to a valid board position; invalid drops keep its original position |
| **Cancel Ship Drag** | Press **`Esc`** or drop outside the board and dock |
| **Fire Weapon** | Select weapon from weapon bar, then click target cell on enemy grid |
| **Auto Deploy** | Click `AUTO PLACE` in deployment dock |

---

## Prerequisites & Build

### Requirements
- **Java 21** or higher
- **Maven 3.8+**

### Compile & Run

```bash
# Compile and build classes
mvn clean compile

# Launch the game with JavaFX Maven Plugin
mvn javafx:run
```

### Package Executable JAR

```bash
mvn clean package

# Run the shaded executable fat JAR (Linux / macOS):
java -jar target/naval-command-1.0.0.jar

# Or on Windows (when packaged with the build-windows profile):
# mvn clean package -Pbuild-windows
# java -jar target/naval-command-1.0.0-windows.jar
```

### Run Tests

```bash
mvn test
```

The test suite (JUnit 5) runs headlessly - **no JavaFX runtime or display required** - comprising **54 tests across 18 test suites**:
- **AI Strategies:** `ParityHunterTest`
- **Combat & Resolution:** `BattleServiceTest`, `ShotResolverTest`
- **Combat Viewport Strategies:** `BattlePerspectiveTest`
- **Controller & DIP:** `GameControllerDIPTest` (hotseat lifecycle, online mode, DI seams)
- **Domain Models:** `TurnTest`, `MatchStatisticsTest`
- **Weapons & Polymorphism:** `BlastPatternRotationTest`, `WeaponPolymorphismTest`
- **Networking:** `NetMessageCodecTest`, `NetworkGameSessionTest`, `NetworkBattleMediatorTest`, `NetworkSessionIntegrationTest`
- **Persistence:** `GameSaveMapperTest`, `SaveGameServiceTest`, `SaveGameIntegrationTest`
- **Audio & Navigation Abstractions:** `InterfaceSegregationTest`, `SilentAudioTest`

---

## Key OOP Principles Implemented

- **Strategy Pattern & Open/Closed Principle (OCP):**
  - Viewport perspective in [`LocalBattleView`](src/main/java/com/battleship/view/LocalBattleView.java) is abstracted via [`BattlePerspective`](src/main/java/com/battleship/view/battle/BattlePerspective.java) with [`FixedPerspective`](src/main/java/com/battleship/view/battle/FixedPerspective.java) for vs-AI and [`AlternatingPerspective`](src/main/java/com/battleship/view/battle/AlternatingPerspective.java) for Hotseat, eliminating procedural conditionals in the view layer.
- **Interface Segregation Principle (ISP):**
  - Screen routing is isolated into [`ScreenNavigator`](src/main/java/com/battleship/view/ScreenNavigator.java), allowing screens to depend only on navigation without coupling to audio or stage providers.
  - [`Player`](src/main/java/com/battleship/model/Player.java) directly exposes focused component accessors (`primaryGrid()`, `ammoReadout()`), eliminating the Middle Man code smell.
- **Polymorphism over Conditionals (OCP / LSP):**
  - The `Weapon` hierarchy (`StandardShell`, `SalvoBarrage`, `NuclearWarhead`) encapsulates blast pattern generation, ammo constraints, launch authorization protocol (`requiresAuthorization()`), and audio dispatch (`playFiringSound(...)`), eliminating `instanceof` checks and switch statements.
- **Strict Fog-of-War Encapsulation:**
  - Opponents observe enemy waters exclusively through `TrackingGrid` populated by `MarkerStatus` records. Private fleet positions on `PrimaryGrid` are never exposed or serialized across the network.
- **Single Responsibility Principle (SRP):**
  - `MatchStatistics` encapsulates post-battle metric computation (shots, hits, misses, accuracy, sunk ships).
  - `GameSaveMapper` decouples serialization DTO mapping from controller orchestration.
  - UI components like `BattleLog` and `WeaponConsole` manage their own rendering and layout.
- **Information Expert:**
  - `ShipType` encapsulates its own UI asset names (`getAssetName()`), eliminating switch statements in image resource caches.
- **Composition over Inheritance:**
  - `SmartAI` and `HuntTargetAI` compose independent `TargetingQueue` and `ParityHunter` components rather than sharing brittle inheritance trees.
- **Strict Encapsulation & Immutability:**
  - `Ship` hit tracking uses an internal `Set<Coordinate>` for idempotent hit registration, preventing duplicate damage counts.
  - `Coordinate` is an immutable record.
  - Collections returned by domain queries are unmodifiable.
- **Interface Segregation & Dependency Inversion:**
  - Views interact with domain models via narrow read-only interfaces (`FleetReadout`, `AmmoReadout`).
  - Audio is accessed through `GameAudio`, composed of distinct `SfxAudio`, `MusicAudio`, and `AudioSettings` roles.
  - `SilentAudio` serves as a headless Null Object implementation for unit tests.
- **No Primitive Obsession:**
  - Self-documenting enums and value types replace raw primitives: `Orientation` (direction), `Turn` (turn ownership), `Role` (HOST/CLIENT), `CellStatus`, and `MarkerStatus`.
- **Domain Independence:**
  - All domain and AI logic executes independently of JavaFX or any graphical library, enabling fast, headless unit and integration testing.
