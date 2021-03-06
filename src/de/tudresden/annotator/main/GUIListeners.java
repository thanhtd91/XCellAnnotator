/**
 * 
 */
package de.tudresden.annotator.main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.OleEvent;
import org.eclipse.swt.ole.win32.OleListener;
import org.eclipse.swt.ole.win32.Variant;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;

import de.tudresden.annotator.annotations.AnnotationClass;
import de.tudresden.annotator.annotations.RangeAnnotation;
import de.tudresden.annotator.annotations.WorkbookAnnotation;
import de.tudresden.annotator.annotations.WorksheetAnnotation;
import de.tudresden.annotator.annotations.utils.AnnotationHandler;
import de.tudresden.annotator.annotations.utils.AnnotationStatusSheet;
import de.tudresden.annotator.annotations.utils.RangeAnnotationsSheet;
import de.tudresden.annotator.oleutils.ApplicationUtils;
import de.tudresden.annotator.oleutils.RangeUtils;
import de.tudresden.annotator.oleutils.WindowUtils;
import de.tudresden.annotator.oleutils.WorkbookUtils;
import de.tudresden.annotator.oleutils.WorksheetUtils;

/**
 * @author Elvis Koci
 */
public class GUIListeners {
	
	private static final Logger logger = LogManager.getLogger(GUIListeners.class.getName());
	public static boolean skipActionsOnSheetActivation = false;
	
	/**
	 * 
	 * @return
	 */
	protected static Listener createCloseApplicationEventListener(){
				
		return new Listener()
	    {
	        public void handleEvent(Event event)
	        {	
	        	Launcher.getInstance().setExcelPanelEnabled(false);
	        	
	        	if(!Launcher.getInstance().isControlSiteNull() && 
						AnnotationHandler.getWorkbookAnnotation().hashCode()!= AnnotationHandler.getOldWorkbookAnnotationHash()){
	    			
		    		Launcher wm = Launcher.getInstance();
		    		String directoryPath = wm.getDirectoryPath();
		    		String fileName = wm.getFileName();
		    		OleAutomation embeddedWorkbook = wm.getEmbeddedWorkbook();

	        		int style = SWT.YES | SWT.NO | SWT.CANCEL | SWT.ICON_WARNING ;
	        		MessageBox messageBox = Launcher.getInstance().createMessageBox(style);
	 	            messageBox.setMessage("Want to save your changes?");
	 	            
	 	            int response = messageBox.open();	 	 	            
	 	            if( response== SWT.YES){	
	 	            	
	 	            	String filePath = directoryPath+"\\"+fileName;
	 	            	
	 	            	skipActionsOnSheetActivation = true;
	 	            	boolean isSaved = FileUtils.saveProgress(embeddedWorkbook, filePath, true);
	 	            	skipActionsOnSheetActivation = false;
	 	            	
	 	            	if(!isSaved){
	 	            		int styleError = SWT.ICON_ERROR;
	 		        		MessageBox errorMessageBox = Launcher.getInstance().createMessageBox(styleError);
	 		        		errorMessageBox.setMessage("ERROR: Could not save the file!");
	 	            		event.doit = false;
	 	            	}
	 	            	
	 	            	WorkbookUtils.closeEmbeddedWorkbook(embeddedWorkbook, false);
	 	            	Launcher.getInstance().disposeControlSite();
	 	            	Launcher.getInstance().disposeShell();
	 	            	event.doit = true;
	 	            	
	 	            	
	 	            } 
	 	            
	 	            if(response == SWT.NO){
	 	            	Launcher.getInstance().disposeControlSite();
	 	            	Launcher.getInstance().disposeShell();
	 	            	event.doit = true;
	 	            } 
	 	            
	 	            event.doit = false;
	        	}else{
	        		int style = SWT.YES | SWT.NO | SWT.ICON_QUESTION;
	        		MessageBox messageBox = Launcher.getInstance().createMessageBox(style);
	 	            messageBox.setMessage("Do you want to close the application?");
	 	            
	 	            int response = messageBox.open();
	 	            if( response== SWT.YES){
	 	        	    Launcher.getInstance().disposeControlSite();
	 	            	Launcher.getInstance().disposeShell();
	 	            	event.doit = true;
	 	            }
	 	            event.doit = false;
	        	}
	        	
	        	Launcher.getInstance().setExcelPanelEnabled(true);
	        }
	    };
	}	
	
	
	/**
	 * Create a SheetSelection OLE event listener
	 * @param application
	 * @return an OleListener 
	 */
	protected static OleListener createSheetSelectionEventListener(){
		
		OleListener listener = new OleListener() {
	        public void handleEvent (OleEvent e) {
	        	
	        	Variant[] args = e.arguments;
	        	
	            /*
	             * the first argument is a Range object. Get the number and range of selected areas 
	             */	        	
	        	OleAutomation rangeAutomation = args[0].getAutomation();
	        	Launcher.getInstance().setCurrentSelection(RangeUtils.getRangeAddress(rangeAutomation).split(","));
	        	args[0].dispose();
	        	rangeAutomation.dispose();	
	        	
				/*
				 * the second argument is a Worksheet object. Get the name and index of the worksheet.
				 */
	        	OleAutomation worksheetAutomation = args[1].getAutomation();		        
	        	Launcher.getInstance().setActiveWorksheetName(WorksheetUtils.getWorksheetName(worksheetAutomation));
	        	Launcher.getInstance().setActiveWorksheetIndex(WorksheetUtils.getWorksheetIndex(worksheetAutomation));
				args[1].dispose();	
				worksheetAutomation.dispose();
						
				Launcher.getInstance().setFocusToShell();
				Launcher.getInstance().setFocusToShell();
				
	        }
	    };	       
	    return listener;
	}
	
	
	/**
	 * Create a SheetActivate OLE event listener
	 * @param application
	 * @return
	 */
	protected static OleListener createSheetActivationEventListener(){
		
		OleListener listener = new OleListener() {
	        public void handleEvent (OleEvent e) {
	        	        	
	       	
	        	Variant[] args = e.arguments;
	        	
	        	/*
	             * This event returns only one argument, a Worksheet object. Get the name and index of the activated worksheet.
	             */ 					
				OleAutomation worksheetAutomation = args[0].getAutomation();        
				String activeSheetName = WorksheetUtils.getWorksheetName(worksheetAutomation);
				int activeSheetIndex = WorksheetUtils.getWorksheetIndex(worksheetAutomation);
				args[0].dispose();
				worksheetAutomation.dispose();
				
				String previousSheetName =Launcher.getInstance().getActiveWorksheetName();
	        	
				// update the information about the active sheet
				Launcher.getInstance().setActiveWorksheetName(activeSheetName);
				Launcher.getInstance().setActiveWorksheetIndex(activeSheetIndex);				
				
				// hide any existing tooltip, before showing new one about the active (current) sheet
	            Launcher.getInstance().getTooltip().setVisible(false);
				
	            if(!skipActionsOnSheetActivation){
					
					// when a new sheet is activate the redo and undo list are cleared.  
					WorksheetAnnotation activeSheetAnnotation = AnnotationHandler.getWorkbookAnnotation()
							.getWorksheetAnnotations().get(activeSheetName);
					
					WorksheetAnnotation previousSheetAnnotation = AnnotationHandler.getWorkbookAnnotation()
							.getWorksheetAnnotations().get(previousSheetName);
									
					if(!Launcher.getInstance().isControlSiteNull() && ((activeSheetAnnotation!=null && 
							!activeSheetAnnotation.getAllAnnotations().isEmpty()) || 
							activeSheetName.compareTo(RangeAnnotationsSheet.getName())==0) && 
							previousSheetAnnotation!=null ){ 		
							
							// Keep Redo and Undo list per sheet. Erase when sheet changes
							AnnotationHandler.clearRedoList();
							AnnotationHandler.clearUndoList();
							
							// should not remember selection from previous sheet
							Launcher.getInstance().setCurrentSelection(null);						
		        	}
							
					if(!Launcher.getInstance().isControlSiteNull() && activeSheetAnnotation!=null && 
							( previousSheetAnnotation!=null || previousSheetName.compareTo(RangeAnnotationsSheet.getName())==0)){
							// update display (appearance) for activated sheet
							Launcher.getInstance().updateActiveSheetDisplay();
		        	}
		        
					// adjust the bar menu according to the properties of the workbook and the active sheet
					BarMenuUtils.adjustBarMenuForWorkbook();		
									
					// return the focus to the embedded excel workbook, if it does not have it already
					if(!Launcher.getInstance().isControlSiteFocusControl())
						Launcher.getInstance().setFocusToControlSite();
				}			
	        }
	    };	       
	    return listener;
	}
	
	/**
	 * 
	 * @return
	 */
	protected static Listener createArrowButtonPressedEventListener(){
		return new Listener() {
			 @Override
	         public void handleEvent(Event e) {
	        	if(e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN || 
	        	   e.keyCode == SWT.ARROW_LEFT || e.keyCode == SWT.ARROW_RIGHT)
	            {
	        		Launcher.getInstance().setFocusToControlSite();
	            }
	        }
		};
	}
	
	/**
	 * 
	 * @return
	 */
	protected static Listener createMouseWheelEventListener(){		
		return new Listener() {
			@Override
			public void handleEvent(Event e) {
					Launcher.getInstance().setFocusToControlSite();
			}		
		};
	}
		
	/**
	 * 
	 * @return
	 */
	protected static SelectionListener createFileOpenSelectionListener(){
		
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				
				// block user input (keyboard mouse) while file is loading
				Launcher.getInstance().setExcelPanelEnabled(false);
				
				// warn the user user if there exist an opened file
				// and offer them to save their progress
				if(!Launcher.getInstance().isControlSiteNull()  && 
					AnnotationHandler.getWorkbookAnnotation().hashCode()!=
						AnnotationHandler.getOldWorkbookAnnotationHash()){
									
	        		int style = SWT.YES | SWT.NO | SWT.CANCEL | SWT.ICON_WARNING ;
	        		MessageBox messageBox = Launcher.getInstance().createMessageBox(style);
	 	            messageBox.setMessage("Do you want to save the progress on the existing file ?");
	 	            
	 	            int response = messageBox.open();	 	 	            
	 	            if( response== SWT.YES){	
	 	            
	 	            	OleAutomation embeddedWorkbook = Launcher.getInstance().getEmbeddedWorkbook();
	 	            	String filePath =  Launcher.getInstance().getDirectoryPath()+"\\"+Launcher.getInstance().getFileName();
	 	            	
	 	            	skipActionsOnSheetActivation=true;
	 	            	boolean isSaved = FileUtils.saveProgress(embeddedWorkbook, filePath, true);
	 	            	skipActionsOnSheetActivation=false;
	 	            	
	 	            	if(!isSaved){
	 	            		int messageStyle = SWT.ICON_ERROR;
	 						MessageBox message = Launcher.getInstance().createMessageBox(messageStyle);
	 						message.setMessage("ERROR: The file could not be saved!");
	 						message.open();
	 	            	}
	 	            	
	 	            	
	 	            } 	 	            
				}
				
				// clear the resources used by the previous file, if there was an opened file
				if(!Launcher.getInstance().isControlSiteNull()) {
					OleAutomation embeddedWorkbook  = Launcher.getInstance().getEmbeddedWorkbook();
					WorkbookUtils.closeEmbeddedWorkbook(embeddedWorkbook, false);		
					Launcher.getInstance().setEmbeddedWorkbook(null);
					Launcher.getInstance().disposeControlSite();
				}
			
				// open the files selection window
				FileUtils.fileOpen();
				
				// get the OleAutomation for the embedded workbook
				OleAutomation workbookAutomation = Launcher.getInstance().getEmbeddedWorkbook();
				if (workbookAutomation == null) {
					Launcher.getInstance().setExcelPanelEnabled(true);
					return; // there is no embedded workbook (file)
				}
				
						
				// clear all existing annotations in memory structure, 
				// if they exist from the previously opened file 
				AnnotationHandler.getWorkbookAnnotation().removeAllAnnotations();
				
				// turn off screen updating to speed the following actions.  
				OleAutomation application = WorkbookUtils.getApplicationAutomation(workbookAutomation);
				ApplicationUtils.setScreenUpdating(application, false);
	
				// create the base in memory structure for storing annotation data
				// retrieve the annotation statuses from previous session
				AnnotationStatusSheet.readAnnotationStatuses(workbookAutomation);
				
				// read the data and re-create the range annotation objects
				RangeAnnotation[] rangeAnnotations = RangeAnnotationsSheet.readRangeAnnotations(workbookAutomation);				
				if(rangeAnnotations!=null){		
					// update workbook annotation and re-draw all the range annotations  
					AnnotationHandler.recreateRangeAnnotations(workbookAutomation, rangeAnnotations);	
				}
				
				// turn on screen updating after all previous annotations are restored
				ApplicationUtils.setScreenUpdating(application, true);
				
				// save the current hash of the workbook annotation
				// will be used later to determine if user has made some changes 
				// and the file needs to be saved
				AnnotationHandler.setOldWorkbookAnnotationHash(
						AnnotationHandler.getWorkbookAnnotation().hashCode()); 
				
				// Move to the first sheet in the workbook
				OleAutomation sheetAuto = 
						WorkbookUtils.getWorksheetAutomationByIndex(workbookAutomation, 1);	
				WorksheetUtils.makeWorksheetActive(sheetAuto);
				sheetAuto.dispose();
								
				//adjust display of active sheet for annotation
				Launcher.getInstance().updateActiveSheetDisplay();
				
				// adjust the menu items in the menu bar for the file that was just openned
				BarMenuUtils.adjustBarMenuForOpennedFile();
				
				// enable the excel panel to accept user inputs
				Launcher.getInstance().setExcelPanelEnabled(true);
			}
		};
	}
		
	/**
	 * 
	 * @return
	 */
	protected static SelectionListener createFileSaveSelectionListener(){
		
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				Launcher.getInstance().setExcelPanelEnabled(false);
				
				String fileName = Launcher.getInstance().getFileName()+"";
				String directory = Launcher.getInstance().getDirectoryPath();
				String filePath = directory+"\\"+fileName;
				
				skipActionsOnSheetActivation=true;
				boolean result = FileUtils.saveProgress(
						Launcher.getInstance().getEmbeddedWorkbook(), filePath, false);
				skipActionsOnSheetActivation=false;
					
				if(result){		
					
					OleAutomation firstSheet = WorkbookUtils.getWorksheetAutomationByIndex(
									Launcher.getInstance().getEmbeddedWorkbook(), 1);
					WorksheetUtils.makeWorksheetActive(firstSheet);
					firstSheet.dispose();
					
					AnnotationHandler.clearRedoList();
					AnnotationHandler.clearUndoList();
					
					// to check if the workbook has changed since last save
					int hash = AnnotationHandler.getWorkbookAnnotation().hashCode();
					AnnotationHandler.setOldWorkbookAnnotationHash(hash);
					
					BarMenuUtils.adjustBarMenuForWorkbook();
					
            		int style = SWT.ICON_INFORMATION;
					MessageBox message = Launcher.getInstance().createMessageBox(style);
					message.setMessage("The file was successfully saved.");
					message.open();
				}else{
					int style = SWT.ICON_ERROR;
					MessageBox message = Launcher.getInstance().createMessageBox(style);
					message.setMessage("ERROR: The file could not be saved!");
					message.open();
				}		
				
				Launcher.getInstance().setExcelPanelEnabled(true);
			}
		};
	}
	
	/**
	 * 
	 * @return
	 */
	protected static SelectionListener createFileCloseSelectionListener(){
		
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				Launcher.getInstance().setExcelPanelEnabled(false);
				
				if(AnnotationHandler.getWorkbookAnnotation().hashCode()!=
						AnnotationHandler.getOldWorkbookAnnotationHash()){
									
	        		int style = SWT.YES | SWT.NO | SWT.CANCEL | SWT.ICON_WARNING ;
	        		MessageBox messageBox = Launcher.getInstance().createMessageBox(style);
	 	            messageBox.setMessage("Want to save your changes?");
	 	            
	 	            int response = messageBox.open();	 	 	            
	 	            if( response== SWT.YES){	
	 	            	OleAutomation embeddedWorkbook = Launcher.getInstance().getEmbeddedWorkbook();
	 	            	String filePath =  Launcher.getInstance().getDirectoryPath()+"\\"+Launcher.getInstance().getFileName();
	 	            	
	 	            	skipActionsOnSheetActivation=true;
	 	            	boolean isSaved = FileUtils.saveProgress(embeddedWorkbook, filePath, true);
	 	            	skipActionsOnSheetActivation=false;
	 	            	
	 	            	if(!isSaved){
	 	            		int messageStyle = SWT.ICON_ERROR;
	 						MessageBox message = Launcher.getInstance().createMessageBox(messageStyle);
	 						message.setMessage("ERROR: The file could not be saved!");
	 						message.open();
	 	            	}
	 	            } 
	 	            
	 	            if(response == SWT.NO || response == SWT.YES){
	 	            	
	 	            	OleAutomation embeddedWorkbook  = Launcher.getInstance().getEmbeddedWorkbook();
	 					WorkbookUtils.closeEmbeddedWorkbook(embeddedWorkbook, false);
	 					Launcher.getInstance().setEmbeddedWorkbook(null);
	 					
	 					Launcher.getInstance().disposeControlSite();
	 					
	 					Color lightGreyShade = new Color (Display.getCurrent(), 247, 247, 247);
	 					Launcher.getInstance().setColorToExcelPanel(lightGreyShade);
	 					
	 					BarMenuUtils.adjustBarMenuForFileClose();
	 	            } 
	 	            
				}else{
					
	        		int style = SWT.YES | SWT.NO | SWT.ICON_QUESTION;
	        		MessageBox messageBox = Launcher.getInstance().createMessageBox(style);
	 	            messageBox.setMessage("Are you sure you want to close the file?");
	 	            
	 	            int response = messageBox.open();
	 	            if( response== SWT.YES){
	 	            	 	            	
	 	            	OleAutomation embeddedWorkbook  = Launcher.getInstance().getEmbeddedWorkbook();
	 					WorkbookUtils.closeEmbeddedWorkbook(embeddedWorkbook, false);
	 					Launcher.getInstance().setEmbeddedWorkbook(null);
	 					
	 					Launcher.getInstance().disposeControlSite();
	 					
	 					Color lightGreyShade = new Color (Display.getCurrent(), 247, 247, 247);
	 					Launcher.getInstance().setColorToExcelPanel(lightGreyShade);
	 					
	 					BarMenuUtils.adjustBarMenuForFileClose();
	 	            }
	        	}		
				
				Launcher.getInstance().setExcelPanelEnabled(true);
			}
		};
	}
	
	/**
	 * 
	 * @return
	 */
	protected static SelectionListener createExportAsCSVSelectionListener(){
		
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				Launcher.getInstance().setExcelPanelEnabled(false);
				
				OleAutomation workbookAutomation = Launcher.getInstance().getEmbeddedWorkbook();
				String directoryPath = Launcher.getInstance().getDirectoryPath();
				String fileName = Launcher.getInstance().getFileName();				
				boolean isSuccess = RangeAnnotationsSheet.exportRangeAnnotationsAsCSV(workbookAutomation, directoryPath, fileName);
				
				if(isSuccess){
					MessageBox messageBox = Launcher.getInstance().createMessageBox(SWT.ICON_INFORMATION);
					messageBox.setText("Information");
		            messageBox.setMessage("The annotation data were successfully exported at:\n"+directoryPath);
		            messageBox.open();
				}else{
					MessageBox messageBox = Launcher.getInstance().createMessageBox(SWT.ICON_ERROR);
					messageBox.setText("Error Message");
		            messageBox.setMessage("An error occured. Could not export the annotation data as csv.");
		            messageBox.open();
				}
				
				Launcher.getInstance().setExcelPanelEnabled(true);
			}
		};
	}
	
	
	/**
	 * 
	 * @return
	 */
	protected static SelectionListener createExportAsWorkbookSelectionListener(){
		
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
					MessageBox messageBox = Launcher.getInstance().createMessageBox(SWT.ICON_INFORMATION);
					messageBox.setText("Information");
		            messageBox.setMessage("This option is not implemented yet");
			}
		};
	}
	
	/**
	 * 
	 * @return
	 */
	protected static SelectionListener createFileExitSelectionListener(){
		
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {	
				
				Launcher.getInstance().setExcelPanelEnabled(false);
				
				if(!Launcher.getInstance().isControlSiteNull() && 
					AnnotationHandler.getWorkbookAnnotation().hashCode()!= AnnotationHandler.getOldWorkbookAnnotationHash()){
								
	        		int style = SWT.YES | SWT.NO | SWT.CANCEL | SWT.ICON_WARNING ;
	        		MessageBox messageBox = Launcher.getInstance().createMessageBox(style);
	 	            messageBox.setMessage("Want to save your changes?");
	 	            
	 	            int response = messageBox.open();	 	 	            
	 	            if( response== SWT.YES){	
	 	            	OleAutomation embeddedWorkbook = Launcher.getInstance().getEmbeddedWorkbook();
	 	            	String filePath =  Launcher.getInstance().getDirectoryPath()+"\\"+Launcher.getInstance().getFileName();
	 	            	
	 	            	skipActionsOnSheetActivation=true;
	 	            	boolean isSaved = FileUtils.saveProgress(embeddedWorkbook, filePath, true);
	 	            	skipActionsOnSheetActivation=false;
	 	            	
	 	            	if(!isSaved){
	 	            		int messageStyle = SWT.ICON_ERROR;
	 						MessageBox message = Launcher.getInstance().createMessageBox(messageStyle);
	 						message.setMessage("ERROR: The file could not be saved!");
	 						message.open();
	 	            	}else{
	 	            		//String directory = MainWindow.getInstance().getDirectoryPath();
	 	            		//String fileName = MainWindow.getInstance().getFileName();
	 	            		//FileUtils.markFileAsAnnotated(directory, fileName, 1);
	 	            		
		 	            	WorkbookUtils.closeEmbeddedWorkbook(embeddedWorkbook, false);
		 	            	Launcher.getInstance().disposeControlSite();
		 	            	Launcher.getInstance().disposeShell();
	 	            	}
	 	            } 
	 	            
	 	            if(response == SWT.NO){
	 	            	Launcher.getInstance().disposeControlSite();
	 	            	Launcher.getInstance().disposeShell();
	 	            } 
	 	            
	        	}else{
	        		int style = SWT.YES | SWT.NO | SWT.ICON_QUESTION;
	        		MessageBox messageBox = Launcher.getInstance().createMessageBox(style);
	 	            messageBox.setMessage("Do you want to close the application?");
	 	            
	 	            int response = messageBox.open();
	 	            if( response== SWT.YES){
	 	        	    Launcher.getInstance().disposeControlSite();
	 	            	Launcher.getInstance().disposeShell();
	 	            }
	        	}
				
				Launcher.getInstance().setExcelPanelEnabled(true);
			}
		};
	}
	
	/**
	 * 
	 * @return
	 */
	protected static SelectionListener createSheetCompletedSelectionListener(){
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				
				Launcher.getInstance().setExcelPanelEnabled(false);
				
				String sheetName = Launcher.getInstance().getActiveWorksheetName();
				WorkbookAnnotation workbookAnnotation = AnnotationHandler.getWorkbookAnnotation();
				WorksheetAnnotation  sheetAnnotation = workbookAnnotation.getWorksheetAnnotations().get(sheetName);
				
				if(sheetAnnotation==null){
					Launcher.getInstance().setExcelPanelEnabled(true);
					return;
				}
						
				boolean wasUpdated = false;  
				
				if(!sheetAnnotation.isCompleted()){
					if( sheetAnnotation.getAllAnnotations().isEmpty()){
						int style = SWT.ICON_WARNING ;
						MessageBox mb = Launcher.getInstance().createMessageBox(style);
						mb.setMessage("You can not mark this sheet as completed. "
								+ "It does not contain any annotations yet!"); 
						mb.open();
					}else{
						if(sheetAnnotation.getAllAnnotations().size()<3){
							int style = SWT.YES | SWT.NO | SWT.ICON_WARNING ;
							MessageBox mb = Launcher.getInstance().createMessageBox(style);
							mb.setMessage("This sheet contains very few annotations. "
									+ "Do you still want to mark it as \"Completed\" ?"); 
							int option = mb.open();
							
							if(option == SWT.YES){
								sheetAnnotation.setCompleted(true);
								wasUpdated=true;
								
							}
							
						}else{
							OleAutomation unannotated = AnnotationHandler.getUnannotatedRanges(
									Launcher.getInstance().getEmbeddedWorkbook(), sheetName);
							
							if(unannotated!=null){
								
								RangeUtils.selectRange(unannotated);
								
//								// go to the cell that is the farthest down right  
//								String address = RangeUtils.getRangeAddress(unannotated);
//								
//								OleAutomation workbookAuto = Launcher.getInstance().getEmbeddedWorkbook();
//								OleAutomation sheetAuto = WorkbookUtils.getWorksheetAutomationByName(workbookAuto, sheetName);
//							
//								int index = address.lastIndexOf(",");
//								
//								if(index==-1)
//									index = address.lastIndexOf(":");
//								
//								if(index==-1)
//									index = 0;
//								
//								String lastCell = address.substring(index+1);
//								OleAutomation lastCellAuto = WorksheetUtils.getRangeAutomation(sheetAuto, lastCell);
//								
//								int col = RangeUtils.getFirstColumnIndex(lastCellAuto);
//								int row = RangeUtils.getFirstRowIndex(lastCellAuto);
//								lastCellAuto.dispose();
//								sheetAuto.dispose();
//								
//								OleAutomation window = Launcher.getInstance().getEmbeddedWindow();
//								WindowUtils.setScrollColumn(window, col);
//								WindowUtils.setScrollRow(window, row);
								
								int style = SWT.YES | SWT.NO |SWT.ICON_WARNING ;
								MessageBox mb = Launcher.getInstance().createMessageBox(style);
								mb.setMessage("The sheet contains cells that have not been annotated yet!\n" 
										+ "\na) Click \"No\" to review the highlighted areas."
										+ "\nb) Click \"Yes\" to ignore this warning message and "
										+ "mark the sheet as \"Completed\"."); 
								int option = mb.open();
								
								if(option == SWT.YES){
									sheetAnnotation.setCompleted(true);
									wasUpdated=true;									
								}
								
							}else{
								
								sheetAnnotation.setCompleted(true);
								wasUpdated=true;
							}
						}
					}	
				}else{
					sheetAnnotation.setCompleted(false);
					wasUpdated=true;
				}
								
				BarMenuUtils.adjustBarMenuForSheet(sheetName);
				
				if(wasUpdated){
					AnnotationHandler.clearRedoList();
					AnnotationHandler.clearUndoList();
									
					int style = SWT.ICON_INFORMATION;
					MessageBox mb = Launcher.getInstance().createMessageBox(style);
					String value = String.valueOf((sheetAnnotation.isCompleted())).toUpperCase();
					mb.setMessage(" Sheet status was updated to Completed := "+value); 
					mb.open();
				}
				
				Launcher.getInstance().setExcelPanelEnabled(true);
			}
		};
	}
	
	/**
	 * 
	 * @return
	 */
	protected static SelectionListener createSheetNotApplicableSelectionListener(){
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				
				Launcher.getInstance().setExcelPanelEnabled(false);
				
				String sheetName = Launcher.getInstance().getActiveWorksheetName();
				WorkbookAnnotation workbookAnnotation = AnnotationHandler.getWorkbookAnnotation();
				WorksheetAnnotation  sheetAnnotation = workbookAnnotation.getWorksheetAnnotations().get(sheetName);
				
				if(sheetAnnotation==null){
					Launcher.getInstance().setExcelPanelEnabled(true);
					return;
				}
				
				boolean wasUpdated = false;  

				if(!sheetAnnotation.isNotApplicable()){
					
					if(!sheetAnnotation.getAllAnnotations().isEmpty()){
						int style = SWT.YES | SWT.NO | SWT.ICON_WARNING ;
						MessageBox mb = Launcher.getInstance().createMessageBox(style);
						mb.setMessage("Marking this sheet as \"Not Applicable\" will erase all the existing annotations "
								+ "in the sheet. Do you want to proceed ?"); 
						int option = mb.open();
						
						if(option == SWT.YES){
							OleAutomation embeddedWorkbook = Launcher.getInstance().getEmbeddedWorkbook();
							AnnotationHandler.deleteShapeAnnotationsInSheet(embeddedWorkbook, sheetName);
							RangeAnnotationsSheet.deleteRangeAnnotationDataFromSheet(embeddedWorkbook, sheetName, true);
							
							AnnotationHandler.clearRedoList();
							AnnotationHandler.clearUndoList();
							
							workbookAnnotation.removeAllRangeAnnotationsFromSheet(sheetName);
							
							sheetAnnotation.setNotApplicable(true);
							wasUpdated = true;
						}
					}else{
						sheetAnnotation.setNotApplicable(true);
						wasUpdated = true;
					}
					
				}else{
					sheetAnnotation.setNotApplicable(false);
					wasUpdated = true;
				}

				
				BarMenuUtils.adjustBarMenuForSheet(sheetName);
				
				if(wasUpdated){
					AnnotationHandler.clearRedoList();
					AnnotationHandler.clearUndoList();
					
					int style = SWT.ICON_INFORMATION;
					MessageBox mb = Launcher.getInstance().createMessageBox(style);
					String value = String.valueOf((sheetAnnotation.isNotApplicable())).toUpperCase();
					mb.setMessage(" Sheet status was updated to NotApplicable := "+value); 
					mb.open();
				}
				
				Launcher.getInstance().setExcelPanelEnabled(true);
			}
		};
	}
	
	/**
	 * 
	 * @return
	 */
	protected static SelectionListener createFileCompletedSelectionListener(){
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				
				Launcher.getInstance().setExcelPanelEnabled(false);
				
				WorkbookAnnotation wa = AnnotationHandler.getWorkbookAnnotation();		
				boolean wasUpdated = false; 
				
				if(!wa.isCompleted()){

					if( wa.getAllAnnotations().isEmpty()){
						int style = SWT.ICON_WARNING ;
						MessageBox mb = Launcher.getInstance().createMessageBox(style);
						mb.setMessage("You can not mark the file (workbook) as \"Completed\". "
								+ "It does not contain any annotations yet!"); 
						mb.open();
						
					}else{
					
						Collection<WorksheetAnnotation> sheetAnnotations 
											= wa.getWorksheetAnnotations().values();
						Iterator<WorksheetAnnotation> itr = sheetAnnotations.iterator();
						
						boolean hasPendingSheets =  false;
						while (itr.hasNext()) {
							WorksheetAnnotation sa = itr.next();
							
							if(!sa.isCompleted() && !sa.isNotApplicable()){
								
								if(sa.getAllAnnotations().isEmpty() || sa.getAllAnnotations().size()<3){
									
									hasPendingSheets = true; 
									
									OleAutomation embeddedWorkbook = Launcher.getInstance().getEmbeddedWorkbook();
									OleAutomation worksheetAutomation = 
											WorkbookUtils.getWorksheetAutomationByName(embeddedWorkbook, sa.getSheetName());
									WorksheetUtils.makeWorksheetActive(worksheetAutomation);
									
									int style = SWT.YES | SWT.NO | SWT.ICON_WARNING ;
									MessageBox mb = Launcher.getInstance().createMessageBox(style);
									mb.setMessage("The \""+sa.getSheetName()+"\" does not contain any or "
											+ "contains very few annotations. "
											+ "\nDo you still want to mark this file (workbook) as \"Completed\" ?"); 
									int option = mb.open();
									
									if(option == SWT.YES){
										wasUpdated=true;
										wa.setCompleted(true);
									}
									
									if(option == SWT.NO){
										wasUpdated = false;
										wa.setCompleted(false);
										AnnotationHandler.clearRedoList();
										AnnotationHandler.clearUndoList();
										break;
									}		
									
								}else{
													
									OleAutomation unannotated = AnnotationHandler.getUnannotatedRanges(
											Launcher.getInstance().getEmbeddedWorkbook(), sa.getSheetName());
									
									if(unannotated!=null){
										
										hasPendingSheets = true;
										
										OleAutomation embeddedWorkbook = Launcher.getInstance().getEmbeddedWorkbook();
										OleAutomation worksheetAutomation = 
												WorkbookUtils.getWorksheetAutomationByName(embeddedWorkbook, sa.getSheetName());
										WorksheetUtils.makeWorksheetActive(worksheetAutomation);
										
										RangeUtils.selectRange(unannotated);
										
//										String address = RangeUtils.getRangeAddress(unannotated);
//										
//										int index = address.lastIndexOf(",");
//										
//										if(index==-1)
//											index = address.lastIndexOf(":");
//										
//										if(index==-1)
//											index = 0;
//										
//										String lastCell = address.substring(index+1);
//										OleAutomation sheetAuto = WorkbookUtils.getWorksheetAutomationByName(embeddedWorkbook, sa.getSheetName());	
//										OleAutomation lastCellAuto = WorksheetUtils.getRangeAutomation(sheetAuto, lastCell);
//										
//										int col = RangeUtils.getFirstColumnIndex(lastCellAuto);
//										int row = RangeUtils.getFirstRowIndex(lastCellAuto);										
//										OleAutomation window = Launcher.getInstance().getEmbeddedWindow();
//										WindowUtils.setScrollColumn(window, col);
//										WindowUtils.setScrollRow(window, row);
							
										
										int style = SWT.YES | SWT.NO |SWT.ICON_WARNING ;
										MessageBox mb = Launcher.getInstance().createMessageBox(style);
										mb.setMessage("The \""+sa.getSheetName()+"\" sheet contains cells "
												+ "that have not been annotated yet!\n" 
												+ "\na) Click \"No\" to review the highlighted areas."
												+ "\nb) Click \"Yes\" to ignore this warning message and "
												+ "mark the sheet as \"Completed\"."); 
										int option = mb.open();
										
										if(option == SWT.YES){
											wasUpdated=true;
											wa.setCompleted(true);
										}
										
										if(option == SWT.NO){
											wasUpdated = false;
											wa.setCompleted(false);
											AnnotationHandler.clearRedoList();
											AnnotationHandler.clearUndoList();
											break;
										}										
									}
								}
							}
						}
						
						if(!hasPendingSheets ){
							wa.setCompleted(true);
							wasUpdated=true;
						}
					}
				}else{
					wa.setCompleted(false);
					wasUpdated=true;
				}
				
				BarMenuUtils.adjustBarMenuForWorkbook();
				
				if(wasUpdated){			
					AnnotationHandler.clearRedoList();
					AnnotationHandler.clearUndoList();
					
					BarMenuUtils.adjustBarMenuForWorkbook();
					
					int style = SWT.ICON_INFORMATION;
					MessageBox mb = Launcher.getInstance().createMessageBox(style);
					String value = String.valueOf((wa.isCompleted())).toUpperCase();
					mb.setMessage("File (Workbook) status was updated to Completed := "+value); 
					mb.open();
				}
				
				Launcher.getInstance().setExcelPanelEnabled(true);
			}
		};
	}
	
	/**
	 * 
	 * @return
	 */
	protected static SelectionListener createFileNotApplicableSelectionListener(){
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				
				Launcher.getInstance().setExcelPanelEnabled(false);
				
				WorkbookAnnotation workbookAnnotation = AnnotationHandler.getWorkbookAnnotation();		
				boolean wasUpdated = false;
				
				if(!workbookAnnotation.isNotApplicable()){
					
					if(!workbookAnnotation.getAllAnnotations().isEmpty()){
						int style = SWT.YES | SWT.NO | SWT.ICON_WARNING ;
						MessageBox mb = Launcher.getInstance().createMessageBox(style);
						mb.setMessage("Marking this file (workbook) as \"Not Applicable\" "
								+ "will erase all the existing annotations. "
								+ "Do you want to proceed ?"); 
						int option = mb.open();
						
						if(option == SWT.YES){
							OleAutomation embeddedWorkbook = Launcher.getInstance().getEmbeddedWorkbook();
							AnnotationHandler.deleteAllShapeAnnotations(embeddedWorkbook);
							RangeAnnotationsSheet.deleteAllRangeAnnotationData(embeddedWorkbook);
							
							AnnotationHandler.clearRedoList();
							AnnotationHandler.clearUndoList();
							AnnotationHandler.getWorkbookAnnotation().removeAllAnnotations();
							
							workbookAnnotation.setNotApplicable(true);
							wasUpdated = true;
						}
					}else{
						workbookAnnotation.setNotApplicable(true);
						wasUpdated = true;
					}
					
				}else{
					workbookAnnotation.setNotApplicable(false);
					wasUpdated = true;
				}
				
				BarMenuUtils.adjustBarMenuForWorkbook();
				
				if(wasUpdated){
					
					int style = SWT.ICON_INFORMATION;
					MessageBox mb = Launcher.getInstance().createMessageBox(style);
					String value = String.valueOf((workbookAnnotation.isNotApplicable())).toUpperCase();
					mb.setMessage("File (Workbook) status was updated to NotApplicable := "+value); 
					mb.open();
				}
				
				Launcher.getInstance().setExcelPanelEnabled(true);
			}
		};
	}
	
	
	/**
	 * 
	 * @param annotationClass
	 * @return
	 */
	protected static SelectionListener createRangeAnnotationSelectionListener(AnnotationClass annotationClass){
		
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				 
				Launcher.getInstance().setExcelPanelEnabled(false);
				
				 OleAutomation workbookAutomation = Launcher.getInstance().getEmbeddedWorkbook();
				 OleAutomation application = WorksheetUtils.getApplicationAutomation(workbookAutomation);
					
				 String sheetName = Launcher.getInstance().getActiveWorksheetName();
				 int sheetIndex = Launcher.getInstance().getActiveWorksheetIndex();
				 String[] currentSelection = Launcher.getInstance().getCurrentSelection();
				 
				 // turn off screen updating to speed up the following actions
				 ApplicationUtils.setScreenUpdating(application, false);
					
				 try{
					 AnnotationHandler.annotate(workbookAutomation, sheetName, sheetIndex,   
				 		 currentSelection, annotationClass);								 
				 }catch (Exception ex){
					 logger.error("Generic exception on create new annotation", ex);
				 }
				 
				 // turn on screen updating after annotating
				 ApplicationUtils.setScreenUpdating(application, true);
				 		 
				 // if the sheet was empty, had no annotations, 
				 // the menu needs to be updated
				 BarMenuUtils.adjustBarMenuForSheet(sheetName);
				 
				 if(Launcher.getInstance().isControlSiteFocusControl())
					 	Launcher.getInstance().setFocusToShell();	
				 
				 Launcher.getInstance().setExcelPanelEnabled(true);
			}
		};
	}
	
	/**
	 * 
	 * @return
	 */
	protected static SelectionListener createUndoLastAnnotationSelectionListener(){
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				Launcher.getInstance().setExcelPanelEnabled(false);
				
				RangeAnnotation  ra = AnnotationHandler.getLastFromUndoList();	
				if(ra==null){
					Launcher.getInstance().setExcelPanelEnabled(true);
					return;
				}
				
				OleAutomation workbookAutomation = Launcher.getInstance().getEmbeddedWorkbook(); 
				OleAutomation sheetAutomation = 
						WorkbookUtils.getWorksheetAutomationByName(workbookAutomation, ra.getSheetName());
				OleAutomation application = WorksheetUtils.getApplicationAutomation(workbookAutomation);
				
				// turn off screen updating to speed up the following actions
				ApplicationUtils.setScreenUpdating(application, true);
										 
				WorksheetUtils.unprotectWorksheet(sheetAutomation);		
				boolean isSuccess = AnnotationHandler.deleteShapeAnnotation(sheetAutomation, ra);
				WorksheetUtils.protectWorksheet(sheetAutomation);
				sheetAutomation.dispose();
				
				if(isSuccess){
					AnnotationHandler.removeLastFromUndoList();
					AnnotationHandler.addToRedoList(ra);
						
					RangeAnnotationsSheet.deleteRangeAnnotationData(workbookAutomation, ra, true);
					
					AnnotationHandler.getWorkbookAnnotation().removeRangeAnnotation(ra);
						
					Launcher.getInstance().setActiveWorksheetIndex(ra.getSheetIndex());
					Launcher.getInstance().setActiveWorksheetName(ra.getSheetName());
					Launcher.getInstance().setCurrentSelection(new String[]{ra.getRangeAddress()});
									
				}else{
					MessageBox messageBox = Launcher.getInstance().createMessageBox(SWT.ICON_ERROR);
	 	            messageBox.setMessage("Could not undo the last range annotation!!!");
	 	            messageBox.open();
				}
				
				// turn on screen updating after all range annotations are re-drawn
				ApplicationUtils.setScreenUpdating(application, true);
				
				BarMenuUtils.adjustBarMenuForSheet(ra.getSheetName());
				
				Launcher.getInstance().setExcelPanelEnabled(true);
			}
		};
	}	
	
	/**
	 * 
	 * @return
	 */
	protected static SelectionListener createRedoLastAnnotationSelectionListener(){
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				Launcher.getInstance().setExcelPanelEnabled(false);
				
				RangeAnnotation  ra = AnnotationHandler.getLastFromRedoList();		
				if(ra==null){
					Launcher.getInstance().setExcelPanelEnabled(true);
					return;
				}
				
				OleAutomation workbookAutomation = Launcher.getInstance().getEmbeddedWorkbook(); 
				OleAutomation worksheetAutomation = 
						WorkbookUtils.getWorksheetAutomationByName(workbookAutomation, ra.getSheetName());
				OleAutomation application = WorksheetUtils.getApplicationAutomation(workbookAutomation);
			
				// turn off screen updating to speed up the following actions
				ApplicationUtils.setScreenUpdating(application, true);
				
				WorksheetUtils.unprotectWorksheet(worksheetAutomation);						
				Boolean result = false;
				try{
					result = AnnotationHandler.drawRangeAnnotation(workbookAutomation, ra, true);
				}catch (Exception ex){			
					logger.error("Generic exception on redo last annotation", ex);
				}
				
				WorksheetUtils.protectWorksheet(worksheetAutomation);
				worksheetAutomation.dispose();
				
				AnnotationHandler.removeLastFromRedoList();
				if(!result){
					AnnotationHandler.getWorkbookAnnotation().removeRangeAnnotation(ra);
					ApplicationUtils.setScreenUpdating(application, true);
					
					BarMenuUtils.adjustBarMenuForSheet(ra.getSheetName());
					Launcher.getInstance().setExcelPanelEnabled(true);
					
					return;
				}
				
				RangeAnnotationsSheet.saveRangeAnnotationData(workbookAutomation, ra);
				
				// turn on screen updating after all range annotations are re-drawn
				ApplicationUtils.setScreenUpdating(application, true);
				
				AnnotationHandler.addToUndoList(ra);			
				AnnotationHandler.getWorkbookAnnotation().addRangeAnnotation(ra);
							
				BarMenuUtils.adjustBarMenuForSheet(ra.getSheetName());			
				
				Launcher.getInstance().setExcelPanelEnabled(true);
			}
		};
	}	
	
	/**
	 * 
	 * @return
	 */
	protected static SelectionListener createHideAllAnnotationsSelectionListener(){
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Launcher.getInstance().setExcelPanelEnabled(false);
				
				OleAutomation workbookAutomation = Launcher.getInstance().getEmbeddedWorkbook();	
				AnnotationHandler.setVisilityForAllAnnotations(workbookAutomation, false);
				
				Launcher.getInstance().setExcelPanelEnabled(true);
			}
		};
	}
	
	/**
	 * 
	 * @return
	 */
	protected static SelectionListener createHideInSheetAnnotationsSelectionListener(){
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Launcher.getInstance().setExcelPanelEnabled(false);
				
				OleAutomation workbookAutomation = Launcher.getInstance().getEmbeddedWorkbook();
				String sheetName = Launcher.getInstance().getActiveWorksheetName();
				AnnotationHandler.setVisibilityForAnnotationsInSheet(workbookAutomation, sheetName, false);
				
				Launcher.getInstance().setExcelPanelEnabled(true);
			}
		};
	}

	/**
	 * 
	 * @return
	 */
	protected static SelectionListener createShowAllAnnotationsSelectionListener(){
		return new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				Launcher.getInstance().setExcelPanelEnabled(false);
				
				OleAutomation workbookAutomation = Launcher.getInstance().getEmbeddedWorkbook();
				AnnotationHandler.setVisilityForAllAnnotations(workbookAutomation, true);
				
				Launcher.getInstance().setExcelPanelEnabled(true);
			}			
		};
	}
	
	/**
	 * 
	 * @return
	 */
	protected static SelectionListener createShowInSheetAnnotationsSelectionListener(){
		return new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				Launcher.getInstance().setExcelPanelEnabled(false);		
				
				OleAutomation workbookAutomation = Launcher.getInstance().getEmbeddedWorkbook();
				String sheetName = Launcher.getInstance().getActiveWorksheetName();
				AnnotationHandler.setVisibilityForAnnotationsInSheet(workbookAutomation, sheetName, true);				
				
				Launcher.getInstance().setExcelPanelEnabled(true);
			}
			
		};
	}
	
	/**
	 * 
	 * @return
	 */
	protected static SelectionListener createDeleteAllAnnotationsSelectionListener(){
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				Launcher.getInstance().setExcelPanelEnabled(false);
				   
				int style = SWT.YES | SWT.NO | SWT.ICON_QUESTION;
        		MessageBox messageBox = Launcher.getInstance().createMessageBox(style);
 	            messageBox.setMessage("Are you sure you want to delete all annotations?");
 	            
 	            int response = messageBox.open();
 	            if( response== SWT.YES){
 	            	
// 	        	    Launcher.getInstance().disposeControlSite();
// 	            	Launcher.getInstance().disposeShell();
 	            	
					OleAutomation workbookAutomation = Launcher.getInstance().getEmbeddedWorkbook();	
					String sheetName = Launcher.getInstance().getActiveWorksheetName();
					
					AnnotationHandler.deleteAllShapeAnnotations(workbookAutomation);
					
					WorkbookAnnotation workbookAnnotation = AnnotationHandler.getWorkbookAnnotation();
					workbookAnnotation.removeAllAnnotations();
					AnnotationHandler.createBaseAnnotations(workbookAutomation);
					
					RangeAnnotationsSheet.deleteAllRangeAnnotationData(workbookAutomation);
					
					AnnotationHandler.clearRedoList();
					AnnotationHandler.clearUndoList();
					
					BarMenuUtils.adjustBarMenuForSheet(sheetName);
 	            }
 	    	    
				Launcher.getInstance().setExcelPanelEnabled(true);
				
			}
		};
	}
	
	/**
	 * 
	 * @return
	 */
	protected static SelectionListener createDeleteAnnotationsInSheetSelectionListener(){
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				Launcher.getInstance().setExcelPanelEnabled(false);
				   
				int style = SWT.YES | SWT.NO | SWT.ICON_QUESTION;
        		MessageBox messageBox = Launcher.getInstance().createMessageBox(style);
 	            messageBox.setMessage("Are you sure you want to delete the annotations in this sheet?");
 	            
 	            int response = messageBox.open();
 	            if( response== SWT.YES){
 	            	
					OleAutomation workbookAutomation = Launcher.getInstance().getEmbeddedWorkbook();
					String sheetName = Launcher.getInstance().getActiveWorksheetName();
					
					AnnotationHandler.deleteShapeAnnotationsInSheet(workbookAutomation, sheetName);
					
					WorkbookAnnotation workbookAnnotation = AnnotationHandler.getWorkbookAnnotation();
					workbookAnnotation.removeAllRangeAnnotationsFromSheet(sheetName);
					
					RangeAnnotationsSheet.deleteRangeAnnotationDataFromSheet(workbookAutomation, 
							sheetName, true);		
					
					AnnotationHandler.clearRedoList();
					AnnotationHandler.clearUndoList();
					
					BarMenuUtils.adjustBarMenuForSheet(sheetName);
					
 	            }
				Launcher.getInstance().setExcelPanelEnabled(true);
			}
		};
	}
	
	
	/**
	 * 
	 * @return
	 */
	protected static SelectionListener createDeleteAnnotationsInRangeSelectionListener(){
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				Launcher.getInstance().setExcelPanelEnabled(false);
				   
				int style = SWT.YES | SWT.NO | SWT.ICON_QUESTION;
        		MessageBox messageBox = Launcher.getInstance().createMessageBox(style);
 	            messageBox.setMessage("Are you sure you want to delete the annotations inside the selected range?");
 	            
 	            int response = messageBox.open();
 	            if( response== SWT.YES){
				
					OleAutomation workbookAutomation = Launcher.getInstance().getEmbeddedWorkbook();
					String sheetName = Launcher.getInstance().getActiveWorksheetName();
					String[] selection = Launcher.getInstance().getCurrentSelection();
					
					WorkbookAnnotation wa = AnnotationHandler.getWorkbookAnnotation();
					WorksheetAnnotation sa = wa.getWorksheetAnnotations().get(sheetName);
					ArrayList<RangeAnnotation> annotations = new ArrayList<RangeAnnotation>(sa.getAllAnnotations());
					
					HashSet<RangeAnnotation> contained = new HashSet<RangeAnnotation>();
					for (RangeAnnotation ra : annotations) {
						for (String area : selection) {
							boolean isContained = RangeUtils.checkForContainment(area, ra.getRangeAddress());
							if(isContained){
								contained.add(ra);
							}
						}					
					}
					
					if(!contained.isEmpty()){
					
						OleAutomation sheetAuto = WorkbookUtils.getWorksheetAutomationByName(workbookAutomation, sheetName);
						WorksheetUtils.unprotectWorksheet(sheetAuto);
						
						for (RangeAnnotation cra : contained) {
							
							AnnotationHandler.deleteShapeAnnotation(sheetAuto, cra);		
							wa.removeRangeAnnotation(cra);
							RangeAnnotationsSheet.deleteRangeAnnotationData(workbookAutomation, cra, true);
						}
						
						WorksheetUtils.protectWorksheet(sheetAuto);
						sheetAuto.dispose();
						
						AnnotationHandler.clearRedoList();
						AnnotationHandler.clearUndoList();
						
						BarMenuUtils.adjustBarMenuForSheet(sheetName);				
					}			
				}
 	            
 	            Launcher.getInstance().setExcelPanelEnabled(true);
			}
		};
	}
	
	
	protected static SelectionListener createShowFormulasSelectionListener(){
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				Launcher.getInstance().setExcelPanelEnabled(false);
				
				OleAutomation window = Launcher.getInstance().getEmbeddedWindow();
				boolean areFormulasDisplayed = WindowUtils.getDisplayFormulas(window);		
				WindowUtils.setDisplayFormulas(window, !areFormulasDisplayed);
				
				Launcher.getInstance().setExcelPanelEnabled(true);
			}
		};
	}
	
	protected static SelectionListener createZoomInSelectionListener(){
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				OleAutomation window = Launcher.getInstance().getEmbeddedWindow();
				int zoom = WindowUtils.getZoom(window);
				
				Launcher lr =Launcher.getInstance();
				lr.getTooltip().setVisible(false);	
				lr.recreateToolTipWithStyle(SWT.ICON_INFORMATION);
				lr.getTooltip().setText("Information");
				lr.placeToolTipOnExcelPanel();
				if(zoom>=400){
					lr.getTooltip().setMessage("Maximum Zoom level: "+400+"%");
					lr.getTooltip().setVisible(true);
					return;
				}			
				
				int newZoom = (zoom+10);
				if(newZoom%10 != 0){
					newZoom=(int) (Math.floor(newZoom/10)*10);
				}
				
				WindowUtils.setZoom(window, newZoom);
				
				
				lr.getTooltip().setMessage("Zoom level: "+newZoom+"%");
				lr.getTooltip().setVisible(true);	
			}
		};
	}
	
	protected static SelectionListener createZoomOutSelectionListener(){
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				OleAutomation window = Launcher.getInstance().getEmbeddedWindow();
				int zoom = WindowUtils.getZoom(window);
				
				Launcher lr =Launcher.getInstance();
				lr.getTooltip().setVisible(false);	
				lr.recreateToolTipWithStyle(SWT.ICON_INFORMATION);
				lr.getTooltip().setText("Information");
				lr.placeToolTipOnExcelPanel();
				
				if(zoom<=10){
					lr.getTooltip().setMessage("Minimum Zoom level: "+zoom+"%");
					lr.getTooltip().setVisible(true);
					return;
				}
				
				int newZoom = (zoom-10);
				if(newZoom%10 != 0){
					newZoom=(int) (Math.ceil(newZoom/10)*10);
				}
				
				WindowUtils.setZoom(window, newZoom);
				
				lr.getTooltip().setMessage("Zoom level: "+newZoom+"%");
				lr.getTooltip().setVisible(true);
					
			}
		};
	}
}
