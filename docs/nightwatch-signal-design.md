# Nightwatch Signal design system

Figma file: [TrustGuard — Nightwatch Signal Mobile](https://www.figma.com/design/YfyIXgrWlieuJpmVwWP1XX)

## Direction

Nightwatch Signal uses a quiet midnight-navy canvas, cyan for primary actions and verification, and restrained violet for secondary emphasis. Bricolage Grotesque carries display text, DM Sans carries interface copy, and JetBrains Mono is reserved for URLs and technical references. The shield mark and restrained scan motion make the identity recognizable without making every surface look like a warning.

## Semantic palette

| Token | Nightwatch dark | Daylight |
| --- | --- | --- |
| Background | `#070B16` | `#F2F6FC` |
| Surface | `#0C1223` | `#FFFFFF` |
| Raised | `#131B2F` | `#E9EEF8` |
| Elevated | `#1A2139` | `#DFE7F5` |
| Border | `#303B59` | `#C7D1E6` |
| Primary text | `#E8F2FF` | `#0E172B` |
| Secondary text | `#96A8C7` | `#3D4D69` |
| Signal cyan | `#2EE3F7` | `#05829E` |
| Signal violet | `#A67AFA` | `#6140BA` |
| High risk | `#FF575E` | `#B81F33` |
| Suspicious | `#FFB547` | `#915705` |
| No obvious threat | `#47DEAD` | `#0A7556` |
| Inconclusive | `#9CADD1` | `#4D5E82` |

The palettes, type ramp, spacing, and corner-radius tokens are represented in Figma and [Theme.kt](../app/src/main/java/ai/scamchain/mobile/ui/theme/Theme.kt). The Figma Starter plan available to this account supports one mode per collection, so dark and light colors are stored as separate semantic collections.

## Reusable patterns

The Figma foundations page contains variable-bound specimens for the primary action, message input, risk verdicts, evidence card, and five existing navigation destinations. Compose reuses the same palette and typography through the shared theme, risk components, cards, text fields, and bottom navigation.

The actual app flows remain backed by existing Firebase and API behavior: sign-in, registration, reset, text/screenshot analysis, chat, report preparation, and session-only activity references. The Activity screen states that the API does not store or file reports. Image review states that screenshot pixels are not covered by text masking.

## Figma screen coverage

The Figma account's Starter-plan MCP call limit was reached while building and validating the component specimens. The file currently contains the foundations/components page and an empty application-screens page; screen frames still need to be added when Figma MCP calls are available again. The Compose implementation includes the existing splash, onboarding, auth, dashboard, scan, results, assistant, education, activity, settings, permission setup, screenshot review, and report routes.
