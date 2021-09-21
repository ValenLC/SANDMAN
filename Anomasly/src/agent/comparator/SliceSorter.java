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
public class SliceSorter implements Comparator<LogSituationAgent> {

	@Override
	public int compare(LogSituationAgent ca1, LogSituationAgent ca2) {
		if (ca1.getSlice().isBefore(ca2.getSlice())) {
			return -1;
		}
		return 1;
	}
}
