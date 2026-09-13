# Ordered and Highlighted Attribute Lists

**Date**: 2026-09-13  
**Issue**: [#29](https://github.com/jianlins/ehost/issues/29)  
**Status**: ✅ Completed  
**Contributors**: Jianlin Shi, GitHub Copilot CLI

---

## 📋 Summary

The attributes of an annotation are now listed in a predictable order — by default the
order of the schema configuration file — on the annotation editor panel and on the
side by side comparison panel of the adjudication mode. Attributes whose values differ
between the two annotations shown side by side are painted in **bold dark red** on a
pale yellow background.

Both behaviours are configurable in **System Config → Annotation Display** and are
stored in `eHOST.sys`.

---

## 🔍 Problem

An annotation keeps its attributes in the order they were read from the
`.knowtator.xml` file. Two annotators working on the same document rarely produce the
same order, so in adjudication mode the *Attributes* list of the editor panel and the
*Attributes* list of the comparator panel were shuffled against each other:

```
editor panel (annotator A)     comparator panel (annotator B)
 "certainty" = positive         "temporality" = current
 "experiencer" = patient        "certainty" = negated
 "temporality" = current        "experiencer" = patient
```

With more than a couple of attributes, spotting *which* value the annotators disagreed
on meant reading both lists line by line.

---

## ✅ Solution

### Ordering

`resultEditor.annotations.AttributeDisplayUtil` returns the attributes of an annotation
in the order the user asked for:

| Order | Behaviour |
|---|---|
| `SCHEMA` (default) | The order of the schema configuration: inherited public attributes first, then the private attributes of the annotation class in the order they were defined. Attributes that are not in the schema are listed last, alphabetically. |
| `NAME` | Alphabetically by attribute name, ignoring the case. |
| `UNSORTED` | The legacy behaviour: the order the attributes were read from disk. |

The same helper is used by both panels, so the two lists always line up.

### Highlighting

`AttributeDisplayUtil.getDifferingAttributeNames(...)` compares the annotation of the
editor panel with the annotation selected on the comparator panel. An attribute counts
as different when the two values differ after trimming, or when only one of the two
annotations carries it. Null and blank values are treated the same, as "not set".

`userInterface.AttributeListCellRenderer` paints those rows in bold dark red with a
pale yellow background and adds an explanatory tooltip.

Selecting another annotation on the comparator panel refreshes the highlighting on both
sides.

### Configuration

New `eHOST.sys` parameters:

```
[ATTRIBUTE_DISPLAY_ORDER]
SCHEMA

[HIGHLIGHT_ATTRIBUTE_DIFFERENCES]
true
```

Both are exposed on the new **Annotation Display** tab of the *System Configuration*
dialog and are applied to the lists on screen as soon as **Save** is clicked. Missing or
unreadable values fall back to `SCHEMA` and `true`, so older `eHOST.sys` files keep
working.

---

## 🛠️ Changed files

| File | Change |
|---|---|
| `src/main/java/resultEditor/annotations/AttributeDisplayUtil.java` | **New** — ordering and difference detection |
| `src/main/java/userInterface/AttributeListEntry.java` | **New** — one row of an attribute list |
| `src/main/java/userInterface/AttributeListCellRenderer.java` | **New** — paints the differing rows |
| `src/main/java/env/Parameters.java` | New `AttributeDisplay` settings |
| `src/main/java/userInterface/GUI.java` | Editor panel lists attributes in order, highlighted |
| `src/main/java/userInterface/annotationCompare/Comparator.java` | Comparator panel does the same; `getPrimaryAnnotation()` extracted |
| `src/main/java/userInterface/annotationCompare/ExpandButton.java` | Exposes the annotation shown on the comparator panel |
| `src/main/java/resultEditor/annotations/Annotation.java` | Attribute editor rows follow the `NAME` order when asked |
| `src/main/java/config/system/ParameterGather.java` | Reads the two new parameters |
| `src/main/java/config/system/SaveConf.java` | Writes the two new parameters |
| `src/main/java/userInterface/SystemConfigDialog.java` | New *Annotation Display* tab |

---

## 🧪 Testing

`src/test/java/resultEditor/annotations/AttributeDisplayUtilTest.java` covers:

- schema order, including attributes outside the schema
- alphabetical order, ignoring the case
- the legacy unsorted order
- two annotators storing the same attributes differently ending up aligned
- null annotations, missing attribute names
- differing values, attributes present on one side only, blank versus null values
- no counterpart annotation meaning nothing is highlighted

`src/test/java/userInterface/AttributeListEntryTest.java` covers the wording of a row, the red
bold styling of a difference, and the styling being reset between rows.

`src/test/java/config/system/AttributeDisplayConfigTest.java` covers the `eHOST.sys` round trip,
`eHOST.sys` files written by older versions, and unreadable values.

```bash
mvn test -Dtest=AttributeDisplayUtilTest,AttributeListEntryTest,AttributeDisplayConfigTest
```

The full suite (`mvn clean package`) passes. The new *Annotation Display* tab was also opened and
inspected on screen.
