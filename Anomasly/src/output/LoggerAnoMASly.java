/**
 * 
 */
package output;

import java.util.HashMap;
import java.util.Map.Entry;

import agent.WeightAgent;
import core.AnoMASly;

/**
 * @author Maxime Houssin - maxime.houssin@irit.fr
 *
 */
public class LoggerAnoMASly {

	private AnoMASly refToClassifier;
	
	public LoggerAnoMASly(AnoMASly classifier) {
		this.refToClassifier = classifier;
	}

	public void printWeights() {
		HashMap<String, WeightAgent> weights = refToClassifier.getWeightAgentMap();
		System.out.println("\nWeights : \n");
		for (Entry<String, WeightAgent> entry : weights.entrySet()) {
			System.out.println(entry.getKey() + " : " + entry.getValue().getMyWeight() + "\t");
		}
		System.out.println();
	}
}
