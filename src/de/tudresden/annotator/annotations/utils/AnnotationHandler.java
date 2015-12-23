/**
 * 
 */
package de.tudresden.annotator.annotations.utils;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.swt.ole.win32.OleAutomation;

import de.tudresden.annotator.annotations.AnnotationClass;
import de.tudresden.annotator.annotations.AnnotationTool;
import de.tudresden.annotator.annotations.RangeAnnotation;
import de.tudresden.annotator.annotations.WorkbookAnnotation;
import de.tudresden.annotator.oleutils.CharactersUtils;
import de.tudresden.annotator.oleutils.CollectionsUtils;
import de.tudresden.annotator.oleutils.ColorFormatUtils;
import de.tudresden.annotator.oleutils.FillFormatUtils;
import de.tudresden.annotator.oleutils.FontUtils;
import de.tudresden.annotator.oleutils.LineFormatUtils;
import de.tudresden.annotator.oleutils.RangeUtils;
import de.tudresden.annotator.oleutils.ShadowFormatUtils;
import de.tudresden.annotator.oleutils.ShapeUtils;
import de.tudresden.annotator.oleutils.TextFrameUtils;
import de.tudresden.annotator.oleutils.WorkbookUtils;
import de.tudresden.annotator.oleutils.WorksheetUtils;

/**
 * This class handles all the operations related to the creation and management of the annotations 
 * @author Elvis Koci
 */
public class AnnotationHandler {

	/**
	 * This object stores and provides access to all annotations that are created in the embedded workbook  
	 */
	private static final WorkbookAnnotation workbookAnnotation = new WorkbookAnnotation();
	

	/**
	 * @return the workbookannotation
	 */
	public static WorkbookAnnotation getWorkbookAnnotation() {
		return workbookAnnotation;
	}

	
	/**
	 * Annotate the selected ranges (areas) of cells 
	 * @param workbookAutomation an OleAutomation for accessing the functionalities of the embedded workbook
	 * @param sheetName the name of the worksheet that contains the selected range 
	 * @param sheetIndex the index of the worksheet that contains the selected range
	 * @param selectedAreas an array of strings that represent all the selected ranges (areas)   
	 * @param annotationClass the class to use for the annotation
	 * @return an AnnotationResult object that holds information about the results from the execution of this method
	 */
	public static AnnotationResult annotate(OleAutomation workbookAutomation, String sheetName, int sheetIndex,
								String selectedAreas[], AnnotationClass annotationClass) {

		// get the OleAutomation object for the worksheet using its name
		OleAutomation sheetAutomation = WorkbookUtils.getWorksheetAutomationByName(workbookAutomation, sheetName);

		// unprotect the worksheet in order to create the annotations
		WorksheetUtils.unprotectWorksheet(sheetAutomation);

		// for each area in the range create an annotation
		for (String area : selectedAreas) {
			
			String[] subStrings = area.split(":");

			String topRightCell = subStrings[0];
			String downLeftCell = null;
			if (subStrings.length == 2)
				downLeftCell = subStrings[1];
			
			
			String classLabel = annotationClass.getLabel();
			String annotationName = generateAnnotationName(sheetName, classLabel, area);
			
			RangeAnnotation ra = new RangeAnnotation(sheetName, sheetIndex, annotationClass, annotationName, area);
			
			ValidationResult validationResult = validateAnnotation(ra);
			
			if(validationResult!=ValidationResult.OK){
				
				if(validationResult==ValidationResult.NOTCONTAINED){
					String containerLabel = annotationClass.getContainer().getLabel();
					String message = "Could not create a \""+classLabel+"\" annotation! The range it is not inside the borders of a "+containerLabel+" annotation range";
					return new AnnotationResult(validationResult, message);
				}
				
				if(validationResult==ValidationResult.OVERLAPPING){
					String message = "Could not create a \""+classLabel+"\" annotation! The selected range \""+area+"\" overlaps with an existing annotation."; 
					return new AnnotationResult(validationResult, message);
				}
			}
			

			OleAutomation rangeAutomation = WorksheetUtils.getRangeAutomation(sheetAutomation, topRightCell, downLeftCell);				
			callAnnotationMethod(sheetAutomation, rangeAutomation, annotationClass, annotationName);
			
			// save on the AnnotationDataSheet metadata about the annotation 
			AnnotationDataSheet.saveAnnotationData(workbookAutomation, ra);	
			
			// save on memory metadata about the annotation
			if(workbookAnnotation.getWorkbookName() == null || workbookAnnotation.getWorkbookName().compareTo("")==0){		
				String workbookName = WorkbookUtils.getWorkbookName(workbookAutomation);
				workbookAnnotation.setWorkbookName(workbookName);
			}
			workbookAnnotation.addRangeAnnotation(ra);
							
		}
					
		// protect the worksheet to prevent user from modifying the annotations
		WorksheetUtils.protectWorksheet(sheetAutomation);
		WorksheetUtils.makeWorksheetActive(sheetAutomation);
		sheetAutomation.dispose();
		
		return new AnnotationResult(ValidationResult.OK, "The annotation was successfully created");
	}
	
	
	/**
	 * Generate the name of the annotation 
	 * @param sheetName the name of the worksheet that contains the annotation
	 * @param classLabel the label of the class the annotation is member of
	 * @param rangeAddress a string that represents the address of the selected range
	 * @return a string that represents the annotation name
	 */
	public static String generateAnnotationName(String sheetName, String classLabel, String rangeAddress){	
		 String startOfAnnotationName = getStartOfAnnotationName(sheetName);
		 String endofAnnotationName = classLabel+"_"+rangeAddress.replace("$", "").replace(":", "_");
		 String formatedName = startOfAnnotationName+"_"+endofAnnotationName;
		 
		 return formatedName.toUpperCase();
	}
	
	
	/**
	 * Get the string that the names of all the annotations from the same worksheet begin with.    
	 * @param sheetName the name of the worksheet that contains the annotation
	 * @return  a string that is used as the beginning of the annotation name
	 */
	public static String getStartOfAnnotationName(String sheetName){
		
		String name = sheetName.replace(" ", "_")+"_Annotation"; 
		return  name.toUpperCase(); 
	}
	
	
	/**
	 * Validate the annotation to prevent inconsistencies. This method checks if the dependencies 
	 * between annotations are satisfied. Also, ensures that there are not overlaps between the annotations.   
	 * @param annotation an object that represents a range annotation  
	 * @return a ValidationResponse object that contains information about the validation
	 */
	public static ValidationResult validateAnnotation(RangeAnnotation annotation){

		AnnotationClass annotationClass =  annotation.getAnnotationClass();
		String sheetKey = annotation.getSheetName();
		//WorksheetAnnotation.generateKey(annotation.getSheetName(), annotation.getSheetIndex());
		
		if(annotationClass.isDependent()){
						
			AnnotationClass  containerClass = annotationClass.getContainer();
			String containerLabel =  containerClass.getLabel();
			
			Collection<RangeAnnotation> collection = workbookAnnotation.getSheetAnnotationsByClass(sheetKey, containerLabel);
			RangeAnnotation containerAnnotation = getSmallestParent(collection, annotation.getRangeAddress());
			
			if(containerAnnotation!=null){
				annotation.setParent(containerAnnotation);
			 
				Collection<RangeAnnotation> containersCollection = containerAnnotation.getAllAnnotations();
				if(containersCollection!=null){
					ArrayList<RangeAnnotation> dependentAnnotations = new ArrayList<RangeAnnotation>(containersCollection);
					return checkForOverlaps(dependentAnnotations, annotation.getRangeAddress(), false);
				}
				
			}else{
				return ValidationResult.NOTCONTAINED;
			}
						
		}else{
			
			if(annotationClass.isContainable()){
				
				 Collection<RangeAnnotation> collection = workbookAnnotation.getAllRangeAnnotationsForSheet(sheetKey);
				 ValidationResult result = checkForOverlaps(collection, annotation.getRangeAddress(), true);
				 if(result==ValidationResult.OK){
					 RangeAnnotation smallestParent = getSmallestParent(collection, annotation.getRangeAddress());
					 if(smallestParent!=null){
							annotation.setParent(smallestParent);
					}
				 }
				 return result;			 
			}else{
				 Collection<RangeAnnotation> collection = workbookAnnotation.getAllRangeAnnotationsForSheet(sheetKey);
				 return checkForOverlaps(collection, annotation.getRangeAddress(), false);
			}	
		}		
		return ValidationResult.OK;
	}
	
	
	/**
	 * Check if the new annotation overlaps with existing annotations
	 * @param collection the new annotation will be compared with each element of this collection for overlaps
	 * @param newAnnotationRange a string that represents the range address of the new annotation  
	 * @param ignoreContainers true to skip annotations that are members of classes that are marked as containers, false to consider them. 
	 * This argument was included especially for the cases where annotations can be contained, but do not have a specific parent annotation class.
	 * @return true if there are overlaps, false otherwise
	 */
	public static ValidationResult checkForOverlaps(Collection<RangeAnnotation> collection, String newAnnotationRange, boolean ignoreContainers){
		
		if(collection==null){
			 return ValidationResult.OK;
		}
		
		ArrayList<RangeAnnotation> annotations = new ArrayList<RangeAnnotation>(collection);
		for (int i = 0; i < annotations.size(); i++) {
			String annotatedRange = annotations.get(i).getRangeAddress();
			boolean isPartialContainment = RangeUtils.checkForPartialContainment(annotatedRange, newAnnotationRange);
			if(isPartialContainment){
				if(ignoreContainers){
					boolean isContainer = annotations.get(i).getAnnotationClass().isContainer();
					if(isContainer)
						continue;
				}
				return ValidationResult.OVERLAPPING;
			}
		}
		return ValidationResult.OK;
	}
	
	
	/**
	 * Get the smallest annotated range that contains completely the given range (annotation). 
	 * There might be several existing annotations that contain the range. These annotations 
	 * have to be members of AnnotationClasses that are defined as containers (i.e., can contain other annotations). 
	 * As annotations can not overlap with each other we are certain to find the smallest parent, which is also the direct
	 * (in hierarchy) parent of the given range (annotation).
	 * @param collection the collection of annotations to search for the smallest parent. 
	 * @param newAnnotationRange the range address of the new annotation 
	 * @return an annotation object that represents the smallest parent 
	 */
	public static RangeAnnotation getSmallestParent(Collection<RangeAnnotation> collection, String newAnnotationRange){
		
		if(collection==null){
			 return null;
		}
		
		ArrayList<RangeAnnotation> annotations = new ArrayList<RangeAnnotation>(collection);
		ArrayList<RangeAnnotation> parents = new ArrayList<RangeAnnotation>();
		for (int i = 0; i < annotations.size(); i++) {
			String annotatedRange = annotations.get(i).getRangeAddress();
			boolean isFullContainment = RangeUtils.checkForContainment(annotatedRange, newAnnotationRange);
			if(isFullContainment){
				RangeAnnotation annotation = annotations.get(i);
				boolean isContainer = annotation.getAnnotationClass().isContainer();
				if(isContainer)
					parents.add(annotation);
			}	
		}
		
		if(parents.isEmpty())
			return null;
		
		RangeAnnotation smallestParent = parents.get(0); 
		for (int j = 1; j < parents.size(); j++){
			String smallestRange = smallestParent.getRangeAddress();
			RangeAnnotation candidate = parents.get(j);
			String candidateRange = candidate.getRangeAddress(); 
			
			boolean result = RangeUtils.checkForContainment(smallestRange, candidateRange);		
			if(result)
				smallestParent = candidate;
		}
		
		return smallestParent;
	}
	
	
	/**
	 * Call an annotation function depending on the annotation tool
	 * @param sheetAutomation an OleAutomation for accessing the active worksheet functionalities
	 * @param rangeAutomation an OleAutomation for accessing the selected range functionalities
	 * @param annotationClass the (annotation) class that will be used  for the annotation
	 * @param annotationName the name of the annotation to create
	 */
	public static void callAnnotationMethod(OleAutomation sheetAutomation, OleAutomation rangeAutomation, 
															AnnotationClass annotationClass, String annotationName){
		switch (annotationClass.getAnnotationTool()) {
		case SHAPE  : annotateSelectedAreaWithShapes(sheetAutomation, rangeAutomation, annotationClass, annotationName); break;
		case TEXTBOX  :  annotateSelectedAreaWithShapes(sheetAutomation, rangeAutomation, annotationClass, annotationName); break;
		case BORDERAROUND: annotateByBorderingSelectedArea(rangeAutomation, annotationClass, annotationName); break;
		default: System.out.println("Option not recognized"); System.exit(1); break;
		}	
	}
	
	
	/**
	 * Annotate the selected range by drawing a border around it 
	 * @param rangeAutomation rangeAutomation an OleAutomation for accessing the selected range functionalities
	 * @param annotationClass the (annotation) class that will be used  for the annotation
	 * @param annotationName the name of the annotation to create
	 */
	public static void annotateByBorderingSelectedArea(OleAutomation rangeAutomation, AnnotationClass annotationClass, String annotationName){
	
		long color = annotationClass.getLineColor();
		if(annotationClass.getLineColor()<0){
			color = annotationClass.getColor();
		}
			
		RangeUtils.drawBorderAroundRange(rangeAutomation, annotationClass.getLineStyle(), annotationClass.getLineWeight(), color);	
		rangeAutomation.dispose();
	}
	
	
	/**
	 * Annotate the selected range of cells (area) using a shape object
	 * @param sheetAutomation an OleAutomation for accessing the active worksheet functionalities
	 * @param rangeAutomation rangeAutomation an OleAutomation for accessing the selected range functionalities
	 * @param annotationClass the (annotation) class that will be used  for the annotation
	 * @param annotationName the name of the annotation to create 
	 */
	public static void annotateSelectedAreaWithShapes(OleAutomation sheetAutomation, OleAutomation rangeAutomation, 
																		AnnotationClass annotationClass, String annotationName){
		
		double left = RangeUtils.getRangeLeftPosition(rangeAutomation);  
		double top = RangeUtils.getRangeTopPosition(rangeAutomation);
		double width = RangeUtils.getRangeWidth(rangeAutomation);
		double height = RangeUtils.getRangeHeight(rangeAutomation);
		rangeAutomation.dispose();
		
		OleAutomation shapesAutomation = WorksheetUtils.getWorksheetShapes(sheetAutomation);	
		
		if(annotationClass.getAnnotationTool()==AnnotationTool.TEXTBOX){	
			
			OleAutomation textboxAutomation = ShapeUtils.drawTextBox(shapesAutomation, left, top, width, height); 
			setAnnotationProperties(textboxAutomation, annotationClass);
			ShapeUtils.setShapeName(textboxAutomation, annotationName);
			textboxAutomation.dispose();
		}
		
		if(annotationClass.getAnnotationTool()==AnnotationTool.SHAPE){		
			
			OleAutomation shapeAuto = ShapeUtils.drawShape(shapesAutomation, annotationClass.getShapeType(), left, top, width, height);
			setAnnotationProperties(shapeAuto, annotationClass);  
			ShapeUtils.setShapeName(shapeAuto, annotationName);
			shapeAuto.dispose();
		}
				
		shapesAutomation.dispose();
	}
			
	
	/**
	 * Format the annotation object (shape, textbox, etc) used to annotate 
	 * @param annotation an OleAutomation to access the functionalities of the annotation object
	 * @param annotationClass an object that represents an annotation class
	 */
	public static void setAnnotationProperties(OleAutomation annotation, AnnotationClass annotationClass ){
		
		// set fill
		OleAutomation fillFormatAutomation = ShapeUtils.getFillFormatAutomation(annotation);	
		if(annotationClass.hasFill()){
			//TODO: Handle specific cases  
			ColorFormatUtils.setBackColor(fillFormatAutomation, annotationClass.getColor());
			ColorFormatUtils.setForeColor(fillFormatAutomation, annotationClass.getColor());
			FillFormatUtils.setFillTransparency(fillFormatAutomation, annotationClass.getFillTransparency());
		}else{
			FillFormatUtils.setFillVisibility(fillFormatAutomation, false);  	
		}
		
		// set border/line
		OleAutomation lineFormatAutomation = ShapeUtils.getLineFormatAutomation(annotation);
		if(annotationClass.useLine()){					
			LineFormatUtils.setLineStyle(lineFormatAutomation, annotationClass.getLineStyle());
			LineFormatUtils.setLineWeight(lineFormatAutomation, annotationClass.getLineWeight());
			ColorFormatUtils.setForeColor(lineFormatAutomation, annotationClass.getLineColor());
			LineFormatUtils.setLineTransparency(lineFormatAutomation, annotationClass.getLineTransparency());	
			
		}else{
			LineFormatUtils.setLineVisibility(lineFormatAutomation, false);
		}
		
		// set shadow
		OleAutomation shadowFormatAutomation = ShapeUtils.getShadowFormatAutomation(annotation);
		if(annotationClass.useShadow()){
			ShadowFormatUtils.setShadowType(shadowFormatAutomation, annotationClass.getShadowType()); 
			ShadowFormatUtils.setShadowStyle(shadowFormatAutomation, annotationClass.getShadowStyle());
			ShadowFormatUtils.setShadowSize(shadowFormatAutomation, annotationClass.getShadowSize());
			ShadowFormatUtils.setShadowBlur(shadowFormatAutomation, annotationClass.getShadowBlur());
			ColorFormatUtils.setForeColor(shadowFormatAutomation, annotationClass.getColor());
			ShadowFormatUtils.setShadowTransparency(shadowFormatAutomation, annotationClass.getShadowTransparency());				
		}else{
			ShadowFormatUtils.setShadowVisibility(shadowFormatAutomation, false);
		}
	
		// set text properties
		if(annotationClass.useText()){	
			
			OleAutomation textFrameAutomation = ShapeUtils.getTextFrameAutomation(annotation);
			TextFrameUtils.setHorizontalAlignment(textFrameAutomation, annotationClass.getTextHAlignment());  
			TextFrameUtils.setVerticalAlignment(textFrameAutomation, annotationClass.getTextVAlignment());
			
			OleAutomation charactersAutomation = TextFrameUtils.getCharactersAutomation(textFrameAutomation);
			CharactersUtils.setText(charactersAutomation, annotationClass.getText());
			
			OleAutomation fontAutomation = CharactersUtils.getFontAutomation(charactersAutomation);
			FontUtils.setFontColor(fontAutomation, annotationClass.getTextColor());
			FontUtils.setBoldFont(fontAutomation, annotationClass.isBoldText()); 
			FontUtils.setFontSize(fontAutomation, annotationClass.getFontSize()); // TODO: should be relative to the size of the range 
			
			fontAutomation.dispose();
			charactersAutomation.dispose();
			textFrameAutomation.dispose();
		}

		shadowFormatAutomation.dispose();
		lineFormatAutomation.dispose();
		fillFormatAutomation.dispose();
	}
	
	
	/**
	 * Hide/Show all shapes in the embedded workbook that are used for annotating ranges of cells
	 * @param workbookAutomation an OleAutomation to access the functionalities of the embedded workbook
	 * @param visible true to make annotation shapes visible, false to hide them
	 */
	public static void setVisilityForShapeAnnotations(OleAutomation workbookAutomation, boolean visible){
		
		OleAutomation worksheets = WorkbookUtils.getWorksheetsAutomation(workbookAutomation);
		int count = CollectionsUtils.countItemsInCollection(worksheets);
		
		// indexing starts from 1 
		for (int i = 1; i < count; i++) {
			OleAutomation sheet = CollectionsUtils.getItemByIndex(worksheets, i, false);
			setVisibilityForWorksheetShapeAnnotations(sheet, visible);
		}		
	}
	
	
	/**
	 * Hide/Show all the shapes used for annotations in the worksheet having the given name. 
	 * This method will loop through the collection of shapes in the worksheet and set their visibility to false or true,
	 * if they are used for annotations. The other shapes, that were present in the original file, will not be affected. 
	 * @param workbookAutomation an OleAutomation to access the functionalities of the embedded workbook
	 * @param sheetName the name of the worksheet for which the action will be performed
	 * @param visible true to make annotation shapes visible, false to hide them
	 */
	public static void setVisibilityForWorksheetShapeAnnotations(OleAutomation workbookAutomation, String sheetName, boolean visible ){
		
		// worksheet that stores the annotation (meta-)data does not have annotation shapes 
		if(sheetName.compareTo(AnnotationDataSheet.name)==0)
			return;
		
		// get the OleAutomation object for the worksheet using the given name
		OleAutomation worksheetAutomation = WorkbookUtils.getWorksheetAutomationByName(workbookAutomation, sheetName);
		setVisibilityForWorksheetShapeAnnotations(worksheetAutomation, visible);
	}
	
	
	/**
	 * Hide/Show all the shapes used for annotations in the worksheet having the given name. 
	 * This method will loop through the collection of shapes in the worksheet and set their visibility to false or true,
	 * if they are used for annotations. The other shapes, that were present in the original file, will not be affected.
	 * @param worksheetAutomation an OleAutoamtion to access the functionalities of the worksheet that action will be applied on
	 * @param visible true to make annotation shapes visible, false to hide them
	 */
	public static void setVisibilityForWorksheetShapeAnnotations(OleAutomation worksheetAutomation,  boolean visible){
		
		// unprotect the worksheet in order to change the visibility of the shapes
		WorksheetUtils.unprotectWorksheet(worksheetAutomation);
		
		// get the collection of shapes in the worksheet
		OleAutomation shapesAutomation = WorksheetUtils.getWorksheetShapes(worksheetAutomation);
		
		// if there are no shapes in the worksheet, skip the process
		if(shapesAutomation==null)
			return;
		 		
		// all shapes that are used for annotations have names that start with the following string pattern 
		String sheetName = WorksheetUtils.getWorksheetName(worksheetAutomation);
		String startString = getStartOfAnnotationName(sheetName);
		
		int count = CollectionsUtils.countItemsInCollection(shapesAutomation);	
		for (int i = 1; i <= count; i++) {
			 OleAutomation shapeAutomation = CollectionsUtils.getItemByIndex(shapesAutomation, i, true);	 
			 String name = ShapeUtils.getShapeName(shapeAutomation);
			 if(name.indexOf(startString)== 0){
				 ShapeUtils.setShapeVisibility(shapeAutomation, visible);
			 }
			 // shapeAutomation.dispose();
		}
				
		shapesAutomation.dispose();
		
		// protect the worksheet from further user manipulation 
		WorksheetUtils.protectWorksheet(worksheetAutomation);
		worksheetAutomation.dispose();	
	}
	
	
	/**
	 * Delete all shapes in the embedded workbook that are used for annotating ranges of cells
	 * @param workbookAutomation an OleAutomation to access the functionalities of the embedded workbook
	 */
	public static void deleteAllShapeAnnotations(OleAutomation workbookAutomation){
		
		OleAutomation worksheets = WorkbookUtils.getWorksheetsAutomation(workbookAutomation);		
		int count = CollectionsUtils.countItemsInCollection(worksheets);
		
		// indexing starts from 1 
		for (int i = 1; i < count; i++) {
			OleAutomation sheet = CollectionsUtils.getItemByIndex(worksheets, i, false);
			deleteShapeAnnotationsFromWorksheet(sheet);
		}
	}
	
	
	/**
	 * Delete all the shapes used for annotations in the worksheet having the given name 
	 * @param workbookAutomation an OleAutomation to access the functionalities of the embedded workbook
	 * @param sheetName the name of the worksheet for which the annotation shapes will be deleted
	 */
	public static void deleteShapeAnnotationsFromWorksheet(OleAutomation workbookAutomation, String sheetName ){
		
		// can not apply this method to the worksheet that stores the annotation (meta-)data, as it does not contain any shapes 
		if(sheetName.compareTo(AnnotationDataSheet.name)==0)
			return;
		
		// get the OleAutomation object for the active worksheet using its name
		OleAutomation worksheetAutomation = WorkbookUtils.getWorksheetAutomationByName(workbookAutomation, sheetName);
		deleteShapeAnnotationsFromWorksheet(worksheetAutomation);
	}

	
	/**
	 * Delete all the shapes used for annotations in the given worksheet 
	 * @param worksheetAutomation an OleAutoamtion to access the functionalities of the worksheet that action will be applied on
	 */
	public static void deleteShapeAnnotationsFromWorksheet(OleAutomation worksheetAutomation){
		
		// unprotect the worksheet in order delete the shape annotations
		WorksheetUtils.unprotectWorksheet(worksheetAutomation);
		
		// delete all shapes that are used for annotating ranges of cells
		OleAutomation shapesAutomation = WorksheetUtils.getWorksheetShapes(worksheetAutomation);	
		int count = CollectionsUtils.countItemsInCollection(shapesAutomation);	
		int processed = 0; 
		
		// all shapes that are used for annotating have names that start with the following string pattern 
		String sheetName = WorksheetUtils.getWorksheetName(worksheetAutomation);
		String startString =  getStartOfAnnotationName(sheetName);
		while (processed!=count){
			 OleAutomation shapeAutomation = CollectionsUtils.getItemByIndex(shapesAutomation, 1, true);	 
			 String name = ShapeUtils.getShapeName(shapeAutomation);
			 if(name.indexOf(startString)==0){
				 boolean result = ShapeUtils.deleteShape(shapeAutomation);
				 if(result)
					 workbookAnnotation.removeRangeAnnotation(sheetName, name);
			 }
			 processed++;
			 shapeAutomation.dispose();
			 // TODO: Dispose shapeAutomation ?
		}			
		shapesAutomation.dispose();
		
		// protect the worksheet from further user manipulation 
		WorksheetUtils.protectWorksheet(worksheetAutomation);
		worksheetAutomation.dispose();	
	}
}