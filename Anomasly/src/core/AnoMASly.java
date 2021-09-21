package core;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


import agent.SituationAgent;
import agent.SituationAgent.Relation;
import agent.SituationAgentVirtual;
import agent.WeightAgent;
import time.Slice;

import java.util.Random;

/**
 * This class describes AnoMASly, a multi-agent system designed to learn to
 * detect anomalies from feedback. It involves cooperation between two set of
 * agents: Constraint agents which models inequalities and Weight agent modeling
 * weights involved in the detection process.
 * 
 * @author Nicolas Verstaevel - nicolas.verstaevel@irit.fr
 *
 */

public class AnoMASly {
	

	/**
	 * The map of Weight Agent indexed by name of the variable. There are one Weight
	 * agent for each variable to consider.
	 */
	private HashMap<String, WeightAgent> weightAgentsMap = new HashMap<String, WeightAgent>();

	/**
	 * List of constraint agents above the threshold
	 */
	private ArrayList<SituationAgent> situationAgentsAnomalous = new ArrayList<SituationAgent>();
	
	/**
	 * List of constraint agents below the threshold
	 */
	private ArrayList<SituationAgent> situationAgentsNormal = new ArrayList<SituationAgent>();

	
	private SituationAgent virtualSAA;

	private double alertThreshold;
	
	private int nbConstraintsTotal = 80;
	private final int CAN_LIFETIME = 80;

	private double currentSituationCriticality = 0;

	private SituationAgent maxCritSAA;
	private SituationAgent maxCritSAN;
	private SituationAgent minCritSAA;
	private SituationAgent minCritSAN;

	private int NUMBEROFCONSTRAINTS_A;
	private int NUMBEROFCONSTRAINTS_N;

	private SituationAgentVirtual virtualSAN;

	/**
	 * This enum models the feedback that are sent to the system. There are four
	 * types of feedbacks: - FALSEPOSITIVE: an alert has been raised whereas there
	 * was no anomaly to detect. - FALSENEGATIVE: no alert were risen whereas there
	 * was an anomaly to detect. - TRUEPOSITIVE: an alert has been raised and there
	 * was an anomaly to detect. - TRUENEGATIVE: no alert were risen and there was
	 * no anomaly to detect.
	 * 
	 * @author Nicolas Verstaevel - nicolas.verstaevel@irit.fr
	 *
	 */
	public static enum Feedback {
		FALSEPOSITIVE, FALSENEGATIVE, TRUEPOSITIVE, TRUENEGATIVE, NEUTRAL;
	};


	/**
	 * This enum models the output of the system. There are two outputs: - ALERT: An
	 * alert is raised. - NOALERT: No alert has to be risen.
	 * 
	 * @author Nicolas Verstaevel - nicolas.verstaevel@irit.fr
	 */
	public static enum EventClass {
		ALERT, REGULAR;
	};
	/**
	 * Construct a new alert triggering system.
	 * 
	 * @param sensors_values
	 *            A map describing the current value of each parameter indexed by
	 *            the parameter name.
	 * @param alertThreshold
	 *            The value to fix the alert threshold.
	 */
	public AnoMASly(List<String> sensorsNames, int alertThreshold) {
		this.alertThreshold = alertThreshold;
		for (String s : sensorsNames) {
			weightAgentsMap.put(s, new WeightAgent());
		}
		/* This number restrains the maximum number of Constraint Agent */
		this.NUMBEROFCONSTRAINTS_A = 100;
		this.NUMBEROFCONSTRAINTS_N = 100;

		
		Map<String, Double> virtualDispA = new HashMap<String, Double>();
		for (String s : sensorsNames) {
			virtualDispA.put(s, new Double(10.0));
		}
		this.virtualSAA = new SituationAgentVirtual(virtualDispA, 
													weightAgentsMap, 
													Relation.HIGHERTHAN,
													this, new Slice(LocalDateTime.of(1900,1,1,0,0), Duration.ofHours(1)), 1.2);
		this.maxCritSAA = this.virtualSAA;
		this.situationAgentsAnomalous.add(this.virtualSAA);
		
		Map<String, Double> virtualDispN = new HashMap<String, Double>();
		for (String s : sensorsNames) {
			virtualDispN.put(s, new Double(10.0));
		}
		this.virtualSAN = new SituationAgentVirtual(virtualDispN, 
													weightAgentsMap, 
													Relation.LOWERTHANOREQUAL,
													this, new Slice(LocalDateTime.of(1900,1,1,0,0), Duration.ofHours(1)), 0.8);
		this.maxCritSAN = this.virtualSAN;
		this.situationAgentsNormal.add(this.virtualSAN);
	}

	
	public SituationAgent findSituationAgent(int slice) {
			    Iterator<SituationAgent> iteratorNormal = situationAgentsNormal.iterator();
			    while (iteratorNormal.hasNext()) {
			        SituationAgent situationAgentNormal = iteratorNormal.next();
			        if (situationAgentNormal.getSlice().equals(slice)) {
			            return situationAgentNormal;
			        }
			    }
			    Iterator<SituationAgent> iteratorAnomalous = situationAgentsAnomalous.iterator();
			    while (iteratorAnomalous.hasNext()) {
			        SituationAgent situationAgentAnomalous = iteratorAnomalous.next();
			        if (situationAgentAnomalous.getSlice().equals(slice)) {
			            return situationAgentAnomalous;
			        }
			    }
			    return null;
			}
	
	/**
	 * Send a new event to the system.
	 * 
	 * @param sensors_values
	 *            The map containing the value of each parameter indexed by the
	 *            parameter name.
	 * @return EventClass.ALERT if an alert is raised, EventClass.REGULAR otherwise.
	 */
	public String newEvents(Map<String, Double> disparity_values) {
		currentSituationCriticality = computeSituationDA(disparity_values);
		if (currentSituationCriticality >= alertThreshold) {
			return "ALERT";
		} else {
			return "REGULAR";
		}
	}

	public double computeSituationDA(Map<String, Double> disparityValues) {
		System.out.println("weight agents : "+ weightAgentsMap);
		double situationCriticality = 0.0;
		for (String key : weightAgentsMap.keySet()) {
			situationCriticality += Math.max(weightAgentsMap.get(key).getMyWeight(), 0.0) * disparityValues.get(key);
		}
		return situationCriticality;
	}

	/**
	 * Get the current situation criticality value.
	 * 
	 * @return The current situation criticality value.
	 */
	public double getCurrentSituationCriticality() {
		return currentSituationCriticality;
	}

	/**
	 * Send a new feedback to the system.
	 * 
	 * @param feedback
	 *            The feedback (FALSEPOSITIVE, FALSENEGATIVE, TRUEPOSITIVE,
	 *            TRUENEGATIVE).
	 * @param sensors_values
	 *            The map containing the value of each parameter indexed by the
	 *            parameter name.
	 * @return 
	 * @return 
	 */
	
	//AJOUT SITUATION
	public void createSituation(EventClass classification, Map<String, Double> disparity_values, Slice slice) {
		nbConstraintsTotal++;
		switch (classification) {
		case REGULAR :
			addSituationAgent(
				new SituationAgent(disparity_values, weightAgentsMap, SituationAgent.Relation.LOWERTHANOREQUAL, this, slice));
			break;
		case ALERT:
			addSituationAgent(
				new SituationAgent(disparity_values, weightAgentsMap, SituationAgent.Relation.HIGHERTHAN, this, slice));
		default:
			break;
		}
		updateMinMaxCrit();
		//resolve();
	}
	

	
	/**
	 * Add a Constraint agent to the Constraint agent pool
	 * 
	 * @param c
	 *            The Constraint agent to add
	 */
	public void addSituationAgent(SituationAgent c) {
		
		c.computeCriticality();
		System.out.println("situation : "+c);
		// higher
		if (c.getType() == Relation.HIGHERTHAN) {
			// removing the virtual if needed
			if (situationAgentsAnomalous.contains(virtualSAA)) {
				situationAgentsAnomalous.remove(virtualSAA);
			}
			if (situationAgentsAnomalous.size() < NUMBEROFCONSTRAINTS_A) {
				situationAgentsAnomalous.add(c);
			} else if (minCritSAA.getCriticality() <= c.getCriticality()) {
				situationAgentsAnomalous.remove(0);
				situationAgentsAnomalous.add(c);
			}
			// lower
		} else {
			// removing the virtual if needed
			if (situationAgentsNormal.contains(virtualSAN)) {
				situationAgentsNormal.remove(virtualSAN);
			}
			if (situationAgentsNormal.size() < NUMBEROFCONSTRAINTS_N) {
				situationAgentsNormal.add(c);
			} else if (minCritSAN.getCriticality() <= c.getCriticality()) {
				situationAgentsNormal.remove(0);
				situationAgentsNormal.add(c);
			}
		}
	}


	/**
	 * Perform the life cycle of each Weight agent contained in the Weight agent
	 * map.
	 */
	private void doWeightAgentsLifeCycles() {
		weightAgentsMap.entrySet().parallelStream().forEach(entry -> entry.getValue().doCycle());
	}

	/**
	 * Perform one resolution cycle of the system. It includes to perform the life
	 * cycle of Constraint agents and then the life cycle of Weight agents.
	 */
	public void resolve() {
		updateMinMaxCrit();

		if (maxCritSAA != null)
			maxCritSAA.sendFeedback();
		if (maxCritSAN != null)
			maxCritSAN.sendFeedback();
		System.out.println("weightAgentsMap : "+weightAgentsMap);
		doWeightAgentsLifeCycles();
		
		for (SituationAgent c : situationAgentsNormal) {

			c.computeCriticality();
		}
		for (SituationAgent c : situationAgentsAnomalous) {

			c.computeCriticality();
		}
	}

	private void updateMinMaxCrit() {
		maxCritSAA = null;
		maxCritSAN = null;
		minCritSAA = null;
		minCritSAN = null;
		double tmpMaxH = Double.NEGATIVE_INFINITY;
		double tmpMaxL = Double.NEGATIVE_INFINITY;
		double tmpMinH = Double.POSITIVE_INFINITY;
		double tmpMinL = Double.POSITIVE_INFINITY;

		System.out.println("agents situation Anomalie : "+situationAgentsAnomalous);
		for (SituationAgent c : situationAgentsAnomalous) {
			c.doCycle();
			
			if (c.getCriticality() > tmpMaxH) {
				maxCritSAA = c;
				tmpMaxH = c.getCriticality();
			} else if (c.getCriticality() < tmpMinH) {
				minCritSAA = c;
				tmpMinH = c.getCriticality();
			}
		}
		System.out.println("agents situation normale : "+situationAgentsNormal);
		for (SituationAgent c : situationAgentsNormal) {
			c.doCycle();
			
			if (c.getCriticality() > tmpMaxL) {
				maxCritSAN = c;
				
				tmpMaxL = c.getCriticality();
			} else if (c.getCriticality() < tmpMinL) {

				minCritSAN = c;
				tmpMinL = c.getCriticality();
			}
		}
	//	System.out.println("maxCritSAN : "+maxCritSAN);
	//	System.out.println("minCritSAN : "+minCritSAN);
	//	System.out.println("maxCritSAA : "+maxCritSAA);
	//	System.out.println("minCritSAA : "+minCritSAA);
	}

	/**
	 * Get the Chigher agent.
	 * 
	 * @return The agent with an higher than constraint and the maximum criticality.
	 */
	public SituationAgent getMaxCritSAA() {
		return maxCritSAA;
	}

	/**
	 * Get the Clower agent.
	 * 
	 * @return The agent with a lower than constraint and the maximum criticality.
	 */
	public SituationAgent getMaxCritSAN() {
		return maxCritSAN;
	}

	/**
	 * Get the Chigher agent.
	 * 
	 * @return The agent with an higher than constraint and the maximum criticality.
	 */
	public double getMaxLowerHigherDistance() {
		if (maxCritSAA == null || maxCritSAN == null) {
			return -1;
		}
		//System.out.println("maxCritSAA.getCriticality : "+maxCritSAA.getCriticality());
		//System.out.println("maxCritSAN.getCriticality : "+maxCritSAN.getCriticality());
		return Math.abs(maxCritSAA.getCriticality() + maxCritSAN.getCriticality());
	}

	/**
	 * Get the number of Constraint agents.
	 * 
	 * @return The number of Constraint agents.
	 */
	public int getNumberOfConstraintAgents() {
		return situationAgentsAnomalous.size() + situationAgentsNormal.size();
	}

	public ArrayList<SituationAgent> getSituationAgentsAnomalous() {
		return situationAgentsAnomalous;
	}

	public ArrayList<SituationAgent> getSituationAgentsNormal() {
		return situationAgentsNormal;
	}
	
	public Double getSituationAgentsNormal_MEAN() {
		Double nbElem = 0.0;
		System.out.println(situationAgentsNormal);
		Double situationAgentsNormal_MEAN = 0.0;
		
		for (int i=1; i<=24 && i<=situationAgentsNormal.size(); i++) {
		
			if (situationAgentsNormal.get(situationAgentsNormal.size()-i).getDA()>0) {
				
				situationAgentsNormal_MEAN+=situationAgentsNormal.get(situationAgentsNormal.size()-i).getDA();
				nbElem+=1;
			}
		}
		
		if (nbElem==0) return 0.0;
	
		return situationAgentsNormal_MEAN/nbElem;
	}

	public void setupDeserialize() {
		// TODO setup correctly
	}

	public Set<String> getSensorsList() {
		return weightAgentsMap.keySet();
	}

	public Map<String, Double> computeSensorsDA(Map<String, Double> disparityValues) {
		Map<String, Double> sumValues = new HashMap<String, Double>();
		for (Entry<String, Double> disp : disparityValues.entrySet()) {
			sumValues.put(disp.getKey(), disp.getValue() * weightAgentsMap.get(disp.getKey()).getMyWeight());
		}
		
		return sumValues;
	}

	public double computeDispWeightProduct(String maxSensor, double disparity) {
		return weightAgentsMap.get(maxSensor).getMyWeight() * disparity;
	}

	/*
	 * Removes constraints with positive criticality
	 *//*
	public void removePositiveCritSituations() {
		int count = 0;
		List<SituationAgent> removeList = new ArrayList<SituationAgent>();
		for (Iterator<SituationAgent> it = situationAgentsAnomalous.iterator();it.hasNext();) {
			SituationAgent a = it.next();
			if (a.getCriticality() > 0) {
				it.remove();
				removeList.add(a);
				count++;
			}
		}
		for (Iterator<SituationAgent> it = situationAgentsNormal.iterator();it.hasNext();) {
			SituationAgent a = it.next();
			if (a.getCriticality() > 0) {
				it.remove();
				removeList.add(a);
				count++;
			}
		}
		System.out.println(count + " situations with positive criticality removed from history before new run\n"+removeList);
	}*/

	/**
	 * Get the value of the alert threshold.
	 * 
	 * @return The value of the alert threshold.
	 */
	public double getAlertThreshold() {
		return alertThreshold;
	}

	/**
	 * Get the Weight agents map.
	 * 
	 * @return A Map containing the Weight Agents indexed by the name of their
	 *         associated parameter.
	 */
	public HashMap<String, WeightAgent> getWeightAgentMap() {
		return weightAgentsMap;
	}

	public Double getWeight(String sensorName) {
		return weightAgentsMap.get(sensorName).getMyWeight();
	}
	
	public SituationAgent getSituation(Slice slice) {
		if (situationAgentsNormal.stream().anyMatch(agent -> agent.hasSameSlice(slice))) {
			return situationAgentsNormal.stream().filter(agent -> agent.hasSameSlice(slice)).findFirst().get();
		}
			return situationAgentsAnomalous.stream().filter(agent -> agent.hasSameSlice(slice)).findFirst().get();
	}
	
	
	public String WeightToString() {
		String weightToString = "";
	
		for  (int nbSensor=1; nbSensor<= weightAgentsMap.size() ; nbSensor++) {
			weightToString+= weightAgentsMap.get("daily_"+nbSensor).getMyWeight()+"; ";
		}
	
		return weightToString;

	}
	
	public void removeSituation(Slice slice) {
		// only removes normal
		if (situationAgentsNormal.stream().anyMatch(agent -> agent.hasSameSlice(slice))) {
			situationAgentsNormal.remove(situationAgentsNormal.stream().filter(agent -> agent.hasSameSlice(slice)).findFirst().get());
		}
		
	}


	public void setSituationAgentsAnomalous( ArrayList<SituationAgent> situationAgentsAnomalous) {
		// TODO Auto-generated method stub
		this.situationAgentsAnomalous= situationAgentsAnomalous;
		for (SituationAgent SAA : this.situationAgentsAnomalous) {
			SAA.computeCriticality();
		}
			
	}
	
	public void setSituationAgentsNormal( ArrayList<SituationAgent> situationAgentsNormal) {
		// TODO Auto-generated method stub
		this.situationAgentsNormal = situationAgentsNormal;
		for (SituationAgent SAN : this.situationAgentsNormal) {
			SAN.computeCriticality();
		}
	}
	
	
	public void setweightAgentsMap( HashMap<String, WeightAgent> weightAgentsMap ) {
		// TODO Auto-generated method stub
		this.weightAgentsMap = weightAgentsMap;
		
	}
}