/**
 * 
 */
package agent.logging;

import java.util.HashMap;

import agent.SituationAgent;
import agent.SituationAgent.Relation;
import time.Slice;

/**
 * @author Maxime Houssin - maxime.houssin@irit.fr
 *
 */
public class LogSituationAgent implements Comparable<LogSituationAgent> {
	private Slice slice;
	private double criticality;
	private double constraintValue;
	private Relation inequalityRelation;
	private HashMap<String, Double> weightInfluence;
	HashMap<String, Double> disparityList;
	
	public LogSituationAgent(SituationAgent a) {
		this.slice = a.getSlice();
		this.criticality = a.getCriticality();
		this.constraintValue = (double) a.getDA();
		this.inequalityRelation = a.getInequalityRelation();
		this.weightInfluence = new HashMap<>();
//		for (String s : a.getWeightInfluence().keySet()) {
//			this.weightInfluence.put(new String(s), new Double(a.getWeightInfluence().get(s)));
//		}
		this.disparityList = a.getDisparityList(); // maybe copy
	}
	
	public Relation getInequalityRelation() {
		return inequalityRelation;
	}

	public HashMap<String, Double> getWeightInfluence() {
		return weightInfluence;
	}

	public HashMap<String, Double> getDisparityList() {
		return disparityList;
	}

	public Slice getSlice() {
		return slice;
	}
	
	public double getCriticality() {
		return criticality;
	}
	
	public double getConstraintValue() {
		return constraintValue;
	}
	
	@Override
	public int compareTo(LogSituationAgent arg0) {
		// TODO Auto-generated method stub
		return 0;
	}
}
