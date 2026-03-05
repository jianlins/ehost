# EHOST-002: Improved Attribute Display in IAA Report HTML Output
## Summary
Redesigned how annotation attributes are displayed in IAA (Inter-Annotator Agreement) HTML reports to improve readability and make differences between annotators easier to identify.
## Problem
Previously, attributes were displayed in a single "Attributes" row with all attribute name-value pairs concatenated together (e.g., `AttrName1=Value1; AttrName2=Value2`). This made it difficult to:
- Compare individual attribute values across annotators
- Identify which specific attribute differed
- See when an annotator was missing a specific attribute
## Solution
Each attribute is now displayed as its own row in the comparison table:
- **First column**: Shows the attribute name with `&nbsp;&nbsp;` (non-breaking space indent) to visually distinguish attributes from class-level rows (e.g., Class, Span, Annotation Text)
- **Value columns**: Each annotator column shows only the attribute value
- **Missing attributes**: If an annotator's annotation does not have a particular attribute, the cell is left empty with a gray background
- **Differences**: Value mismatches are highlighted in red (`#FFD0D0`)
### Example Output
| | Annotator: A | Annotator: B |
|---|---|---|
| Class | Medication | Medication |
| &nbsp;&nbsp;Dosage | 10mg | 20mg (red) |
| &nbsp;&nbsp;Route | oral | |
## Files Modified
- `src/main/java/report/iaaReport/genHtml/GenHtmlForMatches.java` — Matched annotations report
- `src/main/java/report/iaaReport/genHtml/GenHtmlForNonMatches.java` — Unmatched annotations report
- `src/main/java/report/iaaReport/genHtml/GenHtmlForNonMatches2.java` — Unmatched annotations report (alternate version)
## Version
1.39 (Unreleased)