# Game Engine Bridges

SIMS1337 connects with real-time 3D game engines to render hex grid telemetry, model orb positions, and agent interaction states.

## Supported Engine Bridges
- **Unity**: `Unity_Ollama_Bridge.cs` (UnityWebRequest + Coroutines)
- **Unreal Engine**: `Unreal_Ollama_Bridge.py` (Python API + Blueprint Nodes)
- **Godot Engine**: `Godot_Ollama_Bridge.gd` (HTTPRequest + Signals)

## Integration Setup
Connect your game engine client to `http://localhost:1337/api/status` for real-time JSON telemetry feeds.
