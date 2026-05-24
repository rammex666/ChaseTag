# ChaseTag - Project Documentation

ChaseTag is a Minecraft minigame project designed with a multi-server architecture. It features a Lobby system, dynamic Game servers, and a Velocity proxy for seamless player movement and server management.

## Architecture Overview

The project follows a distributed architecture:
- **Proxy (Velocity):** Entry point for players, handles dynamic server registration via Redis.
- **Lobby (Spigot/Paper):** Where players congregate, join matches, manage duels, and participate in tournaments. Persistent data is stored in MongoDB.
- **Game (Spigot/Paper):** Temporary instances where the actual Chase Tag game takes place.
- **Common:** Shared logic, messaging protocols, and data models used across all modules.

## Technology Stack

- **Language:** Java 17+
- **Build Tool:** Gradle (Multi-module project)
- **Platforms:**
  - **Spigot/Paper API:** For Game and Lobby modules.
  - **Velocity API:** For the Proxy module.
- **Infrastucture:**
  - **Redis:** Primary communication layer (Pub/Sub) for cross-server events (e.g., server readiness, game end).
  - **MongoDB:** Persistent storage for player statistics, ranks, and tournament data (used in Lobby).
  - **Pterodactyl API:** Used for dynamic game server orchestration (spinning up/down instances).

## Modules Breakdown

### 1. `common`
- **Package:** `fr.rammex.chasetag.common`
- **Responsibility:** Contains shared classes like `ServerState`, `RedisChannel`, and message models (`GameEndMessage`, `ServerReadyMessage`).
- **Key Classes:**
  - `MessageSerializer`: Handles JSON serialization for Redis messages.

### 2. `game`
- **Package:** `fr.rammex.chaseTag.game` (Note the capital 'T' in `chaseTag`)
- **Responsibility:** Core game logic.
- **Key Components:**
  - `ArenaManager`: Manages game maps/arenas.
  - `GameManager`: Handles game states (WAITING, STARTING, IN_GAME, FINISHED).
  - `ScoreboardManager`: Real-time scoreboard updates.
  - `TimerManager`: Custom timer system for game events.
  - `Role`: Defines player roles (STAFF, PLAYER, SPEC).

### 3. `lobby`
- **Package:** `fr.rammex.chasetag.lobby`
- **Responsibility:** Matchmaking and persistent systems.
- **Key Components:**
  - `DuelRequestManager`: Handles 1v1 challenges.
  - `TournamentManager`: Logic for larger organized competitions.
  - `PterodactylClient`: Integration for dynamic server scaling.
  - `LobbyMenu`: GUI system for joining games and settings.

### 4. `velocity`
- **Package:** `fr.rammex.chasetag.velocity`
- **Responsibility:** Proxy-level routing and server registration.
- **Key Logic:** Listens to Redis for `SERVER_READY` messages to dynamically register game servers in the proxy and `GAME_END` to unregister them.

## Key Workflows

### Server Lifecycle
1. **Game Server Boot:** A game server starts (via Pterodactyl), initializes its arena, and publishes `SERVER_READY` to Redis.
2. **Proxy Registration:** `velocity` receives the message and registers the server in the proxy.
3. **Lobby Redirection:** The `lobby` module detects the available server and sends players to it.
4. **Game End:** Once the match finishes, the game server publishes `GAME_END` and likely shuts down or resets.

## Development Guidelines

- **Naming Conventions:** 
  - Be careful with package naming inconsistencies (`chaseTag` vs `chasetag`). Stick to the established package for each module.
  - Follow standard Java naming conventions for classes (PascalCase) and methods/variables (camelCase).
- **Communication:** Always use `common` messages for Redis communication to ensure compatibility between modules.
- **Database:** 
  - Use `Jedis` for Redis.
  - Use `MongoManager` for MongoDB interactions in the lobby.
- **UI:** Menus in Spigot modules are implemented using a custom listener-based system (`MenuListener` in Lobby).

## Commands

- **Lobby:** `/chasetag` (play), `/duel` (challenge players).
- **Game:** `/arena` (setup), `/testgame`, `/leave`.
- **Roles:** `/setplayer`, `/setspec`, `/setstaff`.

## Modification History

All future modifications to the project must be logged here with the date of the change.

- **2026-05-02:** Initial project inspection and creation of `GEMINI.md`.
- **2026-05-02:** Enhanced Tournament System: Added dual-readiness check before match start, added 'ready' toggle block in lobby inventory (slot 8), and improved thread safety in `TournamentManager`.
- **2026-05-02:** Integrated Discord Bot (JDA) for administrative tasks: Whitelist system (add/remove) and In-game rank management via slash commands. Added `discord` configuration section in `config.yml`.
- **2026-05-02:** Added Game Logging system: Match starts are now logged to a Discord channel (configurable via `/setlogchannel`).
- **2026-05-02:** Improved Tournament Flow: Players can now toggle ready/not ready status. Matches require an administrator's authorization (via "Lancement des matchs" in Admin Menu) AND dual-readiness to start. Ready status is automatically reset after each game.
- **2026-05-05:** Fixed Discord logging for game starts.
- **2026-05-05:** Added In-game Staff Module: /mute and /ban commands. Sanctions are pending and require validation via Discord using `/acceptstaff <id>`. Added staff-validation-channel-id to config.yml.
- **2026-05-05:** Added /unmute and /unban commands (direct).
- **2026-05-05:** Added /staff mode: Puts staff in GM 3, gives a compass to see and teleport to active games (cross-server), and an ice tool to freeze/unfreeze players.
- **2026-05-05:** Added custom Cobweb item in-game: Spawns a 2x2 cobweb box for 2 seconds upon right-click. Item is given at the start of each round.
- **2026-05-05:** Updated Discord whitelist command to support offline players: Automatically fetches UUID from Mojang API if the player has never joined.
- **2026-05-06:** Modification de l'item du mode Staff : Remplacement de la boussole par une Nether Star pour la téléportation inter-serveurs.
- **2026-05-20:** Mise à jour du système de tournoi : Suppression de la Phase 4, ajout des Quarts de finale et de la Petite Finale avant la Finale. Correction de la configuration des 8èmes de finale (8 matches) et amélioration de l'affichage de la phase suivante dans le menu admin. Renommage des poules (1, 2, 3...) en lettres (A, B, C...) dans toute l'interface.
- **2026-05-24:** Ajout des statistiques de meilleur temps en tant que Chasseur (temps le plus court) et meilleur temps en tant que Chassé (temps le plus long). Les statistiques sont suivies pendant la partie et sauvegardées dans MongoDB via le Lobby.
- **2026-05-24:** Ajout d'une commande Discord `/participants` pour obtenir la liste des joueurs encore en lice et des joueurs éliminés du tournoi.
- **2026-05-24:** Ajout d'une commande Discord `/stats <pseudo>` qui génère une image stylisée contenant le skin du joueur et ses statistiques (victoires, défaites, meilleurs temps, etc.) sur un fond personnalisé. Le fond doit être placé dans `plugins/ChaseTagLobby/assets/stats_bg.png`.
- **2026-05-24:** Ajout d'une commande Discord `/wllist` pour afficher la liste de tous les joueurs inscrits dans la whitelist.
- **2026-05-24:** Correction d'un crash au démarrage du module Game lié à une URI MongoDB vide ou mal formatée. Ajout d'une valeur par défaut dans `config.yml` et amélioration de la robustesse de `MongoManager` (gestion automatique des caractères spéciaux et repli sur localhost en cas d'erreur).
- **2026-05-24:** Modification de la méthode d'assignation des joueurs aux poules et matches de tournoi : remplacement de la sélection par clic sur tête par une saisie du pseudo dans le chat pour plus de précision et support des joueurs hors-ligne (tant qu'ils sont whitelists).
- **2026-05-24:** Ajout du suivi des points cumulés par les joueurs dans le Lobby. Les joueurs reçoivent désormais un message récapitulatif des points gagnés à leur retour d'une partie.
- **2026-05-24:** Remplacement du bloc de statut "Prêt" par une commande `/ready` (ou `/pret`). Les joueurs reçoivent une notification dans le chat et leur adversaire est également prévenu. Le système est désormais insensible à la casse.
