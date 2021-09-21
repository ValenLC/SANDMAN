/**
 * 
 */
package input;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import profile.SensorProfile;
import time.Slice;
import tools.transform.DataTransform;

import java.util.TreeMap;

/**
 * @author Maxime Houssin - maxime.houssin@irit.fr
 * Describes a single sensor or data flow storing measured values ordered by timestamps
 * Each Double value is indexed by a timestamp
 */
public class Sensor {
	
	/**
	 * The name of the sensor
	 */
	private String name;
	
	/**
	 * The profile of the sensor, in charge of managing everything profile related
	 */
	private SensorProfile profileManager;
	
	/**
	 * Sensor measured values
	 */
	private TreeMap<LocalDateTime, Double> values;
	
	/**
	 * Sliced values
	 */
	private TreeMap<Slice, Double> slices;

	private double meanSlice;

	
	
	
	/**
	 * Constructor
	 * @param name : name of the sensor
	 */
	public Sensor(String name) {
		super();
		this.name = name;
		values = new TreeMap<LocalDateTime, Double>();
		slices = new TreeMap<Slice, Double>();
	}
	
	public void sliceValues(Slice firstSlice) {
		Set<LocalDateTime> times = values.keySet();
		Iterator<LocalDateTime> it = times.iterator();
		ArrayList<Double> valuesInSlice = new ArrayList<Double>();
		if (slices.isEmpty()) {
			slices.put(firstSlice,null);
		}
		
		LocalDateTime timestamp;
		do {
			timestamp = it.next();
		} while (!slices.lastKey().isBefore(timestamp) && !slices.lastKey().contains(timestamp));
			
		do {
			if (slices.lastKey().contains(timestamp)) {
				valuesInSlice.add(values.get(timestamp));
				timestamp = it.next();
			} else {
				if (valuesInSlice.isEmpty()) {
					slices.put(slices.lastKey().nextSlice(), null);
				} else {
					double total = 0.0;
					for (Double val : valuesInSlice) {
						total += val;
					}
					slices.put(slices.lastKey(), total/valuesInSlice.size());
					valuesInSlice.clear();
				}
			}
		} while (it.hasNext());			
		fillNullSlices();
	}

	private void fillNullSlices() {
		// Naive solution, might have to be redone
		// Assigns previous slice value to null slices
		for (Slice slice : slices.keySet()) {
			if (slices.get(slice) == null) {
				slices.put(slice, slices.get(slice.previousSlice()));
			}
		}
	}
	
	public List<Entry> getSensorFirstDay() {
		List<Entry> list = new ArrayList<Entry>(slices.entrySet());
		return list.subList(0, 24);
	}

	/**
	 * Adds a data instance for the sensor
	 * @param timestamp : measure timestamp
	 * @param value : measure value
	 */
	public void addValue(LocalDateTime timestamp, Double value) {
		values.put(timestamp, value);
	}


	public SensorProfile getProfileManager() {
		return profileManager;
	}

	public void setProfileManager(SensorProfile profileManager) {
		this.profileManager = profileManager;
	}
	
	public void updateProfile(Slice slice) {
		profileManager.update(slice, slices.get(slice));
	}
	
	public String getName() {
		return name;
	}

	public void testSlices() {
		for (Entry<Slice, Double> e : slices.entrySet()) {
			System.out.println(e.getKey() + "\t" + e.getValue());
		}
	}

	private double computeMeanSlice() {
		double sum = 0.0;
		for (Double d : slices.values()) {
			sum += d;
		}
		meanSlice = sum / slices.size();
		return meanSlice;
	}
	
	public double getMeanSlice() {
		return meanSlice;
	}

	public Double getSliceValue(Slice slice) {
		return slices.get(slice);
	}

	public Double getProfileValue(Slice slice) {
		return profileManager.getValue(slice);
	}
	
	public void linear(double coeff) {
		computeMeanSlice();
		for (Slice slice : slices.keySet()) {
			slices.put(slice, DataTransform.linear(slices.firstKey(), slice, coeff, getMeanSlice(), slices.get(slice)));
		}
	}

	public Slice getFirstSlice() {
		return slices.firstKey();
	}

	public Slice getLastSlice() {
		return slices.lastKey();
	}
	
	public LocalDateTime getFirstTime() {
		return values.firstKey();
	}
}
