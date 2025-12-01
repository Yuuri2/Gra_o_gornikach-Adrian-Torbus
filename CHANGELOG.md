# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

The versioning system is based on [Semantic Versioning](https://semver.org/spec/v2.0.0.html), but instead of
a PATCH number, it uses the number of commits since the last major version tag.

## [1.1.x] - 2025-11-26

### Added
- server can send commands to the client
- server can broadcast commands to all clients
- `UpdateState*` command classes
- `GameState` interface and concrete records for some game elements:
  `BoardInfo`, `BoardSquareInfo`, `PlayerInfo` and `PlayerListInfo`


## [1.0.x] - 2025-11-11

### Added
- `GetInfo*` command classes
- `LeaveGame*` command classes
- `JoinGame*` command classes
- `Handshake*` command classes
- server logger

### Fixed


## [0.x.x] - 2025-11-10
- `Echo*` command classes
- basic functionality of the server
- project structure: 3 separate modules (lib, server, common)
