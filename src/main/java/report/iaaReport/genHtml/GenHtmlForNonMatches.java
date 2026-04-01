/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package report.iaaReport.genHtml;

import report.iaaReport.IAA;
import report.iaaReport.analysis.detailsNonMatches.*;

import resultEditor.annotations.Annotation;
import resultEditor.annotations.AnnotationAttributeDef;

import java.io.*;
import java.util.Vector;
import java.util.logging.Level;

/**
 *
 * @author Chris 2011-9-9 21:33 Jianlins updated with StringBuilder
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
                "font-size: 12px; display: none; color: #006400; text-align: right;'></div>");


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
                                    "data-url=\"/ehost/%s/%s\" " +
                                    "onclick=\"return showStatus(this, '%s');\">"+
                                    "File: ",
                            projectName, fileStemName, fileStemName)
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

                    // Pre-compute paired diffs to ensure correct ordering
                    // (match other annotations to main annotations by class)
                    AnalyzedAnnotationDifference[][] pairedDiffs = buildPairedDiffs(analyzedAnnotation, maxsize);

                    // Track already-emitted main-annotation rows to avoid duplicating
                    // adjudication-only rows when overlaps create multiple alignments
                    java.util.HashSet<String> emittedMainKeys = new java.util.HashSet<String>();

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

                        // Skip duplicate main annotations that were already emitted
                        if (mainAnnotation != null) {
                            String key = mainAnnotation.annotationText + "|" +
                                    mainAnnotation.getSpansInText() + "|" +
                                    String.valueOf(mainAnnotation.annotationclass);
                            if (emittedMainKeys.contains(key)) {
                                // Skip emitting duplicate main annotation block
                                continue;
                            }
                        }

                        // Skip rows where main and all other columns are empty
                        if (mainAnnotation == null) {
                            boolean anyData = false;
                            for (int jc = 0; jc < analyzedAnnotation.othersAnnotations.length; jc++) {
                                if (pairedDiffs[jc][i] != null) { anyData = true; break; }
                            }
                            if (!anyData) continue;
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
                            AnalyzedAnnotationDifference diff = pairedDiffs[j][i];

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
                            AnalyzedAnnotationDifference diff = pairedDiffs[j][i];
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
                            AnalyzedAnnotationDifference diff = pairedDiffs[j][i];
                            if(diff!=null)
                            {
                                // Re-compute class difference against the actual main annotation
                                // at this row index, not the pre-stored diffInClass which was
                                // computed against the row head (index 0).
                                boolean actualClassDiff;
                                if (mainAnnotation != null && mainAnnotation.annotationclass != null
                                        && diff.annotation != null && diff.annotation.annotationclass != null) {
                                    actualClassDiff = !mainAnnotation.annotationclass.trim().equals(
                                            diff.annotation.annotationclass.trim());
                                } else {
                                    actualClassDiff = diff.diffInClass;
                                }
                                if(actualClassDiff){
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
                                AnalyzedAnnotationDifference diff = pairedDiffs[j][i];
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


                        //#### get annotation attributes - one attribute per row
                        if(IAA.CHECK_ATTRIBUTES)
                        {
                            Vector<AnnotationAttributeDef> mainAttrs = mainAnnotation != null ? mainAnnotation.attributes : null;
                            
                            Vector<AnnotationAttributeDef> allUniqueAttrs = new Vector<AnnotationAttributeDef>();
                            if(mainAttrs != null) {
                                for(AnnotationAttributeDef attr : mainAttrs) {
                                    if(attr != null && attr.name != null) {
                                        allUniqueAttrs.add(attr);
                                    }
                                }
                            }
                            for(int j = 0; j < size_other; j++) {
                                AnalyzedAnnotationDifference diff = pairedDiffs[j][i];
                                if(diff != null && diff.annotation != null && diff.annotation.attributes != null) {
                                    for(AnnotationAttributeDef attr : diff.annotation.attributes) {
                                        if(attr != null && attr.name != null) {
                                            boolean found = false;
                                            for(AnnotationAttributeDef existing : allUniqueAttrs) {
                                                if(existing.name.equals(attr.name)) {
                                                    found = true;
                                                    break;
                                                }
                                            }
                                            if(!found) {
                                                allUniqueAttrs.add(attr);
                                            }
                                        }
                                    }
                                }
                            }
                            
                            for(AnnotationAttributeDef uniqueAttr : allUniqueAttrs) {
                                Onerecord.add("<tr>");
                                Onerecord.add("<td>&nbsp;&nbsp;" + uniqueAttr.name + "</td>");
                                
                                for(int colIdx = 0; colIdx <= size_other; colIdx++) {
                                    String cellValue = "";
                                    boolean isDiff = false;
                                    
                                    if(colIdx == 0) {
                                        if(mainAttrs != null) {
                                            for(AnnotationAttributeDef ma : mainAttrs) {
                                                if(ma != null && ma.name != null && ma.name.equals(uniqueAttr.name)) {
                                                    cellValue = ma.value;
                                                    break;
                                                }
                                            }
                                        }
                                    } else {
                                        AnalyzedAnnotationDifference otherDiff = pairedDiffs[colIdx - 1][i];
                                        if(otherDiff != null && otherDiff.annotation != null && otherDiff.annotation.attributes != null) {
                                            for(AnnotationAttributeDef oa : otherDiff.annotation.attributes) {
                                                if(oa != null && oa.name != null && oa.name.equals(uniqueAttr.name)) {
                                                    cellValue = oa.value;
                                                    
                                                    if(mainAttrs != null) {
                                                        for(AnnotationAttributeDef ma : mainAttrs) {
                                                            if(ma != null && ma.name != null && ma.name.equals(uniqueAttr.name)) {
                                                                if(ma.value != null && !ma.value.equals(oa.value)) {
                                                                    isDiff = true;
                                                                }
                                                                break;
                                                            }
                                                        }
                                                        if(mainAttrs.isEmpty() || (mainAttrs.size() == 1 && mainAttrs.get(0).name.equals(uniqueAttr.name) == false)) {
                                                            isDiff = true;
                                                        }
                                                    } else {
                                                        isDiff = true;
                                                    }
                                                    break;
                                                }
                                            }
                                        }
                                        
                                        if(cellValue.isEmpty() && mainAttrs != null && !mainAttrs.isEmpty()) {
                                            for(AnnotationAttributeDef ma : mainAttrs) {
                                                if(ma != null && ma.name != null && ma.name.equals(uniqueAttr.name)) {
                                                    isDiff = true;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    
                                    if(cellValue.isEmpty()) {
                                        cellValue = "";
                                    }
                                    
                                    if(isDiff) {
                                        Onerecord.add("<td BGCOLOR=\"#FFD0D0\">" + cellValue + "</td>");
                                        foundDifference = true;
                                    } else if(cellValue.isEmpty() && colIdx > 0) {
                                        Onerecord.add("<td BGCOLOR=\"#E0E0E0\"></td>");
                                    } else {
                                        Onerecord.add("<td>" + cellValue + "</td>");
                                    }
                                }
                                Onerecord.add("</tr>");
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
                                AnalyzedAnnotationDifference diff = pairedDiffs[j][i];
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

                        // Mark this main row as emitted to avoid duplicates
                        if (mainAnnotation != null) {
                            String key = mainAnnotation.annotationText + "|" +
                                    mainAnnotation.getSpansInText() + "|" +
                                    String.valueOf(mainAnnotation.annotationclass);
                            emittedMainKeys.add(key);
                        }


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

    /**
     * Build a paired mapping of other annotators' annotations to main annotations.
     * 
     * <h3>PROBLEM SOLVED:</h3>
     * When generating HTML reports for non-matching annotations, the diffs from 
     * other annotators were added in article order (the order they appeared in 
     * the document), not in the order of mainAnnotations. This caused misalignment
     * when displaying annotation comparisons in the HTML table. For example:
     * <ul>
     *   <li>If mainAnnotations = [A1(class=X), A2(class=Y)]</li>
     *   <li>And diffs from annotator2 = [D1(class=Y), D2(class=X)] (in document order)</li>
     *   <li>Without pairing, the table would incorrectly compare A1 with D1 and A2 with D2</li>
     * </ul>
     * 
     * <h3>SOLUTION:</h3>
     * This function reorders the diffs to align with mainAnnotations by:
     * <ol>
     *   <li>First matching diffs to mainAnnotations by annotation class (same-class 
     *       annotations should be compared against each other)</li>
     *   <li>Then filling remaining unmatched diffs into available slots</li>
     * </ol>
     * 
     * <h3>ALGORITHM:</h3>
     * Two-pass matching algorithm for each other annotator:
     * <p><b>PASS 1 - Class-based Matching:</b></p>
     * <pre>
     *   For each mainAnnotation at index i:
     *     Search through diffs to find one with matching annotationclass
     *     If found, pair them: paired[j][i] = diff
     *     Mark both the diff and slot as used
     * </pre>
     * <p><b>PASS 2 - Fill Remaining Slots:</b></p>
     * <pre>
     *   For each unused diff:
     *     Find the next available slot in paired[j][]
     *     Place the diff there (for annotations without class matches)
     * </pre>
     * 
     * <h3>EXAMPLE:</h3>
     * <pre>
     * Input:
     *   mainAnnotations = [A1(class="CONCEPT"), A2(class="EVENT")]
     *   diffs from annotator2 = [D1(class="EVENT"), D2(class="CONCEPT"), D3(class="DATE")]
     * 
     * Output (paired[0]):
     *   paired[0][0] = D2 (matched with A1 by class "CONCEPT")
     *   paired[0][1] = D1 (matched with A2 by class "EVENT")
     *   paired[0][2] = D3 (unmatched, placed in next available slot)
     * </pre>
     * 
     * @param analyzedAnnotation The analyzed annotation containing comparison data:
     *        <ul>
     *          <li><b>mainAnnotations</b>: Vector<Annotation> - The main annotator's annotations 
     *              (first annotator in the comparison)</li>
     *          <li><b>othersAnnotations</b>: OthersAnnotations[] - Array where each element represents
     *              another annotator, containing:</li>
     *          <ul>
     *            <li><b>annotator</b>: String - Name of the other annotator</li>
     *            <li><b>annotationsDiffs</b>: Vector<AnalyzedAnnotationDifference> - List of 
     *                differences found when comparing this annotator's annotations</li>
     *          </ul>
     *        </ul>
     * 
     * @param maxsize Maximum number of rows needed in the HTML comparison table.
     *        Calculated as the maximum of:
     *        <ul>
     *          <li>mainAnnotations.size()</li>
     *          <li>annotationsDiffs.size() for each other annotator</li>
     *        </ul>
     * 
     * @return pairedDiffs[otherAnnotatorIndex][rowIndex]
     *         A 2D array where:
     *         <ul>
     *           <li><b>First dimension (numOthers)</b>: Index of other annotator 
     *               (corresponds to othersAnnotations array index)</li>
     *           <li><b>Second dimension (maxsize)</b>: Row index in the HTML table,
     *               corresponding to mainAnnotations[i]</li>
     *           <li><b>paired[j][i]</b>: Contains the AnalyzedAnnotationDifference for 
     *               comparing against mainAnnotations[i]</li>
     *           <li><b>null</b>: If no diff available for that slot</li>
     *         </ul>
     * 
     * @see AnalyzedAnnotation
     * @see AnalyzedAnnotationDifference
     * @see OthersAnnotations
     * @see Annotation
     */
    private AnalyzedAnnotationDifference[][] buildPairedDiffs(AnalyzedAnnotation analyzedAnnotation, int maxsize) {
        // numOthers: Number of other annotators being compared against the main annotator
        // This equals analyzedAnnotation.othersAnnotations.length
        int numOthers = analyzedAnnotation.othersAnnotations.length;
        
        // paired: 2D result array storing the paired mapping
        // Dimensions: [otherAnnotatorIndex][rowIndex]
        // - paired[j][i] will hold the diff to compare against mainAnnotations[i] for annotator j
        AnalyzedAnnotationDifference[][] paired = new AnalyzedAnnotationDifference[numOthers][maxsize];

        // Iterate through each other annotator (j is the annotator index)
        for (int j = 0; j < numOthers; j++) {
            // diffs: Vector of AnalyzedAnnotationDifference for the current other annotator
            // Each diff contains:
            //   - annotation: The actual Annotation from this other annotator
            //   - diffInClass, diffInSpan, diffInAttribute, etc.: Boolean flags indicating
            //     what differs from the main annotation
            Vector<AnalyzedAnnotationDifference> diffs = analyzedAnnotation.othersAnnotations[j].annotationsDiffs;

            // Deduplicate diffs: remove duplicate annotations (same text+span+class)
            {
                Vector<AnalyzedAnnotationDifference> uniqueDiffs = new Vector<>();
                java.util.HashSet<String> seenDiffKeys = new java.util.HashSet<>();
                for (AnalyzedAnnotationDifference dd : diffs) {
                    if (dd == null || dd.annotation == null) { uniqueDiffs.add(dd); continue; }
                    String dk = dd.annotation.annotationText + "|" + dd.annotation.getSpansInText() + "|" + String.valueOf(dd.annotation.annotationclass);
                    if (seenDiffKeys.add(dk)) uniqueDiffs.add(dd);
                }
                diffs = uniqueDiffs;
            }

            // usedDiff: Boolean array tracking which diffs have been assigned to slots
            // usedDiff[d] = true means diffs.get(d) has been placed in the paired array
            boolean[] usedDiff = new boolean[diffs.size()];
            
            // usedSlot: Boolean array tracking which slots in paired[j][] have been filled
            // usedSlot[i] = true means paired[j][i] already has a diff assigned
            boolean[] usedSlot = new boolean[maxsize];

            // ==================== PASS 1: Class-based Matching ====================
            // Match diffs to mainAnnotations by annotation class
            // This ensures annotations of the same class are compared against each other
            
            // mainSize: Number of main annotations to process
            int mainSize = analyzedAnnotation.mainAnnotations.size();
            
            for (int i = 0; i < mainSize && i < maxsize; i++) {
                // mainAnn: Current main annotation at index i
                // We try to find a diff with matching annotationclass
                Annotation mainAnn = analyzedAnnotation.mainAnnotations.get(i);
                if (mainAnn == null || mainAnn.annotationclass == null) continue;

                // Search through all diffs to find one with matching class
                for (int d = 0; d < diffs.size(); d++) {
                    // Skip if this diff has already been assigned
                    if (usedDiff[d]) continue;
                    
                    // diff: Current AnalyzedAnnotationDifference being evaluated
                    AnalyzedAnnotationDifference diff = diffs.get(d);
                    if (diff == null || diff.annotation == null || diff.annotation.annotationclass == null) continue;

                    // Check if the annotation classes match (case-insensitive after trim)
                    if (mainAnn.annotationclass.trim().equals(diff.annotation.annotationclass.trim())) {
                        // Found a match! Assign this diff to the current slot
                        paired[j][i] = diff;
                        usedDiff[d] = true;   // Mark this diff as used
                        usedSlot[i] = true;   // Mark this slot as filled
                        break;  // Move to next main annotation
                    }
                }
            }

            // ==================== PASS 2: Fill Remaining Slots ====================
            // Place any unmatched diffs into available slots
            // These are diffs that didn't have a class match in pass 1
            
            // nextSlot: Index tracker for finding next available slot in paired[j][]
            // Used to sequentially fill empty slots with remaining diffs
            int nextSlot = 0;
            for (int d = 0; d < diffs.size(); d++) {
                // Skip diffs that were already matched in pass 1
                if (usedDiff[d]) continue;

                // Find next available slot that hasn't been filled
                while (nextSlot < maxsize && usedSlot[nextSlot]) {
                    nextSlot++;
                }
                
                // If there's still room, place the unmatched diff
                if (nextSlot < maxsize) {
                    paired[j][nextSlot] = diffs.get(d);
                    usedSlot[nextSlot] = true;
                    nextSlot++;
                }
            }
        }

        return paired;
    }
}
