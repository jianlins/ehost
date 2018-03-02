package converter.fileConverters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class PipelineToKnowtator {

	private static Pattern regexStart = Pattern.compile("(\\d{1,3}:\\d{1,3}(?=\\s\\d{1,3}))");
	private static Pattern regexEnd = Pattern.compile("(?<=\\d\\s)\\d{1,3}:\\d{1,3}");
	private static Pattern regexTextSpan = Pattern.compile("((?<=\").*(?=\"))");
	
	private static int _ID;
	
	private static ArrayList<File> _filesSrc;
	private static ArrayList<File> _filesConcept;
	private static ArrayList<File> _filesAssertion;
	private static ArrayList<File> _filesRelation;
	
	private ArrayList<Element> _listNodes;
	private ArrayList<ConceptAnnotation> _concepts;
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		if (args.length < 2)
			System.out.println("Usage: i2b2.challenge.PipelineToKnowtator [source dir] [i2b2 dir] [output_dir]");
		else 
		{	
			PipelineToKnowtator imp  = new PipelineToKnowtator();
			imp.conceptsToKnowtator(args[0], args[1], args[2]);
		}
	}
	
	/**
	 * 
	 * @param input
	 * @param output
	 * @throws Exception
	 */
	public void conceptsToKnowtator(String srcDir, String i2b2FileDir, String outputDirPath) throws Exception
	{
		loadFileList(srcDir, i2b2FileDir);
		File outputDir = new File(outputDirPath);
		
		System.out.println("========================================================================");
		System.out.println("     KNOWTATOR Importer Tool (i2b2 pipeline format to knowtator XML)");
		System.out.println("     julien.thibault@utah.edu");
		System.out.println("========================================================================");
		System.out.println("Number of files to convert: " + _filesConcept.size());
		
		//for each file in the direcory
		for (int i=0; i < _filesConcept.size(); i++)
		{
			File srcFile = _filesSrc.get(i);
			File conceptFile = _filesConcept.get(i);
			File assertionFile = _filesAssertion.get(i);
			File relationFile = _filesRelation.get(i);
			
			// Create XML
			Document xmlDoc = convertI2B2toXML(srcFile, conceptFile, relationFile, assertionFile);
			
			//Save XML file
			FileOutputStream fos = new FileOutputStream(outputDir.getPath() + "/" + srcFile.getName() + ".txt.knowtator.xml");
			OutputFormat of = new OutputFormat("XML","ISO-8859-1",true);
			of.setIndenting(true);
			XMLSerializer ser = new XMLSerializer(fos, of);
			ser.serialize(xmlDoc.getDocumentElement());
			fos.close();
			
		}//end for loop
		System.out.println("Files successfully converted!");
	}
	
	/**
	 * Convert I2b2 file to XML file
	 * @param srcFile
	 * @param conceptFile
	 * @param relationFile
	 * @param assertionFile
	 * @return XML document
	 * @throws Exception
	 */
	public Document convertI2B2toXML(File srcFile, File conceptFile, File relationFile, File assertionFile) throws Exception
	{
		/*System.out.println("========================================================================");
		System.out.println("Converting file '" + conceptFile.getName() + "' to Knowtator XML file...");
		System.out.println("========================================================================");*/
		try{
			//load CONCEPT file
			BufferedReader br =  new BufferedReader(new FileReader(conceptFile));
			
		    String line = null;
		    ArrayList<String> linesConcepts = new ArrayList<String>();
	        while (( line = br.readLine()) != null){
	        	linesConcepts.add(line);
	        }
			//load ASSERTION file
	        br =  new BufferedReader(new FileReader(assertionFile));
	        line = null;
		    ArrayList<String> linesAssertion = new ArrayList<String>();
	        while (( line = br.readLine()) != null){
	        	linesAssertion.add(line);
	        }
	        
	        //load RELATIONSHIPS file
	        ArrayList<String> linesRelations = null;
	        ArrayList<ArrayList<String>> linesRelationGrouped = null;
	        
			if (relationFile != null)
			{
				br =  new BufferedReader(new FileReader(relationFile));
			    line = null;
			    linesRelations = new ArrayList<String>();
		        while (( line = br.readLine()) != null){
		        	linesRelations.add(line);
		        }
		        
		        linesRelationGrouped = new ArrayList<ArrayList<String>>();
				for (int r=0; r < linesRelations.size(); r++)
				{
					String currentLine = linesRelations.get(r);
					ArrayList<String> entry = new ArrayList<String>();
					entry.add(currentLine);
					
					String currentLinePrefix = currentLine.substring(0, currentLine.lastIndexOf("||"));
					
					int d = r+1;
					//look for similar relationships
					while (d < linesRelations.size())
					{
						String currentLinePotential = linesRelations.get(d);
						String currentLinePotentialPrefix = currentLinePotential.substring(0, currentLinePotential.lastIndexOf("||"));
						if (currentLinePotentialPrefix.compareTo(currentLinePrefix)==0)
						{
							entry.add(currentLinePotential);
							linesRelations.remove(d);
						}
						else d++;
					}
					linesRelationGrouped.add(entry);
				}
			}
			else System.out.println("Warning: no relationship files found for '"+ srcFile.getName() +"'");
	        
			//load src text (patient record)
			StringBuilder contents = new StringBuilder();
		    br =  new BufferedReader(new FileReader(srcFile));
		    line = null;
	        while (( line = br.readLine()) != null){
	          contents.append(line);
	          contents.append(System.getProperty("line.separator"));
	        }
		    String srcText = contents.toString();
			
		    //tokenize source file
			Token.Tokenize(srcText);
			
			//------------------------------------------------------------------
			//Build XML
			//------------------------------------------------------------------
			
			//get the factory
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.newDocument();
			
			//add root to XML
			Element root = doc.createElement("annotations");
			root.setAttribute("textSource", srcFile.getName());
			doc.appendChild(root);
			
			_listNodes = new ArrayList<Element>();
			_concepts = new ArrayList<ConceptAnnotation>();
			
			//System.out.println("----------------------------------------------");
			
			//for each CONCEPT entry
			for (int j=0; j < linesConcepts.size(); j++){
				addConceptLineToXml(linesConcepts.get(j), root, doc, srcText, linesAssertion);
			}
			for (int j=0; j < _listNodes.size(); j++){
				root.appendChild(_listNodes.get(j));					
			}
			
			if (linesRelationGrouped != null){
				//for each RELATIONSHIP entry
				for (int j=0; j < linesRelationGrouped.size(); j++){
					addRelationLineToXml(linesRelationGrouped.get(j), root, doc, srcText);
				}
			}
			return doc;
		}
		
		catch(Exception e)
		{
			System.out.println("Error while converting '" + conceptFile.getName() + "':");
			System.out.println(e.getMessage());
			throw e;
		}
	}
	
	/**
	 * Convert line from concept file to XML nodes
	 * @param line
	 * @throws Exception 
	 */
	private void addConceptLineToXml(String line, Node root, Document doc, String text, ArrayList<String> linesAssertion) throws Exception
	{
		int begin = 0;
		int end = 0;
                int lineOffset = 0;
		Element node;
		
		String idConcept = "";
		String conceptType = "";
		String conceptAssertion = "";
		String idAssertion = "";
		
		Element nodeAssertion = null;
		
		//System.out.println("Converting: " + line);
		
		String attributes[] = line.split("\\|\\|");
		if (attributes.length < 2)
			throw new Exception("Wrong format in entry '"+ line +"' (expecting 'c=...||t=...||a=...')");
			
		String attribute;
		
		//--------------------------------------------------------------
		//get CONCEPT attribute (offset and span)
		String offsets = "";
		attribute = attributes[0].trim();
		if (attribute.indexOf("c=\"")>-1)
		{
			begin = getBegin(attribute);
			end = getEnd(attribute);
                        lineOffset = getLineOffset(attribute);
			//create 'Annotation' xml node
			idConcept = addNewInstance(root, doc, begin, end,lineOffset, text, attribute);
			offsets = attribute.substring(attribute.lastIndexOf('"')+2).trim();
		}
		else throw new Exception("CONCEPT not found for entry '"+ line +"'");
			
		//--------------------------------------------------------------
		//get TYPE attribute
		boolean hasConceptAssertion = false;
		
		attribute = attributes[1].trim();
		if (attribute.indexOf("t=")==0)
		{
			if (attribute.indexOf("problem")>-1){
				conceptType = "Problem";
				hasConceptAssertion = true;
			}
			else if (attribute.indexOf("test")>-1)
				conceptType = "Test";
			else if (attribute.indexOf("treatment")>-1)
				conceptType = "Treatment";
			else throw new Exception("Unknown concept type: '"+ attribute +"'");
		}
		else throw new Exception("Concept TYPE not found for entry '"+ line +"'");
		
		//--------------------------------------------------------------
		//get ASSERTION attribute
		if (hasConceptAssertion)
		{	
			//find concept in assertion file (.ast) to retrieve assertion attribute
			boolean assertionFound = false;
			int a = 0;
			while (!assertionFound && a <linesAssertion.size())
			{
				String lineAssertion = linesAssertion.get(a);
				String attrAssertion[] = lineAssertion.split("\\|\\|");
				
				if (attrAssertion[0].indexOf(offsets)>0)
				{
					assertionFound=true;
					if (attrAssertion.length>2)
					{
						attribute = attrAssertion[2].trim();
						if (attribute.indexOf("a=")==0)
						{
							if (attribute.indexOf("present")>-1)
								conceptAssertion = "Present";
							else if (attribute.indexOf("absent")>-1)
								conceptAssertion = "Absent";
							else if (attribute.indexOf("conditional")>-1)
								conceptAssertion = "Conditional";
							else if (attribute.indexOf("hypothetical")>-1)
								conceptAssertion = "Hypothetical";
							else if (attribute.indexOf("possible")>-1)
								conceptAssertion = "Possible";
							else if (attribute.indexOf("associated_with_someone_else")>-1)
								conceptAssertion = "Associated_with_someone_else";
							else throw new Exception("Unknown assertion type: "+ attribute +" (" + line + ")");
						}
						else throw new Exception("concept ASSERTION not found for entry '"+ line +"'");
					} 
					else throw new Exception("concept ASSERTION not found for entry '"+ line +"'");
				
					//create XML node concept assertion
					idAssertion = getNewInstanceId();
						
					nodeAssertion = doc.createElement("stringSlotMention");
					nodeAssertion.setAttribute("id", idAssertion);
					
					node = doc.createElement("mentionSlot");
					node.setAttribute("id", "Assertion");
					nodeAssertion.appendChild(node);
					
					node = doc.createElement("stringSlotMentionValue");
					node.setAttribute("value", conceptAssertion);
					nodeAssertion.appendChild(node);
				}
				a++;
			}
			if (nodeAssertion == null)
                        {
                            hasConceptAssertion = false;
                        }
				//throw new Exception("concept ASSERTION not found for entry '"+ line +"'");
		}
		
		//create XML node for concept
		Element nodeConceptClass = doc.createElement("classMention");
		nodeConceptClass.setAttribute("id", idConcept);
		
		node = doc.createElement("mentionClass");
		node.setAttribute("id", conceptType);
		node.setTextContent(conceptType);
		nodeConceptClass.appendChild(node);
		
		_listNodes.add(nodeConceptClass);
		
		ConceptAnnotation conceptAnnotation = new ConceptAnnotation();
		conceptAnnotation.id = idConcept;
		conceptAnnotation.offsets = offsets;
		conceptAnnotation.type = conceptType;
		conceptAnnotation.assertion = "na";
		
		if (hasConceptAssertion){
			node = doc.createElement("hasSlotMention");
			node.setAttribute("id", idAssertion);
			nodeConceptClass.appendChild(node);
			conceptAnnotation.assertion = conceptAssertion;
			
			_listNodes.add(nodeAssertion);
		}
		conceptAnnotation.node = nodeConceptClass;
		_concepts.add(conceptAnnotation);
	}
	
	/**
	 * Convert line from relationships file to XML nodes
	 * @param line
	 * @throws Exception 
	 */
	private void addRelationLineToXml(ArrayList<String> lines, Node root, Document doc, String text) throws Exception
	{
		//System.out.println("Converting: " + line);
		
		String line = lines.get(0);
		String attributes[] = line.split("\\|\\|");
		if (attributes.length < 3)
			throw new Exception("Wrong format in entry '"+ line +"' (expecting 'c=...||r=...||c=...')");
			
		//look for concept object
		String conceptOffset = attributes[0].substring(attributes[0].lastIndexOf('"')+1).trim();
		boolean found = false;
		int i = 0;
		ConceptAnnotation concept = null;
		
		while (!found && i < _concepts.size())
		{
			if (_concepts.get(i).offsets.compareTo(conceptOffset)==0){
				found = true;
				concept = _concepts.get(i);
			}
			i++;
		}
		if (!found) return;//throw new Exception("Concept '" + attributes[0] + "'  found in relationship file (.rel) but not defined in concept file (.con). Check that spans are matching!");
		
		Element nodeClass = concept.node;
		
		String type = attributes[1];
		type = type.substring(type.indexOf('"')+1, type.lastIndexOf('"'));
		String typeGeneric;
		String typeSpecific;
		
		// TREATMENT-PROBLEM RELATIONSHIPS
		if (type.compareTo("TrIP")==0)
		{
			typeGeneric = "Type of Relationship (Tr-P)";
			typeSpecific = "Improves";
			type = "Treatment-Problem Relationship";
		}
		else if (type.compareTo("TrWP")==0)
		{
			typeGeneric = "Type of Relationship (Tr-P)";
			typeSpecific = "Worsens";
			type = "Treatment-Problem Relationship";
		}
		else if (type.compareTo("TrCP")==0)
		{
			typeGeneric = "Type of Relationship (Tr-P)";
			typeSpecific = "Causes";
			type = "Treatment-Problem Relationship";
		}
		else if (type.compareTo("TrAP")==0)
		{
			typeGeneric = "Type of Relationship (Tr-P)";
			typeSpecific = "Administered_for";
			type = "Treatment-Problem Relationship";
		}
		else if (type.compareTo("TrNAP")==0)
		{
			typeGeneric = "Type of Relationship (Tr-P)";
			typeSpecific = "Not_administered_for";
			type = "Treatment-Problem Relationship";
		}
		else if (type.compareTo("NoneTrP")==0)
		{
			typeGeneric = "";
			typeSpecific = "";
			type = "Treatment-Problem Relationship";
		}
		// TEST-PROBLEM RELATIONSHIPS
		else if (type.compareTo("TeRP")==0)
		{
			typeGeneric = "Type of Relationship (Te-P)";
			typeSpecific = "Reveals";
			type = "Test-Problem Relationship";
		}
		else if (type.compareTo("TeCP")==0)
		{
			typeGeneric = "Type of Relationship (Te-P)";
			typeSpecific = "Conducted_to_investigate";
			type = "Test-Problem Relationship";
		}
		else if (type.compareTo("NoneTeP")==0)
		{
			typeGeneric = "";
			typeSpecific = "";
			type = "Test-Problem Relationship";
		}
		// PROBLEM-PROBLEM RELATIONSHIPS
		else if (type.compareTo("PIP")==0)
		{
			typeGeneric = "Type of Relationship (P-P)";
			typeSpecific = "Indicates";
			type = "Problem-Problem Relationship";
		}
		else if (type.compareTo("NonePP")==0)
		{
			typeGeneric = "";
			typeSpecific = "";
			type = "Problem-Problem Relationship";
		}
		else throw new Exception("Unknown relationship type: " + type);
		
		//create XML nodes for relationships
		
		//if the relationship type is not "None", created a node to represent the type
		if (typeGeneric != "")
		{
			String typeId1  = getNewInstanceId();
		
			Element nodeType1 = doc.createElement("hasSlotMention");
			nodeType1.setAttribute("id", typeId1);
			nodeClass.appendChild(nodeType1);
			
			nodeType1 = doc.createElement("stringSlotMention");
			nodeType1.setAttribute("id", typeId1);
			
			Element nodeTypeDetails = doc.createElement("mentionSlot");
			nodeTypeDetails.setAttribute("id", typeGeneric);
			nodeType1.appendChild(nodeTypeDetails);
			for (int t=0; t<lines.size(); t++){
				nodeTypeDetails = doc.createElement("stringSlotMentionValue");
				nodeTypeDetails.setAttribute("value", typeSpecific);
				nodeType1.appendChild(nodeTypeDetails);
			}
			root.appendChild(nodeType1);
		}
		
		String typeId2  = getNewInstanceId();
		Element nodeType2 = doc.createElement("hasSlotMention");
		nodeType2.setAttribute("id", typeId2);
		nodeClass.appendChild(nodeType2);
		
		nodeType2 = doc.createElement("complexSlotMention");
		nodeType2.setAttribute("id", typeId2);
		
		Element nodeTypeDetails = doc.createElement("mentionSlot");
		nodeTypeDetails.setAttribute("id", type);
		nodeType2.appendChild(nodeTypeDetails);
		
		
		//retrieve each concept involved in the relationships
		for (int c=0; c<lines.size(); c++)
		{
			String[] currentAttributes = lines.get(c).split("\\|\\|");
			String conceptRelatedOffset = currentAttributes[2];
			conceptRelatedOffset = conceptRelatedOffset.substring(conceptRelatedOffset.lastIndexOf('"')+2);
			//look for concept ID
			found = false;
			int ic = 0;
			while (!found && ic < _concepts.size())
			{
				//System.out.println("Compare '"+conceptRelatedOffset +"' to '"+ _concepts.get(ic).offsets +"'");
				if (_concepts.get(ic).offsets.compareTo(conceptRelatedOffset)==0){
					found = true;
				}
				else ic++;
			}
			if (!found)
                            return;
				//throw new Exception("Concept '" + currentAttributes[2] + "' found in relationship file (.rel) but not defined in concept file (.con). Check that spans are matching!");
			
			nodeTypeDetails = doc.createElement("complexSlotMentionValue");
			nodeTypeDetails.setAttribute("value", _concepts.get(ic).id);
			nodeType2.appendChild(nodeTypeDetails);
		}
		root.appendChild(nodeType2);
	}
	
	/**
	 * 
	 * @param root
	 * @param doc
	 * @param begin
	 * @param end
	 * @param text
	 * @return
	 */
	private String addNewInstance(Node root, Document doc, int begin, int end, int lineOffset,  String text, String attribute)
	{
		if (begin > -1 && end > begin)
		{
			String instanceId = getNewInstanceId();
			Matcher m = regexTextSpan.matcher(attribute);
			//System.out.println(attribute);
			m.find();
			
			Node mainNode = doc.createElement("annotation");
			root.appendChild(mainNode);
			
			Element node = doc.createElement("mention");
			node.setAttribute("id", instanceId);
			mainNode.appendChild(node);
			
			node = doc.createElement("annotator");
			node.setAttribute("id", "I2B2_2010");
			node.setTextContent("i2b2_auto_generated");
			mainNode.appendChild(node);
			
			node = doc.createElement("span");
			node.setAttribute("start", Integer.toString(begin -(lineOffset -1)));
			node.setAttribute("end", Integer.toString(end - (lineOffset -1)));
			mainNode.appendChild(node);
			
			node = doc.createElement("spannedText");
			node.setTextContent(text.substring(begin, end));
			mainNode.appendChild(node);
			
			node = doc.createElement("creationDate");
			node.setTextContent((new java.util.Date()).toString());
			mainNode.appendChild(node);
			
			return instanceId;
		}	
		else return null;
	}
	private int getLineOffset(String attribute)
        {
            int lineOffset = -1;
		try{
			Matcher matcherS = regexStart.matcher(attribute);

			if (matcherS.find())
			{
				String offsetBegin = attribute.substring(matcherS.start(),matcherS.end());
				int sepIndex = offsetBegin.indexOf(':');
				lineOffset = Integer.parseInt(offsetBegin.substring(0,sepIndex));

			}
		}
		catch (Exception exc)
		{}
		return lineOffset;
        }
	/**
	 * 
	 * @param attribute
	 * @return
	 */
	private int getBegin(String attribute)
	{
		int start = -1;
		try{
			Matcher matcherS = regexStart.matcher(attribute);
			
			if (matcherS.find())
			{
				String offsetBegin = attribute.substring(matcherS.start(),matcherS.end());
				int sepIndex = offsetBegin.indexOf(':');
				int lineOffset = Integer.parseInt(offsetBegin.substring(0,sepIndex));
				int tokenOffset = Integer.parseInt(offsetBegin.substring(sepIndex + 1));
				
				start = Token.lines.get(lineOffset-1).get(tokenOffset).begin;// - (lineOffset-1);
			}
		}
		catch (Exception exc)
		{}		
		return start;
	}
	
	/**
	 * 
	 * @param attribute
	 * @return
	 */
	private int getEnd(String attribute)
	{
		int end = -1;
		try{
			Matcher matcherE = regexEnd.matcher(attribute);
			
			if (matcherE.find())
			{
				String offsetEnd = attribute.substring(matcherE.start(),matcherE.end());
				int sepIndex = offsetEnd.indexOf(':');
				int lineOffset = Integer.parseInt(offsetEnd.substring(0,sepIndex));
				int tokenOffset = Integer.parseInt(offsetEnd.substring(sepIndex + 1));
				
				end = Token.lines.get(lineOffset-1).get(tokenOffset).end;// - (lineOffset-1);
	
			}
		}
		catch (Exception exc)
		{}
		return end;
	}
	
	/**
	 * 
	 * @return
	 */
	private String getNewInstanceId()
	{
		_ID++;
		return ("i2b2_instance_" + _ID);
	}
	
	/**
	 * 
	 * @param input
	 * @param output
	 * @throws Exception 
	 */
	private void loadFileList(String input, String output) throws Exception
	{
		ArrayList<File> sourceFiles = new ArrayList<File>();
		ArrayList<File> conceptFiles = new ArrayList<File>();
		ArrayList<File> assertionFiles = new ArrayList<File>();
		ArrayList<File> relationFiles = new ArrayList<File>();
		
		output = output.trim();
		
		//=================================================================
		//load i2b2 files
		//=================================================================
		//check that subdirectory structure exists
		File subDirAssertion = new File(output + "/assertions");
		if (!subDirAssertion.exists()){
			throw new Exception("Subfolder '"+subDirAssertion+"' could not be found");
		}
		File[] filesI2b2 = subDirAssertion.listFiles();
		for (int i = 0; i < filesI2b2.length; i++)
		{
			if (!filesI2b2[i].isDirectory() && filesI2b2[i].getName().endsWith(".ast")){
				assertionFiles.add(filesI2b2[i]);
			}
		}
		
		File subDirConcept = new File(output + "/concepts");
		if (!subDirConcept.exists()){
			throw new Exception("Subfolder '"+subDirConcept+"' could not be found");
		}
		filesI2b2 = subDirConcept.listFiles();
		for (int i = 0; i < filesI2b2.length; i++)
		{
			if (!filesI2b2[i].isDirectory() && filesI2b2[i].getName().endsWith(".con")){
				conceptFiles.add(filesI2b2[i]);
			}
		}
		
		File subDirRelation = new File(output + "/relations");
		if (!subDirRelation.exists()){
			throw new Exception("Subfolder '"+subDirRelation+"' could not be found");
		}
		filesI2b2 = subDirRelation.listFiles();
		for (int i = 0; i < filesI2b2.length; i++)
		{
			if (!filesI2b2[i].isDirectory() && filesI2b2[i].getName().endsWith(".rel")){
				relationFiles.add(filesI2b2[i]);
			}
		}
		
		//=================================================================
		//load source files
		//=================================================================
		File directory = new File(input.trim());
		if (!directory.exists() || !directory.isDirectory()) {
			throw new Exception("Specified directory does not exist");
		}
		sourceFiles = new ArrayList<File>();
		File[] filesSource = directory.listFiles();
		for (int i = 0; i < filesSource.length; i++)
		{
			if (!filesSource[i].isDirectory() && filesSource[i].getName().charAt(0)!='.') {
				sourceFiles.add(filesSource[i]);
			}
		}


		//=================================================================
		//try to map source files and annotation files
		//=================================================================
		_filesConcept = new ArrayList<File>();
		_filesRelation = new ArrayList<File>();
		_filesAssertion = new ArrayList<File>();
		_filesSrc = new ArrayList<File>();
		
		//for each i2b2 CONCEPT file
		for (File conceptFile : conceptFiles)
		{
			String fileNameRoot = conceptFile.getName().substring(0,conceptFile.getName().length()-3);
			//System.out.println("CONCEPT: " + conceptFile.getName());
			//System.out.println("\tROOT: " + fileNameRoot);
			
			int i = 0;
			boolean found = false;
			
			//find related original document
			while (!found && i < sourceFiles.size())
			{
				if (sourceFiles.get(i).getName().compareTo(fileNameRoot + "txt")==0)
				{
					found = true;
					_filesConcept.add(conceptFile);
					
					_filesSrc.add(sourceFiles.get(i));
					//System.out.println("\tORIGINAL: " + sourceFiles.get(i).getName());
				}
				i++;
			}

			if (found){
				i = 0;
				found = false;
				//find related RELATIONSHIP file
				while (!found && i < relationFiles.size())
				{
					File relationFile = relationFiles.get(i);
					if (relationFile.getName().compareTo(fileNameRoot + "rel")==0)
					{
						found = true;
						_filesRelation.add(relationFile);
						//System.out.println("\tRELATION: " + relationFile.getName());
					}
					i++;
				}
			}
			else _filesRelation.add(null);
			
			if (found){
				i = 0;
				found = false;
				//find related ASSERTION file
				while (!found && i < assertionFiles.size())
				{
					File assertionFile = assertionFiles.get(i);
					if (assertionFile.getName().compareTo(fileNameRoot + "ast")==0)
					{
						found = true;
						_filesAssertion.add(assertionFile);
						//System.out.println("\tAssertion: " + assertionFile());
					}
					i++;
				}
			}
			else _filesAssertion.add(null);
		}
	}
}
