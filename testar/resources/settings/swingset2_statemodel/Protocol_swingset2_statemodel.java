/***************************************************************************************************
 *
 * Copyright (c) 2020 Universitat Politecnica de Valencia - www.upv.es
 * Copyright (c) 2020 Open Universiteit - www.ou.nl
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.fruit.Util;
import org.fruit.alayer.Action;
import org.fruit.alayer.exceptions.ActionBuildException;
import org.fruit.alayer.windows.WinProcess;
import org.fruit.monkey.ConfigTags;
import org.fruit.monkey.Settings;
import org.fruit.alayer.SUT;
import org.fruit.alayer.State;
import org.fruit.alayer.Widget;
import org.fruit.alayer.actions.AnnotatingActionCompiler;
import org.fruit.alayer.actions.StdActionCompiler;
import org.fruit.alayer.devices.AWTKeyboard;
import org.fruit.alayer.devices.KBKeys;
import org.fruit.alayer.devices.Keyboard;
import org.fruit.alayer.Tags;
import org.testar.OutputStructure;
import org.testar.jacoco.JacocoFilesCreator;
import org.testar.jacoco.JacocoReportReader;
import org.testar.jacoco.MBeanClient;
import org.testar.jacoco.MergeJacocoFiles;
import org.testar.protocols.JavaSwingProtocol;

import com.google.common.io.Files;

import nl.ou.testar.RandomActionSelector;

import static org.fruit.alayer.Tags.Blocked;
import static org.fruit.alayer.Tags.Enabled;

import org.fruit.alayer.*;

import java.io.FileWriter;

import org.fruit.monkey.Main;

/**
 * This protocol together with the settings provides a specific behavior to test SwingSet2
 * We will use Java Access Bridge settings (AccessBridgeEnabled = true) for widget tree extraction
 *
 * It uses StateModel Unvisited Actions algorithm.
 */
public class Protocol_swingset2_statemodel extends JavaSwingProtocol {
	
	private long startSequenceTime;
	private String reportTimeDir;
	
	//Java Coverage: It may happen that the SUT and its JVM unexpectedly close or stop responding
	// we use this variable to store after each action the last correct coverage
	private String lastCorrectJacocoCoverageFile = "";

	// Java Coverage: Save all JaCoCO sequence reports, to merge them at the end of the execution
	private Set<String> jacocoFiles = new HashSet<>();

	/**
	 * Called once during the life time of TESTAR
	 * This method can be used to perform initial setup work
	 * @param   settings  the current TESTAR settings as specified by the user.
	 */
	@Override
	protected void initialize(Settings settings){
		
		// For experimental purposes we need to disconnect from Windows Remote Desktop
		// without close the GUI session.
		/*try {
			// bat file that uses tscon.exe to disconnect without stop GUI session
			File disconnectBatFile = new File(Main.settingsDir + File.separator + "disconnectRDP.bat").getCanonicalFile();

			// Launch and disconnect from RDP session
			// This will prompt the UAC permission window if enabled in the System
			if(disconnectBatFile.exists()) {
				System.out.println("Running: " + disconnectBatFile);
				Runtime.getRuntime().exec("cmd /c start \"\" " + disconnectBatFile);
			} else {
				System.out.println("THIS BAT DOES NOT EXIST: " + disconnectBatFile);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		// Wait because disconnect from system modifies internal Screen resolution
		Util.pause(30);*/
		
		super.initialize(settings);

		// SwingSet2: Requires Java Access Bridge
		System.out.println("Are we running Java Access Bridge ? " + settings.get(ConfigTags.AccessBridgeEnabled, false));
		
		// StateModel Unvisited Action: This should be "unvisited", if not execution is not valid
		System.out.println("StateModel Algorithm: " + settings.get(ConfigTags.ActionSelectionAlgorithm, "nothing"));
		// This should be false for fastest executions
		System.out.println("StateModel StateModelStoreWidgets: " + settings.get(ConfigTags.StateModelStoreWidgets, true));
		
		// TESTAR will execute the SUT with Java
		// We need this to add JMX parameters properly (-Dcom.sun.management.jmxremote.port=5000)
		WinProcess.java_execution = true;
	}
	
	/**
	 * This method is invoked each time the TESTAR starts the SUT to generate a new sequence.
	 * This can be used for example for bypassing a login screen by filling the username and password
	 * or bringing the system into a specific start state which is identical on each start (e.g. one has to delete or restore
	 * the SUT's configuration files etc.)
	 */
	 @Override
	protected void beginSequence(SUT system, State state){
		startSequenceTime = System.currentTimeMillis();
		try{
			reportTimeDir = new File(OutputStructure.outerLoopOutputDir).getCanonicalPath();
			//myWriter = new FileWriter(reportTimeDir + "/_sequenceTimeUntilActions.txt");
		} catch (Exception e) {
				System.out.println("sequenceTimeUntilActions.txt can not be created " );
				e.printStackTrace();
		}
	 	super.beginSequence(system, state);
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
	 protected Set<Action> deriveActions(SUT system, State state) throws ActionBuildException{

		 //The super method returns a ONLY actions for killing unwanted processes if needed, or bringing the SUT to
		 //the foreground. You should add all other actions here yourself.
		 // These "special" actions are prioritized over the normal GUI actions in selectAction() / preSelectAction().
		 Set<Action> actions = super.deriveActions(system,state);

		 // To derive actions (such as clicks, drag&drop, typing ...) we should first create an action compiler.
		 StdActionCompiler ac = new AnnotatingActionCompiler();

		 /**
		  * Specific Action Derivation for SwingSet2 SUT
		  * To avoid deriving actions on non-desired widgets
		  * 
		  * Optional : iterate through top level widgets based on Z-index
		  * for(Widget w : getTopWidgets(state))
		  * If selected also change it for all SwingSet2 protocols
		  */

		 // iterate through all widgets
		 for(Widget w : state){

			 if(w.get(Enabled, true) && !w.get(Blocked, false)){ // only consider enabled and non-blocked widgets

				 if (!blackListed(w)){  // do not build actions for tabu widgets  

					 // left clicks
					 if(isClickable(w) && (isUnfiltered(w) || whiteListed(w))) {
						 actions.add(ac.leftClickAt(w));
					 }

					 // type into text boxes
					 // SwingSet2: isSourceCodeEditWidget feature
					 if((isTypeable(w) && (isUnfiltered(w) || whiteListed(w))) && !isSourceCodeEditWidget(w)) {
						 actions.add(ac.clickTypeInto(w, this.getRandomText(w), true));
					 }

					 // GENERIC: All swing apps
					 //Force actions on some widgets with a wrong accessibility
					 //Optional, comment this changes if your Swing applications doesn't need it
					 if(w.get(Tags.Role).toString().contains("Tree") ||
							 w.get(Tags.Role).toString().contains("ComboBox") ||
							 w.get(Tags.Role).toString().contains("List")) {
						 widgetTree(w, actions);
					 }
					 //End of Force action
				 }
			 }
		 }

		 return actions;
	 }
	
	 /**
	  * SwingSet2 application contains a TabElement called "SourceCode"
	  * that internally contains UIAEdit widgets that are not modifiable.
	  * Because these widgets have the property ToolTipText with the value "text/html",
	  * use this Tag to recognize and ignore.
	  */
	 private boolean isSourceCodeEditWidget(Widget w) {
		 return w.get(Tags.ToolTipText, "").contains("text/html");
	 }

	 //Force actions on Tree widgets with a wrong accessibility
	 public void widgetTree(Widget w, Set<Action> actions) {
		 StdActionCompiler ac = new AnnotatingActionCompiler();
		 actions.add(ac.leftClickAt(w));
		 w.set(Tags.ActionSet, actions);
		 for(int i = 0; i<w.childCount(); i++) {
			 widgetTree(w.child(i), actions);
		 }
	 }
	 
	 /**
	  * Select one of the available actions using an action selection algorithm (for example random action selection)
	  *
	  * @param state the SUT's current state
	  * @param actions the set of derived actions
	  * @return  the selected action (non-null!)
	  */
	 @Override
	 protected Action selectAction(State state, Set<Action> actions){

		 //Call the preSelectAction method from the AbstractProtocol so that, if necessary,
		 //unwanted processes are killed and SUT is put into foreground.
		 Action retAction = preSelectAction(state, actions);
		 if (retAction== null) {
			 //if no preSelected actions are needed, then implement your own action selection strategy
			 // StateModel Unvisited Action: using the action selector of the state model (random, unvisited)
			 retAction = stateModelManager.getAbstractActionToExecute(actions);
		 }
		 if(retAction==null) {
			 System.out.println("State model based action selection did not find an action. Using random action selection.");
			 // if state model fails, use random (default would call preSelectAction() again, causing double actions HTML report):
			 retAction = RandomActionSelector.selectAction(actions);
		 }
		 return retAction;
	 }

	/**
	 * Execute the selected action.
	 * Extract and create JaCoCo coverage report (After each action JaCoCo report will be created).
	 */
	@Override
	protected boolean executeAction(SUT system, State state, Action action){
		boolean actionExecuted = super.executeAction(system, state, action);

		// Only create JaCoCo report for Generate Mode
		// Modify if desired
		if(settings.get(ConfigTags.Mode).equals(Modes.Generate)) {

			// Dump the JaCoCo report from the remote JVM and Get the name/path of this file
			try {
				System.out.println("Extract JaCoCO report for Action number: " + actionCount);
				
				// Write sequence duration to CLI and to file
				long  sequenceDurationSoFar = System.currentTimeMillis() - startSequenceTime;
				System.out.println();
				System.out.println("Elapsed time until action " + actionCount + ": " + sequenceDurationSoFar);
	
				long minutes = (sequenceDurationSoFar / 1000)  / 60;
				int seconds = (int)((sequenceDurationSoFar / 1000) % 60);
				System.out.println("Elapsed time until action " + actionCount + ": " + + minutes + " minutes, "+ seconds + " seconds.");
				System.out.println();
				// Write sequence duration to file
				try {
					FileWriter myWriter = new FileWriter(reportTimeDir + "/" + OutputStructure.startInnerLoopDateString + "_" + OutputStructure.executedSUTname + "_actionTimeStamps.txt", true);
					//myWriter.write("Elapsed time until action " + actionCount + " : " + minutes + " minutes, " + seconds + " seconds   (" + sequenceDurationSoFar + " mili). \r\n");
					myWriter.write(sequenceDurationSoFar + "\r\n");
					myWriter.close();
					System.out.println("Wrote time so far to file." + reportTimeDir + "/_sequenceTimeUntilAction.txt");
				} catch (IOException e) {
					System.out.println("An error occurred.");
					e.printStackTrace();
				}
				
				// Dump the JaCoCo Action report from the remote JVM
				String jacocoFile = JacocoFilesCreator.dumpAndGetJacocoActionFileName(Integer.toString(actionCount));

				// If not empty save as last correct file in case the SUT crashes
				if(!jacocoFile.isEmpty()) {
					lastCorrectJacocoCoverageFile = jacocoFile;
				}

				// Create the output JaCoCo Action report
				JacocoFilesCreator.createJacocoActionReport(jacocoFile, Integer.toString(actionCount));

			} catch (Exception e) {
				System.out.println("ERROR Creating JaCoCo covergae for specific action: " + actionCount);
			}
		}

		return actionExecuted;
	}

	/**
	 * This method is invoked each time the TESTAR has reached the stop criteria for generating a sequence.
	 * This can be used for example for graceful shutdown of the SUT, maybe pressing "Close" or "Exit" button
	 */
	@Override
	protected void finishSequence() {

		// Only create JaCoCo report for Generate Mode
		// Modify if desired
		if(settings.get(ConfigTags.Mode).equals(Modes.Generate)) {

			// Dump the JaCoCo report from the remote JVM and Get the name/path of this file
			String jacocoFile = JacocoFilesCreator.dumpAndGetJacocoSequenceFileName();
			if(!jacocoFile.isEmpty()) {
				lastCorrectJacocoCoverageFile = jacocoFile;
			}

			// Add lastCorrectJacocoCoverageFile file to this set list, for merging at the end of the TESTAR run
			// If everything works correctly will be the sequence report, if not last correct action report
			jacocoFiles.add(lastCorrectJacocoCoverageFile);

			// Create the output JaCoCo report
			JacocoFilesCreator.createJacocoSequenceReport(jacocoFile);
		}
 
		super.finishSequence();
		
		// Write sequence duration to CLI and to file
		long  sequenceDuration = System.currentTimeMillis() - startSequenceTime;
		System.out.println();
		System.out.println("Sequence duration: " + sequenceDuration);
		long minutes = (sequenceDuration / 1000)  / 60;
		int seconds = (int)((sequenceDuration / 1000) % 60);
		System.out.println("Sequence duration: " + minutes + " minutes, "+ seconds + " seconds.");
		System.out.println();
		try {
			String reportDir = new File(OutputStructure.outerLoopOutputDir).getCanonicalPath();//  + File.separator;
			FileWriter myWriter = new FileWriter(reportDir + "/" + OutputStructure.startInnerLoopDateString + "_" + OutputStructure.executedSUTname + "_sequenceDuration.txt");
			myWriter.write("Sequence duration: " + minutes + " minutes, " + seconds + " seconds.   (" + sequenceDuration + " mili)");
			myWriter.close();
			System.out.println("Wrote time to file." + reportDir + "/_sequenceDuration.txt");
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}

	/**
	 * This methods stops the SUT
	 *
	 * @param system
	 */
	@Override
	protected void stopSystem(SUT system) {
		super.stopSystem(system);

		// This is the default JaCoCo generated file, we dumped our desired file with MBeanClient (finishSequence)
		// In this protocol this one is residual, so just delete
		if(new File("jacoco.exec").exists()) {
			System.out.println("Deleted residual jacoco.exec file ? " + new File("jacoco.exec").delete());
		}
	}
	
	/**
	 * This method is called after the last sequence, to allow for example handling the reporting of the session
	 */
	@Override
	protected void closeTestSession() {
		super.closeTestSession();
		try {
			// Merge all jacoco.exec sequence files
			MergeJacocoFiles mergeJacoco = new MergeJacocoFiles();
			File mergedJacocoFile = new File(OutputStructure.outerLoopOutputDir + File.separator + "jacoco_merged.exec");
			mergeJacoco.testarExecuteMojo(new ArrayList<>(jacocoFiles), mergedJacocoFile);
			// And create the report that contains the coverage of all executed sequences
			JacocoFilesCreator.createJacocoMergedReport(mergedJacocoFile.getCanonicalPath());

			//TODO: We also need to delete original folder after compression
			//Compress all JaCoCo report files
			String jacocoReportFolder = new File(OutputStructure.outerLoopOutputDir).getCanonicalPath() + File.separator + "JaCoCo_reports";
			JacocoFilesCreator.compressJacocoReportFile(jacocoReportFolder);

		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.out.println("ERROR: Trying to MergeMojo Jacoco Files");
		}
	}
}