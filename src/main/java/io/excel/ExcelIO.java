//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package io.excel;

import env.Parameters.WorkSpace;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import resultEditor.annotations.Annotation;
import resultEditor.annotations.AnnotationAttributeDef;
import resultEditor.annotations.Article;
import resultEditor.annotations.Depot;
import resultEditor.workSpace.WorkSet;

public class ExcelIO {
    public ExcelIO() {
    }

    public void save() {
        try {
            File project = WorkSpace.CurrentProject;
            Depot depot = new Depot();
            File[] files = WorkSet.getAllTextFile();
            XSSFWorkbook wb = new XSSFWorkbook();
            XSSFSheet sheet1 = wb.createSheet("PBM_Review_Summary");
            XSSFRow row0 = sheet1.createRow(0);
            row0.createCell(0).setCellValue("File Name with extension");
            row0.createCell(1).setCellValue("File Name");
            row0.createCell(2).setCellValue("Term");
            row0.createCell(3).setCellValue("Span");
            row0.createCell(4).setCellValue("Class");
            int j = 1;
            File[] var8 = files;
            int var9 = files.length;

            label68:
            for(int var10 = 0; var10 < var9; ++var10) {
                File file = var8[var10];
                if (file != null) {
                    String filename = file.getName();
                    String fullfilename = "";

                    try {
                        fullfilename = getFileNameNoEx(filename);
                    } catch (Exception var22) {
                        ;
                    }

                    String filepath = file.getAbsolutePath();
                    Article article = Depot.getArticleByFilename(filename);
                    if (article != null && article.annotations != null) {
                        Iterator var16 = article.annotations.iterator();

                        while(true) {
                            Annotation annotation;
                            do {
                                if (!var16.hasNext()) {
                                    continue label68;
                                }

                                annotation = (Annotation)var16.next();
                            } while(annotation == null);

                            XSSFRow row = sheet1.createRow(j);
                            row.createCell(0).setCellValue(fullfilename);
                            row.createCell(1).setCellValue(filename);
                            row.createCell(2).setCellValue(annotation.annotationText);
                            row.createCell(3).setCellValue(String.valueOf(annotation.getSpansInText()));
                            row.createCell(4).setCellValue(annotation.annotationclass);
                            int attributeStartPoint = 5;
                            if (annotation.attributes != null && !annotation.attributes.isEmpty()) {
                                Iterator var20 = annotation.attributes.iterator();

                                while(var20.hasNext()) {
                                    AnnotationAttributeDef aad = (AnnotationAttributeDef)var20.next();
                                    if (aad != null) {
                                        row.createCell((short)attributeStartPoint).setCellValue(aad.name);
                                        ++attributeStartPoint;
                                        row.createCell((short)attributeStartPoint).setCellValue(aad.value);
                                        ++attributeStartPoint;
                                    }
                                }
                            }

                            ++j;
                        }
                    }
                }
            }

            FileOutputStream fileOut = new FileOutputStream(project.getAbsolutePath() + File.separatorChar + "saved" + File.separatorChar + "Annotations.xlsx");
            wb.write(fileOut);
            fileOut.close();
        } catch (Exception var23) {
            var23.printStackTrace();
        }

    }

    public static String getFileNameNoEx(String filename) {
        if (filename != null && filename.length() > 0) {
            int dot = filename.lastIndexOf(46);
            if (dot > -1 && dot < filename.length()) {
                return filename.substring(0, dot);
            }
        }

        return filename;
    }
}
