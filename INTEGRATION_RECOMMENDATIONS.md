# Integration Recommendations

## Top Table

| Layer | Current State | Recommended Integration | Reason |
| --- | --- | --- | --- |
| Main bridge | Python HTTP bridge on `8080` | Keep as local primary orchestration bridge | Already central to GUI, datapoints, lens, and routing |
| GUI | Three.js + static HTML | Preserve and evolve additively | User explicitly wants GUI preserved |
| Retrieval | Lens sidecar plus DB scans | Keep retrieval in sidecar, inject top-5 cards only | Avoid context crowding and keep behavior shaping compact |
| Behavioral memory | Chat memory, likes, topology profile | Use nominal context plus weighted behavior pack | Thin feedback should not swamp real context |
| Karoo | Sidecar producing candidates and reports | Keep proposal-only until full proof loop is wired | Safe evolution path without auto-mutation |
| Testing lab | Partially present in Java notes suite and scripts | Consolidate into explicit testing lab subsections | Current evidence exists but is scattered |
| Persistence | SQLite plus logs plus local queues | Keep local-first, harden writer path, then twin remote | DB locks are the current reliability bottleneck |
| Cloud twin | Planned | Oracle Always Free VM plus persistent Cloudflare tunnel | Good fit for low-cost mirrored continuity |
| Tunnel | Rotating `trycloudflare` link file exists | Add tunnel watch and rebind logic | Needed for durable public reachability |
| Email | Not wired | Defer until credentials are available | Not safely automatable without auth |

## Best-Fit Stack Based On Today

1. Local primary runtime:
   `risc_bridge_server.py` on `8080`, `house_inference_engine.py` on `11435`,
   `logic_blockchain_shipper.py` on `18081`, `topology_loop.ps1` for Karoo scans.
2. Compact behavior shaping:
   keep behavior context in the lens layer, not in the main chat prompt bulk.
3. Retrieval path:
   query local DB first, rank by trust and route fit, inject only the top five
   behavior or logic cards when relevant.
4. Karoo path:
   compare project-local chunks, successful ledger blocks, and proof signals;
   never jump straight to autonomous file mutation.
5. Twin path:
   mirror hashes, approved reports, reduced behavior context, and checkpoint
   metadata before mirroring any heavier payloads.

## Immediate Technical Priorities

1. Fix SQLite lock pressure before adding more autonomous writers.
2. Replace the current build-route hardcoded summary with a true routed
   assessment/generation path.
3. Keep the behavior pack capped and auditable.
4. Keep cloud persistence twin-oriented, not migration-oriented.

## Known Risks

1. `database is locked` faults can invalidate retrieval, feedback, benchmarks,
   and sync records.
2. The build route is still partially stubbed in the live bridge.
3. Likes currently describe user-intent approval more than answer-quality
   precision.
4. Quick tunnels rotate; named or persisted Cloudflare tunnel config is better
   once credentials are available.

