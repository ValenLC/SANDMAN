/**
 * 
 */
package core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import agent.WeightAgent;
import core.AnoMASly.EventClass;
import core.AnoMASly.Feedback;
import core.logging.LogSandman;
import input.SensorDB;
import javafx.util.Pair;
import profile.BuildingProfile;
import profile.Profile;
import time.Slice;
import tools.LimitedQueue;

/**
 * @author Maxime Houssin - maxime.houssin@irit.fr
 *
 */
public class ModuleContext {

	public AnoMASly classifier;
	public AnoMASly constantClassifier;
	public ModuleProfile moduleProfile;
	private SensorDB sensorDB;
	private Double classifierPrecision;
	private Integer cycle = 0;
	private int classifierMaxTries;
	private LogSandman logSandman;
	private Slice sliceBeforeLearning;
	private  Set<AnomalyCharacterisation> inhibitionSet = new HashSet<AnomalyCharacterisation>();
	private NavigableMap<Slice, EventClass> classificationHistory = new TreeMap<Slice, EventClass>();
	private NavigableMap<Slice, EventClass> feedbackHistory = new TreeMap<Slice, EventClass>();
	private Map<Slice, List<AnomalyCharacterisation>> inhibitedSituations = new HashMap<>();
	private int nbSliceDisparity;
	
	private boolean useContextUpdate = true;
	private boolean useProfileUpdate = true;
	private LimitedQueue<Double> previousSituationsDA;
	public List<AnomalyCharacterisation> currentRelevantAC= new ArrayList<>();
	
	
	private NavigableMap<Slice, String> expertClassificationHistory = new TreeMap<Slice, String>();

	private List<String> sensorNames;

	public String currentProfil = "profil0";
	
	private Map<Slice, Double> disparityValuesALL = new HashMap<Slice, Double>();
	private String previousState = "TN";
	
	private Map<Slice, String> situationsToAdd = new HashMap<Slice, String>();
	private Slice firstSliceToAdd;
	private boolean weightUpdate = false;

	private Map<Slice, String> profileHistory = new HashMap<Slice, String>();
	private static int FP = 0; 
	private static int FN = 0;
	private static int TP = 0;
	private static int TN = 0;
	private static Map<Slice, String> historyState = new HashMap<Slice, String>();
	
	private int jourSemaine=-1;
	
	private static double lambda = 0.25;
	
	public Map<Integer, Double> getProfile(String SensorName) {
		return moduleProfile.getProfile(currentProfil).getSensorProfiles().get(SensorName);
	}

	private Map<Slice, Double> historyDA = new HashMap<Slice, Double>();

	
	/**
	 * Construct a new classification module
	 * 
	 * @param sensorDB
	 *            Database containing every values of each sensor at each slice
	 * @param logSandman
	 *            LogSandman.
	 * @param classifierPrecision
	 *            The value of the precision for the classifier 
	 * @param classifierMaxTries
	 *            The maximum number of cycle resolution.	
	 * @param slicesBeforeLearning
	 *            Slice which start the learning
	 * @param nbSliceDisparity
	 *            The number of disparity slice 
	 * @param previousDisparityTotalLimit		
	 */
	public ModuleContext(SensorDB sensorDB, LogSandman logSandman, double classifierPrecision, int classifierMaxTries, 
						Slice slicesBeforeLearning, int nbSliceDisparity, int previousDisparityTotalLimit) {
		this.sensorDB = sensorDB;
		this.logSandman = logSandman;
		this.classifierPrecision = classifierPrecision;
		this.classifierMaxTries = classifierMaxTries;
		this.sliceBeforeLearning = slicesBeforeLearning;
		this.nbSliceDisparity = nbSliceDisparity;
		this.previousSituationsDA = new LimitedQueue<>(previousDisparityTotalLimit);
		
		logSandman.setClassificationHistory(getClassificationHistory());
		logSandman.setFeedbackHistory(getFeedbackHistory());
	}
	
	/**
	 * Initialize the classifier
	 * 
	 * @param sensors
	 *            List containing each sensor name
	 * @param alertThreshold
	 *            The value to fix the alert threshold.
	 */
	public void init(List<String> sensors, int alertThreshold) {
		this.classifier = new AnoMASly(sensors, alertThreshold);
	 	this.sensorNames = sensors;
    	
	}

	/**
	 * Compute the classification for a slice
	 * 
	 * @param slice
	 *
	 * @return The classification (EventClass.ALERT or EventClass.REGULAR)
	 *            
	 */
	public EventClass computeClassification(Slice slice) {
		EventClass classification = EventClass.REGULAR;
		Map<String, Double> disparityValues = computeSumDisparityValues(slice, sensorNames, 1, currentProfil);
        double situationDA = moduleProfile.getProfile(currentProfil).getClassifierSituation().computeSituationDA(disparityValues);
        historyDA.put(slice, situationDA);		
        disparityValuesALL.put(slice,disparityValues.values().stream().reduce(0.0, Double::sum));
		System.out.println("situation DA : "+situationDA);
		if (!slice.isBefore(sliceBeforeLearning)) {
			if (situationDA >= 1000 ) {
				classification = EventClass.ALERT;
			}
		}
			
		return classification;
	}
	
	/**
	 * Compute the classification for a slice with a specific profile
	 * 
	 * @param slice
	 * @param profileName
	 * 				The profile name
	 * 
	 * @return The classification (EventClass.ALERT or EventClass.REGULAR) 
	 * 			and the degree of anomaly       
	 */
    public Pair<EventClass,Double> computeClassificationProfile(Slice slice, String profileName) {
    	EventClass classification = EventClass.REGULAR;
		Map<String, Double> disparityValues = computeSumDisparityValues(slice, sensorNames, 1, profileName);
        double situationDA = moduleProfile.getProfile(profileName).getClassifierSituation().computeSituationDA(disparityValues);
		System.out.println("situation DA : "+situationDA);
		if (situationDA >= 1000 ) {
			classification =EventClass.ALERT;
		}
		Pair<EventClass,Double> result=new Pair<EventClass, Double>(classification, situationDA);
		
		return result;
	}
    

    public void FN_resolution(Slice lastSlice, Slice sliceBegin, String anomaly, String profileName){

    	Map<String, Double> disparityValues = computeUpdatedSumDisparityValues(lastSlice, this.sensorNames, this.nbSliceDisparity, sliceBegin, profileName);
    	moduleProfile.getProfile(profileName).getClassifierSituation().createSituation(getFeedbackHistory().get(lastSlice), disparityValues, lastSlice);
    	solveSlice(lastSlice, anomaly, profileName);
    }
    
    public void FP_resolution(Slice lastSlice, Slice sliceBegin, String anomaly, String profileName){

    	Map<String, Double> disparityValues = computeUpdatedSumDisparityValues(lastSlice, this.sensorNames, this.nbSliceDisparity, sliceBegin, profileName);
    	moduleProfile.getProfile(profileName).getClassifierSituation().createSituation(getFeedbackHistory().get(lastSlice), disparityValues, lastSlice);
    	solveSlice(lastSlice, anomaly, profileName);
    	updateProfileValues(lastSlice,profileName);

    }

	/**
	 * Resolution of the expert feedback 
	 * 
	 * @param slice
	 * @param anomaly
	 * 			Expert classification (INCORRECT : Classification system was wrong,
	 * 			VALID : The system was right
	 * @param profileName
	 * 				The profile name
	 * 
	 * @return The classification (EventClass.ALERT or EventClass.REGULAR) 
	 * 			and the degree of anomaly       
	 */
    public void newFeedback(Slice slice, String anomaly, String profileName) {
    	// case False Negative (FN)
		if (anomaly.equals("INCORRECT") && getClassificationHistory().get(slice).equals(EventClass.REGULAR)) {
		
			getFeedbackHistory().put(slice, EventClass.ALERT);
		
			currentProfil=moduleProfile.switchProfile(slice, profileName );
			EventClass classification = computeClassification(slice);
			getClassificationHistory().put(slice, classification);
			System.out.println(classification+ slice.toString());
			
			if (classification.equals(EventClass.ALERT)) {
				return ;
			
			}
			if (previousState.equals("TP")){
				
				situationsToAdd.clear();
				previousState = "FN";
			
			}
			else if (!situationsToAdd.isEmpty()){
			
				String savecurrentProfil = profileName;
				for (Slice sliceToAdd : situationsToAdd.keySet()) {
					currentProfil = situationsToAdd.get(sliceToAdd);
					FN_resolution(sliceToAdd, firstSliceToAdd, "ANOMALY", currentProfil);
				
				}
				situationsToAdd.clear();
				currentProfil = savecurrentProfil;
				firstSliceToAdd = slice;
				situationsToAdd.put(slice,currentProfil);
				previousState = "FN";
			}
			else {
			
				firstSliceToAdd = slice;
				situationsToAdd.put(slice,profileName);
				previousState = "FN";
			}
		} 
		
		// case True Negative (TN)
		else if ((anomaly.equals("VALID")|| anomaly.equals("DERIVE")) && getClassificationHistory().get(slice).equals(EventClass.REGULAR)) {
			getFeedbackHistory().put(slice, EventClass.REGULAR);
		
		
			if (previousState.equals("FN") ){
				String savecurrentProfil = profileName;
				for (Slice sliceToAdd : situationsToAdd.keySet()) {
					currentProfil = situationsToAdd.get(sliceToAdd);
					FN_resolution(sliceToAdd, firstSliceToAdd, "ANOMALY", currentProfil);
				
				}
				situationsToAdd.clear();
				currentProfil = savecurrentProfil;
			}
			
			if (!slice.isBefore(sliceBeforeLearning)) {
				
				Map<String, Double> disparityValues = computeUpdatedSumDisparityValues(slice, this.sensorNames, this.nbSliceDisparity, slice, profileName);
				System.out.println(disparityValues);
				moduleProfile.getProfile(profileName).getClassifierSituation().createSituation(getFeedbackHistory().get(slice), disparityValues, slice);
				
				
				updateProfileValues(slice, profileName);
				
			}
			previousState = "TN";
		}
		
		// case True Positive (TP)
		else if ((anomaly.equals("VALID") || anomaly.equals("DERIVE")) && getClassificationHistory().get(slice).equals(EventClass.ALERT)) {
		
			getFeedbackHistory().put(slice, EventClass.ALERT);
			
			String anomalySensor = createAnomalySensor(moduleProfile.getProfile(profileName).getClassifierSituation().getWeightAgentMap(),  computeSumDisparityValues(slice,sensorNames, 1, profileName) );
			if (!moduleProfile.getProfile(profileName).getAnomalySensors().contains(anomalySensor)) {
				List <String> anomalySensors = moduleProfile.getProfile(profileName).getAnomalySensors();
				anomalySensors.add(anomalySensor);
				moduleProfile.getProfile(profileName).setAnomalySensors(anomalySensors); 
			}
				   
			if (previousState.equals("FN")){
				String savecurrentProfil = profileName;
				for (Slice sliceToAdd : situationsToAdd.keySet()) {
    				if (!sliceToAdd.equals(slice.substractSlices(1)))
    					currentProfil = situationsToAdd.get(sliceToAdd);
    					FN_resolution(sliceToAdd, firstSliceToAdd, "ANOMALY", currentProfil);
				
				}
				situationsToAdd.clear();
				previousState = "TP";
				currentProfil = savecurrentProfil;
			}
			else {
				situationsToAdd.clear();
				previousState = "TP";
				
			}
			
		}
		
		// case False Positive (FP)
		else if (anomaly.equals("INCORRECT") && getClassificationHistory().get(slice).equals(EventClass.ALERT)) {
			
			getFeedbackHistory().put(slice, EventClass.REGULAR);
	
			if (previousState.equals("FN") ){
				String savecurrentProfil = profileName;
			//	situationsToAdd.remove(situationsToAdd.size()-1);
				for (Slice sliceToAdd : situationsToAdd.keySet()) {
					currentProfil = situationsToAdd.get(sliceToAdd);
					FN_resolution(sliceToAdd, firstSliceToAdd, "ANOMALY", currentProfil);
				
				}
				situationsToAdd.clear();
				currentProfil = savecurrentProfil;
			}
			
			FP_resolution(slice,slice, "NORMAL", profileName);
			previousState = "FP";
			
		}

	}

    
    
	/**
	 * Analyze the new expert feedback 
	 * 
	 * @param slice
	 * @param isAnom
	 * 			True = anomaly, False = normal
	 * @param expertClassification
	 *      
	 */
    public void readFeedback(Slice slice, boolean isAnom, String expertFeedback){
    	
        boolean isAnomalie = false;
 
    	expertClassificationHistory.put(slice, expertFeedback);
        
        if (classificationHistory.get(slice).equals( EventClass.ALERT)) {
        	isAnomalie = true;
        }
        else {
        	isAnomalie = false;
        }

        newFeedback(slice, expertFeedback, profileHistory.get(slice)); 
        if (isAnomalie==true && expertFeedback.equals("VALID")) {TP++;historyState.put(slice, "TP");}
        else if (isAnomalie==true && expertFeedback.equals("DERIVE")) {TN++;historyState.put(slice, "TP");}
        else if (isAnomalie==false && expertFeedback.equals("VALID")) {TN++;historyState.put(slice, "TN");}
        else if (isAnomalie==true && expertFeedback.equals("INCORRECT")) {FP++;historyState.put(slice, "FP");}
        else if (isAnomalie==false && expertFeedback.equals("INCORRECT")) {FN++;historyState.put(slice, "FN");}
        else if (isAnomalie==false && expertFeedback.equals("NORMAL")) {TN++;historyState.put(slice, "TN");}
        System.out.println("\n"+"TP : "+TP+", TN : "+TN+", FP : "+FP+", FN : "+FN+"\n" );
    
    
    	
    }
    
    
    
	/**
	 * Creation of a String for an encountered anomaly
	 * 
	 * @param weightAgentMap
	 *          A map containing the WeightAgents of each sensor indexed by
	 *          the sensor name.
	 * @param disparityValues
	 * 			A map describing the current disparity of each sensor compared
	 *          to their profile values.
	 *          
	 * @return String containing the names of the sensors responsible for the anomaly
	 * 			ordered by disparity
	 *      
	 */
    public String createAnomalySensor (HashMap<String, WeightAgent> weightAgentMap,   Map<String, Double> disparityValues) {
    	String anomalySensor ="";
    	TreeMap<String, Double> DASensors= new TreeMap<String, Double>(Collections.reverseOrder());
    	for (String sensorName : weightAgentMap.keySet()) {
    		DASensors.put(sensorName, weightAgentMap.get(sensorName).getMyWeight()*(disparityValues.get(sensorName)));
    	}
    	LinkedHashMap<String, Double> sortedDASensors = new LinkedHashMap<>();
    	
    	Double totalDA = 0.0;
    	
    	DASensors.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).forEachOrdered(x -> sortedDASensors.put(x.getKey(), x.getValue()));
    	
    	
    	for (String sensorName : sortedDASensors.keySet()) {
    		anomalySensor+=sensorName;
    		totalDA+=sortedDASensors.get(sensorName);
    		if (totalDA >=1000) {
    			return anomalySensor;
    		}
    	}
    	System.out.println(anomalySensor);
    	return anomalySensor;
    }
	
    
    /*
	private List<Slice> computeZone(Slice slice, NavigableMap<Slice, String> history, NavigableMap<Slice, String> historyOther) {
		
		Slice top = slice;
		Slice bot = slice;
		boolean search = true;
		

		while ((history.get(top) == "ALERT" || history.get(bot) == "ALERT")
				&& search) {

			if (history.get(top) == "ALERT") {
				top = top.substractSlices(1);
			}
			if (history.get(bot) == "ALERT") {
				bot = bot.addSlices(1);
			}
			if (!history.containsKey(top) || !history.containsKey(bot) || 
				!historyOther.containsKey(top) || !historyOther.containsKey(bot)) {
				search = false;
			}
		}

		if (search) {
			List<Slice> zone = new ArrayList<>();
			top = top.addSlices(1);

			while (top.isBefore(bot)) {
				zone.add(top);
				top = top.addSlices(1);
			}

			return zone;
		}

		return null;
	}
	*/

    /*
	private SortedSet<SimpleImmutableEntry<String, Slice>> sort1DMap(Map<String, Map<Slice, Double>> sensorsDA, Slice slice) {
		SortedSet<SimpleImmutableEntry<String, Slice>> sortedSet = new TreeSet<SimpleImmutableEntry<String, Slice>>(new Comparator<SimpleImmutableEntry<String, Slice>>() {
			@Override
			public int compare(SimpleImmutableEntry<String, Slice> e1, SimpleImmutableEntry<String, Slice> e2) {
				return - sensorsDA.get(e1.getKey()).get(e1.getValue()).compareTo(sensorsDA.get(e2.getKey()).get(e2.getValue()));
			}
		});

		for (String s : sensorsDA.keySet()) {
			sortedSet.add(new SimpleImmutableEntry<String, Slice>(s, slice));
		}

		return sortedSet;
	}
*/
    /*
	private Feedback computeFeedback(EventClass opinion, boolean anomaly) {
		Feedback feedback = null;
		if (opinion == EventClass.REGULAR && !anomaly) {
			feedback = AnoMASly.Feedback.TRUENEGATIVE;
		}
		if (opinion == EventClass.REGULAR && anomaly) {
			feedback = AnoMASly.Feedback.FALSENEGATIVE;
		}
		if (opinion == EventClass.ALERT && !anomaly) {
			feedback = AnoMASly.Feedback.FALSEPOSITIVE;
		}
		if (opinion == EventClass.ALERT && anomaly) {
			feedback = AnoMASly.Feedback.TRUEPOSITIVE;
		}
		return feedback;
	}
	*/
/*
	private void updatePreviousDAs(Map<String, Double> disparityValues) {
		previousSituationsDA.add(classifier.computeSituationDA(disparityValues));
	}

*/
/*
	private Map<String, Double> getInstantDisparityValues(Slice slice) {
		return buildingProfile.getInstantDisparityValues(slice);
	}

*/


    private void updateProfileValues(Slice slice, String profileName) {
    	for (Entry<String, Map<Integer, Double>> e : moduleProfile.getProfile(profileName).getSensorProfiles().entrySet()) {
			try {
				moduleProfile.getProfile(profileName).update(slice, sensorDB.getSensorValue(slice,e.getKey()),  e.getKey());
			} 
			catch (java.lang.NullPointerException exception) {
			}
			
		}
	}

    
	/**
	 * Perform the cycle of Weight update in case of FP or FN
	 * 
	 * @param slice
	 * @param anomaly
	 * @param profileName
	 *            
	 */
	private void solveSlice(Slice slice, String anomaly, String profileName) {
		
		cycle++;
		
		int tries = 0;
		boolean correctClassification = false;
	
		System.out.println("\n SOLVE SLICE "+slice);
		System.out.println("\nDA MaxCritSAN : "+ moduleProfile.getProfile(profileName).getClassifierSituation().getMaxCritSAN().getDA());
		System.out.println("DA MaxCritSAA : "+ moduleProfile.getProfile(profileName).getClassifierSituation().getMaxCritSAA().getDA());
	
		while (( moduleProfile.getProfile(profileName).getClassifierSituation().getMaxCritSAA().getDA() < 1000 || moduleProfile.getProfile(profileName).getClassifierSituation().getMaxCritSAN().getDA() > 1000 || !correctClassification ) 
				&&  tries++ < classifierMaxTries ) {
			System.out.println("==> tries"+tries);
			weightUpdate = true;
			moduleProfile.getProfile(profileName).getClassifierSituation().resolve();
		
			if ((anomaly.equals("VALID") && computeClassification(slice).equals(EventClass.ALERT))||(anomaly.equals("VALID") && computeClassification(slice).equals(EventClass.REGULAR))) {
				correctClassification = true;
			}
			else {
				correctClassification = false;
			}

			System.out.println("DA MaxCritSAN : "+moduleProfile.getProfile(profileName).getClassifierSituation().getMaxCritSAN() );
			System.out.println("DA MaxCritSAA : "+moduleProfile.getProfile(profileName).getClassifierSituation().getMaxCritSAA() );
			
			cycle++;
		
		}
		
			
	}
	
	
	private Map<String, Map<Slice,Double>> computeDisparityValues(Slice slice, List<String> sensorsList, int nbSliceDisparity, String profileName) {
		Map<String, Map<Slice, Double>> disparity_Values = new HashMap<>();
		for (String sensorName : sensorsList) {
			Map<Slice, Double> slices = sensorDB.getSensorLastValues(sensorName, slice, nbSliceDisparity);
			
			Map<Slice, Double> m = moduleProfile.getProfile(profileName).computeSensorDisparityValues(sensorName, slices);
			disparity_Values.put(sensorName, m);
		}
		return disparity_Values;
	}
    

    public Map<String, Double> computeSumDisparityValues(Slice slice, List<String> sensors2, int nbSliceDisparity, String profileName) {
        Map<String, Double> disparityValuesTotal = new HashMap<String, Double>();
        Map<String, Map<Slice, Double>> disparityValues = new HashMap<>();
        for (String sensorName : sensors2) {
            Map<Slice, Double> slices = sensorDB.getSensorLastValues(sensorName, slice, nbSliceDisparity);
          
            Map<Slice, Double> m = moduleProfile.getProfile(profileName).computeSensorDisparityValues(sensorName, slices);
            disparityValues.put(sensorName, m);
        }
        for (String sensorName : sensors2) {			
            Double sensorDisparity = disparityValues.get(sensorName).values().stream().reduce(0.0, Double::sum);
            disparityValuesTotal.put(sensorName, sensorDisparity);
        }

        return disparityValuesTotal;
    }
	private Map<String, Double> computeUpdatedSumDisparityValues(Slice slice, List<String> sensorsList, int nbSliceDisparity, Slice zoneBegin, String profileName) {
		Map<String, Map<Slice, Double>> disparityValues = computeDisparityValues(slice, sensorsList, this.nbSliceDisparity, profileName);
		// remove instant disparity values from known anomalous situations
		if (getFeedbackHistory().get(slice).equals(EventClass.REGULAR)) {
			Slice currentSlice = slice;
			for (int i = 0; i < nbSliceDisparity - 1; i++) {
				currentSlice = currentSlice.substractSlices(1);
				if (getFeedbackHistory().containsKey(currentSlice) && getFeedbackHistory().get(currentSlice).equals(EventClass.ALERT)) {
					for (String s : sensorsList) {
						disparityValues.get(s).put(currentSlice, 0.0);
					}
				}
			}
		}
		Map<String, Double> disparityValuesTotal = new HashMap<>();
		for (String sensorName : sensorsList) {			
			Double sensorDisparity = disparityValues.get(sensorName).values().stream().reduce(0.0, Double::sum);
			disparityValuesTotal.put(sensorName, sensorDisparity);
		}

		if (getFeedbackHistory().get(slice).equals(EventClass.ALERT)) {
			Set<String> sensorSet = computeInhibitionSensorsSet(slice, zoneBegin);
			for (String s : sensorSet) {
				disparityValuesTotal.put(s, 0.0);
			}		
		}
		
		return disparityValuesTotal;
	}
	
	private Set<String> computeInhibitionSensorsSet(Slice slice, Slice zoneBegin) {
		currentRelevantAC.clear();
		Set<String> setSensors = new TreeSet<>();

		for (AnomalyCharacterisation ac : inhibitionSet) {
			if (!ac.lastTimeAnomaly.isBefore(slice.substractSlices(nbSliceDisparity)) && ac.firstTimeAnomaly.isBefore(zoneBegin)) {
				currentRelevantAC.add(ac);
				setSensors.addAll(ac.computeListMostInfluencialSensors());
			}
		}
		return setSensors;
	}

	private void log(AnoMASly classifier, Slice slice, Feedback feedback ) {
		logSandman.addCycle(cycle, slice);
		logSandman.addConstraints(cycle, classifier.getSituationAgentsNormal(), classifier.getSituationAgentsAnomalous());
		logSandman.addWeights(cycle, classifier.getWeightAgentMap());
		if (feedback == Feedback.TRUEPOSITIVE || feedback == Feedback.FALSENEGATIVE) {
			logSandman.addAnomaly(slice);
		}
		logSandman.setThreshold(classifier.getAlertThreshold());
	}


	
	/**
	 * Compute the classification for the current slice
	 * 
	 * @param currentSlice
	 * 
	 * @return Classification done by the system (EventClass.ALERT or EventClass.REGULAR).
	 */
	public EventClass runClassification(Slice currentSlice) {
		
		if (currentSlice.equals(sliceBeforeLearning))
			currentProfil = "profil0";
	
		if (currentSlice.isBefore(sliceBeforeLearning) ) {
			
			if (currentSlice.getBegin().getHour()==0 ) {
	    		
				jourSemaine=(jourSemaine+1)%7;
	    		currentProfil =  "profil"+jourSemaine;
	    	    moduleProfile.listProfiles.put(currentProfil, new Profile(currentProfil, lambda,sensorNames, 1000, sensorNames.size())); 
	
	    	}
			System.out.println(currentProfil);
			System.out.println(moduleProfile.getProfile(currentProfil).getSensorProfiles());
	    	disparityValuesALL.put(currentSlice,0.0);
	    	historyDA.put(currentSlice, 0.0);
	    	historyState.put(currentSlice, "TN");
	    	Map<String, String> lastValue = sensorDB.getSliceValues(currentSlice);
	    	Profile tmpProfil = moduleProfile.getProfile(currentProfil);
			for (String sensorName : lastValue.keySet()) {
				tmpProfil.addSensorProfile(sensorName, Double.valueOf(lastValue.get(sensorName)), currentSlice);
			}
			moduleProfile.setProfile(currentProfil,tmpProfil);
			classificationHistory.put(currentSlice, EventClass.REGULAR);
			Map<String, Double> disparity_values = new HashMap<String, Double>();
			for (String sensorName : sensorNames) {
				disparity_values.put(sensorName, 0.0);
			}
			moduleProfile.getProfile(currentProfil).getClassifierSituation().createSituation(EventClass.REGULAR,  disparity_values,currentSlice);
		//	readFeedback(currentSlice,currentSlice, false, "NORMAL");
			profileHistory.put(currentSlice, currentProfil);
			System.out.println(moduleProfile.getProfile("profil0").getSensorProfiles());
			return EventClass.REGULAR;
	    }
		
		else {
	

	    	System.out.println(moduleProfile.getProfile(currentProfil).getSensorProfiles());
	    	EventClass classification = computeClassification(currentSlice);
	    	System.out.println(historyDA.get(currentSlice) + " > " +  moduleProfile.getProfile(currentProfil).getClassifierSituation().getSituationAgentsNormal_MEAN()*2);
	        if ( classification.equals(EventClass.ALERT) || historyDA.get(currentSlice)> moduleProfile.getProfile(currentProfil).getClassifierSituation().getSituationAgentsNormal_MEAN()*2){
	        	String anomalySensor = createAnomalySensor ( moduleProfile.getProfile(currentProfil).getClassifierSituation().getWeightAgentMap(),  computeSumDisparityValues(currentSlice,sensorNames, 1, currentProfil) );
	        	//if (!listProfiles.get(currentProfile).getAnomalySensors().contains(anomalySensor)) {
	        	currentProfil=moduleProfile.switchProfile(currentSlice, currentProfil);
	    		classification = computeClassification(currentSlice);
	        	//}
	        	
	        }
	        profileHistory.put(currentSlice, currentProfil);
	        
	
	        classificationHistory.put(currentSlice, classification);
	        System.out.println("classification Sandman : "+classification);
	
	        return classification;
		}
	    
	    
	}
	
	public Map<Slice, Double> getHistoryDA() {
		return historyDA;
	}

	public void setHistoryDA(Map<Slice, Double> historyDA) {
		this.historyDA = historyDA;
	}

	public AnoMASly getClassifier() {
		return classifier;
	}

	public void setClassifier(AnoMASly classifier) {
		this.classifier = classifier;
	}

	public int getNbSliceDisparity() {
		return nbSliceDisparity;
	}

	public void setNbSliceDisparity(int nbSliceDisparity) {
		this.nbSliceDisparity = nbSliceDisparity;
	}

	public SensorDB getSensorDB() {
		return sensorDB;
	}
	public Double getCorrespondingDA(Slice currentSlice) {
		return historyDA.get(currentSlice.getBegin());
	}

	public NavigableMap<Slice, EventClass> getFeedbackHistory() {
		return feedbackHistory;
	}
	
	public void setFeedbackHistory (NavigableMap<Slice, EventClass> FeedbackHistory) {
		this.feedbackHistory=FeedbackHistory;
	}
	
	public Map<Slice, Double> getdisparityValuesALL() {
		return disparityValuesALL;
	}
	
	public void setdisparityValuesALL(Map<Slice, Double> disparityValuesALL) {
		this.disparityValuesALL = disparityValuesALL;
	}

	public NavigableMap<Slice, EventClass> getClassificationHistory() {
		return classificationHistory;
	}

	public void setClassificationHistory(NavigableMap<Slice, EventClass> classificationHistory) {
		this.classificationHistory = classificationHistory;
	}

	public NavigableMap<Slice, String> getExpertClassificationHistory() {
		return expertClassificationHistory;
	}

	public void setProfile(ModuleProfile profile) {
		this.moduleProfile = profile;
		
	}
	
	
	
	
	
	
	
	
	private class AnomalyCharacterisation {
		public Map<String, Double> instantDisparityValues;
		public Map<String, Double> disparityValues;
		public Slice lastTimeAnomaly;
		public Slice firstTimeAnomaly;
		
//		public boolean isSameAnomaly(AnomalyCharacterisation other) {
//			boolean isSame = false;
//			List<String> mostInfluencialSensors = new ArrayList<>();
//			
//			Map<String, Double> sensorsDAMe = classifier.computeSensorsDA(disparityValues);
//			Map<String, Double> sensorsDAOther = classifier.computeSensorsDA(other.disparityValues);
//
//		    Map<String, Double> sortedSensorsDAMe = sensorsDAMe
//		            .entrySet()
//		            .stream()
//		            .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
//	                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
//		             
//			return isSame;
//		}
		/*
		public double computeSimilarity(AnomalyCharacterisation historyInhibitionData) {
			Map<String, Double> sensorsDAMe = classifier.computeSensorsDA(disparityValues);
			Map<String, Double> sensorsDAOther = classifier.computeSensorsDA(historyInhibitionData.disparityValues);
			double situationDAMe = sensorsDAMe.values().stream().reduce(0.0, Double::sum);
			double situationDAOther = sensorsDAOther.values().stream().reduce(0.0, Double::sum);

			double distance = 0.0;
			for (String sensor : sensorsDAMe.keySet()) {
				distance += Math.abs((sensorsDAMe.get(sensor) / situationDAMe) - (sensorsDAOther.get(sensor) / situationDAOther));
			}
			
			return 1 / (1 + distance);
		}
		*/
		
		public List<String> computeListMostInfluencialSensors() {
			List<String> listMostInfluencialSensors = new ArrayList<>();
			
			Map<String, Double> sensorsDA = classifier.computeSensorsDA(disparityValues);

		    Map<String, Double> sortedSensorsDA = sensorsDA
		            .entrySet()
		            .stream()
		            .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
	                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
			
			double situationDA = sensorsDA.values().stream().reduce(0.0, Double::sum);
		    double daToRemove = situationDA - classifier.getAlertThreshold();
		    double totalDARemoved = 0.0;
		    Iterator<Entry<String, Double>> it = sortedSensorsDA.entrySet().iterator();
		    double lastValueRemoved = Double.MAX_VALUE;
		    
		    while (it.hasNext() && totalDARemoved <= daToRemove) {
		    	Entry<String, Double> e = it.next();
		    	listMostInfluencialSensors.add(e.getKey());
		    	lastValueRemoved = e.getValue();
		    	totalDARemoved += lastValueRemoved;
		    }
		    
		    double ceilingToRemove = lastValueRemoved;
		    while (it.hasNext() && lastValueRemoved >= ceilingToRemove * 0.1) {
		    	Entry<String, Double> e = it.next();
		    	listMostInfluencialSensors.add(e.getKey());
		    	lastValueRemoved = e.getValue();
		    }
		    
			return listMostInfluencialSensors;
		}
		

		public boolean isBackToNormal(AnomalyCharacterisation other) {
			// computing margin
			Map<String, Double> instantDAMe = classifier.computeSensorsDA(instantDisparityValues);
			Map<String, Double> instantDAOther = classifier.computeSensorsDA(other.instantDisparityValues);

			double marginMe = instantDAMe.values().stream().reduce(0.0, Double::sum);
			double marginOther = instantDAOther.values().stream().reduce(0.0, Double::sum);
			
			// Check if same order of magnitude
			if (marginMe <= marginOther * 0.5) {
				return true;
			}
			return false;
		}
		
		public String toString() {
			return "firstTime = " + firstTimeAnomaly.toString();
		}
	}




}




	


