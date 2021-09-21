package profile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import core.AnoMASly;
import core.Sandman;
import input.SensorDB;
import time.Slice;

public class BuildingProfile {

	private Map<String, SensorProfile> sensorProfiles = new HashMap<String, SensorProfile>();
	private String name;
	private double lambda;
	private Duration sliceDuration;
	private SensorDB sensorDB;
	private Map<String, Map<Slice, Double>> disparityValues = new HashMap<String, Map<Slice, Double>>();

	public BuildingProfile(SensorDB sensorDB, String profileName, double lambda, Duration sliceDuration) {
		this.sensorDB = sensorDB;
		this.name = profileName;
		this.lambda = lambda;
		this.sliceDuration = sliceDuration;
	}

	public void addSensorProfile(String s, SensorProfile pm) {
		sensorProfiles.put(s, pm);
	}

	public void loadSensorProfiles(List<String> buildingProfileContent) {
		SensorProfile sensor = null;
		for (String line : buildingProfileContent) {
			if (line.charAt(0) == '&') { // new sensor
				String sensorName = line.split("&")[1];
				sensor = new SensorProfileDay(lambda, sliceDuration);
				addSensorProfile(sensorName, sensor);
			} else {
				String[] lineSplit = line.split(",");
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
		        LocalDateTime formatDateTime = LocalDateTime.parse(lineSplit[0], formatter);
				Slice slice = new Slice(formatDateTime, Duration.ofHours(1));
				Double value = Double.parseDouble(lineSplit[1]);
				sensor.add(slice, value);
			}
		}
	}

	public String toCSV() {
		String csv = new String();
		for (Entry<String, SensorProfile> e : sensorProfiles.entrySet()) {
			csv += "&" + e.getKey() + "\n";
			csv += e.getValue().toCSV();
		}
		return csv;
	}

	@Override
	public String toString() {
		return "BuildingProfile [sensorProfiles=" + sensorProfiles + "]";
	}

	public Map<String, Map<Slice, Double>> getSensorDisparityValues() {
		return disparityValues;
	}
	
	public Map<Slice, Double> computeSensorDisparityValues(String sensorName, Map<Slice, Double> slices) {
		this.disparityValues.put(sensorName, sensorProfiles.get(sensorName).computeDisparityValues(slices));
		return this.disparityValues.get(sensorName);
	}
	
	public SensorProfile getProfile(String name) {
		return sensorProfiles.get(name);
	}
	

	public void updateProfileValues(Slice slice) {
		for (Entry<String, SensorProfile> e : sensorProfiles.entrySet()) {
			try {
				//System.out.println("value from sensor profil "+e.getKey() + " : "+ e.getValue());
				e.getValue().update(slice, sensorDB.getSensorValue(slice, e.getKey()));
			} catch (java.lang.NullPointerException exception) {
				// NullPointerException thrown
			}
			
		}
	}

	/*
	 * How many previous disparities must be taken into account? 
	 * Let's try with PREVIOUS_DISPARITY_TOTAL_LIMIT
	 * 
	 * 
	 * THIS NEEDS TO BE REDONE
	 * 
	 */
	public double computeLowerDisparity(Slice slice, double lowestDisparityTotal, AnoMASly classifier) {
		int nbPreviousSlices = Sandman.PREVIOUS_DISPARITY_TOTAL_LIMIT;
		double disparityTotal = 0.0;
		Slice currentSlice = slice;
		Map<Slice, Double> slices;
		Map<String, Double> disparityValues = new HashMap<String, Double>();
		
		// computing disparity values (for each sensor) of last nbPreviousSlices
		for (int i = 0; i < nbPreviousSlices; i++) {
			if (!sensorDB.isAnomaly(currentSlice)) {
				for (String sensorName : sensorProfiles.keySet()) {
					slices = sensorDB.getSensorLastValues(sensorName, currentSlice, Sandman.NB_SLICE_DISPARITY);
//					computeSensorDisparityValues(sensorName, slices);
//					double sensorDisparity = getDisparityValues(sensorName).values().stream().reduce(0.0, Double::sum);
//					disparityValues.put(sensorName, sensorDisparity);
				}
				double degreeOfAnomaly = classifier.computeSituationDA(disparityValues);
				disparityTotal += degreeOfAnomaly;
			}

			if (disparityTotal > lowestDisparityTotal) {
				return Double.MAX_VALUE;
			}
				
			currentSlice = currentSlice.previousSlice();
			disparityValues.clear();
		}
		return disparityTotal;
	}

	public Map<String, Double> getInstantDisparityValues(Slice slice) {
		Map<String, Double> instantDisp = new HashMap<String, Double>();
		for (String sensor : disparityValues.keySet()) {
			instantDisp.put(sensor, disparityValues.get(sensor).get(slice));
		}
		return instantDisp;
	}

}
