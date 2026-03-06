# ADR-0008: "Luminous" Dark Design System

## Status
Accepted

## Context
The app needed a visual identity. Options: Material You defaults, light theme, custom dark theme. The app is a music/audio tool — its aesthetic should reflect the domain.

## Decision
Custom dark design system ("Luminous") inspired by professional audio equipment and studio environments:
- 5-layer surface hierarchy from near-black (#08081A) to dark indigo (#252545)
- Coral accent (#E94560) — warm against cool darks, high contrast, not the typical blue/green
- 8dp spacing grid, 24dp card corner radius
- Monospace for data (BPM), sans-serif for labels
- Material Design text opacity guidelines (87/60/40% white)
- Responsive glow and pulse animations tied to audio signal

## Consequences
- **Pro:** Distinctive identity — doesn't look like a default Material app
- **Pro:** Dark theme is functional for the use case (stage, dim rooms, studio)
- **Pro:** Coral accent stands out on app icon and in-app, creating brand recognition
- **Con:** Fully custom means no automatic Material You dynamic color support
- **Con:** Accessibility contrast ratios need manual verification (tertiary text at 40% may be too low for some users)
