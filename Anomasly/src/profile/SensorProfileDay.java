package profile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.TreeMap;

import core.Sandman;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import input.Sensor;
import time.Slice;

/**
 * @author Maxime Houssin - maxime.houssin@irit.fr
 *
 */
public class SensorProfileDay implements SensorProfile {

	/**
	 * Single profile for the sensor
	 */
	private TreeMap<Integer, Double> profile = new TreeMap<Integer, Double>();
	
	/**
	 * Coefficient for new values vs old ones in profile generation
	 */
	private double lambda;
	private double sensorNoise = Sandman.SENSOR_NOISE;
	
	/**
	 * Duration of slice
	 */
	private Duration duration;

	private double valuesRange;
	
	public SensorProfileDay(double lambda, Duration duration) {
		this.lambda = lambda;
		this.duration = duration;
	}
	
	public Integer sliceToInt(Slice slice) {
		return ((int)slice.getBegin().toString().charAt(11))*10 + 
				((int)slice.getBegin().toString().charAt(12));
	}
	
	@Override
	public Map<Slice, Double> computeDisparityValues(Map<Slice, Double> slices) {
		double total = 0.0;
		Map<Slice, Double> disparityValues = new HashMap<Slice, Double>();
		for (Slice current : slices.keySet()) {
			double valCurrent = slices.get(current);
			try {
				double valProfile = profile.get(sliceToInt(current));
				double disp = Math.max(Math.abs(valCurrent - valProfile) - (valuesRange * Sandman.SENSOR_NOISE), 0);
				//double disp = Math.max(Math.abs(valCurrent - valProfile), 0);
				//disp =(disp/valProfile);
				//System.out.println("Disparity : valeurCourante "+valCurrent+ " - valeurProfil "+ valProfile+ " = "+disp);
				disparityValues.put(current, disp);
			} catch (java.util.NoSuchElementException e) {
				//System.out.println("(NSEE)profiles = " + profile.toString());
				//System.out.println("(NSEE)valcurrent = " + valCurrent);
				// Error when Map is empty -> firstKey() throws NoSuchElementException
			} catch (java.lang.NullPointerException e) {
				//System.out.println("(NPE)profiles = " + profile.toString());
				//System.out.println("(NPE)valcurrent = " + valCurrent);
				// Error when Map is empty -> firstKey() throws NoSuchElementException
			}
//			total += Math.abs(valCurrent - valProfile);
			
		}
//		return total / slices.size();
		return disparityValues;
	}

	/*
	 * BE CAREFUL : THIS IS ASSUMING THEY COME IN ORDER
	 * If the profile is empty, the first value is randomized a bit
	 */
	@Override
	public void update(Slice slice, Double value) {
		//System.out.println("======update profil : "+ value);
		if (profile.size() < 24) {
			profile.put(sliceToInt(slice), value /** (Math.random() / 100 + 0.995)*/); // tester sans le random + verifier où lamba est appellé 
		} else {
			Integer toUpdate = sliceToInt(slice);
			//System.out.println("======LAMBDA : "+ lambda);
			Double newValue = value * lambda + profile.get(toUpdate) * (1 - lambda);
			profile.put(toUpdate, newValue);
		}
		updateValuesRange();
	}

	private void updateValuesRange() {
		valuesRange = Math.abs(	profile.values().stream().max(Comparator.comparing(Double::valueOf)).get() -
								profile.values().stream().min(Comparator.comparing(Double::valueOf)).get());
	}

	@Override
	public Double getValue(Slice slice) {
		return profile.get(sliceToInt(slice));
	}
	
	@Override
	public Double getValueFromTimeStamp(Slice slice) {
		return profile.get(sliceToInt(slice));
	}

	/**
	 * Puts the value inside the profile at the place of the corresponding hour
	 */
	@Override
	public void add(Slice slice, Double value) {
		profile.put(sliceToInt(slice), value);
	}

	public String toString() {
		String s = new String();
		// TODO ici
		for (Entry<Integer, Double> dataPoint : profile.entrySet()) {
			s += dataPoint.getKey() + "\t" + dataPoint.getValue() + "\n";
		}
		return s;
	}

	@Override
	public String toCSV() {
		String csv = new String();
		for (Entry<Integer, Double> situation : profile.entrySet()) {
			csv += situation.getKey() + "," + situation.getValue() + "\n";
		}
		
		return csv;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((profile == null) ? 0 : profile.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SensorProfileDay other = (SensorProfileDay) obj;
		if (profile == null) {
			if (other.profile != null)
				return false;
		} else if (!profile.equals(other.profile))
			return false;
		return true;
	}
}
