# Non-Functional Requirement: Usability

**ID:** NFR-03  
**Version:** 1.0  
**Date:** 2026-01-30  
**Status:** Draft

---

## 1. Statement

The FireStock system shall be easy to learn and efficient to use across all supported devices, accommodating users with varying technical proficiency and operating in challenging station environments.

## 2. Rationale

Firefighters are not IT professionals; they need a tool that works without extensive training. Mobile use in apparatus bays presents challenges: poor lighting, users who may have just removed heavy gloves, and time pressure. If the system is cumbersome, users will resist adoption or find workarounds.

## 3. Specification

### 3.1 Learnability

| Metric | Target |
|--------|--------|
| Time to complete first inventory check | < 15 minutes with no prior training |
| Time to complete inventory check after familiarisation | < 10 minutes per apparatus |
| Training required | Maximum 30-minute introduction session |

### 3.2 Efficiency

| Operation | Target |
|-----------|--------|
| Taps/clicks to start inventory check | ≤ 3 |
| Taps/clicks to mark item as present (scan) | 1 (scan) |
| Taps/clicks to mark item as present (manual) | ≤ 3 |
| Taps/clicks to report damaged item | ≤ 4 |

### 3.3 Device-Specific Requirements

| Device | Requirements |
|--------|--------------|
| **Mobile** | Touch targets minimum 48×48 pixels; single-hand operation possible; portrait orientation primary |
| **Tablet** | Optimised for landscape; support for split-view (list + detail); larger touch targets |
| **Desktop** | Keyboard navigation; efficient bulk operations; denser information display |

### 3.4 Environmental Considerations

| Condition | Accommodation |
|-----------|---------------|
| Poor lighting | High-contrast UI; avoid low-contrast greys |
| Post-glove use | Large touch targets; avoid precision gestures |
| Time pressure | Minimal steps; clear progress indication |
| Interruptions | State preserved; easy to resume |

## 4. Design Principles

1. **Progressive disclosure:** Show essential information first; details on demand
2. **Forgiveness:** Easy to undo mistakes; confirmation for destructive actions
3. **Consistency:** Same patterns across devices; predictable behaviour
4. **Feedback:** Clear indication of success, failure, and progress
5. **Accessibility:** WCAG 2.1 AA compliance; screen reader support

## 5. Verification

| Method | Description |
|--------|-------------|
| **Usability testing** | Observe 5+ firefighters completing tasks; measure time and errors |
| **Heuristic evaluation** | Expert review against Nielsen's heuristics |
| **Accessibility audit** | Automated tools (axe, Lighthouse) + manual testing with screen reader |
| **Field observation** | Watch real use in apparatus bay environment |

## 6. Specific UI Requirements

### 6.1 Inventory Check Flow (Mobile)
- Apparatus selection: one tap from home screen
- Compartment list: scrollable, large tap targets
- Item verification: scan barcode OR tap item name to mark present
- Issue reporting: prominent "Report Issue" button on each item
- Progress indicator: "12 of 47 items checked" always visible
- Completion: clear summary with option to review issues

### 6.2 Search (All Devices)
- Search box always accessible
- Results appear as typing (debounced)
- Filter by: station, apparatus, equipment type, status
- Clear indication of which filters are active

### 6.3 Error States
- Network errors: clear message, retry button, do not lose entered data
- Validation errors: inline, next to the relevant field
- Not found: helpful message, suggest alternatives

## 7. Acceptance Criteria

- [ ] 5 firefighters complete inventory check in under 15 minutes on first attempt
- [ ] Mobile UI passes touch target size guidelines (48×48 px minimum)
- [ ] Desktop UI is fully keyboard-navigable
- [ ] WCAG 2.1 AA compliance verified by automated and manual testing
- [ ] Users rate system as "easy to use" (≥4/5) in post-pilot survey

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |