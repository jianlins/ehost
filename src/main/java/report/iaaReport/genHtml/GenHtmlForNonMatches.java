/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package report.iaaReport.genHtml;

import report.iaaReport.IAA;
import report.iaaReport.analysis.detailsNonMatches.*;
import rest.server.PropertiesUtil;
import resultEditor.annotations.Annotation;

import java.io.*;
import java.util.Vector;
import java.util.logging.Level;

/**
 *
 * @author Chris 2011-9-9 21:33
 */
public class GenHtmlForNonMatches
{
    public void genHtml(File reportfolder) throws Exception
    {
        try
        {

            AnalyzedResult analyedResult = new AnalyzedResult();
            Vector<AnalyzedAnnotator> analyzedAnnotators = analyedResult.getAll();

            for(AnalyzedAnnotator analyzedAnnotator: analyzedAnnotators)
            {
                if(analyzedAnnotator==null)
                    continue;

                if(analyzedAnnotator.mainAnnotator==null){
                    throw new Exception("1109020443::");
                }

                String annotatorName = analyzedAnnotator.mainAnnotator.trim();
                annotatorName = annotatorName.trim();
                annotatorName = annotatorName.replaceAll(" ", "_");
                annotatorName = annotatorName.replaceAll(",", "_");
                annotatorName = annotatorName.replaceAll("=", "_");
                annotatorName = annotatorName.replaceAll("&", "_");
                annotatorName = annotatorName.replaceAll("!", "_");
                annotatorName = annotatorName.replaceAll("\\*", "_");
                annotatorName = annotatorName.replaceAll("@", "_");
                annotatorName = annotatorName.replaceAll("\\+", "_");

                String filenameStr = annotatorName;
                // assemble the name of the file
                File file = new File(reportfolder.getAbsolutePath() + File.separatorChar + filenameStr + "-UNMATCHED-SUMMARY"  + ".html" );
                String projectName=reportfolder.getParentFile().getName();
                StringBuilder sb = new StringBuilder();

                // #### assemble html head
                addHtmlhead(sb, analyzedAnnotator.mainAnnotator.trim() );

                SeparatedDetailsByClass.clear();
                SeparatedDetailsByClass.setAnnotatorName(annotatorName);

                // #### html assemble: output each non-matched annotaions of current annotator
                outputNonMatchedDetails( sb, analyzedAnnotator, analyzedAnnotator.mainAnnotator.trim(),
                        analyzedAnnotator.annotators ,projectName);

                // print separated details of matches
                buildSeparatedDetailsByClass( reportfolder, analyzedAnnotator.mainAnnotator.trim() );

                // record classes which has nonmatches so we can remove class without any annotations in the index.html
                SeparatedDetailsByClass.registerUnMatchesClass(analyzedAnnotator.mainAnnotator.trim());

                writeStringBuilder(sb, file);
            }

        }catch(Exception ex){
            log.LoggingToFile.log(Level.SEVERE, "error 1108262132:: fail to the html of unmatched details !!"
                    + ex.toString() );
            throw new Exception("error 1108262133B:: fail to save the html of unmatched details !!\n"
                    + "error 1108190142::" + ex.getMessage() );
        }
    }

    private void writeStringBuilder(StringBuilder sb, File file) {
        try (FileWriter writer = new FileWriter(file);
             BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            bufferedWriter.write(sb.toString());
        } catch (IOException e) {
            // Handle exception appropriately
            e.printStackTrace();
        }
    }


    /**prepare the html head for each html. This only can be called from
     * method "genHtml" in same class.
     */
    private StringBuilder addHtmlhead(StringBuilder p, String annotatorName){
        p.append("<html> ");
        p.append("<head><title>Non-Matching annotations of Annotator ("+annotatorName+") </title></head>");
        p.append("<body style=\"margin: 2 5 5 5; font-family: Candara;\">");
        p.append("<div><a href=\"index.html\"><b>Back to the index.html</b></a><br></div>");
        p.append("<h1>Non-matches for Annotator: ("+annotatorName+") , </h1>");
        p.append("Each annotation that was considered a non-match is " +
                "shown in the text that it was found in.  If user selected, then overlapping " +
                "annotations from the other annotation sets are also shown.");
        p.append("</font><hr>");
        p.append("<div id='statusMessage' style='position: fixed; bottom: 20px; left: 0; right: 0; " +
                "padding: 5px 10px; background: #f5f5f5; border-top: 1px solid #ddd; " +
                "font-size: 12px; display: none; color: #006400;'></div>");


        return p;
    }



    /*print separated details of matches*/
    private void buildSeparatedDetailsByClass(File reportfolder, String annotatorName) {
        if (SeparatedDetailsByClass.separatedDetails == null) {
            log.LoggingToFile.log(Level.WARNING, "1109301329:: empty results of non-matches to a specific class.");
            return;
        }

        try {
            for (ClassedFormat cf : SeparatedDetailsByClass.separatedDetails) {
                File file = new File(reportfolder.getAbsolutePath() + File.separatorChar + cf.filename);
                StringBuilder sb = new StringBuilder();

                sb.append("<html> \n");
                sb.append("<head><title>Non-Matches annotations of Annotator (").append(annotatorName).append(") to certain class </title></head>\n");
                sb.append("<body style=\"margin: 2 5 5 5; font-family: Candara;\">\n");
                sb.append("<div><a href=\"index.html\"><b>Back to the index.html</b></a><br></div>\n");
                sb.append("<h1>Matches and Non-matches for Annotator: (").append(annotatorName).append(")  to class(")
                        .append(cf.classname)
                        .append("), </h1>\n");
                sb.append("Each annotation that was considered as non-matches is " +
                        "shown in the text that it was found in.  If user selected, then overlapping " +
                        "annotations from the other annotation sets are also shown.\n");
                sb.append("</font><hr>\n");

                for (String code : cf.htmlcodes) {
                    sb.append(code).append("\n");
                }

                sb.append("<div><a href=\"index.html\"><b><br> [Back to the index.html]</b></a><br></div>\n");

                // Use the existing writeStringBuilder method to write the content to file
                writeStringBuilder(sb, file);
            }
        } catch (Exception ex) {
            log.LoggingToFile.log(Level.SEVERE, "1109301030::fail to output separated details of matched annotations");
        }
    }



    /**
     * To the given record of non-matched annotations of an annotator, we
     * list them and assemble them in a html report.
     *
     * @param sb           StringBuilder to set the output.
     * @param projectName
     */
    private StringBuilder outputNonMatchedDetails(
            StringBuilder sb,
            AnalyzedAnnotator analyzedAnnotator,
            String annotatorName,
            String[] annotators, String projectName) throws Exception{

        if(sb==null)
            throw new Exception("1108292228:: null instance of Class PrintStream is given.");

        if(   (analyzedAnnotator==null)
                || (analyzedAnnotator.analyzedArticles==null))
        {
            log.LoggingToFile.log(Level.WARNING, "11082922XX");
            return sb;
        }

        try{

            // #### get all diffed articles of current annotator
            Vector<AnalyzedArticle> articles = analyzedAnnotator.analyzedArticles;
            if(articles==null)
            {
                log.LoggingToFile.log(Level.WARNING, "1108292EEE:: no article format for current annotator.");
                return sb;
            }

            for( AnalyzedArticle article : articles)
            {
                if(article==null)
                    continue;

                if(article.rows==null)
                    continue;

                PreLoadDocumentContents.load(article.filename);

                // #### get all diffed annotations of current article of current annotator
                Vector<AnalyzedAnnotation> analyzedAnnotations = article.rows;



                for(AnalyzedAnnotation analyzedAnnotation : analyzedAnnotations )
                {
                    // two interger defined to record the interval which can
                    // be used to cover all spans of these different annotations
                    int differenceStart = 0, differenceEnd = 0;

                    if(analyzedAnnotation==null){
                        log.LoggingToFile.log(Level.WARNING,  "1110041859KE: null value.");
                        continue;
                    }

                    Classes classes = new Classes();

                    int maxsize=0;
                    // #### get max rows of this table

                    maxsize = analyzedAnnotation.mainAnnotations.size();

                    for(OthersAnnotations otherannotations : analyzedAnnotation.othersAnnotations){
                        if(otherannotations==null)
                            continue;

                        Vector<AnalyzedAnnotationDifference> differences = otherannotations.annotationsDiffs;
                        if( maxsize < differences.size() )
                            maxsize = differences.size();

                    }

                    Vector<String> Onerecord = new Vector<String>();
                    // this flag use to indicate whether we found differences
                    // between these annotations
                    // Default value is false.
                    boolean foundDifference = false;

                    String fileStemName=article.filename.substring(0, article.filename.lastIndexOf("."));
                    Onerecord.add(String.format(
                            "<div><a href=\"#\" class=\"load-content\" " +
                                    "data-url=\"http://localhost:%s/ehost/%s/%s\" " +
                                    "onclick=\"return showStatus(this, '%s');\">"+
                                    "File: ",
                            PropertiesUtil.getPort(), projectName, fileStemName, fileStemName)
                            + article.filename + "</a></div>");

                    Annotation mainAnnotation0 = getMainAnnotation(0, analyzedAnnotation);

                    if(mainAnnotation0!=null){
                        differenceStart = mainAnnotation0.spanstart;
                        differenceEnd = mainAnnotation0.spanend;
                        classes.add( mainAnnotation0.annotationclass ); // record we have this class
                    }

                    String textcontent = null;
                    if(mainAnnotation0!=null)
                        textcontent = PreLoadDocumentContents.get(mainAnnotation0);
                    //#### print the rest rows
                    for(int ii=0; ii<maxsize; ii++)
                    {
                        // get annotations that will appeared in 2nd, 3rd, and other column

                        int size_other = analyzedAnnotation.othersAnnotations.length;
                        for(int jj=0; jj<size_other; jj++){
                            Vector<AnalyzedAnnotationDifference> diffs = analyzedAnnotation.othersAnnotations[jj].annotationsDiffs;
                            AnalyzedAnnotationDifference diff = getOtherAnnotation(ii, diffs);

                            if((textcontent==null)&&(diff!=null)&&(diff.annotation!=null)){
                                textcontent = PreLoadDocumentContents.get(diff.annotation);
                                break;
                            }


                        }
                        if(textcontent!=null)
                            break;
                    }
                    Onerecord.add("<div>"+textcontent+"</div>");
                    Onerecord.add("<table border=\"1\">");

                    //#### print the table head
                    Onerecord.add("<tr>");
                    Onerecord.add("<th></th>");
                    for(String annotator : annotators )
                    {
                        Onerecord.add("<th> Annotator:[ "+ annotator +" ]</th>");
                    }
                    Onerecord.add("</tr>");


                    //#### print the rest rows
                    for(int i=0; i<maxsize; i++)
                    {

                        Annotation mainAnnotation = getMainAnnotation(i, analyzedAnnotation);
                        Annotation mainAnnotation_first = getMainAnnotation(0, analyzedAnnotation);  // main Annotation in first column, first row

                        if (mainAnnotation != null) {

                            differenceStart = mainAnnotation.spanstart;
                            differenceEnd = mainAnnotation.spanend;
                            classes.add(mainAnnotation.annotationclass); // record we have this class

                        }

                        if (mainAnnotation_first != null) {
                            if (mainAnnotation_first.spanstart < differenceStart) {
                                differenceStart = mainAnnotation_first.spanstart;
                            }
                            if (mainAnnotation_first.spanend > differenceEnd) {
                                differenceEnd = mainAnnotation_first.spanend;
                            }
                        }

                        //#### get annotation that will be listed in the first column
                        Onerecord.add("<tr>");
                        Onerecord.add("<td>Annotation Text</td>");
                        if( mainAnnotation != null ){
                            Onerecord.add("<td>"+mainAnnotation.annotationText+"</td>");
                        }else{
                            Onerecord.add("<td BGCOLOR=\"#E0E0E0\"></td>");
                        }


                        boolean foundNull = false;
                        // get annotations that will appeared in 2nd, 3rd, and other column
                        int size_other = analyzedAnnotation.othersAnnotations.length;
                        for(int j=0; j<size_other; j++)
                        {
                            Vector<AnalyzedAnnotationDifference> diffs = analyzedAnnotation.othersAnnotations[j].annotationsDiffs;
                            AnalyzedAnnotationDifference diff = getOtherAnnotation(i, diffs);

                            if (diff != null) {

                                if (diff.annotation.spanstart < differenceStart) {
                                    differenceStart = diff.annotation.spanstart;
                                }
                                if (diff.annotation.spanend > differenceEnd) {
                                    differenceEnd = diff.annotation.spanend;
                                }
                            }


                            if((diff!=null)&&(diff.annotation!=null)&&(diff.annotation.annotationclass!=null))
                                classes.add( diff.annotation.annotationclass );



                            if ((diff != null) && (diff.annotation.annotationText != null)) {
                                // if main annotation is null, and the diff annotation isn't null
                                // they are different
                                if (mainAnnotation_first == null) {
                                    Onerecord.add("<td BGCOLOR=\"#FFD0D0\">" + diff.annotation.annotationText + "</td>");
                                    foundDifference = true;
                                } else {



                                    // if main and diff has same span
                                    if (IAA.CHECK_OVERLAPPED_SPANS) {
                                        if ( (Comparator.equalSpans(diff.annotation, mainAnnotation_first))
                                                ||(Comparator.isSpanOverLap(diff.annotation, mainAnnotation_first)))
                                        {
                                            Onerecord.add("<td>" + diff.annotation.annotationText + "</td>");
                                        } else {
                                            // if main annotation and diff annotation has different span
                                            Onerecord.add("<td BGCOLOR=\"#FFD0D0\">" + diff.annotation.annotationText + "</td>");
                                            foundDifference = true;
                                        }

                                    } else {
                                        if (Comparator.equalSpans(diff.annotation, mainAnnotation_first)) {
                                            Onerecord.add("<td>" + diff.annotation.annotationText + "</td>");
                                        } else {
                                            // if main annotation and diff annotation has different span
                                            Onerecord.add("<td BGCOLOR=\"#FFD0D0\">" + diff.annotation.annotationText + "</td>");
                                            foundDifference = true;
                                        }
                                    }
                                }

                            }
                            // if the diff annotation is null
                            else
                            {
                                foundNull = true;
                                Onerecord.add("<td BGCOLOR=\"#E0E0E0\"></td>");
                            }
                        }
                        Onerecord.add("</tr>");

                        // if main annotaiton != null and null
                        if((foundNull)&&(mainAnnotation_first!=null))
                            foundDifference = true;


                        //#### get annotation span
                        Onerecord.add("<tr>");
                        Onerecord.add("<td>Span</td>");
                        if(mainAnnotation!=null){
                            Onerecord.add("<td>");
                            Onerecord.add( mainAnnotation.getSpansInText() );
                            Onerecord.add( "</td>");

                        }else
                            Onerecord.add("<td BGCOLOR=\"#E0E0E0\"></td>");


                        for(int j=0; j<size_other; j++)
                        {
                            Vector<AnalyzedAnnotationDifference> diffs = analyzedAnnotation.othersAnnotations[j].annotationsDiffs;
                            AnalyzedAnnotationDifference diff = getOtherAnnotation(i, diffs);
                            if(diff!=null){
                                // if main annotation is null, and the diff annotation isn't null
                                // they are different
                                if(mainAnnotation_first==null)
                                {
                                    Onerecord.add("<td BGCOLOR=\"#FFD0D0\">");
                                    Onerecord.add( diff.annotation.getSpansInText() );
                                    Onerecord.add( "</td>");
                                    foundDifference = true;
                                }
                                else
                                {
                                    if (IAA.CHECK_OVERLAPPED_SPANS) {
                                        if ((Comparator.equalSpans(diff.annotation, mainAnnotation_first))
                                                || (Comparator.isSpanOverLap(diff.annotation, mainAnnotation_first))) {
                                            Onerecord.add("<td >");
                                            Onerecord.add( diff.annotation.getSpansInText() ) ;
                                            Onerecord.add( "</td>");
                                        } else {
                                            // if main and diff has same span
                                            Onerecord.add("<td BGCOLOR=\"#FFD0D0\">");
                                            Onerecord.add( diff.annotation.getSpansInText() ) ;
                                            Onerecord.add( "</td>");

                                            foundDifference = true;
                                        }
                                    } else {
                                        // if main and diff has same span
                                        if ((diff.annotation.spanstart == mainAnnotation_first.spanstart) && (diff.annotation.spanend == mainAnnotation_first.spanend))
                                        {
                                            Onerecord.add("<td >");
                                            Onerecord.add( diff.annotation.getSpansInText() ) ;
                                            Onerecord.add( "</td>");

                                        } else {
                                            // if main and diff has same span
                                            Onerecord.add("<td BGCOLOR=\"#FFD0D0\">");
                                            Onerecord.add( diff.annotation.getSpansInText() ) ;
                                            Onerecord.add( "</td>");

                                            foundDifference = true;
                                        }
                                    }
                                }

                            }else
                                Onerecord.add("<td BGCOLOR=\"#E0E0E0\"></td>");
                        }
                        Onerecord.add("</tr>");


                        //#### get annotation class
                        Onerecord.add("<tr>");
                        Onerecord.add("<td>Class</td>");
                        if(mainAnnotation!=null)
                            Onerecord.add("<td>"+mainAnnotation.annotationclass + "</td>");
                        else
                            Onerecord.add("<td BGCOLOR=\"#E0E0E0\"></td>");


                        for(int j=0; j<size_other; j++)
                        {
                            Vector<AnalyzedAnnotationDifference> diffs = analyzedAnnotation.othersAnnotations[j].annotationsDiffs;
                            AnalyzedAnnotationDifference diff = getOtherAnnotation(i, diffs);
                            if(diff!=null)
                            {
                                if(diff.diffInClass){
                                    Onerecord.add("<td BGCOLOR=\"#FFD0D0\"> "+diff.annotation.annotationclass + "</td>");
                                    foundDifference = true;
                                }else
                                    Onerecord.add("<td> "+diff.annotation.annotationclass + "</td>");
                            }
                            else
                                Onerecord.add("<td BGCOLOR=\"#E0E0E0\"></td>");
                        }
                        Onerecord.add("</tr>");

                        if(IAA.CHECK_RELATIONSHIP)
                        {
                            //#### get annotation complex relationship
                            Onerecord.add("<tr>");
                            Onerecord.add("<td>Relationship</td>");

                            if(mainAnnotation!=null){
                                String complexstr = mainAnnotation.getComplexRelationshipString();
                                if(complexstr!=null)
                                    Onerecord.add("<td>"+ complexstr + "</td>");
                                else
                                    Onerecord.add("<td BGCOLOR=\"#E0E0E0\"></td>");

                            }else{
                                Onerecord.add("<td BGCOLOR=\"#E0E0E0\"></td>");
                            }


                            for(int j=0; j<size_other; j++)
                            {
                                Vector<AnalyzedAnnotationDifference> diffs = analyzedAnnotation.othersAnnotations[j].annotationsDiffs;
                                AnalyzedAnnotationDifference diff = getOtherAnnotation(i, diffs);
                                if(diff!=null)
                                {
                                    String complexstr = diff.annotation.getComplexRelationshipString();
                                    if(complexstr!=null)
                                        if(diff.diffInRelationship){
                                            Onerecord.add("<td BGCOLOR=\"#FFD0D0\">"+ complexstr + "</td>");
                                            foundDifference = true;
                                        }else
                                            Onerecord.add("<td>"+ complexstr + "</td>");
                                    else{
                                        if(diff.diffInRelationship)
                                        {
                                            Onerecord.add("<td BGCOLOR=\"#FFD0D0\"></td>");
                                            foundDifference = true;
                                        }else
                                            Onerecord.add("<td BGCOLOR=\"#E0E0E0\"></td>");
                                    }
                                }
                                else
                                {
                                    Onerecord.add("<td BGCOLOR=\"#E0E0E0\"></td>");
                                }
                            }
                        }


                        //#### get annotation attributes
                        if(IAA.CHECK_ATTRIBUTES)
                        {
                            Onerecord.add("<tr>");
                            Onerecord.add("<td>Attributes</td>");

                            if(mainAnnotation!=null){
                                String attributeStr = mainAnnotation.getAttributeString();
                                if(attributeStr!=null)
                                    Onerecord.add("<td>"+ attributeStr + "</td>");
                                else
                                    Onerecord.add("<td BGCOLOR=\"#E0E0E0\"></td>");

                            }else{
                                Onerecord.add("<td BGCOLOR=\"#E0E0E0\"></td>");
                            }


                            for(int j=0; j<size_other; j++)
                            {
                                Vector<AnalyzedAnnotationDifference> diffs = analyzedAnnotation.othersAnnotations[j].annotationsDiffs;
                                AnalyzedAnnotationDifference diff = getOtherAnnotation(i, diffs);
                                if(diff!=null)
                                {
                                    String attributeStr = diff.annotation.getAttributeString();
                                    if(attributeStr!=null)
                                        if(diff.diffInAttribute){
                                            Onerecord.add("<td BGCOLOR=\"#FFD0D0\">"+ attributeStr + "</td>");
                                            foundDifference = true;
                                        }else
                                            Onerecord.add("<td>"+ attributeStr + "</td>");
                                    else{
                                        if(diff.diffInAttribute){
                                            Onerecord.add("<td BGCOLOR=\"#FFD0D0\"></td>");
                                            foundDifference = true;
                                        }else
                                            Onerecord.add("<td BGCOLOR=\"#E0E0E0\"></td>");
                                    }
                                }
                                else
                                {
                                    Onerecord.add("<td BGCOLOR=\"#E0E0E0\"></td>");
                                }
                            }
                        }

                        //#### get annotation attributes
                        if(IAA.CHECK_COMMENT)
                        {
                            Onerecord.add("<tr>");
                            Onerecord.add("<td>Comments</td>");

                            if(mainAnnotation!=null){
                                String commentStr = mainAnnotation.comments;
                                if(commentStr!=null)
                                    Onerecord.add("<td>"+ commentStr + "</td>");
                                else
                                    Onerecord.add("<td BGCOLOR=\"#E0E0E0\"></td>");

                            }else{
                                Onerecord.add("<td BGCOLOR=\"#E0E0E0\"></td>");
                            }


                            for(int j=0; j<size_other; j++)
                            {
                                Vector<AnalyzedAnnotationDifference> diffs = analyzedAnnotation.othersAnnotations[j].annotationsDiffs;
                                AnalyzedAnnotationDifference diff = getOtherAnnotation(i, diffs);
                                if(diff!=null)
                                {
                                    String commentStr = diff.annotation.comments;
                                    if(commentStr!=null)
                                        if(diff.diffInComment){
                                            Onerecord.add("<td BGCOLOR=\"#FFD0D0\">"+ commentStr + "</td>");
                                            foundDifference = true;
                                        }else
                                            Onerecord.add("<td>"+ commentStr + "</td>");
                                    else{
                                        if(diff.diffInComment){
                                            Onerecord.add("<td BGCOLOR=\"#FFD0D0\"></td>");
                                            foundDifference = true;
                                        }else
                                            Onerecord.add("<td BGCOLOR=\"#E0E0E0\"></td>");
                                    }
                                }
                                else
                                {
                                    Onerecord.add("<td BGCOLOR=\"#E0E0E0\"></td>");
                                }
                            }
                        }

                        Onerecord.add("</tr>");


                    }

                    Onerecord.add("</table>");
                    Onerecord.add("<br>");

                    if( foundDifference )
                    {
                        // build html for overall report of unmatches
                        for(String str : Onerecord){
                            sb.append(str);
                        }

                        // build html reports for non-matches and split them by classes
                        for(String cla : classes.allclasses)
                        {
                            SeparatedDetailsByClass.addHtmlLine(cla, Onerecord, false);
                        }


                    }


                }
            }
            sb.append("<script>");
            sb.append("function showStatus(element, filename) {");
            sb.append("    var statusDiv = document.getElementById('statusMessage');");
            sb.append("    statusDiv.textContent = 'try to load ' + filename;");
            sb.append("    statusDiv.style.display = 'block';");
            sb.append("    ");
            sb.append("    var url = element.getAttribute('data-url');");
            sb.append("    fetch(url)");
            sb.append("        .then(response => response.text())");
            sb.append("        .then(data => {");
            sb.append("            statusDiv.textContent =  data;");
            sb.append("        })");
            sb.append("        .catch(error => {");
            sb.append("            statusDiv.textContent = 'Error loading content: ' + error;");
            sb.append("            console.error('Error:', error);");
            sb.append("        });");
            sb.append("    return false;"); // Prevent default link behavior
            sb.append("}");
            sb.append("</script>");

//
//            p.println("<div><a href=\"index.html\"><b><br> [Back to the index.html]</b></a><br></div><div id=\"content-container\"></div>" +
//                    "<script>\n" +
//                    "  const contentLinks = document.querySelectorAll(\".load-content\");\n" +
//                    "\n" +
//                    "  contentLinks.forEach(link => {\n" +
//                    "    link.addEventListener(\"click\", (event) => {\n" +
//                    "      event.preventDefault(); // Prevent default navigation\n" +
//                    "\n" +
//                    "      const url = event.target.dataset.url;\n" +
//                    "\n" +
//                    "      fetch(url)\n" +
//                    "        .then(response => response.text()) // Parse the response as text (assuming the server sends plain text)\n" +
//                    "        .then(data => {\n" +
//                    "          const contentContainer = document.getElementById(\"content-container\");\n" +
//                    "          contentContainer.innerHTML = data; // Update content container with fetched data\n" +
//                    "        })\n" +
//                    "        .catch(error => {\n" +
//                    "          console.error(\"Error fetching content:\", error);\n" +
//                    "          // Handle errors by displaying a message or alternative content\n" +
//                    "          const contentContainer = document.getElementById(\"content-container\");\n" +
//                    "          contentContainer.innerHTML = \"<p>Error: Could not load content.</p>\";\n" +
//                    "        });\n" +
//                    "    });\n" +
//                    "  });\n" +
//                    "  </script>");


        }catch(Exception ex){
            throw new Exception( "1101WOC" + ex.getMessage() );
        }


        return sb;
    }

    private Annotation getMainAnnotation(int index, AnalyzedAnnotation analyzedAnnotation){
        try{
            int size = analyzedAnnotation.mainAnnotations.size();
            if(index>=size)
                return null;

            return analyzedAnnotation.mainAnnotations.get(index);

        }catch(Exception ex){
            log.LoggingToFile.log(Level.SEVERE, "1109020549::fail to get main annotation to build detail matched/nonmatched annotations.");
            return null;
        }
    }

    private AnalyzedAnnotationDifference getOtherAnnotation(int i, Vector<AnalyzedAnnotationDifference> diffs) {
        try{
            int size = diffs.size();
            if(i>=size)
                return null;

            return diffs.get(i);

        }catch(Exception ex){
            log.LoggingToFile.log(Level.SEVERE, "1109020554::fail to get other annotation to build detail matched/nonmatched annotations.");
            return null;
        }
    }


}






