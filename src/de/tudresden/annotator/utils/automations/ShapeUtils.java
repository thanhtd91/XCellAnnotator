/**
 * 
 */
package de.tudresden.annotator.utils.automations;

import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.Variant;

/**
 * @author Elvis Koci
 */
public class ShapeUtils {
	
	
	/**
	 * Draw a textbox at the specified location 
	 * @param shapesAutomation an OleAutomation that provides access to the "Shapes" Ole object. It represents a collection of shapes.
	 * @param left the distance, in points, from the left edge of column A to the left edge of the shape.
	 * @param top the distance, in points, from the top edge of row 1 to the top edge of the shape.
	 * @param width the width, in units, of the shape.
	 * @param height the height, in units, of the shape.
	 * @return an OleAutomation that provides access to the functionalities of the textbox that was just created
	 */
	public static OleAutomation drawTextBox(OleAutomation shapesAutomation, double left, double top, double width, double height){
				
		int[] addTextboxMethodIds = shapesAutomation.getIDsOfNames(new String[]{"AddTextbox", "Orientation", "Left", "Top", "Width", "Height"}); 
		Variant methodParams[] = new Variant[5];
		methodParams[0] = new Variant(1);
		methodParams[1] = new Variant(left); 
		methodParams[2] = new Variant(top); 
		methodParams[3] = new Variant(width); 
		methodParams[4] = new Variant(height);	
		
		Variant textboxVariant = shapesAutomation.invoke(addTextboxMethodIds[0], methodParams);
		
		shapesAutomation.dispose();
		for (Variant v : methodParams) {
			v.dispose();
		}
		
		OleAutomation textboxAutomation = null;
		if(textboxVariant!=null){
			textboxAutomation = textboxVariant.getAutomation();
			textboxVariant.dispose();
		}else{
			System.out.println("ERROR: Failed to draw textbox annotation!!!");
			System.exit(1);
		}
		
		return textboxAutomation;
	}
	

	/**
	 * Get FillFormat OleAutomation. This object can be used to change the format of the shape fill 
	 * @param shapeAutomation an OleAutomation that provides access to a Shape object. It represents an individual shape. 
	 * @return FillFormat OleAutomation for the specified shape. 
	 */
	public static OleAutomation getFillFormatAutomation(OleAutomation shapeAutomation){
		
		int[] fillPropertyIds = shapeAutomation.getIDsOfNames(new String[]{"Fill"}); 
		Variant fillFormatVariant = shapeAutomation.getProperty(fillPropertyIds[0]);
		OleAutomation fillFormatAutomation = fillFormatVariant.getAutomation();
		fillFormatVariant.dispose();
		
		return fillFormatAutomation;
	}
	
	
	/**
	 * Get the TextFrame OleAutomation. This object can be used to manage text in a shape.
	 * @param shapeAutomation an OleAutomation that provides access to a Shape object. It represents an individual shape. 
	 * @return an automation to access the TextFrame property functionalities. 
	 */
	public static OleAutomation getTextFrameAutomation(OleAutomation shapeAutomation){
		
		int[] textFramePropertyIds = shapeAutomation.getIDsOfNames(new String[]{"TextFrame"}); 
		Variant textFrameVariant = shapeAutomation.getProperty(textFramePropertyIds[0]);
		OleAutomation textFrameAutomation = textFrameVariant.getAutomation();
		textFrameVariant.dispose();
		
		return textFrameAutomation;
	}
}