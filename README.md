# Thor Statue – cinematic contract cutscene (Fabric, Minecraft 26.3)

Right-click the Thor statue → the player kneels, mouse-look is locked, and an ~8 second cutscene plays
(dolly-in on the plinth → ground lightning → pull-back → world gets covered by dark cloud sea / fog →
3D Thor constellation draws itself above the statue → "Patung Thor" contract panel slides in).

## Get the statue
Only via command (the item is in no creative tab):

    /give @s thorstatue:thor_statue

## Build
Needs JDK 25. The GitHub Action in `.github/workflows/build.yml` builds it on push, or locally:

    gradle build            # jar -> build/libs/

Edit `gradle.properties` if your Loom / Fabric API versions differ (`loom_version`, `fabric_api_version`).
Runtime requires: Fabric Loader ≥ 0.19, Fabric API ≥ 0.160.5+26.3, GeckoLib 5.5.7 (jar included in `libs/` for compiling).

## Layout
- `block/` statue block + GeckoLib block entity (idle loop, `power_up` plays during the cutscene)
- `client/cutscene/` `CameraPath` (Catmull-Rom spline camera), `CutsceneController` (timeline/state)
- `client/render/FxRenderer` all VFX pass; `Constellation` 3D star graph; GLSL in `assets/thorstatue/shaders/core/cutscene_fx.*`
- `mixin/client/` Camera override, mouse lock, frame-graph VFX pass, kneel pose
- `client/ContractScreen` contract panel + tab view (UI only – no weapon forging / skills logic)

## Tuning
Camera keys: `CameraPath.build` (statue-space coordinates). Constellation: `Constellation.java`.
VFX timing: `FxRenderer.render` (`cover`, `fogUp`, `constAlpha` curves). Panel timing: `CutsceneController.PANEL_AT`.
