package converter.fileConverters;

import java.util.ArrayList;

import org.w3c.dom.Element;

public class ConceptAnnotation {
		public String id;
		public String span;
		public String offsets;
		public String offsetsChar;
		public String type;
		public String relationshipType;
		public ArrayList<String> relationshipDetails;
		public ArrayList<ConceptAnnotation> relatedConcepts;
		public ArrayList<String> relatedConceptsIds;
		public String assertion;
		public Element node;
}
