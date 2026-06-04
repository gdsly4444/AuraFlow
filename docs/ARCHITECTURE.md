# Aura — Clean Architecture

## Modules (dependency rule: outer → inner only)

```
app (presentation + Android entry)
 └── data (implementations, Room, network, Camera, workflow service)
      └── domain (pure Kotlin: models, repository contracts, use cases)
```

- **domain**: No Android dependencies. Business rules and orchestration via use cases.
- **data**: Implements `domain.repository` interfaces; owns persistence, DashScope, ambient capture, `MomentWorkflowEngine`.
- **app**: Fragments, ViewModels, ViewBinding; depends on **domain** use cases only (plus `AmbientCapturePortImpl.bind` for Camera preview).

## Dependency injection

Manual composition root: `app/di/AppContainer.kt` (wires repositories, use cases, workflow engine).
Hilt is not used — incompatible with this project's AGP 9 toolchain.

## Key flows

| Flow | Use case | Repositories |
|------|----------|--------------|
| Home list | `ObserveHomeListUseCase` | cards + active workflows |
| Generating banner | `ObserveGeneratingStatusUseCase` | workflows |
| Delete card | `DeleteMomentCardUseCase` | cards |
| Capture + workflow | `CaptureAmbientMomentUseCase` | `AmbientCapturePort`, `StartMomentWorkflowUseCase` |
| Detail | `GetMomentCardUseCase` | cards |

## Presentation rules

- ViewModels never reference `AuraApplication`, Room, Retrofit, or `MomentWorkflowService` directly.
- Fragments handle UI only (dialogs, MapView lifecycle, `bindCaptureSession` before capture).
