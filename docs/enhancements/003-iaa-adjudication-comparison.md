# EHOST-003: Adjudication Comparison in IAA Reports

## Summary
When adjudication annotations are available in the project's `adjudication/` folder, the IAA report now automatically includes additional pages comparing each annotator's annotations against the adjudicated gold standard.

## Problem
Previously, the IAA report only compared annotations between human annotators (e.g., Annotator A vs Annotator B). There was no way to see how each annotator's annotations compared against the final adjudicated annotations, making it difficult to assess individual annotator performance against the agreed-upon gold standard.

## Solution
The system now detects adjudication `.knowtator.xml` files in the `projectDir/adjudication/` folder and loads them as a special "Adjudication" annotator. This integrates seamlessly with the existing IAA infrastructure to produce:

- **Pairwise agreement tables**: Each annotator vs Adjudication (precision, recall, F-measure)
- **N-way comparison**: All annotators plus Adjudication (e.g., 3-way if 2 annotators + adjudication)
- **Unmatched detail pages**: Per-annotator pages showing where annotations differ from adjudication
- **Per-class breakdowns**: Unmatched details filtered by annotation class
- **Dedicated index section**: An "Adjudication Comparison" section in the report index with direct links

### How It Works
1. Before IAA analysis, `AdjudicationLoader` checks for `projectDir/adjudication/` folder
2. If `.knowtator.xml` files are found, annotations are loaded with annotator name "Adjudication"
3. "Adjudication" is added to the selected annotators list for comparison
4. The existing analysis pipeline runs comparisons including Adjudication
5. The report index adds a highlighted "Adjudication Comparison" section
6. After report generation, adjudication annotations are cleaned up from the Depot

### Example Report Index (with adjudication)
- Class and span matcher (includes Adjudication in pairwise/N-way tables)
- Matched Details - SUMMARY
- Unmatched Details for Annotator A - SUMMARY
- Unmatched Details for Annotator B - SUMMARY
- Unmatched Details for Adjudication - SUMMARY
- **Adjudication Comparison**
  - Annotator A vs Adjudication - Unmatched SUMMARY
  - Annotator B vs Adjudication - Unmatched SUMMARY
  - Adjudication vs All Annotators - Unmatched SUMMARY

## Files Modified
- `src/main/java/report/iaaReport/AdjudicationLoader.java` — New class to load adjudication annotations
- `src/main/java/report/iaaReport/IAA.java` — Integrated adjudication loading into report generation pipeline
- `src/main/java/report/iaaReport/genHtml/GenIndex.java` — Added Adjudication Comparison section to index

## Version
1.39 (Unreleased)
