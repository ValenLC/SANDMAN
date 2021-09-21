/**
 * Interface for oracle knowing where the anomalies are in input data
 */
package Oracle;

import java.time.LocalDateTime;

import time.Slice;

/**
 * @author Maxime Houssin - maxime.houssin@irit.fr
 *
 */
public interface Oracle {

	void addAnomaly(LocalDateTime timestamp);
	boolean isAnomaly(Slice slice);
}
