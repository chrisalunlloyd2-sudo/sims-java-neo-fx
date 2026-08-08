# Changelog

All notable changes to **SIMS1337** will be documented in this file.

## [0.2.0-SNAPSHOT] - 2026-08-08
### Added
- **StrainRatePhysicsKernel**: Implemented strain rate equation $\dot{\gamma} = \Delta \text{deformation} / \Delta t$, dynamic viscosity $\eta = \text{base} \cdot (1 + k \cdot \dot{\gamma})$, internal stress $\sigma = \eta \cdot \dot{\gamma}$, and fastmem interstitial cell reloads.
- **GitSecurityScrubber**: Automated redaction of OAuth tokens, GitHub PATs, and passwords.
- **BruteFoundryCronPipeline**: Scheduled code block mining tied to Gossip Quorum votes.
- **Moltbook Logger**: Unrestricted full model chat feed with ANSI color-coding and 2KB auto-archiving.
- **ScreenshotRecordingLab**: Autonomous snapshot recording suite.
