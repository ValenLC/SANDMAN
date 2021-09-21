/**
 * 
 */
package core.logging;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;

import agent.SituationAgent;
import agent.WeightAgent;
import agent.comparator.CriticalitySorter;
import agent.comparator.SliceSorter;
import agent.logging.LogSituationAgent;
import core.AnoMASly.EventClass;
import core.AnoMASly.Feedback;
import core.ModuleContext;
import profile.SensorProfile;
import time.Slice;
import tools.Pair;

/**
 * @author Maxime Houssin - maxime.houssin@irit.fr
 *
 */

public class LogSandman {
	private int firstCycle = -1;
	private int lastCycle = -1;
	private Map<Integer, List> logCANormal = new HashMap<Integer, List>();
	private Map<Integer, List> logCAAnomaly = new HashMap<Integer, List>();
	
	private Map<Integer, NavigableMap<String, Double>> logWeights = new HashMap<Integer, NavigableMap<String, Double>>();
	
	private List<Slice> logAnomalies = new ArrayList<Slice>();
	
	private SortedMap<Integer, Slice> cycleToSlice = new TreeMap<Integer, Slice>();
	private static SortedMap<Slice, Integer> sliceToCycle = new TreeMap<Slice, Integer>();
	private double threshold;
	
	private static Map<Integer, Feedback> feedbackMap = new TreeMap<Integer, Feedback>();
	private static Map<Feedback, Integer> feedbackCount = new HashMap<Feedback, Integer>();
	private static int multipleTries = 0;
	private static TreeMap<Slice,Integer> sliceToTries = new TreeMap<Slice, Integer>();
	private int maxTries = 0;
	
	private Map<Slice, EventClass> classificationHistory;
	private Map<Slice, EventClass> feedbackHistory;

	
	public void addConstraints(Integer cycle, List<SituationAgent> listLower, List<SituationAgent> listHigher) {
		List<LogSituationAgent> listNormal = new ArrayList<LogSituationAgent>(listLower.size());
		List<LogSituationAgent> listAnomaly = new ArrayList<LogSituationAgent>(listHigher.size());
		
		for (SituationAgent a : listLower) {
			listNormal.add(new LogSituationAgent(a));
		}
		for (SituationAgent a : listHigher) {
			listAnomaly.add(new LogSituationAgent(a));
		}
		
		// sorting by descending crit
		Collections.sort(listNormal, new CriticalitySorter());
		Collections.sort(listAnomaly, new CriticalitySorter());
		Collections.reverse(listNormal);
		Collections.reverse(listAnomaly);
		
		logCANormal.put(cycle, listNormal);
		logCAAnomaly.put(cycle, listAnomaly);
	}

	public void addCycle(Integer cycle, Slice slice) {
		if (firstCycle == -1) {
			firstCycle = cycle;
		}
		lastCycle = cycle;
		
		if (!sliceToCycle.containsKey(slice)) {
			sliceToCycle.put(slice, cycle);
		}
		
		cycleToSlice.put(cycle, slice);
	}
	
	public void addWeights(Integer cycle, HashMap<String, WeightAgent> weightAgentMap) {
		NavigableMap wMap = new TreeMap<String, Double>();
		for (String sensorName : weightAgentMap.keySet()) {
			wMap.put(sensorName, weightAgentMap.get(sensorName).getMyWeight());
		}
		logWeights.put(cycle, wMap);
	}

	public void addAnomaly(Slice slice) {
		if (!logAnomalies.contains(slice)) {
			this.logAnomalies.add(slice);
		}
	}

	public static void addFeedback(Slice slice, Feedback feedback) {
		feedbackMap.put(sliceToCycle.get(slice), feedback);
	}

	public void setThreshold(double alertThreshold) {
		this.threshold = alertThreshold;
	}

	// list of the n most critical agents sorted by slice
	public List getSortedMostCriticalNormal(int cycle, int n) {
		if (logCANormal.get(cycle).size() < n) {
			n = logCANormal.get(cycle).size();
		}
		List sub = logCANormal.get(cycle).subList(0, n);
		// Collections.sort(sub, new SliceSorter());
		return sub;
	}
	
	// list of the n most critical agents sorted by slice
	public List getSortedMostCriticalAnomaly(int cycle, int n) {
		if (logCAAnomaly.get(cycle).size() < n) {
			n = logCAAnomaly.get(cycle).size();
		}
		List sub = logCAAnomaly.get(cycle).subList(0, n);
		// Collections.sort(sub, new SliceSorter());
		return sub;
	}
	
	public Map<Integer, List> getConstraintAgentsNormal() {
		return logCANormal;
	}

	public Map<Integer, List> getConstraintAgentsAnomaly() {
		return logCAAnomaly;
	}

	public Map<String, Double> getLogWeights(int cycle) {
		return logWeights.get(cycle);
	}

	public List<Slice> getLogAnomalies() {
		return logAnomalies;
	}

	public LogSituationAgent getLogConstraintAgent(int cycle, Slice slice) {
		for (LogSituationAgent a : (List<LogSituationAgent>) logCANormal.get(cycle)) {
			if (a.getSlice().equals(slice)) {
				return a;
			}
		}
		for (LogSituationAgent a : (List<LogSituationAgent>) logCAAnomaly.get(cycle)) {
			if (a.getSlice().equals(slice)) {
				return a;
			}
		}
		return null;
	}

	public Integer sliceToCycle(Slice slice) {
		return this.sliceToCycle.get(slice);
	}

	public double getThreshold() {
		return threshold;
	}

	public String toString() {
		String s = new String("---------------------------------------\nFEEDBACK from cycle " + firstCycle + " to " + lastCycle + "\n");
		TreeMap feedbackCount = countFeedback();
		for (Entry<Integer, Feedback> e : feedbackMap.entrySet()) {
			if (e.getKey() >= firstCycle && e.getValue() != Feedback.TRUENEGATIVE) {
				s += (cycleToSlice.get(e.getKey()) + " : " + e.getValue()) + " \t(cycle = " + e.getKey() + ")\n";
			}
		}
		s += "FEEDBACK from cycle " + firstCycle + " to " + lastCycle + "\n---------------------------------------\n";
		s += (lastCycle - firstCycle + 1) + " cyles\n";  
		s += multipleTries + " situations with several cycles\n";
		s += maxTries + " situations with max tries done\n";
		s += feedbackCount.toString();
//		s += printWeights();
		return s;
	}

	private String printWeights() {
		StringBuilder s = new StringBuilder();
		s.append("\nWeights=\n");
		for (Entry<String, Double> e : logWeights.get(cycleToSlice.lastKey()).entrySet()) {
			s.append(e.getKey() + " = " + e.getValue() + "\n");
		}
		return s.toString();
	}

	private TreeMap countFeedback() {
		TreeMap<Feedback, Integer> m = new TreeMap<Feedback, Integer>();
		for (Feedback feedback : feedbackMap.values()) {
			if (!m.containsKey(feedback)) {
				m.put(feedback, 1);
			} else {
				m.put(feedback, m.get(feedback) + 1);
			}
		}

		return m;
	}

	/*
	 * Clearing stuff between runs
	 */
	public void newRun() {
		firstCycle = -1;
		lastCycle = -1;
		sliceToCycle.clear();
		multipleTries = 0;
		maxTries = 0;
		feedbackCount.clear();
		feedbackMap.clear();
	}

	public static void updateTries(int tries, int maxTries, Slice slice) {
		if (tries > 1) {
			multipleTries  += 1;
		}
		if (tries >= maxTries) {
			maxTries += 1;
		}
		sliceToTries .put(slice, tries);
	}
	
	public String cyclesPerSituationToCSV() {
		StringBuilder s = new StringBuilder();
		s.append("Situation,Cycles\n");
		for (Entry<Slice, Integer> e : sliceToTries.entrySet()) {
			s.append(e.getKey() + "," + e.getValue() + "\n");
		}
		
		return s.toString();
	}

	public String classificationFeedbackToCSV() {
		StringBuilder str = new StringBuilder();
		str.append("timestamp,classification,feedback\n");
		for (Slice sl : feedbackHistory.keySet()) {
			str.append(sl + "," + classificationHistory.get(sl) + "," + feedbackHistory.get(sl) + "\n");
		}
		return str.toString();
	}

	public void setClassificationHistory(Map<Slice, EventClass> classificationHistory) {
		this.classificationHistory = classificationHistory;
	}

	public void setFeedbackHistory(Map<Slice, EventClass> feedbackHistory) {
		this.feedbackHistory = feedbackHistory;
	}

//	public void logProfiles(Map<String, Map<String, NominalProfile>> profiles) {
//		for (Entry<String, Map<String, NominalProfile>> profile : profiles.entrySet()) {
//			for (Entry<String, NominalProfile> sensorProfile : profile.entrySet()) {
//				Map<String, NominalProfile> sensorProfileCopy = new HashMap<String, NominalProfile>();
//				
//				if (!logProfiles.containsKey(profile.getKey())) {
//					
//				} else {
//					
//				}
//			}
//
//		}
//	}
}

