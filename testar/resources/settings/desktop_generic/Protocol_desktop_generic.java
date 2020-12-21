/***************************************************************************************************
 *
 * Copyright (c) 2013, 2014, 2015, 2016, 2017, 2018, 2019 Universitat Politecnica de Valencia - www.upv.es
 * Copyright (c) 2018, 2019 Open Universiteit - www.ou.nl
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************************************/


import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fruit.Util;
import org.fruit.alayer.*;
import org.fruit.alayer.exceptions.*;
import org.fruit.alayer.windows.UIARoles;
import org.fruit.monkey.Settings;
import org.testar.action.priorization.ActionTags;
import org.testar.action.priorization.ActionTags.ActionGroupType;
import org.testar.protocols.DesktopProtocol;

// ENFOQUE 4
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import javax.imageio.ImageIO;
import org.testar.OutputStructure;
import nl.ou.testar.a11y.reporting.HTMLReporter;


/**
 * This protocol provides default TESTAR behaviour to test Windows desktop applications.
 *
 * It uses random action selection algorithm.
 */
public class Protocol_desktop_generic extends DesktopProtocol {
	
	String lastWidgetID = "";
	
	private HTMLDifferenceGeneric htmlDifference;			// ENFOQUE 4
	String differenceScreenshot;
	
	// ENFOQUE 3
	int numWidgetsBefore = 0;
	int numWidgetsNow = 0;

	List<String> idCustomsGlobalList = new ArrayList<String>();
	List<String> widgetNamesGlobalList = new ArrayList<String>();
	List<String> actionGroupsGlobalList = new ArrayList<String>();
	List<Double> zIndexesGlobalList = new ArrayList<Double>();
	List<Double> qLearningsGlobalList = new ArrayList<Double>();

	// ENFOQUE 3
	List<String> lastStateWIDList = new ArrayList<String>();
	boolean enfoque3o4 = false;
	
	
	/**
	 * Called once during the life time of TESTAR
	 * This method can be used to perform initial setup work
	 * @param   settings  the current TESTAR settings as specified by the user.
	 */
	@Override
	protected void initialize(Settings settings){
		super.initialize(settings);
		htmlDifference = new HTMLDifferenceGeneric();		// ENFOQUE 4
		System.out.println("*** NEW EXECUTION ***");
	}

	/**
	 * This methods is called before each test sequence, before startSystem(),
	 * allowing for example using external profiling software on the SUT
	 *
	 * HTML sequence report will be initialized in the super.preSequencePreparations() for each sequence
	 */
	@Override
	protected void preSequencePreparations() {
		super.preSequencePreparations();
	}

	/**
	 * This method is called when TESTAR starts the System Under Test (SUT). The method should
	 * take care of
	 *   1) starting the SUT (you can use TESTAR's settings obtainable from <code>settings()</code> to find
	 *      out what executable to run)
	 *   2) waiting until the system is fully loaded and ready to be tested (with large systems, you might have to wait several
	 *      seconds until they have finished loading)
	 * @return  a started SUT, ready to be tested.
	 */
	@Override
	protected SUT startSystem() throws SystemStartException{
		return super.startSystem();
	}

	/**
	 * This method is invoked each time the TESTAR starts the SUT to generate a new sequence.
	 * This can be used for example for bypassing a login screen by filling the username and password
	 * or bringing the system into a specific start state which is identical on each start (e.g. one has to delete or restore
	 * the SUT's configuration files etc.)
	 */
	 @Override
	protected void beginSequence(SUT system, State state){
	 	super.beginSequence(system, state);
	}

	/**
	 * This method is called when the TESTAR requests the state of the SUT.
	 * Here you can add additional information to the SUT's state or write your
	 * own state fetching routine. The state should have attached an oracle
	 * (TagName: <code>Tags.OracleVerdict</code>) which describes whether the
	 * state is erroneous and if so why.
	 *
	 * super.getState(system) puts the state information also to the HTML sequence report
	 *
	 * @return  the current state of the SUT with attached oracle.
	 */
	@Override
	protected State getState(SUT system) throws StateBuildException{
		// ENFOQUE 4
		// Save previous state object
		State previousState = latestState;
				
		// Update state information
		State state = super.getState(system);
				
		if(previousState != null && previousState.get(Tags.ScreenshotPath, null) != null && state.get(Tags.ScreenshotPath, null) != null) {
					
			// Create and obtain the image-diff path
			differenceScreenshot = getDifferenceImage(
				previousState.get(Tags.ScreenshotPath), previousState.get(Tags.ConcreteIDCustom, ""),
				state.get(Tags.ScreenshotPath), state.get(Tags.ConcreteIDCustom, "")
				);
						
			// Update: output\timestamp_app_version\HTMLreports\StateDifferenceReport.html
			//htmlDifference.addStateDifferenceStep(actionCount, previousState.get(Tags.ScreenshotPath), state.get(Tags.ScreenshotPath), differenceScreenshot);
		}
				
		return state;
	}

	/**
	 * The getVerdict methods implements the online state oracles that
	 * examine the SUT's current state and returns an oracle verdict.
	 * @return oracle verdict, which determines whether the state is erroneous and why.
	 */
	@Override
	protected Verdict getVerdict(State state){
		// The super methods implements the implicit online state oracles for:
		// system crashes
		// non-responsiveness
		// suspicious titles
		Verdict verdict = super.getVerdict(state);

		//--------------------------------------------------------
		// MORE SOPHISTICATED STATE ORACLES CAN BE PROGRAMMED HERE
		//--------------------------------------------------------

		return verdict;
	}

	/**
	 * This method is used by TESTAR to determine the set of currently available actions.
	 * You can use the SUT's current state, analyze the widgets and their properties to create
	 * a set of sensible actions, such as: "Click every Button which is enabled" etc.
	 * The return value is supposed to be non-null. If the returned set is empty, TESTAR
	 * will stop generation of the current action and continue with the next one.
	 * @param system the SUT
	 * @param state the SUT's current state
	 * @return  a set of actions
	 */
	@Override
	protected Set<Action> deriveActions(SUT system, State state) throws ActionBuildException{

		//System.out.println(" ··· START ···");
		//System.out.println(" ··· actionNamesGlobalList: " + actionNamesGlobalList);
		//System.out.println(" ··· actionGroupsGlobalList: " + actionGroupsGlobalList);
		//System.out.println(" ··· qLearningsGlobalList: " + qLearningsGlobalList);
		
		//The super method returns a ONLY actions for killing unwanted processes if needed, or bringing the SUT to
		//the foreground. You should add all other actions here yourself.
		// These "special" actions are prioritized over the normal GUI actions in selectAction() / preSelectAction().
		Set<Action> actions = super.deriveActions(system,state);

		// Derive left-click actions, click and type actions, and scroll actions from
		// top level (highest Z-index) widgets of the GUI:
		actions = deriveClickTypeScrollActionsFromTopLevelWidgets(actions, system, state);

		if(actions.isEmpty()){
			// If the top level widgets did not have any executable widgets, try all widgets:
			// Derive left-click actions, click and type actions, and scroll actions from
			// all widgets of the GUI:
			actions = deriveClickTypeScrollActionsFromAllWidgetsOfState(actions, system, state);
		}
		
		
		// ENFOQUE 2: Decrecimiento iterativo de los QValues
		
		for(Widget w : state) {
			System.out.println("NEW WIDGET DETECTED:");
			
			updateListsBefore(w);
			
			// Asignacion de ActionGroup
			ActionTags.ActionGroupType actionGroup = w.get(ActionTags.ActionGroup, null);
			if(actionGroup == null) {
				Role actionRole = w.get(Tags.Role);
				String actionRoleStr = actionRole.toString();
				setActionGroup(actionRoleStr, w);
			}

			// Asignacion de ZIndex
			if(w.get(ActionTags.ZIndex, 0) == 0) {
				double wZIndex = w.get(Tags.ZIndex, 0.0);
				if(wZIndex != 0.0) {
					int ZIndexInt = (int) wZIndex;
					w.set(ActionTags.ZIndex, ZIndexInt);
				}
			}
			
			// Asignacion de QLearning
			double actionQLearning = w.get(ActionTags.QLearning, 0.0);
			if(actionGroup == null && actionQLearning == 0.0) {
				w.set(ActionTags.QLearning, 1.0);
			}
			
			// 1) Si el widget no tiene ActionGroup y tampoco un valor en su tag QLearning, asignar el valor mas alto (1) a su Tag QLearning.
			// 2) Si el widget tiene un ActionGroup pero no un valor en su tag QLearning, comprobar si existen widgets en las listas con su mismo Action Group y ZIndex.
			// 3)		Si existen, comprobar si alguno de los widgets de las listas tienen un valor en su tag QLearning.
			// 4)			Si alg�n widget de las listas tiene un valor, asignar al widget inicial el valor de QLearning del widget de las listas.
			// 5)			Si ning�n widget de las listas tiene un valor, asignar al widget inicial el valor mas alto (1) a su Tag QLearning.
			// 6)		Si no existen, asignar al widget inicial el valor mas alto (1) a su Tag QLearning.
			if(actionGroup != null && actionQLearning == 0.0) {										// 2)
				double auxQValue = 0.0;
				for (int i = 0; i < actionGroupsGlobalList.size(); i ++) {
					String aActionGroup = w.get(ActionTags.ActionGroup, null).toString();
					String a2ActionGroup = actionGroupsGlobalList.get(i);
					if(a2ActionGroup != null && aActionGroup != null) {
						double aZIndex = w.get(ActionTags.ZIndex, null);
						double a2ZIndex = zIndexesGlobalList.get(i);
						if(a2ActionGroup.equals(aActionGroup) && aZIndex == a2ZIndex) {
							double a2QLearning = qLearningsGlobalList.get(i);
							if ((a2QLearning > 0.0) && (a2QLearning < auxQValue)) {									// 3)
								auxQValue = a2QLearning;													// 4)
							}
						}
					}
				}
							
				if(auxQValue != 0.0) {
					w.set(ActionTags.QLearning, auxQValue);
				} else {																			// 5) 6)
					w.set(ActionTags.QLearning, 1.0);
				}
			}
			System.out.println("Name: " + w.get(Tags.Desc, "NULL") + ".\t\t QLearning = " + w.get(ActionTags.QLearning, 0.0) + ".\t\tID: " + w.get(Tags.ConcreteIDCustom));
		}
		
		
		
		
		// ENFOQUE 3: Dar una mayor recompensa a los widgets cuyas acciones produzcan un mayor cambio en el programa
		// Numero de widgets en el estado previo y en el actual
		/*
		enfoque3o4 = true;
		
		numWidgetsNow = state.childCount();
				
		System.out.println("*** numWidgetsBefore: " + numWidgetsBefore);
		System.out.println("*** numWidgetsNow: " + numWidgetsNow);
		
		for (Widget w : state) {			
			updateListsBefore(w);
			
			// Inicializacion del valor de recompensa a 1.0
			if(w.get(ActionTags.QLearning, 0.0) == 0.0) w.set(ActionTags.QLearning, 1.0);
			
			System.out.println("Name: " + w.get(Tags.Desc, "NULL") + ".\t\t QLearning = " + w.get(ActionTags.QLearning, 0.0) + ".\t\tID: " + w.get(Tags.ConcreteIDCustom));
		}
		

		// Si no existe widget tree anterior, no hacer nada
		if(numWidgetsBefore > 0) {
			double persistentDecrement = getPersistentDecrement(state);
			int index = idCustomsGlobalList.indexOf(lastWidgetID);
			if(index != -1) {
				double numWidgetsBeforeDouble = numWidgetsBefore;
				double numWidgetsNowDouble = numWidgetsNow;
				double newQLearningValue = qLearningsGlobalList.get(index);
				
				if(numWidgetsBefore < numWidgetsNow) {
					newQLearningValue = newQLearningValue - persistentDecrement +
							((numWidgetsNowDouble - numWidgetsBeforeDouble) / numWidgetsBeforeDouble);
				} else if(numWidgetsBefore > numWidgetsNow) {
					newQLearningValue = greaterThanZero(newQLearningValue - persistentDecrement -
							(numWidgetsNowDouble / numWidgetsBeforeDouble));
				} else {
					newQLearningValue = greaterThanZero(newQLearningValue - persistentDecrement);
				}
				
				qLearningsGlobalList.set(index, newQLearningValue);
			}
		}
		*/
		
		
		// ENFOQUE 4
		// [state 1] --(a)--> [state 2]
		// Comparar la imagen de los estados [state 1] y [state 2], y ver cuántos píxeles han cambiado.
		// La recompensa de (a) será mayor cuantos más píxeles cambien entre [state 1] y [state 2].
		// Usar la información de "htmlDifference"
		/*
		enfoque3o4 = true;
		
		for (Widget w : state) {
			updateListsBefore(w);
			if(w.get(ActionTags.QLearning, 0.0) == 0.0) w.set(ActionTags.QLearning, 1.0);
		}
		
		if(lastWidgetID != "") {
			int index = idCustomsGlobalList.indexOf(lastWidgetID);
			double qLearningValue = qLearningsGlobalList.get(index);
			try {
				BufferedImage diffScreanshot = ImageIO.read(new File(differenceScreenshot));		
					
				double totalPixels = diffScreanshot.getWidth() * diffScreanshot.getHeight();
				double differentPixels = 0;
				int[] pixelsArray = diffScreanshot.getRGB(0, 0, diffScreanshot.getWidth(), diffScreanshot.getHeight(), null, 0, diffScreanshot.getWidth());
				for (int i = 0; i < totalPixels; i++) {
				    if (pixelsArray[i] != Color.Black.argb32()) {
				    	differentPixels ++;
				    }
				}
				double diffPxPercentage = differentPixels / totalPixels;
					
				System.out.println("*********");
				System.out.println("Totales actuales: " + totalPixels);
				System.out.println("Diferentes: " + differentPixels);
				System.out.println("Proporcion (0..1): " + diffPxPercentage);
				System.out.println("*********");
					
				qLearningValue += diffPxPercentage;
				qLearningsGlobalList.set(index, qLearningValue);
					
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		*/

	

		//return the set of derived actions
		return actions;
	}

	/**
	 * Select one of the available actions using an action selection algorithm (for example random action selection)
	 *
	 * super.selectAction(state, actions) updates information to the HTML sequence report
	 *
	 * @param state the SUT's current state
	 * @param actions the set of derived actions
	 * @return  the selected action (non-null!)
	 */
	@Override
	protected Action selectAction(State state, Set<Action> actions){

		// ENFOQUE 2: Decrecimiento iterativo de los QValues
		
		//Seleccionar el widget con el mayor valor en su tag QLearning.
		double maxQLearning = 0.0;
		double wQLearning = 0.0;
		int index = 0;
		int wIndex = 0;
		
		for(Widget w : state) {
			if((w.get(Tags.Role).toString() == "UIAButton") || (w.get(Tags.Role).toString() == "UIAMenuItem")) {
				wQLearning = w.get(ActionTags.QLearning, 0.0);
				if(wQLearning > maxQLearning) {
					maxQLearning = wQLearning;
					wIndex = index;
				}
				
			}
			index ++;
		}
		
		// Actualizar los valores de QLearning de los widgets. El valor en widgets de un ActionGroup sera menor
		// a medida que se ejecuten acciones de dicho ActionGroup.
		
		index = 0;
		for(Widget w : state) {
			if(index == wIndex) {
				double newQL = greaterThanZero(w.get(ActionTags.QLearning, 0.0) - 0.05);
				w.set(ActionTags.QLearning, newQL);
		
				System.out.println("Widget to be selected: " + w.get(Tags.Desc) + "\t\t New QLearning = " + w.get(ActionTags.QLearning, 0.0));
						
				updateListsAfter(w);
						
				System.out.println(" ... END ...");
				System.out.println(" ... widgetNamesGlobalList: " + widgetNamesGlobalList);
				System.out.println(" ... actionGroupsGlobalList: " + actionGroupsGlobalList);
				System.out.println(" ... qLearningsGlobalList: " + qLearningsGlobalList);
				System.out.println(" ... zIndexesGlobalList: " + zIndexesGlobalList);
						
				Action maxAction = getAction(w, actions);
				return maxAction;
			}
			index ++;
		}
		
		
		
		// ENFOQUE 3 y 4: Dar una mayor recompensa a los widgets que, de ser seleccionados, produzcan un mayor cambio en el programa
		/*
		//Seleccionar el widgetcon el mayor valor en su tag QLearning.
		Widget maxWidget = null;
		double maxQLearning = 0.0;
		for(Widget w : state) {
			if((w.get(Tags.Role).toString() == "UIAButton") || (w.get(Tags.Role).toString() == "UIAMenuItem")) {
				double widgetQLearning = w.get(ActionTags.QLearning, 0.0);
				if(widgetQLearning > maxQLearning) {
					maxWidget = w;
					maxQLearning = widgetQLearning;
				}
				
				// ENFOQUE 3
				//lastStateWIDList.add(w.get(Tags.ConcreteIDCustom));
			}
		}

		// ENFOQUE 3
		//numWidgetsBefore = numWidgetsNow;
		
		if (maxWidget != null) {
			updateListsAfter(maxWidget);
			
			lastWidgetID = maxWidget.get(Tags.ConcreteIDCustom);
			
			System.out.println("... widgetToBeSelectedID: " + lastWidgetID);
			System.out.println("... widgetToBeSelectedQL: " + maxQLearning);
			System.out.println(" ...... widgetNamesGlobalList: " + widgetNamesGlobalList);
			System.out.println(" ...... actionGroupsGlobalList: " + actionGroupsGlobalList);
			System.out.println(" ...... qLearningsGlobalList: " + qLearningsGlobalList);
			System.out.println(" ...... zIndexesGlobalList: " + zIndexesGlobalList);
			
			
			Action maxAction = getAction(maxWidget, actions);
			return maxAction;
		}
		*/
		
		
		return(super.selectAction(state, actions));
		
	}
	

	/**
	 * Execute the selected action.
	 *
	 * super.executeAction(system, state, action) is updating the HTML sequence report with selected action
	 *
	 * @param system the SUT
	 * @param state the SUT's current state
	 * @param action the action to execute
	 * @return whether or not the execution succeeded
	 */
	@Override
	protected boolean executeAction(SUT system, State state, Action action){
		return super.executeAction(system, state, action);
	}

	/**
	 * TESTAR uses this method to determine when to stop the generation of actions for the
	 * current sequence. You can stop deriving more actions after:
	 * - a specified amount of executed actions, which is specified through the SequenceLength setting, or
	 * - after a specific time, that is set in the MaxTime setting
	 * @return  if <code>true</code> continue generation, else stop
	 */
	@Override
	protected boolean moreActions(State state) {
		return super.moreActions(state);
	}


	/**
	 * TESTAR uses this method to determine when to stop the entire test sequence
	 * You could stop the test after:
	 * - a specified amount of sequences, which is specified through the Sequences setting, or
	 * - after a specific time, that is set in the MaxTime setting
	 * @return  if <code>true</code> continue test, else stop
	 */
	@Override
	protected boolean moreSequences() {
		return super.moreSequences();
	}

	/**
	 * Here you can put graceful shutdown sequence for your SUT
	 * @param system
	 */
	@Override
	protected void stopSystem(SUT system) {
		super.stopSystem(system);
	}

	/**
	 * This methods is called after each test sequence, allowing for example using external profiling software on the SUT
	 *
	 * super.postSequenceProcessing() is adding test verdict into the HTML sequence report
	 */
	@Override
	protected void postSequenceProcessing() {
		super.postSequenceProcessing();
	}
	
	private double greaterThanZero (double d) {
		if(d >= 0.0) return d;
		else return 0.1;
	}
	
	private void setActionGroup(String actionRoleStr, Widget w) {
		ActionTags.ActionGroupType actionGroup = null;
		switch(actionRoleStr) {
			case "UIAWidget":
				actionGroup = ActionGroupType.UIAWidget;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIAAppBar":
				actionGroup = ActionGroupType.UIAAppBar;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIAButton":
				actionGroup = ActionGroupType.UIAButton;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIACalendar":
				actionGroup = ActionGroupType.UIACalendar;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIACheckBox":
				actionGroup = ActionGroupType.UIACheckBox;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIAComboBox":
				actionGroup = ActionGroupType.UIAComboBox;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIACustomControl":
				actionGroup = ActionGroupType.UIACustomControl;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIADataGrid":
				actionGroup = ActionGroupType.UIADataGrid;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIADataItem":
				actionGroup = ActionGroupType.UIADataItem;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIADocument":
				actionGroup = ActionGroupType.UIADocument;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIAEdit":
				actionGroup = ActionGroupType.UIAEdit;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIAGroup":
				actionGroup = ActionGroupType.UIAGroup;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIAHeader":
				actionGroup = ActionGroupType.UIAHeader;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIAHeaderItem":
				actionGroup = ActionGroupType.UIAHeaderItem;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIAHyperlink":
				actionGroup = ActionGroupType.UIAHyperlink;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIAImage":
				actionGroup = ActionGroupType.UIAImage;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIAList":
				actionGroup = ActionGroupType.UIAList;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIAListItem":
				actionGroup = ActionGroupType.UIAListItem;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIAMenuBar":
				actionGroup = ActionGroupType.UIAMenuBar;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIAMenu":
				actionGroup = ActionGroupType.UIAMenu;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIAMenuItem":
				actionGroup = ActionGroupType.UIAMenuItem;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIAPane":
				actionGroup = ActionGroupType.UIAPane;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIAProgressBar":
				actionGroup = ActionGroupType.UIAProgressBar;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIARadioButton":
				actionGroup = ActionGroupType.UIARadioButton;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIAScrollBar":
				actionGroup = ActionGroupType.UIAScrollBar;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIASemanticZoom":
				actionGroup = ActionGroupType.UIASemanticZoom;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIASeparator":
				actionGroup = ActionGroupType.UIASeparator;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIASlider":
				actionGroup = ActionGroupType.UIASlider;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIASpinner":
				actionGroup = ActionGroupType.UIASpinner;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIASplitButton":
				actionGroup = ActionGroupType.UIASplitButton;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIAStatusBar":
				actionGroup = ActionGroupType.UIAStatusBar;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIATabControl":
				actionGroup = ActionGroupType.UIATabControl;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIATabItem":
				actionGroup = ActionGroupType.UIATabItem;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIATable":
				actionGroup = ActionGroupType.UIATable;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIAText":
				actionGroup = ActionGroupType.UIAText;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIAThumb":
				actionGroup = ActionGroupType.UIAThumb;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIATitleBar":
				actionGroup = ActionGroupType.UIATitleBar;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIAToolBar":
				actionGroup = ActionGroupType.UIAToolBar;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIAToolTip":
				actionGroup = ActionGroupType.UIAToolTip;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIATree":
				actionGroup = ActionGroupType.UIATree;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIATreeItem":
				actionGroup = ActionGroupType.UIATreeItem;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
			case "UIAWindow":
				actionGroup = ActionGroupType.UIAWindow;
				w.set(ActionTags.ActionGroup, actionGroup);
				break;
		}
	}
	
	private void updateListsBefore(Widget w) {
		String idCustom = w.get(Tags.ConcreteIDCustom);
		if(idCustomsGlobalList.contains(idCustom)) {
			int index = idCustomsGlobalList.indexOf(idCustom);
			w.set(ActionTags.QLearning, qLearningsGlobalList.get(index));
			int zIndexInt = (int) Math.round(zIndexesGlobalList.get(index));
			w.set(ActionTags.ZIndex, zIndexInt);
			setActionGroup(actionGroupsGlobalList.get(index), w);
			
		}
	}
	
	private void updateListsAfter(Widget w) {
		String idCustom = w.get(Tags.ConcreteIDCustom);
		String maxActionGroup = w.get(ActionTags.ActionGroup, ActionGroupType.UIAWidget).toString();
		double maxActionQL = w.get(ActionTags.QLearning);
		String maxActionDesc = w.get(Tags.Desc, "NULL");
		double maxActionZIndex = w.get(Tags.ZIndex);
		lastWidgetID = idCustom;
				
		if(idCustomsGlobalList.isEmpty()) {
			idCustomsGlobalList.add(idCustom);
			widgetNamesGlobalList.add(maxActionDesc);
			actionGroupsGlobalList.add(maxActionGroup);
			qLearningsGlobalList.add(maxActionQL);
			zIndexesGlobalList.add(maxActionZIndex);
		} else {
			if(idCustomsGlobalList.contains(idCustom)) {
				int index = idCustomsGlobalList.indexOf(idCustom);
				actionGroupsGlobalList.set(index, maxActionGroup);
				if(enfoque3o4) maxActionQL -= 0.01 * maxActionZIndex;
				qLearningsGlobalList.set(index, maxActionQL);
				zIndexesGlobalList.set(index, maxActionZIndex);
			} else {
				idCustomsGlobalList.add(idCustom);
				widgetNamesGlobalList.add(maxActionDesc);
				actionGroupsGlobalList.add(maxActionGroup);
				qLearningsGlobalList.add(maxActionQL);
				zIndexesGlobalList.add(maxActionZIndex);
			}
		}
	}
	
	private Action getAction(Widget w, Set<Action> actions) {
		Action theActions[] = new Action[actions.size()];
		actions.toArray(theActions);
		//Action theAction = null;
		//if(actions.size() > 0) {
			Action theAction = theActions[0];
			for(Action a : actions) {
				if(w.get(Tags.ConcreteIDCustom) == a.get(Tags.OriginWidget).get(Tags.ConcreteIDCustom)) {
					theAction = a;
				}
			}
			
		//}	
		return theAction;
	}
	
	private double getPersistentDecrement(State state) {
		int persistentWidgetNum = 0;
		
		for(Widget w : state) {
			String wID = w.get(Tags.ConcreteIDCustom);
			if(lastStateWIDList.contains(wID)) {
				persistentWidgetNum ++;
			}
		}
		lastStateWIDList.clear();

		double persistentDecrement = persistentWidgetNum * 0.01;
		return persistentDecrement;
	}

	
	// ENFOQUE 4
	/**
	 * State image difference 
	 * 
	 * https://stackoverflow.com/questions/25022578/highlight-differences-between-images
	 * 
	 * @param previousStateDisk
	 * @param namePreviousState
	 * @param stateDisk
	 * @param nameState
	 * @return
	 */
	private String getDifferenceImage(String previousStateDisk, String namePreviousState, String stateDisk, String nameState) {
		try {

			// State Images paths
			String previousStatePath = new File(previousStateDisk).getCanonicalFile().toString();
			String statePath = new File(stateDisk).getCanonicalFile().toString();
			
			System.out.println("Action: " + actionCount);
			System.out.println("PreviousState: " + previousStatePath);
			System.out.println("CurrentState: " + statePath);
			
			// TESTAR launches the process to create the image and move forward without wait if exists
			// thats why we need to check and wait to obtain the image-diff
			while(!new File(previousStatePath).exists() || !new File(statePath).exists()){
				System.out.println("Waiting for Screenshot creation");
				System.out.println("Waiting... PreviousState: " + previousStatePath);
				System.out.println("Waiting... CurrentState: " + statePath);
				Util.pause(2);
			}
			
			BufferedImage img1 = ImageIO.read(new File(previousStatePath));
			BufferedImage img2 = ImageIO.read(new File(statePath));

			int width1 = img1.getWidth(); // Change - getWidth() and getHeight() for BufferedImage
			int width2 = img2.getWidth(); // take no arguments
			int height1 = img1.getHeight();
			int height2 = img2.getHeight();
			if ((width1 != width2) || (height1 != height2)) {
				System.out.println("Error: Images dimensions mismatch");
				return "";
			}

			// NEW - Create output Buffered image of type RGB
			BufferedImage outImg = new BufferedImage(width1, height1, BufferedImage.TYPE_INT_RGB);

			// Modified - Changed to int as pixels are ints
			int diff;
			int result; // Stores output pixel
			for (int i = 0; i < height1; i++) {
				for (int j = 0; j < width1; j++) {
					int rgb1 = img1.getRGB(j, i);
					int rgb2 = img2.getRGB(j, i);
					int r1 = (rgb1 >> 16) & 0xff;
					int g1 = (rgb1 >> 8) & 0xff;
					int b1 = (rgb1) & 0xff;
					int r2 = (rgb2 >> 16) & 0xff;
					int g2 = (rgb2 >> 8) & 0xff;
					int b2 = (rgb2) & 0xff;
					diff = Math.abs(r1 - r2); // Change
					diff += Math.abs(g1 - g2);
					diff += Math.abs(b1 - b2);
					diff /= 3; // Change - Ensure result is between 0 - 255
					// Make the difference image gray scale
					// The RGB components are all the same
					result = (diff << 16) | (diff << 8) | diff;
					outImg.setRGB(j, i, result); // Set result
				}
			}

			// Now save the image on disk
			// check if we have a directory for the screenshots yet
			File screenshotDir = new File(OutputStructure.htmlOutputDir + File.separator);

			// save the file to disk
			File screenshotFile = new File(screenshotDir, "diff_"+ namePreviousState + "_" + nameState + ".png");
			if (screenshotFile.exists()) {
				try {
					return screenshotFile.getCanonicalPath();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			FileOutputStream outputStream = new FileOutputStream(screenshotFile.getCanonicalPath());

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(outImg, "png", baos);
			byte[] bytes = baos.toByteArray();

			outputStream.write(bytes);
			outputStream.flush();
			outputStream.close();

			return screenshotFile.getCanonicalPath();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return "";
	}
}

/**
 * Helper Class to prepare HTML State report Difference
 */
class HTMLDifferenceGeneric {
	
	PrintWriter out = null;

	public HTMLDifferenceGeneric () { 
		String[] HEADER = new String[] {
				"<!DOCTYPE html>",
				"<html>",
				"<style>",
				".container {display: flex;}",
				".float {display:inline-block;}",
				"</style>",
				"<head>",
				"<title>TESTAR State Model difference report</title>",
				"</head>",
				"<body>"
		};
		
		String htmlReportName =  OutputStructure.htmlOutputDir + File.separator + "StateDifferenceReport.html";

		try {
			out = new PrintWriter(new File(htmlReportName).getCanonicalPath(), HTMLReporter.CHARSET);
		} catch (IOException e) { e.printStackTrace(); }

		for(String s:HEADER){
			out.println(s);
			out.flush();
		}
	}
	
	public void addStateDifferenceStep(int actionCount, String previousState, String state, String difference) {
		out.println("<h2> Specific State changes for action " + actionCount + " </h2>");
		try {
			out.println("<p><img src=\"" + new File(previousState).getCanonicalFile().toString() + "\">");
			out.println("<img src=\"" + new File(state).getCanonicalFile().toString() + "\">");
			out.println("<img src=\"" + new File(difference).getCanonicalFile().toString() + "\"></p>");
		} catch (IOException e) {
			out.println("<p> ERROR ADDING IMAGES </p>");;
		}
		out.flush();
	}
}