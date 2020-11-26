/**
 * Copyright (c) 2018, 2019, 2020 Open Universiteit - www.ou.nl
 * Copyright (c) 2019, 2020 Universitat Politecnica de Valencia - www.upv.es
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
 *
 */

import es.upv.staq.testar.CodingManager;
import es.upv.staq.testar.NativeLinker;
import nl.ou.testar.RandomActionSelector;
import org.fruit.Pair;
import org.fruit.alayer.*;
import org.fruit.alayer.actions.*;
import org.fruit.alayer.exceptions.ActionBuildException;
import org.fruit.alayer.exceptions.StateBuildException;
import org.fruit.alayer.exceptions.SystemStartException;
import org.fruit.alayer.webdriver.*;
import org.fruit.alayer.webdriver.enums.WdRoles;
import org.fruit.alayer.webdriver.enums.WdTags;
import org.fruit.monkey.Settings;
import org.testar.protocols.WebdriverProtocol;

import java.util.*;

import static org.fruit.alayer.Tags.Blocked;
import static org.fruit.alayer.Tags.Enabled;


public class Protocol_webdriver_generic_ing extends WebdriverProtocol {

    protected int highWebModalZIndex = 0;
    protected Widget widgetModal;
	
	/**
	 * Called once during the life time of TESTAR
	 * This method can be used to perform initial setup work
	 *
	 * @param settings the current TESTAR settings as specified by the user.
	 */
	@Override
	protected void initialize(Settings settings) {
		NativeLinker.addWdDriverOS();
		super.initialize(settings);
		ensureDomainsAllowed();

		// Classes that are deemed clickable by the web framework
		clickableClasses = Arrays.asList("v-menubar-menuitem", "v-menubar-menuitem-caption");

		// Disallow links and pages with these extensions
		// Set to null to ignore this feature
		deniedExtensions = Arrays.asList("pdf", "jpg", "png");

		// Define a whitelist of allowed domains for links and pages
		// An empty list will be filled with the domain from the sut connector
		// Set to null to ignore this feature
		domainsAllowed = null; //Arrays.asList("www.ou.nl", "mijn.awo.ou.nl", "login.awo.ou.nl");

		// If true, follow links opened in new tabs
		// If false, stay with the original (ignore links opened in new tabs)
		followLinks = true;
		// Propagate followLinks setting
		WdDriver.followLinks = followLinks;

		// URL + form name, username input id + value, password input id + value
		// Set login to null to disable this feature
		login = null ; //Pair.from("https://login.awo.ou.nl/SSO/login", "OUinloggen");
		username = Pair.from("username", "");
		password = Pair.from("password", "");

		// List of atributes to identify and close policy popups
		// Set to null to disable this feature
		policyAttributes = null; /*new HashMap<String, String>() {{
			put("class", "lfr-btn-label");
		}};*/

		WdDriver.fullScreen = true;

		// Override ProtocolUtil to allow WebDriver screenshots
		protocolUtil = new WdProtocolUtil();

	}

	/**
	 * This method is called when TESTAR starts the System Under Test (SUT). The method should
	 * take care of
	 * 1) starting the SUT (you can use TESTAR's settings obtainable from <code>settings()</code> to find
	 * out what executable to run)
	 * 2) bringing the system into a specific start state which is identical on each start (e.g. one has to delete or restore
	 * the SUT's configuratio files etc.)
	 * 3) waiting until the system is fully loaded and ready to be tested (with large systems, you might have to wait several
	 * seconds until they have finished loading)
	 *
	 * @return a started SUT, ready to be tested.
	 */
	@Override
	protected SUT startSystem() throws SystemStartException {
		SUT sut = super.startSystem();

		/* Capture ing-flow state */
		String customElementStateLambda =
				"(element => { if (element.tagName.toLowerCase() === 'ing-flow') " +
						"{ return element._router.state; } else { return null; } } )";

		sut.set(WdTags.WebCustomElementStateLambda, customElementStateLambda);

		return sut;
	}

	/**
	 * This method is invoked each time the TESTAR starts the SUT to generate a new sequence.
	 * This can be used for example for bypassing a login screen by filling the username and password
	 * or bringing the system into a specific start state which is identical on each start (e.g. one has to delete or restore
	 * the SUT's configuration files etc.)
	 */
	@Override
	protected void beginSequence(SUT system, State state) {
		super.beginSequence(system, state);
	}

	protected Object abstractINGFlowState(Object obj) {
		if (obj instanceof Map) {
			Map map = (Map)obj;
			Map newmap = new HashMap();

			for (Object key : map.keySet()) {
				newmap.put(key, abstractINGFlowState(map.get(key)));
			}
			return newmap;
		}
		else if (obj == null) {
			return null;
		}
		else {
			return obj.getClass().getCanonicalName(); // abstract value to class
		}
	}
	/**
	 * This method is called when TESTAR requests the state of the SUT.
	 * Here you can add additional information to the SUT's state or write your
	 * own state fetching routine. The state should have attached an oracle
	 * (TagName: <code>Tags.OracleVerdict</code>) which describes whether the
	 * state is erroneous and if so why.
	 *
	 * @return the current state of the SUT with attached oracle.
	 */
	@Override
	protected State getState(SUT system) throws StateBuildException {
		State state = super.getState(system);

    	// Reset because modal element may disappear
    	highWebModalZIndex = 0;
    	widgetModal = state;

    	Boolean modalMode = false;

    	for(Widget w : state) {
			Object st = w.get(WdTags.WebCustomElementState, null);

			if (st != null) {
				Object filtered = abstractINGFlowState(st);
				System.out.println("ing-flow state: " + st);
				System.out.println("ing-flow abstract state: " + abstractINGFlowState(st));
				state.set(WdTags.WebCustomElementState, filtered);
			}

			String cl = (((WdWidget)w).element).attributeMap.getOrDefault("class", "");
			// if the ING global-overlays element contains children, there is a modal displayed
			if ("global-overlays".equals(cl) && w.childCount() > 0) {
				modalMode = true;
			}

			// Set highWebModalZIndex value. And the widget that represents that block modal.
    		// It can be useful for users in their specific protocols.
    		if(w.get(WdTags.WebIsWindowModal, false) && w.get(WdTags.WebZIndex, -1) > highWebModalZIndex) {
    			highWebModalZIndex = w.get(WdTags.WebZIndex, -1);
    			widgetModal = w;
    		}
    	}

    	state.set(WdTags.WebIsWindowModal, modalMode);
		setAbstractIdCustom(state);

		return state;                                                                        
	}

	protected void setAbstractIdCustom(State state) {
		Object cstate = state.get(WdTags.WebCustomElementState, null);
		if (cstate != null) {
			String id = CodingManager.ID_PREFIX_ABSTRACT_CUSTOM + CodingManager.lowCollisionID(cstate.toString());
			state.set(Tags.AbstractIDCustom, CodingManager.ID_PREFIX_STATE + id);
		}
	}
	/**
	 * This is a helper method used by the default implementation of <code>buildState()</code>
	 * It examines the SUT's current state and returns an oracle verdict.
	 *
	 * @return oracle verdict, which determines whether the state is erroneous and why.
	 */
	@Override
	protected Verdict getVerdict(State state) {
		setAbstractIdCustom(state);
		Verdict verdict = super.getVerdict(state);
		setAbstractIdCustom(state);

		// system crashes, non-responsiveness and suspicious titles automatically detected!

		//-----------------------------------------------------------------------------
		// MORE SOPHISTICATED ORACLES CAN BE PROGRAMMED HERE (the sky is the limit ;-)
		//-----------------------------------------------------------------------------

		// ... YOU MAY WANT TO CHECK YOUR CUSTOM ORACLES HERE ...

		return verdict;
	}

	/**
	 * This method is used by TESTAR to determine the set of currently available actions.
	 * You can use the SUT's current state, analyze the widgets and their properties to create
	 * a set of sensible actions, such as: "Click every Button which is enabled" etc.
	 * The return value is supposed to be non-null. If the returned set is empty, TESTAR
	 * will stop generation of the current action and continue with the next one.
	 *
	 * @param system the SUT
	 * @param state  the SUT's current state
	 * @return a set of actions
	 */
	@Override
	protected Set<Action> deriveActions(SUT system, State state)
			throws ActionBuildException {

		setAbstractIdCustom(state);
		System.out.println("abstract custom state id: " + state.get(Tags.AbstractIDCustom));
		
		// Kill unwanted processes, force SUT to foreground
		Set<Action> actions = super.deriveActions(system, state);
		setAbstractIdCustom(state);

		// create an action compiler, which helps us create actions
		// such as clicks, drag&drop, typing ...
		StdActionCompiler ac = new AnnotatingActionCompiler();

		// Check if forced actions are needed to stay within allowed domains
		Set<Action> forcedActions = detectForcedActions(state, ac);
		if (forcedActions != null && forcedActions.size() > 0) {
			return forcedActions;
		}

		boolean modalMode = state.get(WdTags.WebIsWindowModal, false);

		// iterate through all widgets
		for (Widget widget : state) {
			// slides can happen, even though the widget might be blocked
			//addSlidingActions(actions, ac, scrollArrowSize, scrollThick, widget, state);

			// ignore aria-hidden = true
			if (isAriaHidden(widget)) {
				continue;
			}

			// only consider enabled and non-tabu widgets
			if (!widget.get(Enabled, true) || blackListed(widget)) {
				continue;
			}

			/* We only care about Widgets that are contained by an ing-flow when we are not in modal mode */
			if (!isINGFlowStep(widget) && !modalMode) {
				continue;
			}

			// If the element is blocked, TESTAR can't click on or type in the widget
			if (widget.get(Blocked, false) && !widget.get(WdTags.WebIsShadow, false)) {
				continue;
			}

			// NOTE: this doesn't work for this specific ING use case - the z-index blocking algo is too naive
			// Check if the element is blocked by web modal element with high z-index
			/*if (isBlockedByModal(widget)) {
				continue;
			} */

			// type into text boxes
			if (isAtBrowserCanvas(widget) && isTypeable(widget) && (whiteListed(widget) || isUnfiltered(widget))) {
				actions.add(ac.clickTypeInto(widget, this.getRandomText(widget), true));
			}

			// left clicks, but ignore links outside domain
			if (isAtBrowserCanvas(widget) && isClickable(widget) && (whiteListed(widget) || isUnfiltered(widget))) {
				if (!isLinkDenied(widget)) {
					WdElement element = ((WdWidget) widget).element;
					Role role = widget.get(Tags.Role, Roles.Widget);
					actions.add(ac.leftClickAt(widget));
				}
			}
		}

		return actions;
	}

	@Override
	protected String getRandomText(Widget w) {
		String sText = super.getRandomText(w);

		String name = w.get(WdTags.WebName, "").toLowerCase();

		int multiplier = 100;

		if (name.contains("monthly")) {
			multiplier = 9;
		}
		if (name.contains("age")) {
			return "" + ((multiplier * (((new Random()).nextInt(90) + 15))) / 100);
		}
		if (name.contains("income")) {
			return "" + ((multiplier * (((new Random()).nextInt(100000) + 10))) / 100);
		}
		if (name.contains("profit")) {
			return "" + ((multiplier * (((new Random()).nextInt(10000) + 10))) / 100);
		}
		if (name.contains("years")) {
			return "" + ((multiplier * (((new Random()).nextInt(100000) + 10))) / 100);
		}
		if (name.contains("studyloan")) {
			return "" + ((multiplier * (((new Random()).nextInt(100000) + 10))) / 100);
		}
		if (name.contains("liabilities")) {
			return "" + ((multiplier * (((new Random()).nextInt(100000) + 10))) / 100);
		}
		if (name.contains("alimony")) {
			return "" + (((new Random()).nextInt(300)));
		}
		return sText;
	}  
	
	/* Check whether the Widget is contained by the ing-step container and is not hidden */
	protected boolean isINGFlowStep(Widget widget) {
		WdElement element = ((WdWidget) widget).element;
		String id = element.attributeMap.getOrDefault("id","");

		if ("flow-step".equals(id)) {
			return true;
		}
		else {
			Widget parent = widget.parent();
			if (parent != null) {
				return isINGFlowStep(widget.parent());
			}
			return false;
		}
	}

	protected boolean isAriaHidden(Widget widget) {
		WdElement element = ((WdWidget) widget).element;

		if ("true".equals(element.attributeMap.getOrDefault("aria-hidden","false"))) {
			return true;
		}
		Widget parent = widget.parent();
		if (parent != null) {
			return isAriaHidden(parent);
		}
		else {
			return false;
		}
	}

	@Override
	protected boolean isClickable(Widget widget) {
		WdElement element = ((WdWidget) widget).element;

		if (element.attributeMap.getOrDefault("href", "").contains("bel-me-nu")) {
			return false;
		}
		if (element.attributeMap.getOrDefault("href", "").endsWith("hypotheek-berekenen/")) {
			return false;
		}
		if("_blank".equals(element.attributeMap.getOrDefault("target", ""))) {
			return false;
		}

		Role role = widget.get(Tags.Role, Roles.Widget);
		if (Role.isOneOf(role, NativeLinker.getNativeClickableRoles())) {
			// Input type are special...
			if (role.equals(WdRoles.WdINPUT)) {
				String type = ((WdWidget) widget).element.type;
				return WdRoles.clickableInputTypes().contains(type);
			}
			return true;
		}
		
		if (element.isClickable) {
			return true;
		}

		Set<String> clickSet = new HashSet<>(clickableClasses);
		clickSet.retainAll(element.cssClasses);
		return clickSet.size() > 0;
	}

	@Override
	protected boolean isTypeable(Widget widget) {
		Role role = widget.get(Tags.Role, Roles.Widget);
		if (Role.isOneOf(role, NativeLinker.getNativeTypeableRoles())) {
			// Input type are special...
			if (role.equals(WdRoles.WdINPUT)) {
				String type = ((WdWidget) widget).element.type;
				return WdRoles.typeableInputTypes().contains(type);
			}
			return true;
		}

		return false;
	}
	
	/**
	 * Check if the desired widget is blocked by some widgetModal.
	 * By default, this widgetModal object is the TESTAR State widget.
	 * 
	 * If a web element with a display "block" property appears,
	 * the z-index property will be used to determine the new web modal element
	 * 
	 * @param w
	 * @return
	 */
	private boolean isBlockedByModal(Widget w) {
		return !widgetIsChildOfParent(w, widgetModal);
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
		setAbstractIdCustom(state);

		//Call the preSelectAction method from the AbstractProtocol so that, if necessary,
		//unwanted processes are killed and SUT is put into foreground.
		Action retAction = preSelectAction(state, actions);
		if (retAction== null) {
			//if no preSelected actions are needed, then implement your own action selection strategy
			//using the action selector of the state model:
			setAbstractIdCustom(state);
			retAction = stateModelManager.getAbstractActionToExecute(actions);
		}
		if(retAction==null) {
			System.out.println("State model based action selection did not find an action. Using random action selection.");
			// if state model fails, using random:
			retAction = RandomActionSelector.selectAction(actions);
		}
		return retAction;
	}

	/**
	 * Execute the selected action
	 *
	 * @param system the SUT
	 * @param state  the SUT's current state
	 * @param action the action to execute
	 * @return whether or not the execution succeeded
	 */
	@Override
	protected boolean executeAction(SUT system, State state, Action action) {
		return super.executeAction(system, state, action);
	}

	/**
	 * TESTAR uses this method to determine when to stop the generation of actions for the
	 * current sequence. You could stop the sequence's generation after a given amount of executed
	 * actions or after a specific time etc.
	 *
	 * @return if <code>true</code> continue generation, else stop
	 */
	@Override
	protected boolean moreActions(State state) {
		return super.moreActions(state);
	}

	/**
	 * This method is invoked each time after TESTAR finished the generation of a sequence.
	 */
	@Override
	protected void finishSequence() {
		super.finishSequence();
	}

	/**
	 * TESTAR uses this method to determine when to stop the entire test.
	 * You could stop the test after a given amount of generated sequences or
	 * after a specific time etc.
	 *
	 * @return if <code>true</code> continue test, else stop
	 */
	@Override
	protected boolean moreSequences() {
		return super.moreSequences();
	}
}