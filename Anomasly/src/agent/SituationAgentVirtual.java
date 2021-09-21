/**
 * 
 */
package agent;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import core.AnoMASly;
import time.Slice;


/**
 * @author Maxime Houssin - maxime.houssin@irit.fr
 *
 */
public class SituationAgentVirtual extends SituationAgent {
	
	private double threshold;
	private double coefficientThreshold;

	public SituationAgentVirtual(Map<String, Double> sensors_values, HashMap<String, WeightAgent> wList, Relation type,
			AnoMASly classifierSituation, Slice slice, double coefficientThreshold) {
		super(sensors_values, wList, type, null,new Slice(LocalDateTime.of(1900,1,1,0,0), Duration.ofHours(1)));
		this.threshold = classifierSituation.getAlertThreshold();
		this.coefficientThreshold = coefficientThreshold;
	}

	@Override
	public double getCriticality() {
		return -(Math.abs(1 - coefficientThreshold)) * threshold;
	}

	@Override
	public double computeCriticality() {
		return getCriticality();
	}

	@Override
	public double getWeightInfluence(String key) {
		return 0.5;
		//return Math.random();
	}

	@Override
	public double getDA() {
		return coefficientThreshold * threshold;
	}
}