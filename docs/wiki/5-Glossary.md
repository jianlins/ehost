# 5. Glossary

Terms used throughout this documentation and in the eHOST interface.

| Term | Meaning |
|---|---|
| **Adjudication** | The process of resolving disagreements between annotators into a single agreed result. In eHOST it is a distinct mode, selected at the bottom right of the main window. See [3.6 Adjudication Mode](3.6-Adjudication-Mode.md). |
| **Adjudication folder** | `<project>/adjudication/` — holds the adjudicated result plus the items still awaiting a decision. Written only when you save while in Adjudication Mode. See [3.6.2](3.6.2-Adjudication-Data-Storage.md). |
| **Annotation** | A span of text in a document that has been marked and assigned a class, optionally with attributes, relationships and a comment. |
| **Annotator** | The person (or process) credited with an annotation. Each annotator's work is stored under their own name so it can be compared. See [2.6 Assign Annotator](2.6-Assign-Annotator.md). |
| **Assertion** | A ConText attribute recording whether a finding is affirmed, possible or negated. See [4.3](4.3-ConTEXT-algorithm.md). |
| **Attribute** | A named property of an annotation, usually with a controlled list of allowed values (for example `Severity` = mild / moderate / severe). See [1.5.2](1.5.2_Define_Attributes.md). |
| **Class** | The category assigned to an annotation, e.g. `Diagnosis` or `Medication`. Classes are defined per project. See [1.5.1](1.5.1_Define_Classes.md). |
| **ConText** | An algorithm that inspects the sentence around a finding to decide experiencer, temporality and assertion. See [4.3](4.3-ConTEXT-algorithm.md). |
| **Corpus** | The set of documents in a project — the `corpus/` folder. |
| **Dictionary** | A two-column text file of terms and their classes, used to pre-annotate documents. See [4.1](4.1-Generate-Pre-annotations-using-Custom-Dictionary.md). |
| **F-measure** | The harmonic mean of precision and recall; the single number usually quoted for inter-annotator agreement in IAA reports. |
| **Gold standard** | The reference annotation set that other work is measured against. In eHOST this is normally the adjudicated result. |
| **IAA** | Inter-Annotator Agreement — how much two or more annotators agree. See [3.5 IAA Reports](3.5-IAA-Report.md). |
| **Knowtator XML** | The `.knowtator.xml` file format eHOST reads and writes, inherited from the Knowtator Protégé plug-in. |
| **Matched / Unmatched** | In an IAA report, annotations that satisfy the configured matching criteria (matched) versus those that do not (unmatched). |
| **NegEx** | The negation-detection algorithm that ConText's assertion component builds on. |
| **Oracle** | eHOST's "annotations like me" feature: it searches the corpus for text similar to something you have already annotated. See [3.4](3.4-Oracle-Mode.md). |
| **Overlap matching** | An IAA option where two annotations count as matching if their spans overlap, rather than being identical. Configured in the IAA Reports dialog. |
| **Position indicator graph** | A report that plots where annotations fall within documents. See [3.2.1](3.2.1-Generating-Graph-Reports-of-Position-Indicated.md). |
| **Pre-annotation** | Annotations created automatically (by dictionary, regular expression or NLP) for a human to review, rather than created from scratch by hand. eHOST writes them under the annotator name `eHOST`. See [4. Beyond Manual Annotation](4.1-Generate-Pre-annotations-using-Custom-Dictionary.md). |
| **Precision** | Of the annotations one annotator made, the fraction that the comparison annotator also made. |
| **Project** | A folder inside the workspace holding one corpus, its schema, annotations, adjudication data and reports. See [1.3.2](1.3.2_Create_Project.md). |
| **Project lock** | The `.lock` file eHOST writes into a project folder so two instances do not edit it at the same time. Refreshed by a heartbeat and treated as stale after a couple of minutes without one. |
| **Recall** | Of the annotations the comparison annotator made, the fraction this annotator also made. |
| **Relationship** | A named link between two annotations, e.g. *Medication* → *treats* → *Diagnosis*. See [1.5.3](1.5.3_Define_Relationships.md). |
| **RESTful server** | eHOST's built-in web server, used for report serving and for driving eHOST from a browser or script. See the [RESTful Server Guide](../RESTful-Server-Guide.md). |
| **Saved folder** | `<project>/saved/` — where each annotator's own annotations are stored. |
| **Schema** | The set of classes, attributes and relationships a project allows. |
| **Span** | The character offsets `(start, end)` of an annotation within its document. |
| **Temporality** | A ConText attribute recording whether a finding is recent, historical or hypothetical. See [4.3](4.3-ConTEXT-algorithm.md). |
| **Workspace** | The folder that contains your eHOST projects. See [1.3 Assign Workspace and Project](1.3_Assign_Workspace_and_Project.md). |
