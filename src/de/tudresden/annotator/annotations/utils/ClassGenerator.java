/**
 * 
 */
package de.tudresden.annotator.annotations.utils;

import java.util.LinkedHashMap;

import de.tudresden.annotator.annotations.AnnotationClass;
import de.tudresden.annotator.annotations.AnnotationTool;
import de.tudresden.annotator.oleutils.ColorFormatUtils;

/**
 * @author Elvis Koci
 */
public class ClassGenerator {
	
	private static final LinkedHashMap<String, AnnotationClass> annotationClasses;
	
	static{
		annotationClasses = new LinkedHashMap<String, AnnotationClass>();
		AnnotationClass[] classes = createAnnotationClasses();
		for (AnnotationClass annotationClass : classes) {
			annotationClasses.put(annotationClass.getLabel(), annotationClass);
		}
	}
		
	private static AnnotationClass[] createAnnotationClasses(){
		
		AnnotationClass[] classes =  new AnnotationClass[7];
		
		long white =  ColorFormatUtils.getRGBColorAsLong(255, 255, 255);
		long bordo = ColorFormatUtils.getRGBColorAsLong(192, 0, 0);
		long blue_accent5 = ColorFormatUtils.getRGBColorAsLong(68, 114, 196);
		long yellow = ColorFormatUtils.getRGBColorAsLong(255, 255, 49);
		long green_accent6 = ColorFormatUtils.getRGBColorAsLong(112, 173, 71);
		long orange_accent2 = ColorFormatUtils.getRGBColorAsLong(237, 125, 49);
		long blue_accent1 = ColorFormatUtils.getRGBColorAsLong(91, 155, 213);
		// long greyLight =  ColorFormatUtils.getRGBColorAsLong(217, 217, 217);
		long greyDark = ColorFormatUtils.getRGBColorAsLong(118, 113, 113);
		long purple = ColorFormatUtils.getRGBColorAsLong(112, 48, 160);
		
		// table can contains all the other classes
		classes[0] = createShapeAnnotationClass("Table", 1, 0, true, blue_accent5, 2, true, greyDark, true, false, false, null); 
		
		// Annotations of the following classes can be outside of a table or inside. Ex: tables can share metadata
		classes[1] = createTextBoxAnnotationClass("Metadata", orange_accent2, true, white, true, false, null); 
		classes[6] = createTextBoxAnnotationClass("Notes", yellow,  true, white, true, false, null);
		classes[5] = createTextBoxAnnotationClass("Derived", bordo,  true, white, true, false, null);
		
		// Annotations of the following classes can only be inside a table
		classes[2] = createTextBoxAnnotationClass("Header", blue_accent1,  true, white, true, true, classes[0]);
		classes[3] = createTextBoxAnnotationClass("Attributes", purple, true, white, true, true, classes[0]);
		classes[4] = createTextBoxAnnotationClass("Data", green_accent6,  true, white, true, true, classes[0]);
	
		return classes;
	}
	
	
	private static AnnotationClass createShapeAnnotationClass(String label, int shapeType, long fillColor, 
													boolean useLine, long lineColor, int lineWeight, boolean useShadow, long shadowColor, 
												    boolean isContainer, boolean isContainable, boolean isDependent, AnnotationClass container){
		
		AnnotationClass c = new AnnotationClass(label, AnnotationTool.SHAPE, false);
		
		if(isContainer){
			c.setHasFill(false);
		}else{
			c.setHasFill(true);
		}
		c.setColor(fillColor);
		
		
		c.setUseShadow(useShadow);
		c.setShadowColor(shadowColor);
		
		c.setUseLine(useLine);
		c.setLineColor(lineColor);
		c.setLineWeight(lineWeight);
		
		c.setShapeType(shapeType);
		
		c.setUseText(false);
		
		c.setIsContainer(isContainer);
		c.setCanBeContained(isContainable);
		c.setIsDependent(isDependent);
		c.setContainer(container);
			
		return c; 
	}

	private  static AnnotationClass createTextBoxAnnotationClass(String label, long backcolor, boolean useText, long textColor,
														    boolean isContainable, boolean isDependent, AnnotationClass container){
		
		AnnotationClass c = new AnnotationClass(label, AnnotationTool.TEXTBOX, backcolor);
		
		c.setHasFill(true);
		c.setColor(backcolor);
		
		c.setUseShadow(false);
		c.setUseLine(false);
		
		c.setUseText(useText);
		c.setText(label.toUpperCase());
		c.setTextColor(textColor);
		
		c.setCanBeContained(isContainable);
		c.setIsDependent(isDependent);
		c.setContainer(container);
		
		return c; 
	}


	/**
	 * @return the annotationclasses
	 */
	public static LinkedHashMap<String, AnnotationClass> getAnnotationClasses() {
		return annotationClasses;
	}
	
}
