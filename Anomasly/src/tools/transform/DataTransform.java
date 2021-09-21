/**
 * 
 */
package tools.transform;

import time.Slice;

/**
 * @author Maxime Houssin - maxime.houssin@irit.fr
 *
 */
public class DataTransform {
	
	public static double linear(Slice begin, Slice current, double coeff, double mean, double value) {
		int steps = Math.abs(begin.relativeDistanceTo(current));
		return value + mean * steps * coeff;
	}
}
