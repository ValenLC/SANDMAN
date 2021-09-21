/**
 * 
 */
package profile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import input.Sensor;

import java.util.AbstractMap.SimpleImmutableEntry;

import time.Slice;

/**
 * @author Maxime Houssin - maxime.houssin@irit.fr
 * Interface specifying responsabilities of a profile class
 */
public interface SensorProfile {
	void update(Slice slice, Double value);
	Map<Slice, Double> computeDisparityValues(Map<Slice, Double> slices);
	Double getValue(Slice slice);
	void add(Slice slice , Double value);
	String toCSV();
	Double getValueFromTimeStamp(Slice slice);
}