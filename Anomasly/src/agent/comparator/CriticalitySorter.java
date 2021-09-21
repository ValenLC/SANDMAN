/**
 * 
 */
package agent.comparator;

import java.util.Comparator;

import agent.SituationAgent;
import agent.logging.LogSituationAgent;

/**
 * @author Maxime Houssin - maxime.houssin@irit.fr
 *
 */
public class CriticalitySorter implements Comparator<LogSituationAgent> {

	@Override
	public int compare(LogSituationAgent ca1, LogSituationAgent ca2) {
		if (ca1.getCriticality() < ca2.getCriticality()) {
			return -1;
		}
		if (ca1.getCriticality() > ca2.getCriticality()) {
			return 1;
		}
		return 0;
	}
}
