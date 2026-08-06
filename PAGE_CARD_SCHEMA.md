# Page Card Schema

## Purpose

Define the exact single-page contract each language agent receives.

The goal is to eliminate prompt drift and keep every agent focused on one page
only.

## Page Card Shape

```json
{
  "pageId": "PAGE_AUTH_ROUTER_001",
  "filePath": "src/auth/router.py",
  "language": "python",
  "nodeType": "page",
  "topology": {
    "parentNode": "auth_module",
    "siblingNodes": ["auth_model", "auth_service"],
    "dependencyNodes": ["shared_logging", "http_contracts"],
    "entryPoints": ["POST /auth/login"],
    "exitPoints": ["auth_result", "session_token"]
  },
  "performativeContract": {
    "mode": "propose",
    "routeOwner": "auth_router",
    "proofTarget": "parse_compile_route_fit",
    "storageRole": "page_candidate_only"
  },
  "languageKernelRef": "python_fastapi_page_kernel_v1",
  "acceptanceTestRef": "AUTH_ROUTER_PAGE_TESTS_V1",
  "dependencySlice": {
    "importsAllowed": ["fastapi", "typing", "shared.logging"],
    "importsRequired": ["fastapi.APIRouter"],
    "importsForbidden": ["sqlalchemy", "torch"]
  },
  "recursiveContextPolicy": {
    "startMode": "minimal_page_only",
    "maxActiveModels": 3,
    "preferredShape": "solo_or_triplet",
    "expandOnlyWhen": ["missing_dependency_context", "proof_failure", "route_ambiguity"],
    "expansionOrder": ["dependency_slice", "sibling_contract", "parent_topology"]
  },
  "editScope": {
    "mayEdit": ["src/auth/router.py"],
    "mustNotEdit": ["src/auth/service.py", "src/auth/model.py"]
  }
}
```

## Required Fields

- `pageId`
- `filePath`
- `language`
- `nodeType`
- `topology`
- `performativeContract`
- `languageKernelRef`
- `acceptanceTestRef`
- `dependencySlice`
- `recursiveContextPolicy`
- `editScope`

## Prompt Projection

Each page card should project into the agent prompt as:

```text
PAGE_ID: <pageId>
FILE_PATH: <filePath>
LANGUAGE: <language>
TOPOLOGY_PARENT: <parentNode>
TOPOLOGY_SIBLINGS: <siblingNodes>
DEPENDENCIES: <dependencyNodes>
ENTRY_POINTS: <entryPoints>
EXIT_POINTS: <exitPoints>
PERFORMATIVE_MODE: <mode>
ROUTE_OWNER: <routeOwner>
PROOF_TARGET: <proofTarget>
LANGUAGE_KERNEL_REF: <languageKernelRef>
ACCEPTANCE_TEST_REF: <acceptanceTestRef>
ALLOWED_IMPORTS: <importsAllowed>
REQUIRED_IMPORTS: <importsRequired>
FORBIDDEN_IMPORTS: <importsForbidden>
RECURSIVE_START_MODE: <startMode>
MAX_ACTIVE_MODELS: <maxActiveModels>
EXPAND_ONLY_WHEN: <expandOnlyWhen>
EXPANSION_ORDER: <expansionOrder>
MAY_EDIT: <mayEdit>
MUST_NOT_EDIT: <mustNotEdit>
```

## Language Kernel Companion

Each page card should resolve one matching language kernel:

```json
{
  "languageKernelRef": "python_fastapi_page_kernel_v1",
  "language": "python",
  "style": "typed_compact_service_page",
  "parseCommand": "python -m py_compile",
  "testMode": "page_scope",
  "rules": [
    "no global side effects",
    "single-page ownership",
    "explicit imports only",
    "route fit must remain stable"
  ]
}
```

## Agent Outputs

### Author agent

```json
{
  "pageId": "PAGE_AUTH_ROUTER_001",
  "candidateCode": "...",
  "claimedImports": ["fastapi", "typing"],
  "expectedProofs": ["parse", "route_fit", "import_scope"]
}
```

### Verifier agent

```json
{
  "pageId": "PAGE_AUTH_ROUTER_001",
  "status": "fail",
  "deficiencies": [
    {
      "type": "missing_required_import",
      "detail": "APIRouter not imported"
    }
  ]
}
```

### Repair agent

```json
{
  "pageId": "PAGE_AUTH_ROUTER_001",
  "revisedCode": "...",
  "fixedDeficiencies": ["missing_required_import"]
}
```

## Storage Tables

- `TOPOLOGY_PAGE_CARDS`
- `LANGUAGE_KERNELS`
- `PAGE_CONTEXT_EXPANSIONS`
- `PAGE_AGENT_OUTPUTS`
- `PAGE_DEFICIENCY_REPORTS`
- `PAGE_REPAIR_RESULTS`

## Rule

No agent should ever see the whole project prompt if a page card exists.
