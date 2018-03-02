package converter.fileConverters;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class KnowtatorToPipeline {
	
	private static ArrayList<File> _filesSrc;
	private static ArrayList<File> _filesOut;
	private ArrayList<ConceptAnnotation> conceptAnnotations;
	private String _currentFile;
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		if (args.length < 2)
			System.out.println("Usage: Converter [source_dir] [knowtator_xml_dir] [output_dir]");
		else 
		{	
			KnowtatorToPipeline exp  = new KnowtatorToPipeline();
			exp.transformToI2b2(args[0], args[1], args[2]);
		}
	}
	
	/**
	 * 
	 * @param input
	 * @param output
	 * @throws Exception
	 */
	public void transformToI2b2(String corpusDir, String knowtatorXmlDir, String outputDir) throws Exception
	{	
		System.out.println("========================================================================");
		System.out.println("     KNOWTATOR Exporter Tool (knowtator XML to i2b2 pipeline format)");
		System.out.println("     julien.thibault@utah.edu");
		System.out.println("========================================================================");
		
		loadFileList(corpusDir, knowtatorXmlDir, outputDir);
		
		//for each file in the direcory
		for (int i=0; i < _filesOut.size(); i++)
		{
			File srcFile = _filesSrc.get(i);
			File xmlFile = _filesOut.get(i);
			File outFile = new File(outputDir);
			
			ArrayList<String> errors = convertXMLtoI2B2(srcFile, xmlFile, outFile);
			//if errors occurred
			if (errors.size()>0){
				System.out.println("\nFile '" + xmlFile.getName() + "'...\n");
				System.out.println("================================================");
				for (String error : errors){
					System.out.println(error);
				}
			}
			
		}
		System.out.println("\nProcess complete!\n");
		System.out.println("================================================");
	}
	
	/**
	 * Convert Knowtator XML file to i2b2 files
	 * @param srcFile
	 * @param xmlFile
	 * @param outFile
	 * @return List of errors
	 * @throws Exception
	 */
	public ArrayList<String> convertXMLtoI2B2(File srcFile, File xmlFile, File outDirectory) throws Exception
	{	
		
		String outputDirPath = outDirectory.getAbsolutePath();
		//create subfolder structure
		createSubFolderStructure(outDirectory);
		
		ArrayList<Element> conceptNodes = new ArrayList<Element>();
		ArrayList<String> linesConcepts = new ArrayList<String>();
		ArrayList<String> linesAssertions = new ArrayList<String>();
		ArrayList<String> linesRelations = new ArrayList<String>();
		conceptAnnotations = new ArrayList<ConceptAnnotation>();
		
		_currentFile = xmlFile.getName();
		
		ArrayList<String> errorStack = new ArrayList<String>();
		
		//load knowtator xml file
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(xmlFile);
		Element root = doc.getDocumentElement();
		
		//load src text (patient record)
		StringBuilder contents = new StringBuilder();
		BufferedReader br =  new BufferedReader(new FileReader(srcFile));
	    String line = null;
        while (( line = br.readLine()) != null){
          contents.append(line);
          contents.append(System.getProperty("line.separator"));
        }
        

	    //String srcText = contents.toString();
	    String srcText = contents.toString().replaceAll("\\r", "");
		
	    //tokenize source file
		Token.Tokenize(srcText);
		
		//------------------------------------------------------------------
		//Parse XML and build i2b2 entries
		//------------------------------------------------------------------
		
		NodeList nodesAnnotation = root.getElementsByTagName("annotation");
		ArrayList<Element> elementsAnnotation = new ArrayList<Element>();
		for (int n=0; n<nodesAnnotation.getLength(); n++){
			elementsAnnotation.add((Element)nodesAnnotation.item(n));
		}
		
		NodeList nodesClassMention = root.getElementsByTagName("classMention");
		ArrayList<Element> elementsClassMention = new ArrayList<Element>();
		for (int n=0; n<nodesClassMention.getLength(); n++){
			elementsClassMention.add((Element)nodesClassMention.item(n));
		}
		
		NodeList nodesStringSlotMention = root.getElementsByTagName("stringSlotMention");
		ArrayList<Element> elementsStringSlotMention = new ArrayList<Element>();
		for (int n=0; n<nodesStringSlotMention.getLength(); n++){
			elementsStringSlotMention.add((Element)nodesStringSlotMention.item(n));
		}
		
		NodeList nodesComplexSlotMention = root.getElementsByTagName("complexSlotMention");
		ArrayList<Element> elementsComplexSlotMention = new ArrayList<Element>();
		for (int n=0; n<nodesComplexSlotMention.getLength(); n++){
			elementsComplexSlotMention.add((Element)nodesComplexSlotMention.item(n));
		}
		
		
		for (int n=0; n<elementsClassMention.size(); n++)
		{
			Element nodeClass = elementsClassMention.get(n);
			Element classDef = (Element)nodeClass.getElementsByTagName("mentionClass").item(0);
			String conceptType = classDef.getAttribute("id").toLowerCase();
			if ( (conceptType.compareTo("problem")==0) || (conceptType.compareTo("treatment")==0) || (conceptType.compareTo("test")==0)) 
			{
				conceptNodes.add(nodeClass);
				
				ConceptAnnotation conceptAnnotation = getConceptInformation(conceptType, nodeClass, elementsAnnotation, elementsStringSlotMention, elementsComplexSlotMention, srcText, errorStack);
				if (conceptAnnotation != null)
				{
					//check that the concept was not already annotated (duplicate)
					//boolean unique = checkUniqueness(conceptAnnotation, conceptAnnotations, errorStack);
					
					//check that the span of the concept does not overlap with any existing concept
					boolean overlap =checkOverlap(conceptAnnotation, conceptAnnotations, errorStack);

					
					conceptAnnotations.add(conceptAnnotation);
				
					line = getLineFromConceptInformation(conceptAnnotation);
					
					//remove assertion info for concept file
					String lineConcept = line.substring(0, line.lastIndexOf("||")).trim();
					linesConcepts.add(lineConcept);
					
					if (conceptAnnotation.type.compareTo("problem")==0){
						//add complete line to assertion file if there is an assertion
						if (conceptAnnotation.assertion.compareTo("na")!=0){
							linesAssertions.add(line);
						}
						else errorStack.add("ERROR: Problem concept ('"+ conceptAnnotation.span + "' " + conceptAnnotation.offsets +" "+ conceptAnnotation.offsetsChar +") misses an assertion attribute");
					}
				}
			}
		}
		
		for (int n=0; n<conceptAnnotations.size(); n++)
		{
			ConceptAnnotation concept = conceptAnnotations.get(n);
			concept.relatedConceptsIds = new ArrayList<String>();
			concept = setRelationInformation(concept, elementsAnnotation, elementsStringSlotMention, elementsComplexSlotMention, errorStack);
			//for each relationship involving current concept
			ArrayList<String> newLines = getLineFromRelationship(concept,conceptAnnotations, errorStack);
			if (newLines.size()>0)
				linesRelations.addAll(newLines);
		}
		
		//--------------------------------------------------------------------
		//COPY XML FILE TO 'FLAGS/' if there were errors during process
		
		if (errorStack.size() > 0)
		{			
			//read content of original XML
			contents = new StringBuilder();
			br =  new BufferedReader(new FileReader(xmlFile));
		    String xmlline = null;
	        while (( xmlline = br.readLine()) != null){
	          contents.append(xmlline);
	          contents.append(System.getProperty("line.separator"));
	        }
		    String xmlContent = contents.toString();
		    
		    //copy to './flags/ directory'
		    File flagfile = new File(outputDirPath + "/flags/" + xmlFile.getName());
		    BufferedWriter w = new BufferedWriter(new FileWriter(flagfile));
		    w.write(xmlContent);
		    w.close();
		    
			System.out.println("================================================");
		}
		
		if (linesConcepts == null)
			System.out.println("\tErrors were found in this file... proces incomplete.");
		else
		{
			//------------------------------------------------------------------
			//Save i2b2 file CONCEPTS (.con)
			//------------------------------------------------------------------
		    File file = new File(outputDirPath + "/concepts/" + xmlFile.getName().replaceAll("(\\.txt)?\\.knowtator\\.xml", "") + ".con");
		    BufferedWriter w = new BufferedWriter(new FileWriter(file));
		    for (int l=0;l<linesConcepts.size();l++){
		    	w.write(linesConcepts.get(l) + "\n");
		    }
		    w.close();
		    
		   // System.out.println("Concept file created! ("+linesConcepts.size()+" concepts)");
		    
			//------------------------------------------------------------------
			//Save i2b2 file ASSERTION (.ast)
			//------------------------------------------------------------------				    
		    file = new File(outputDirPath + "/assertions/" + xmlFile.getName().replaceAll("(\\.txt)?\\.knowtator\\.xml", "") + ".ast");
		    w = new BufferedWriter(new FileWriter(file));
		    for (int l=0;l<linesAssertions.size();l++){
		    	w.write(linesAssertions.get(l) + "\n");
		    }
		    w.close();
		    
		    //System.out.println("Assertion file created! ("+linesAssertions.size()+" concepts with assertions)");
		}
		if (linesRelations == null)
			System.out.println("\tErrors were found in this file... proces incomplete.");
		else
		{   
		  //------------------------------------------------------------------
			//Save i2b2 file RELATIONSHIPS (.rel)
			//------------------------------------------------------------------
			File file = new File(outputDirPath + "/relations/" + xmlFile.getName().replaceAll("(\\.txt)?\\.knowtator\\.xml", "") + ".rel");
		    BufferedWriter w = new BufferedWriter(new FileWriter(file));
		    for (int l=0;l<linesRelations.size();l++){
		    	w.write(linesRelations.get(l) + "\n");
		    }
		    w.close();
		    
		    //System.out.println("Relationship file created! ("+linesRelations.size()+" relationships)");
		}
		return errorStack; 	
	}
	
	/**
	 * 
	 * @param concept
	 * @param listConcepts
	 * @return
	 */
	private boolean checkOverlap(ConceptAnnotation concept, ArrayList<ConceptAnnotation> listConcepts, ArrayList<String>  errorStack)
	{
		boolean overlap = false;
		int i = 0;
		
		String[] offsets = concept.offsetsChar.split("\\-");
		int charStart = Integer.parseInt(offsets[0].substring(1));
		int charEnd = Integer.parseInt(offsets[1].substring(0,offsets[1].length()-1));
		
		while (i < listConcepts.size() && !overlap)
		{
			String[] offsetsCurrent = listConcepts.get(i).offsetsChar.split("\\-");
			int currCharStart = Integer.parseInt(offsetsCurrent[0].substring(1));
			int currCharEnd = Integer.parseInt(offsetsCurrent[1].substring(0,offsetsCurrent[1].length()-1));
			
			if ( 
					(currCharStart <= charStart && currCharEnd > charStart) ||
					(currCharStart <= charEnd && currCharEnd > charEnd) ||
					(currCharStart >= charStart && currCharEnd <= charEnd) ) 
				{
					overlap = true;
					if (concept.span.toLowerCase().compareTo(listConcepts.get(i).span.toLowerCase())==0)
						errorStack.add("ERROR: same concept was annotated twice ('"+concept.span+"' "+concept.offsets +" "+concept.offsetsChar +").");
					else
						errorStack.add("ERROR: span of concept ('"+concept.span+"' "+concept.offsets +" "+concept.offsetsChar +") overlaps with other concept ('"+listConcepts.get(i).span+"' "+listConcepts.get(i).offsets +" "+listConcepts.get(i).offsetsChar +").");
				}
			i++;
		}
		return overlap;
	}
	
	/**
	 * 
	 * @param conceptType
	 * @param conceptNode
	 * @param nodesAnnot
	 * @param nodesAttributes
	 * @param nodesComplex
	 * @return
	 * @throws Exception
	 */
	private ConceptAnnotation getConceptInformation(
			String conceptType,
			Element conceptNode,
			ArrayList<Element> nodesAnnot,
			ArrayList<Element> nodesAttributes,
			ArrayList<Element> nodesComplex,
			String srcText,
			ArrayList<String>  errorStack) throws Exception
	{
		int n = 0;
		boolean found = false;
		ConceptAnnotation conceptAnnotation = new ConceptAnnotation();
		
		conceptAnnotation.id = conceptNode.getAttribute("id");
		conceptAnnotation.type = conceptType;
		conceptAnnotation.assertion = "na";
		conceptAnnotation.node = conceptNode;
		conceptAnnotation.relationshipDetails = new ArrayList<String>();
		
		//System.out.println("-------------------------------------------------------");
		//System.out.println("Building concept '" + conceptAnnotation.id + "'...");
		
		//find corresponding annotation node
		while (!found && n<nodesAnnot.size())
		{
			Element nodeAnnot = nodesAnnot.get(n);
			Element nodeAnnotMention = (Element)nodeAnnot.getElementsByTagName("mention").item(0);
			if (nodeAnnotMention.getAttribute("id").compareTo(conceptAnnotation.id)==0)
			{
				found = true;
				Element nodeSpan = (Element)nodeAnnot.getElementsByTagName("span").item(0);
				if (nodeSpan == null){
					return null;
				}
				int start = Integer.parseInt(nodeSpan.getAttribute("start"));
				int end = Integer.parseInt(nodeSpan.getAttribute("end"));
				
				Token tokenStart = Token.getTokenFromCharOffset(start,true);
				Token tokenEnd = Token.getTokenFromCharOffset(end,false);
				
				int offsetStartChar = Integer.parseInt(nodeSpan.getAttribute("start"));
				int offsetEndChar = Integer.parseInt(nodeSpan.getAttribute("end"));
				
				conceptAnnotation.offsetsChar = "["+nodeSpan.getAttribute("start") + "-" + nodeSpan.getAttribute("end")+"]";
				
				String span = nodeAnnot.getElementsByTagName("spannedText").item(0).getTextContent();
				conceptAnnotation.span = span.replaceAll("\\n", " ");
				conceptAnnotation.offsets = tokenStart.line + ":" + tokenStart.offsetLine + " " + tokenEnd.line + ":" + tokenEnd.offsetLine;
				
				//check that the span is not null
				if (span.length() > 0){
					//check that the span does not start or end by a white space
					if (span.charAt(0)==' ' || span.charAt(span.length()-1)==' ')
						errorStack.add("ERROR: span of concept '"+ span +"' ("+ conceptAnnotation.offsets +" "+ conceptAnnotation.offsetsChar +") starts or end with a white space. Please adjust span to word boundaries.");
					
					//check that the span given by Knowtator matches the span in the original source file
					String actualSpan = srcText.substring(offsetStartChar, offsetEndChar).replaceAll("\\n", " ").trim();
					if (conceptAnnotation.span.trim().compareTo(actualSpan) != 0){
						errorStack.add("ERROR: span of concept '"+ conceptAnnotation.span.trim() +"' ("+ conceptAnnotation.offsets +" "+ conceptAnnotation.offsetsChar +") in Knowtator does not match actual span in original source file ('"+actualSpan+"').");
					}
				}
				else errorStack.add("ERROR: span of concept '" + conceptAnnotation.offsets +" "+ conceptAnnotation.offsetsChar + ") is empty!");
				/*System.out.println("\tSpan:      " + conceptAnnotation.span);
				System.out.println("\tOffsets:   " + conceptAnnotation.offsets);
				System.out.println("\tType:      " + conceptAnnotation.type);*/
			}
			n++;
		}
		if (!found)
			throw new Exception("Annotation node '" + conceptAnnotation.id + "' not found");
		
		// find ASSERTION attribute if concept is problem
		if (conceptType.compareTo("problem")==0){
			NodeList corefNodes = conceptNode.getElementsByTagName("hasSlotMention");
			int l = 0;
			found = false;
			while(!found && l<corefNodes.getLength())
			{
				String corefId = ((Element)corefNodes.item(l)).getAttribute("id");
				n = 0;
				while (!found && n<nodesAttributes.size())
				{
					Element nodeAssertion = nodesAttributes.get(n);
					//if the corresponding node was found
					if (nodeAssertion.getAttribute("id").compareTo(corefId)==0)
					{
						NodeList mentionSlotNodes = nodeAssertion.getElementsByTagName("mentionSlot");
						//if its actually an Assertion node
						if (mentionSlotNodes != null && ((Element)mentionSlotNodes.item(0)).getAttribute("id").compareTo("Assertion")==0)
						{
							found = true;
							conceptAnnotation.assertion = ((Element)nodeAssertion.getElementsByTagName("stringSlotMentionValue").item(0)).getAttribute("value");
							nodesAttributes.remove(n);
							//System.out.println("\tAssertion: " + conceptAnnotation.assertion);
						}
					}
					n++;
				}
				l++;
			}
		}
		return conceptAnnotation;
	}
	
	
	/**
	 * 
	 * @param conceptType
	 * @param conceptNode
	 * @param nodesAnnot
	 * @param nodesAttributes
	 * @param nodesComplex
	 * @return
	 * @throws Exception
	 */
	private ConceptAnnotation setRelationInformation(
			ConceptAnnotation  concept,
			ArrayList<Element> nodesAnnot,
			ArrayList<Element> nodesAttributes,
			ArrayList<Element> nodesComplex,
			ArrayList<String> errorStack) throws Exception
	{
		//System.out.println("Building potential relationship for concept '" + concept.id + "'...");
		
		NodeList relationNodes = concept.node.getElementsByTagName("hasSlotMention");
		if (relationNodes != null && relationNodes.getLength()>0)
		{	
			for (int i=0; i<relationNodes.getLength(); i++)
			{
				Element relNode = (Element)relationNodes.item(i);
				String relAttrId = relNode.getAttribute("id");
				//System.out.println("\tRelationship '" + relAttrId + "'...");
				
				//get corresponding node
				//System.out.println("\tSearching Complex nodes...");
				Element relNodeChild = findNodeById(nodesComplex, relAttrId);
				if (relNodeChild != null)
				{
					NodeList relConceptsInfo = ((Element)relNodeChild).getElementsByTagName("complexSlotMentionValue");
					for (int c = 0; c<relConceptsInfo.getLength(); c++)
					{
						Element relatedConcept = (Element)relConceptsInfo.item(c);
						String relConceptId = relatedConcept.getAttribute("value");
						//check that the current concept was not already associated to 'relConceptId' (duplicate)
						for (String id : concept.relatedConceptsIds)
						{
							if (id.compareTo(relConceptId)==0){
								ConceptAnnotation relConcept = getConceptByID(relConceptId);
								errorStack.add("ERROR: " + _currentFile + "\nRelation between the concepts '"+ concept.span +"' "+ concept.offsets +" "+ concept.offsetsChar +" and '"+ relConcept.span +"' "+ relConcept.offsets +" "+ relConcept.offsetsChar +" was annotated twice.\n" +
										"Please remove duplicate in Knowtator, then re-run this script.");
							}
						}
						concept.relatedConceptsIds.add(relConceptId);
						//System.out.println("\tRelationship with:    " + relConceptId);
					}
				}
				else
				{
					relNodeChild = findNodeById(nodesAttributes, relAttrId);
					//System.out.println("\tSearching Attributes nodes...");
					if (relNodeChild != null)
					{
						NodeList relTypeInfo = ((Element)relNodeChild).getElementsByTagName("mentionSlot");
						String relationType = ((Element)(relTypeInfo.item(0))).getAttribute("id");
						
						if (relationType.compareTo("uncertainty")!=0)
						{
							concept.relationshipType = relationType.substring(relationType.indexOf('(')+1,relationType.length()-1);
							//System.out.println("\tType of relationship: " + concept.relationshipType);
							
							relTypeInfo = ((Element)relNodeChild).getElementsByTagName("stringSlotMentionValue");
							for (int t=0; t<relTypeInfo.getLength(); t++)
							{
								
								String relationTypeDet = ((Element)(relTypeInfo.item(t))).getAttribute("value");
								concept.relationshipDetails.add(relationTypeDet);
							}
						}
					}
				}
			}
		}
		return concept;
	}
	
	/**
	 * 
	 * @param concept
	 * @return
	 */
	private String getLineFromConceptInformation(ConceptAnnotation concept)
	{
		String line = 
			      "c=\"" + concept.span + "\" " + concept.offsets + 
				"||t=\"" + concept.type + "\"" + 
				"||a=\"" + concept.assertion + "\"";	
		return line.toLowerCase();
	}
	
	/**
	 * 
	 * @param concept
	 * @return
	 * @throws Exception 
	 */
	private ArrayList<String> getLineFromRelationship(ConceptAnnotation concept, ArrayList<ConceptAnnotation> conceptList, ArrayList<String> errorStack) throws Exception
	{
		ArrayList<String> lines = new ArrayList<String>();
		
		int r=0;
		for (String relatedConceptId : concept.relatedConceptsIds)
		{	
			// don't allow relationships involving twice the same concept
			if (concept.id.compareTo(relatedConceptId)==0){
				errorStack.add("ERROR: relationship not authorized involving 2 instances of the same concept ('"+ concept.span + "' " +concept.offsets + " " +concept.offsetsChar + ")");
				return lines;
			}
			
			int i = 0;
			while (i < conceptList.size())
			{
				ConceptAnnotation conceptRel = conceptList.get(i);
				if (conceptRel.id.compareTo(relatedConceptId)==0)
				{
					String type = concept.relationshipType;
					if (type != null && concept.relationshipDetails.size()>0)
					{
						String details = concept.relationshipDetails.get(0).toLowerCase();
						//System.out.println("\t\t" + details);
						
						if (type.compareTo("Tr-P")==0)
						{
							if (details.compareTo("improves")==0)
								type = type.replaceFirst("-", "I");
							else if (details.compareTo("worsens")==0)
								type = type.replaceFirst("-", "W");
							else if (details.compareTo("causes")==0)
								type = type.replaceFirst("-", "C");
							else if (details.compareTo("administered_for")==0)
								type = type.replaceFirst("-", "A");
							else if (details.compareTo("not_administered_for")==0)
								type = type.replaceFirst("-", "NA");
							else {
								errorStack.add("WARNING: Tr-P relation unknown: '"+details+"' (choose TrIP, TrWP, TrCP, TrAP or TrNAP)");
								//throw new Exception ("Tr-P relation unknown: '"+details+"' (choose TrIP, TrWP, TrCP, TrAP or TrNAP)");
							}
						}
						else if (type.compareTo("Te-P")==0)
						{
							if (details.compareTo("reveals")==0)
								type = type.replaceFirst("-", "R");
							else if (details.compareTo("conducted_to_investigate")==0)
								type = type.replaceFirst("-", "C");
							else {
								errorStack.add("WARNING: Te-P relation unknown: '"+details+"' (choose TeRP or TeCP)");
								//throw new Exception ("Te-P relation unknown: '"+details+"' (choose TeRP or TeCP)");
							}
						}
						else if (type.compareTo("P-P")==0)
						{
							if (details.compareTo("indicates")==0)
								type = type.replaceFirst("-", "I");
							else {
								errorStack.add("WARNING: P-P relation unknown: '"+details+"' (choose PIP)");
								//throw new Exception ("P-P relation unknown: '"+details+"' (choose PIP)");
							}
						}
					}
					else {
						/*type = "None";
						
						if (concept.type.compareTo("problem")==0 && conceptRel.type.compareTo("problem")==0)
							type = "PP";
						else if ((concept.type.compareTo("test")==0 && conceptRel.type.compareTo("problem")==0)
								|| (conceptRel.type.compareTo("test")==0 && concept.type.compareTo("problem")==0))
							type = "TeP";
						else if ((concept.type.compareTo("treatment")==0 && conceptRel.type.compareTo("problem")==0)
								|| (conceptRel.type.compareTo("treatment")==0 && concept.type.compareTo("problem")==0))
							type = "TrP";
						else
						{*/
						//System.out.println("\n==================================================================");
						errorStack.add("ERROR: unauthorized relationship between '"+concept.span+"' "+ concept.offsets +" ("+concept.type+") and '"+conceptRel.span+"' "+ conceptRel.offsets +" ("+conceptRel.type+"). Please remove it from your knowtator annnotations, or make sure that the type of relationship is specified ('None' is not valid).");
						//throw new Exception();
						
					}
					
					String line = "c=\"" + concept.span.toLowerCase() + "\" " + concept.offsets + 
								"||r=\"" + type + "\"" + 
								"||c=\"" + conceptRel.span.toLowerCase() + "\" " + conceptRel.offsets;	
					lines.add(line);
				}
				i++;
			}
			r++;
		}
		return lines;
	}
	
	/**
	 * Create subfolders to organize output files
	 * @param outFile
	 * @throws Exception
	 */
	private void createSubFolderStructure(File outFile) throws Exception
	{
		String outputDir = outFile.getAbsolutePath();
		
		if (outFile.exists()){
			if (outputDir.charAt(outputDir.length()-1)== '/') 
				outputDir = outputDir.substring(0,outputDir.length()-1);
		}
		else throw new Exception("Output file '"+ outputDir +"' does not exist!");
		
		//create subfolders if necessary
		File subDir = new File(outputDir + "/assertions");
		if (!subDir.exists()){
			if (subDir.mkdir())
				System.out.println("Subfolder '"+subDir+"' created");
		}
		subDir = new File(outputDir + "/concepts");
		if (!subDir.exists()){
			if (subDir.mkdir())
				System.out.println("Subfolder '"+subDir+"' created");
		}
		subDir = new File(outputDir + "/relations");
		if (!subDir.exists()){
			if (subDir.mkdir())
				System.out.println("Subfolder '"+subDir+"' created");
		}
		subDir = new File(outputDir + "/flags");
		if (!subDir.exists()){
			if (subDir.mkdir())
				System.out.println("Subfolder 'flags/' created");
		}
	}
	
	/**
	 * 
	 * @param input
	 * @param output
	 * @throws Exception 
	 */
	private void loadFileList(String input, String knowtatorXml, String output) throws Exception
	{
		ArrayList<File> sourceFiles;
		ArrayList<File> i2b2Files;
		
		//load source files
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
		
		//load i2b2 files
		directory = new File(knowtatorXml.trim());
		if (!directory.exists() || !directory.isDirectory()) {
			throw new Exception("Specified directory does not exist");
		}
		i2b2Files = new ArrayList<File>();
		File[] filesI2b2 = directory.listFiles();
		for (int i = 0; i < filesI2b2.length; i++)
		{
			if (	  !filesI2b2[i].isDirectory() 
					&& filesI2b2[i].getName().charAt(0)!='.' 
					&& filesI2b2[i].getName().endsWith(".knowtator.xml"))
			{
				i2b2Files.add(filesI2b2[i]);
			}
		}
		
		_filesOut = new ArrayList<File>();
		_filesSrc = new ArrayList<File>();
		
		for (File fileI2b2 : i2b2Files)
		{
			String srcname = fileI2b2.getName().substring(0, fileI2b2.getName().indexOf(".knowtator.xml"));
			int i = 0;
			boolean found = false;
			while (!found && i < sourceFiles.size())
			{
				if (sourceFiles.get(i).getName().indexOf(srcname) > -1)
				{
					found = true;
					_filesOut.add(fileI2b2);
					_filesSrc.add(sourceFiles.get(i));
				}
				i++;
			}
		}
	}
	
	/**
	 * 
	 * @param nodes
	 * @param id
	 * @return
	 */
	Element findNodeById(ArrayList<Element> nodes, String id)
	{
		int n = 0;
		//find corresponding annotation node
		while (n<nodes.size())
		{
			Element node = nodes.get(n);
			if (node.getAttribute("id").compareTo(id)==0){
				return node;
			}
			n++;
		}
		return null;
	}
	
	/**
	 * 
	 * @param id
	 * @return
	 */
	ConceptAnnotation getConceptByID(String id)
	{
		int i = 0;
		while (i < conceptAnnotations.size())
		{
			ConceptAnnotation curAnnotation = conceptAnnotations.get(i);
			if (curAnnotation.id.compareTo(id)==0)
				return curAnnotation;
			i++;
		}
		return null;
	}
}
