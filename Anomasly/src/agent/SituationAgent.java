package agent;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import agent.SituationAgent.Relation;
import core.AnoMASly;
import messages.ConstraintAgentFeedbackMessage;
import time.Slice;


public class SituationAgent {

	HashMap<String, Double> disparityList;
	private HashMap<String, WeightAgent> wList;
	private HashMap<String, Double> weightInfluence;
	private Slice slice; 

	private int age = 0;

	private boolean hasBeenUsed = false;

	public static enum Relation {
		HIGHERTHAN, LOWERTHANOREQUAL;
	};

	private Relation inequalityRelation;

	private double currentCriticality = 0;
	
	private boolean sncNoCritChange;

	private AnoMASly refToSystem;
	private double degreeOfAnomaly;


	public Slice getSlice() {
		return slice;
	}

	/**
	 * Create a new Situation agent
	 * 
	 * @param disparity_values
	 *            A map describing the current disparity of each sensor compared
	 *            to their profile values.
	 * @param wList
	 *            A map containing the WeightAgents of each sensor indexed by
	 *            the sensor name.
	 * @param type
	 *            Type of the relation (Relation.HIGHERTHAN or Relation.LOWERTHANOREQUAL).
	 * @param classifierSituation
	 *            The system classifier used.
	 * @param slice
	 *            The slice of the situation.
	 */
	public SituationAgent(Map<String, Double> disparity_values, HashMap<String, WeightAgent> wList, Relation type, AnoMASly classifierSituation, Slice slice) {
		this.disparityList = new HashMap<String, Double>();
		this.sncNoCritChange = false;
		if (disparity_values != null) {
			for (String key : disparity_values.keySet()) {
				this.disparityList.put(key, disparity_values.get(key));
			}
		}
		
		this.weightInfluence = new HashMap<String, Double>();

		this.refToSystem = classifierSituation;
		this.wList = wList;
		this.inequalityRelation = type;
		this.slice = slice;
	}

	/**
	 * Get the Criticality of the agent.
	 * 
	 * @return The criticality of the agent.
	 */
	public double getCriticality() {
		return this.currentCriticality;
	}
	
	/**
	 * Compute the criticality and the degree of anomaly.
	 * 
	 * @return The criticality of the agent.
	 */
	public double computeCriticality() {
		
		double weightSumPositive = 0;
		double weightSumAbsolute = 0;
		double weightSumNegative = 0;
		HashMap<String, Double> products = new HashMap<>();
		
		for (Entry<String, WeightAgent> e : wList.entrySet()) {
			double weight = e.getValue().getMyWeight();
			double disparity = disparityList.get(e.getKey());
			
			double productAbs = Math.abs(weight) * disparity;

			weightSumPositive += Math.max(0.0,weight) * disparity;
			weightSumAbsolute += Math.abs(weight) * disparity;
			weightSumNegative += weight * disparity;
			
			products.put((String) e.getKey(), productAbs);
		}
		this.degreeOfAnomaly = weightSumPositive;
		
		for (String sensorName : wList.keySet()) {
			double influence = products.get(sensorName) / weightSumPositive;
			
			weightInfluence.put(sensorName, influence);
		}

		if (inequalityRelation.equals(Relation.HIGHERTHAN)) {
			this.currentCriticality = refToSystem.getAlertThreshold() - weightSumPositive;
		} else {
			this.currentCriticality = weightSumPositive - refToSystem.getAlertThreshold();
		}

		return this.currentCriticality;
	}

	public double getAge() {
		return age;
	}

	public Relation getType() {
		return inequalityRelation;
	}

	public void doCycle() {
		age++;
	}
	/**
	 * Send feedback to each weightAgent.
	 */
	public void sendFeedback() {
		double crit = this.getCriticality();
		for (String key : wList.keySet()) {
			WeightAgent w = wList.get(key);
			// TODO think about this one
			//System.out.println("disparity key : "+disparityList.get(key));
			if (disparityList.get(key) != 0) {
				w.newConstraintFeedbackMessage(new ConstraintAgentFeedbackMessage(crit, inequalityRelation,
				getWeightInfluence(key), disparityList.get(key), sncNoCritChange,this));
			}
		}
		if (!hasBeenUsed)
			hasBeenUsed = true;
	}

	public String toString() {
		return "Situation= " + slice+" DA="+degreeOfAnomaly;

	}

	
	public double getDA() {
		return this.degreeOfAnomaly;
	}

	public void addInput(int i) {
		this.disparityList.put(String.valueOf(i), 0.0);
	}

	public boolean hasBeenUsed() {
		return this.hasBeenUsed;
	}

	public Relation getInequalityRelation() {
		return inequalityRelation;
	}

	public HashMap<String, Double> getDisparityList() {
		return disparityList;
	}

	public double getWeightInfluence(String sensorName) {
		return weightInfluence.get(sensorName);
	}

	@Override
	public boolean equals(Object o) {
		if(o.getClass().equals(this.getClass())) {
			SituationAgent other = (SituationAgent) o;
			return (other.slice==this.slice);
		}
		return false;
	}

	public boolean hasSameSlice(Slice sliceOther) {
		return (this.slice.equals(sliceOther));
	}
}