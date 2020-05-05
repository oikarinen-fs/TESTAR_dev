package nl.ou.testar.temporal.control;

import com.orientechnologies.orient.core.record.ODirection;
import com.orientechnologies.orient.core.record.OEdge;
import com.orientechnologies.orient.core.record.OVertex;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import es.upv.staq.testar.CodingManager;
import es.upv.staq.testar.StateManagementTags;
import nl.ou.testar.StateModel.Analysis.Representation.AbstractStateModel;

import nl.ou.testar.temporal.foundation.PairBean;
import nl.ou.testar.temporal.foundation.ValStatus;
import nl.ou.testar.temporal.ioutils.CSVHandler;
import nl.ou.testar.temporal.ioutils.JSONHandler;
import nl.ou.testar.temporal.ioutils.SimpleLog;
import nl.ou.testar.temporal.model.*;
import nl.ou.testar.temporal.modelcheck.*;
import nl.ou.testar.temporal.oracle.*;
import nl.ou.testar.temporal.proposition.PropositionConstants;
import nl.ou.testar.temporal.proposition.PropositionManager;
import nl.ou.testar.temporal.foundation.TagBean;
import nl.ou.testar.temporal.util.*;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.fruit.alayer.Tag;
import org.fruit.monkey.ConfigTags;
import org.fruit.monkey.Settings;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static nl.ou.testar.temporal.util.Common.prettyCurrentTime;
import static org.fruit.monkey.ConfigTags.AbstractStateAttributes;


/**
 * Temporal Controller: orchestrates the Model Check function of TESTAR
 */
public class TemporalController {
    private final String ApplicationName;
    private final String ApplicationVersion;
    private String Modelidentifier;
    private final String outputDir;

    private final boolean ltlSPOTToWSLPath;
    private final String ltlSPOTMCCommand;
    private final boolean ltlSPOTEnabled;

    private final boolean ctlITSToWSLPath;
    private final String ctlITSMCCommand;
    private final boolean ctlITSEnabled;

    private final boolean ctlGALToWSLPath;
    private final String ctlGALMCCommand;
    private final boolean ctlGALEnabled;

    private final boolean ltlITSToWSLPath;
    private final String ltlITSMCCommand;
    private final boolean ltlITSEnabled;

    private final boolean ltlLTSMINToWSLPath;
    private final String ltlLTSMINMCCommand;
    private final boolean ltlLTSMINEnabled;

    private final boolean ctlLTSMINToWSLPath;
    private final String ctlLTSMINMCCommand;
    private final boolean ctlLTSMINEnabled;

    private final String propositionManagerFile;
    private final String oracleFile;
    private final boolean verbose;
    private final boolean counterExamples;

    private final boolean instrumentDeadlockState;

    private PropositionManager propositionManager;
    private TemporalModel tModel;
    private final TemporalDBManager tDBManager;
    private List<TemporalOracle> oracleColl;
    private final SimpleLog simpleLog;

    public TemporalController(final Settings settings,  String outputDir) {

        this.ApplicationName = settings.get(ConfigTags.ApplicationName);
        this.ApplicationVersion = settings.get(ConfigTags.ApplicationVersion);
        setModelidentifier(settings);
        if (outputDir.equals("")) {
            this.outputDir = createTemporalFolder(settings);
        } else {
            this.outputDir = outputDir;
        }
        String logFileName = this.outputDir + "log.txt";
        simpleLog= new SimpleLog(logFileName,true);
        simpleLog.append(prettyCurrentTime() + " | " +"Temporal Component uses output folder: "+this.outputDir+"\n");
        tDBManager = new TemporalDBManager(settings,simpleLog);
        tModel = new TemporalModel();
        ltlSPOTToWSLPath = settings.get(ConfigTags.TemporalLTL_SPOTCheckerWSL);
        ltlSPOTMCCommand = settings.get(ConfigTags.TemporalLTL_SPOTChecker);
        ltlSPOTEnabled = settings.get(ConfigTags.TemporalLTL_SPOTChecker_Enabled);

        ctlITSToWSLPath = settings.get(ConfigTags.TemporalCTL_ITSCheckerWSL);
        ctlITSMCCommand = settings.get(ConfigTags.TemporalCTL_ITSChecker);
        ctlITSEnabled = settings.get(ConfigTags.TemporalCTL_ITSChecker_Enabled);

        ctlGALToWSLPath = settings.get(ConfigTags.TemporalCTL_GALCheckerWSL);
        ctlGALMCCommand = settings.get(ConfigTags.TemporalCTL_GALChecker);
        ctlGALEnabled = settings.get(ConfigTags.TemporalCTL_GALChecker_Enabled);

        ltlITSToWSLPath = settings.get(ConfigTags.TemporalLTL_ITSCheckerWSL);
        ltlITSMCCommand = settings.get(ConfigTags.TemporalLTL_ITSChecker);
        ltlITSEnabled = settings.get(ConfigTags.TemporalLTL_ITSChecker_Enabled);

        ltlLTSMINToWSLPath = settings.get(ConfigTags.TemporalLTL_LTSMINCheckerWSL);
        ltlLTSMINMCCommand = settings.get(ConfigTags.TemporalLTL_LTSMINChecker);
        ltlLTSMINEnabled = settings.get(ConfigTags.TemporalLTL_LTSMINChecker_Enabled);

        ctlLTSMINToWSLPath = settings.get(ConfigTags.TemporalCTL_LTSMINCheckerWSL);
        ctlLTSMINMCCommand = settings.get(ConfigTags.TemporalCTL_LTSMINChecker);
        ctlLTSMINEnabled = settings.get(ConfigTags.TemporalCTL_LTSMINChecker_Enabled);

        propositionManagerFile = settings.get(ConfigTags.TemporalPropositionManager);
        oracleFile = settings.get(ConfigTags.TemporalOracles);
        verbose = settings.get(ConfigTags.TemporalVerbose);
        counterExamples = settings.get(ConfigTags.TemporalCounterExamples);
        instrumentDeadlockState = settings.get(ConfigTags.TemporalInstrumentDeadlockState);

        setDefaultPropositionManager();

    }

    public TemporalController(final Settings settings) {
        this(settings, "");
    }

    /**
     * no params
     * @return outputdirectory
     */
    public String getOutputDir() {
        return outputDir;
    }

    private void setTemporalModelMetaData(AbstractStateModel abstractStateModel) {
        if (abstractStateModel != null) {
            tModel.setApplicationName(abstractStateModel.getApplicationName());
            tModel.setApplicationVersion(abstractStateModel.getApplicationVersion());
            tModel.setApplication_ModelIdentifier(abstractStateModel.getModelIdentifier());
            tModel.setApplication_AbstractionAttributes(abstractStateModel.getAbstractionAttributes());
        }
    }

    private void setModelidentifier(Settings settings) {

        //assumption is that the model is created with the same abstraction pon concrete layer as on the abstract layer.
        // we can inspect the graphmodel for the abstract layer,
        // but we cannot inspect the graphmodel for the abstraction that used on the concretelayer.
        // for new models we enforce this by setting "TemporalConcreteEqualsAbstract = true" in the test.settings file
        // copied from Main.initcodingmanager
        if (!settings.get(ConfigTags.AbstractStateAttributes).isEmpty()) {
            Tag<?>[] abstractTags = settings.get(AbstractStateAttributes).stream().map(StateManagementTags::getTagFromSettingsString).filter(Objects::nonNull).toArray(Tag<?>[]::new);
            CodingManager.setCustomTagsForAbstractId(abstractTags);
        }
        //copied from StateModelManagerFactory
        // get the abstraction level identifier that uniquely identifies the state model we are testing against.
        this.Modelidentifier = CodingManager.getAbstractStateModelHash(ApplicationName, ApplicationVersion);

    }

    private String createTemporalFolder(final Settings settings) {
        String outputDir = settings.get(ConfigTags.OutputDir);
        // check if the output directory has a trailing line separator
        if (!outputDir.substring(outputDir.length() - 1).equals(File.separator)) {
            outputDir += File.separator;
        }
        outputDir = outputDir + settings.get(ConfigTags.TemporalDirectory);

        if (settings.get(ConfigTags.TemporalSubDirectories)) {
            String runFolder = Common.CurrentDateToFolder();
            outputDir = outputDir + File.separator + runFolder;
        }
        new File(outputDir).mkdirs();
        outputDir = outputDir + File.separator;
        return outputDir;
    }


    public void savePropositionManager(String filename) {
        simpleLog.append(prettyCurrentTime() + " | " + "generating Proposition Manager file: "+filename);
        JSONHandler.save(propositionManager, outputDir + filename, true);
    }

    private void loadPropositionManager(String filename) {
        this.propositionManager = (PropositionManager) JSONHandler.load(filename, propositionManager.getClass());
        tDBManager.setPropositionManager(this.propositionManager);
    }

    public List<TemporalOracle> getOracleColl() {
        return oracleColl;
    }

    private void setOracleColl(List<TemporalOracle> oracleColl) {
        this.oracleColl = oracleColl;
        this.oracleColl.sort(Comparator.comparing(TemporalOracle::getPatternTemporalType)); //sort by type
    }


    public void setDefaultPropositionManager() {
        this.propositionManager = new PropositionManager(true);
        tDBManager.setPropositionManager(propositionManager);
    }



    public String pingDB() {
        String info =tDBManager.pingDB();
        String dbfilename = outputDir + "Databasemodels.csv";
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dbfilename))) {
            bw.write(info);
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        simpleLog.append("generated file: " + dbfilename);
        return "generated file: " + dbfilename;
    }


    //*********************************
    private void settModel(AbstractStateModel abstractStateModel, boolean instrumentTerminalState) {
        //candidate for refactoring as maintaining Oresultset is responsibility of TemporalDBManager
        long start_time = System.currentTimeMillis();
        int runningWcount=0;
        int stateCount=0;
        int totalStates;
        int chunks=10;
        tDBManager.dbReopen();

        OResultSet resultSet = tDBManager.getConcreteStatesFromOrientDb(abstractStateModel);
        totalStates=tDBManager.getConcreteStateCountFromOrientDb(abstractStateModel);
        Map<String,Integer> commentWidgetDistri = new HashMap<>();
        MultiValuedMap<String,String> logNonDeterministicTransitions = new HashSetValuedHashMap<>();
        List<String> logTerminalStates= new ArrayList<>();
        boolean firstTerminalState = true;
        StateEncoding terminalStateEnc;
        terminalStateEnc = new StateEncoding("#" + PropositionConstants.SETTING.terminalProposition);
        while (resultSet.hasNext()) {
            OResult result = resultSet.next();
            // we're expecting a vertex
            if (result.isVertex()) {
                Optional<OVertex> op = result.getVertex();
                if (!op.isPresent()) continue;
                OVertex stateVertex = op.get();
                StateEncoding senc = new StateEncoding(stateVertex.getIdentity().toString());
                Set<String> propositions = new LinkedHashSet<>();
                boolean terminalState;
                Iterable<OEdge> outedges = stateVertex.getEdges(ODirection.OUT, "ConcreteAction"); //could be a SQL- like query as well
                Iterator<OEdge> edgeiter = outedges.iterator();
                terminalState = !edgeiter.hasNext();

                if (terminalState) {
                    logTerminalStates.add(stateVertex.getIdentity().toString() );
                    //tModel.addLog("State: " + stateVertex.getIdentity().toString() + " is terminal.");
                    if (instrumentTerminalState && firstTerminalState) {
                        //add stateenc for 'Dead', inclusive dead transition selfloop;
                        //terminalStateEnc = new StateEncoding("#" + TemporalModel.getDeadProposition());
                        Set<String> terminalStatePropositions = new LinkedHashSet<>();
                        //terminalStatePropositions.add("dead");   //redundant on transition based automatons
                        terminalStateEnc.setStateAPs(terminalStatePropositions);
                        TransitionEncoding deadTrenc = new TransitionEncoding();
                        deadTrenc.setTransition(PropositionConstants.SETTING.terminalProposition + "_selfloop");
                        deadTrenc.setTargetState(terminalStateEnc.getState());//"#" + TemporalModel.getDeadProposition());
                        Set<String> deadTransitionPropositions = new LinkedHashSet<>();
                        deadTransitionPropositions.add(PropositionConstants.SETTING.terminalProposition);
                        deadTrenc.setTransitionAPs(deadTransitionPropositions);
                        List<TransitionEncoding> deadTrencList = new ArrayList<>();
                        deadTrencList.add(deadTrenc);
                        terminalStateEnc.setTransitionColl(deadTrencList);
                        tModel.addStateEncoding(terminalStateEnc, false);
                        firstTerminalState = false;
                    }
                    if (!instrumentTerminalState)
                        stateVertex.setProperty(TagBean.IsTerminalState.name(), true);  //candidate for refactoring
                }
                for (String propertyName : stateVertex.getPropertyNames()) {
                    tDBManager.computeAtomicPropositions(tModel.getApplication_BackendAbstractionAttributes(),propertyName, stateVertex, propositions, false);
                }
                PairBean<Set<String>,Integer> pb = tDBManager.getWidgetPropositions(senc.getState(), tModel.getApplication_BackendAbstractionAttributes());
                propositions.addAll(pb.left());// concrete widgets
                commentWidgetDistri.put(senc.getState(),pb.right());
                runningWcount=runningWcount+ pb.right();
                senc.setStateAPs(propositions);
                if (instrumentTerminalState && terminalState) {
                    TransitionEncoding deadTrenc = new TransitionEncoding();
                    deadTrenc.setTransition(terminalStateEnc.getState() + "_" + stateVertex.getIdentity().toString());
                    deadTrenc.setTargetState(terminalStateEnc.getState());//"#" + TemporalModel.getDeadProposition());
                    Set<String> deadTransitionPropositions = new LinkedHashSet<>();
                    deadTransitionPropositions.add(PropositionConstants.SETTING.terminalProposition);
                    deadTrenc.setTransitionAPs(deadTransitionPropositions);
                    List<TransitionEncoding> deadTrencList = new ArrayList<>();
                    deadTrencList.add(deadTrenc);
                    senc.setTransitionColl(deadTrencList);
                } else senc.setTransitionColl(tDBManager.getTransitions(senc.getState(),tModel.getApplication_BackendAbstractionAttributes()));

                tModel.addStateEncoding(senc, false);
            }
        stateCount++;
        if (stateCount % (Math.floorDiv(totalStates, chunks)) == 0){
            simpleLog.append(prettyCurrentTime() + " | " + "States processed: "+Math.floorDiv((100*stateCount),totalStates)+"%");
        }
        }


        resultSet.close();
        tModel.finalizeTransitions(); //update once. this is a costly operation
        for (StateEncoding stenc : tModel.getStateEncodings()
        ) {
            List<String> encodedConjuncts = new ArrayList<>();
            for (TransitionEncoding tren : stenc.getTransitionColl()
            ) {
                String enc = tren.getEncodedTransitionAPConjunct();
                if (encodedConjuncts.contains(enc)) {
                    logNonDeterministicTransitions.put(stenc.getState(),tren.getTransition());
                    //tModel.addLog("State: " + stenc.getState() + " has  non-deterministic transition: " + tren.getTransition());
                } else encodedConjuncts.add(enc);
            }
        }


        tModel.addLog("Terminal States : "+logTerminalStates.toString());
        String mapAsString = commentWidgetDistri.keySet().stream()
                .map(key -> key + "->" + commentWidgetDistri.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
        tModel.addComments("#Widgets per State : "+mapAsString);

        mapAsString = logNonDeterministicTransitions.keySet().stream()
                .map(key -> key + "->" + logNonDeterministicTransitions.get(key).toString())
                .collect(Collectors.joining(", ", "{", "}"));
        tModel.addLog("non-deterministic transitions per State: "+mapAsString);


        tModel.setTraces(tDBManager.fetchTraces(tModel.getApplication_ModelIdentifier()));
        Set<String> initStates = new HashSet<>();
        for (TemporalTrace trace : tModel.getTraces()
        ) {
            TemporalTraceEvent traceevent = trace.getTraceEvents().get(0);
            initStates.add(traceevent.getState());
        }
        tModel.setInitialStates(initStates);
        tModel.addComments("Total #Widgets = "+runningWcount);

        simpleLog.append(prettyCurrentTime() + " | " + "Total States : "+tModel.getStateList().size());
        simpleLog.append(prettyCurrentTime() + " | " + "Total Atomic Propositions detected : "+tModel.getModelAPs().size());
        simpleLog.append(prettyCurrentTime() + " | " + "Model has "+(logTerminalStates.size()==0?"no":""+logTerminalStates.size())+ " terminal states");
        simpleLog.append(prettyCurrentTime() + " | " + "Model has "+tModel.getInitialStates().size()+ " initial states");

        long end_time = System.currentTimeMillis();
        long difference = (end_time-start_time)/1000;
        tModel.addComments("Duration to create the model:"+difference +" (s)" );
        tDBManager.dbClose();

    }


    private AbstractStateModel getAbstractStateModel() {
        AbstractStateModel abstractStateModel;
        abstractStateModel = tDBManager.selectAbstractStateModelByModelId(Modelidentifier);
        if (abstractStateModel == null) {
            tModel.addLog("ERROR: Model with identifier : " + Modelidentifier + " was not found in the graph database <" + tDBManager.getDatabase()+">");
            simpleLog.append("ERROR: Model with identifier : " + Modelidentifier + " was not found in the graph database <" + tDBManager.getDatabase()+">");
        }
        return abstractStateModel;
    }


    public boolean saveToGraphMLFile(String file, boolean excludeWidget) {
        simpleLog.append(prettyCurrentTime() + " | " + "generating "+file+" file");
        AbstractStateModel abstractStateModel = getAbstractStateModel();
        if (abstractStateModel != null) {
            return tDBManager.saveToGraphMLFile(abstractStateModel, outputDir + file, excludeWidget);
        } else return false;
    }

    private void saveModelAsJSON(String toFile) {
        simpleLog.append(prettyCurrentTime() + " | " + "generating Model file: "+toFile);
        JSONHandler.save(tModel, outputDir + toFile);
    }



    public void MCheck() {

        MCheck(propositionManagerFile, oracleFile, verbose, counterExamples, instrumentDeadlockState,
                ltlSPOTMCCommand, ltlSPOTToWSLPath, ltlSPOTEnabled,
                ctlITSMCCommand, ctlITSToWSLPath, ctlITSEnabled,
                ltlITSMCCommand, ltlITSToWSLPath, ltlITSEnabled,
                ltlLTSMINMCCommand, ltlLTSMINToWSLPath, ltlLTSMINEnabled,
                ctlGALMCCommand, ctlGALToWSLPath, ctlGALEnabled,
                ctlLTSMINMCCommand, ctlLTSMINToWSLPath, ctlLTSMINEnabled);

    }


    public void MCheck(String propositionManagerFile, String oracleFile,
                       boolean verbose, boolean counterExamples, boolean instrumentTerminalState,
                       String ltlSpotMCCommand, boolean ltlSpotWSLPath, boolean ltlSpotEnabled,
                       String ctlItsMCCommand,  boolean ctlItsWSLPath, boolean ctlItsEnabled,
                       String ltlItsMCCommand, boolean ltlItsWSLPath, boolean ltlItsEnabled,
                       String ltlLtsminMCCommand, boolean ltlLtsminWSLPath, boolean ltlltsminEnabled,
                       String ctlGalMCCommand, boolean ctlGalWSLPath, boolean ctlGalEnabled,
                       String ctlLtsminMCCommand, boolean ctlLtsminWSLPath, boolean ctlltsminEnabled
                       ) {
        try {

            simpleLog.append(prettyCurrentTime() + " | " + "Temporal model-checking started");
            List<TemporalOracle> fromcoll = CSVHandler.load(oracleFile, TemporalOracle.class);
            if (fromcoll == null) {
                simpleLog.append(prettyCurrentTime()+"Error: verify the file at location '" + oracleFile + "'");
            } else {
                tModel = new TemporalModel();
                AbstractStateModel abstractStateModel = getAbstractStateModel();
                if (abstractStateModel == null){
                    simpleLog.append("Error: StateModel not available");
                }
                else {
                String OracleCopy = "copy_of_applied_" + Paths.get(oracleFile).getFileName().toString();
                if (verbose) {
                    Files.copy((new File(oracleFile).toPath()),
                            new File(outputDir + OracleCopy).toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                String strippedFile;
                String filename = Paths.get(oracleFile).getFileName().toString();
                if (filename.contains(".")) strippedFile = filename.substring(0, filename.lastIndexOf("."));
                else strippedFile = filename;
                File inputvalidatedFile = new File(outputDir + strippedFile + "_inputvalidation.csv");
                File modelCheckedFile = new File(outputDir + strippedFile + "_modelchecked.csv");


                makeTemporalModel(propositionManagerFile, verbose, instrumentTerminalState);
                setOracleColl(fromcoll);

                Map<TemporalFormalism, List<TemporalOracle>> oracleTypedMap =fromcoll.stream().collect(Collectors.groupingBy(TemporalOracle::getPatternTemporalType));

                if (verbose) {
                saveToGraphMLFile("GraphML.XML", false);
                saveToGraphMLFile("GraphML_NoWidgets.XML", true);
                }
                List<TemporalOracle> initialoraclelist = new ArrayList<>();
                List<TemporalOracle> finaloraclelist = new ArrayList<>();
                    FormulaVerifier.INSTANCE.setPathToExecutable(ltlSpotMCCommand);
                    FormulaVerifier.INSTANCE.setToWslPath(ltlSpotWSLPath);
                for (Map.Entry<TemporalFormalism, List<TemporalOracle>> oracleentry : oracleTypedMap.entrySet()
                ) {
                    List<TemporalOracle> modelCheckedOracles = null;
                   TemporalFormalism oracleType = oracleentry.getKey();
                    List<TemporalOracle> oracleList = oracleentry.getValue();
                    List<TemporalOracle> acceptedOracleList = oracleList.stream().
                                    filter(o -> (o.getOracle_validationstatus() ==ValStatus.ACCEPTED ||
                                    o.getOracle_validationstatus() ==ValStatus.CANDIDATE)).
                                    collect(Collectors.toList());
                    List<TemporalOracle> rejectedOracleList = oracleList.stream().
                            filter(o -> !(o.getOracle_validationstatus() ==ValStatus.ACCEPTED ||
                                    o.getOracle_validationstatus() ==ValStatus.CANDIDATE)).
                            collect(Collectors.toList());
                    initialoraclelist.addAll(acceptedOracleList);
                    initialoraclelist.addAll(rejectedOracleList);

                    simpleLog.append(prettyCurrentTime() + " | " + oracleType + " invoking the " + "backend model-checker");

                    ModelChecker checker = CheckerFactory.getModelChecker(oracleType);
                    checker.setupModel( verbose,counterExamples,outputDir,tModel,acceptedOracleList);

                    if (ltlSpotEnabled && ( oracleType == TemporalFormalism.LTL_SPOT)) {
                        checker.setExecutable(ltlSpotMCCommand, ltlSpotWSLPath);
                        modelCheckedOracles = checker.modelcheck();

                    }
                    if (ltlItsEnabled && (oracleType == TemporalFormalism.LTL_ITS)){
                         checker.setExecutable(ltlItsMCCommand, ltlItsWSLPath);
                         modelCheckedOracles = checker.modelcheck();
                        }

                    if(ltlltsminEnabled && (oracleType == TemporalFormalism.LTL_LTSMIN)){
                         checker.setExecutable(ltlLtsminMCCommand, ltlLtsminWSLPath);
                         modelCheckedOracles = checker.modelcheck();
                     }
                    if(ctlltsminEnabled && (oracleType == TemporalFormalism.CTL_LTSMIN)){
                        int maxap=450;
                        int maxstate=25000;
                        if (tModel.getModelAPs().size()>maxap ||tModel.getStateList().size()>maxstate){
                            simpleLog.append(prettyCurrentTime() + " | " + oracleType +
                                    " Warning:  model check is not executed: explicit model too complex (propositions>"+maxap+" or states>"+maxstate);
                        }
                        else{
                            checker.setExecutable(ctlLtsminMCCommand, ctlLtsminWSLPath);
                            modelCheckedOracles = checker.modelcheck();
                            simpleLog.append(prettyCurrentTime() + " | " + oracleType + " verifying results for this Model checker is not possible yet");
                        }

                    }

                    if (ctlItsEnabled &&  ( oracleType == TemporalFormalism.CTL_ITS)) {
                        checker.setExecutable(ctlItsMCCommand, ctlItsWSLPath);
                        modelCheckedOracles = checker.modelcheck();
                    }
                    if (ctlGalEnabled &&  (oracleType == TemporalFormalism.CTL_GAL )) {
                        int maxap=200;
                        int maxstate=25000;
                        if (tModel.getModelAPs().size()>maxap ||tModel.getStateList().size()>maxstate){
                            simpleLog.append(prettyCurrentTime() + " | " + oracleType +
                                    " Warning:  model check is not executed: explicit model too complex (propositions>"+maxap+" or states>"+maxstate);
                        }
                        else{
                            checker.setExecutable(ctlGalMCCommand, ctlGalWSLPath);
                            modelCheckedOracles = checker.modelcheck();
                            simpleLog.append(prettyCurrentTime() + " | " + oracleType + " verifying results for this Model checker is not possible yet");
                        }
                    }

                    if(!(
                            (ltlSpotEnabled && (oracleType == TemporalFormalism.LTL_SPOT))||
                            (ltlltsminEnabled && (oracleType == TemporalFormalism.LTL_LTSMIN))||
                            (ctlltsminEnabled && (oracleType == TemporalFormalism.CTL_LTSMIN))||
                            (ltlItsEnabled && (oracleType == TemporalFormalism.LTL_ITS))||
                            (ctlItsEnabled &&  (oracleType == TemporalFormalism.CTL_ITS))||
                            (ctlGalEnabled &&  (oracleType == TemporalFormalism.CTL_GAL))
                        ))
                    {
                        simpleLog.append(prettyCurrentTime() + " | " + oracleType + " Warning:  this oracle type is not implemented or disabled");
                    }

                    if (modelCheckedOracles != null) {
                        finaloraclelist.addAll(modelCheckedOracles);
                    }
                    else {
                        simpleLog.append(prettyCurrentTime() + " | " + oracleType + "  ** Error: no results from the model-checker");
                    }
                    finaloraclelist.addAll(rejectedOracleList);
                    if (!verbose && inputvalidatedFile.exists())    Files.delete(inputvalidatedFile.toPath());
                    simpleLog.append(prettyCurrentTime() + " | " + oracleType + " model-checking completed");
                }
                CSVHandler.save(initialoraclelist, inputvalidatedFile.getAbsolutePath());
                if (finaloraclelist.size() != fromcoll.size()) {
                    simpleLog.append(prettyCurrentTime() + " | " + "** Warning: less oracle verdicts " +
                            "received than requested in: "+ Paths.get(oracleFile).getFileName());
                }
                CSVHandler.save(finaloraclelist, modelCheckedFile.getAbsolutePath());
            }
            }
            simpleLog.append(prettyCurrentTime() + " | " + "Temporal model-checking completed");
        } catch (Exception f) {
            f.printStackTrace();
        }
    }

    public void makeTemporalModel(String propositionManagerFile, boolean verbose, boolean instrumentTerminalState) {
        try {
            simpleLog.append(prettyCurrentTime() + " | " + "compute temporal model started");
            tModel = new TemporalModel();

            AbstractStateModel abstractStateModel = getAbstractStateModel();
            if (abstractStateModel == null) {
                simpleLog.append("Error: StateModel not available");
            } else {
                setTemporalModelMetaData(abstractStateModel);
                if (propositionManagerFile.equals("")) {
                    setDefaultPropositionManager();
                    savePropositionManager("PropositionManager_default.json");
                }
                else {
                    String APCopy = "copy_of_applied_" + Paths.get(propositionManagerFile).getFileName().toString();
                    if (verbose) {
                        Files.copy((new File(propositionManagerFile).toPath()),
                                new File(outputDir + APCopy).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                loadPropositionManager(propositionManagerFile);
                settModel(abstractStateModel, instrumentTerminalState);
                if (verbose) {
                    saveModelAsJSON("PropositionEncodedModel.json");
                }

                simpleLog.append(prettyCurrentTime() + " | " + "compute temporal model completed");
            }
        } catch (Exception f) {
            f.printStackTrace();
        }

    }

    public void generateOraclesFromPatterns(String propositionManagerfile, String patternFile, String patternConstraintFile, int tactic_oraclesPerPattern) {
        try {
            simpleLog.append(" potential Oracle generator started \n");
            makeTemporalModel(propositionManagerfile, false, true);
            List<TemporalPattern> patterns = CSVHandler.load(patternFile, TemporalPattern.class);
            List<TemporalPatternConstraint> patternConstraints = null;
            if (!patternConstraintFile.equals("")) {
                patternConstraints = CSVHandler.load(patternConstraintFile, TemporalPatternConstraint.class);
            }

            File PotentialoracleFile = new File(outputDir + "TemporalPotentialOracles.csv");

            List<TemporalOracle> fromcoll;
            assert patterns != null;
            fromcoll = getPotentialOracles(patterns, patternConstraints, tactic_oraclesPerPattern);
            CSVHandler.save(fromcoll, PotentialoracleFile.getAbsolutePath());

            simpleLog.append(" potential Oracle generator completed \n");
        } catch (Exception f) {
            f.printStackTrace();
        }

    }


    private List<TemporalOracle> getPotentialOracles(List<TemporalPattern> patterns, List<TemporalPatternConstraint> patternConstraints, int tactic_oraclesPerPattern) {
        // there is no check on duplicate assignments:  a pattern can emerge more than once with exactly the same assignments.
        // the likelyhood is few percent due to the randomness on AP selection and e=randomness on constraint-set selection.
        // the impact is low as a duplicate oracle will be executed , only twice!
        // refactor to Set? nr of oracles will then be less than the 'tactic'
        List<TemporalOracle> potentialOracleColl = new ArrayList<>();
        List<String> modelAPSet = new ArrayList<>(tModel.getModelAPs());
        int trylimitConstraint = Math.min(250, 2 * modelAPSet.size());
        Random APRnd = new Random(5000000);
        for (TemporalPattern pat : patterns
        ) {
            Map<String, String> ParamSubstitutions;
            TemporalPatternConstraint patternConstraint = null;
            int patcIndex;
            TreeMap<Integer, Map<String, String>> constrainSets = null;
            boolean passConstraint = false;
            Random constraintRnd = new Random(6000000);
            int cSetindex = -1;
            Map<String, String> constraintSet;
            patcIndex = -1;
            if (patternConstraints != null) {
                for (int h = 0; h < patternConstraints.size(); h++) {
                    patternConstraint = patternConstraints.get(h);
                    if (pat.getPattern_Formula().equals(patternConstraint.getPattern_Formula())) {
                        patcIndex = h;
                        break;
                    }
                }
            }
            if (patcIndex != -1) {
                constrainSets = patternConstraint.getConstraintSets();
            }
            for (int i = 0; i < tactic_oraclesPerPattern; i++) {
                TemporalOracle potentialOracle = new TemporalOracle();
                if (constrainSets != null) {
                    cSetindex = constraintRnd.nextInt(constrainSets.size());//start set. constrainset number is 1,2,3,...
                }
                ParamSubstitutions = new HashMap<>();
                for (String param : pat.getPattern_Parameters()
                ) {
                    passConstraint = false;
                    String provisionalParamSubstitution;
                    if (constrainSets == null) {
                        provisionalParamSubstitution = modelAPSet.get(APRnd.nextInt(modelAPSet.size() - 1));
                        ParamSubstitutions.put(param, provisionalParamSubstitution);
                        passConstraint = true;  //virtually true
                    } else {
                        for (int k = 1; k < constrainSets.size() + 1; k++) {//constrainset number is 1,2,3,...
                            int ind = (k + cSetindex) % (constrainSets.size() + 1);
                            constraintSet = constrainSets.get(ind);
                            if (constraintSet.containsKey(param)) {
                                Pattern regexPattern = CachedRegexPatterns.addAndGet(constraintSet.get(param));
                                if (regexPattern == null) {
                                    continue; //no pass for this constraint-set due to invalid pattern
                                } else {
                                    for (int j = 0; j < trylimitConstraint; j++) {
                                        provisionalParamSubstitution = modelAPSet.get(APRnd.nextInt(modelAPSet.size() - 1));
                                        Matcher m = regexPattern.matcher(provisionalParamSubstitution);
                                        if (m.matches()) {
                                            ParamSubstitutions.put(param, provisionalParamSubstitution);
                                            passConstraint = true;
                                            break;// go to next parameter
                                        }
                                    }
                                }
                            } else {
                                provisionalParamSubstitution = modelAPSet.get(APRnd.nextInt(modelAPSet.size() - 1));
                                ParamSubstitutions.put(param, provisionalParamSubstitution);
                                passConstraint = true;  //virtually true
                                break;// go to next parameter
                            }
                            if (passConstraint) {
                                break;
                            }
                        }
                    }
                }
                potentialOracle.setPatternBase(pat); //downcasting of pat
                potentialOracle.setApplicationName(tModel.getApplicationName());
                potentialOracle.setApplicationVersion(tModel.getApplicationVersion());
                potentialOracle.setApplication_AbstractionAttributes(tModel.getApplication_AbstractionAttributes());
                potentialOracle.setApplication_ModelIdentifier(tModel.getApplication_ModelIdentifier());
                if (passConstraint) { //assignment found, save and go to next round for a pattern
                    if (cSetindex != -1) {
                        potentialOracle.setPattern_ConstraintSet(cSetindex + 1);// sets numbers from 1,2,3,...
                    }
                    MultiValuedMap<String, String> pattern_Substitutions = new HashSetValuedHashMap<>();
                    for (Map.Entry<String, String> paramsubst : ParamSubstitutions.entrySet()
                    ) {
                        pattern_Substitutions.put("PATTERN_SUBSTITUTION_" + paramsubst.getKey(), paramsubst.getValue());// improve?
                    }
                    potentialOracle.setPattern_Substitutions(pattern_Substitutions);
                    potentialOracle.setOracle_validationstatus(ValStatus.CANDIDATE);
                } else {
                    // no assignment found
                    potentialOracle.setOracle_validationstatus(ValStatus.ERROR);
                    potentialOracle.addLog("No valid assignment of substitutions found. Advise: review ConstraintSets");
                }
                potentialOracleColl.add(potentialOracle);
            }
        }
        return potentialOracleColl;
    }
}


