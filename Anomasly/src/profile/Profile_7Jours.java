package profile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import time.Slice;

import java.util.Set;

import core.AnoMASly;



public class Profile_7Jours {

	private Map<String, Map<Integer,Double>> sensorProfiles = new HashMap<String, Map<Integer,Double>>();
	private String name;
	private double lambda;
	private AnoMASly classifier;
	private Map<String, Map<Slice, Double>> disparityValues = new HashMap<String, Map<Slice, Double>>();

	public Profile_7Jours(String profileName, double lambda, List<String> sensorsNames, int alertThreshold, int nbr_sensors) {
		this.classifier = new AnoMASly(sensorsNames, alertThreshold);
		this.name = profileName;
		this.lambda = lambda;

	}
	
	public String getName () {
		return this.name;
	}
	
	public void setName (String newName) {
		this.name=newName;
	}
	
	public AnoMASly getClassifierSituation () {
		return this.classifier;
	}

	public Map<String, Map<Integer,Double>> getSensorProfiles () {
		return sensorProfiles;
	}
	
	public void setSensorProfiles (Map<String, Map<Integer,Double>> newSensorProfiles) {
		 this.sensorProfiles=newSensorProfiles;
	}

	public Map<String, Map<Slice, Double>> getDisparityValues () {
		return this.disparityValues;
	}
	
    public void addSensorProfile(String sensorName, Double value, int sliceHour) {
    	
	    if (this.sensorProfiles.containsKey(sensorName) && (sensorProfiles.get(sensorName).size() < 7)){
	    	Map<Integer,Double> profile = this.sensorProfiles.get(sensorName);
	    	profile.put(sliceHour, value);
	    	this.sensorProfiles.put(sensorName, profile);
	    }
	    else if (this.sensorProfiles.containsKey(sensorName)) {
	    	update(sliceHour, value, sensorName);
	    }
	    else {
	    	Map<Integer,Double> profile = new HashMap<Integer,Double>();
	    	profile.put(sliceHour,value);
	    	this.sensorProfiles.put(sensorName, profile);
	    }
    }
    
	public Map<String, Double> getInstantDisparityValues(Slice slice) {
		Map<String, Double> instantDisp = new HashMap<String, Double>();
		for (String sensor : this.disparityValues.keySet()) {
			instantDisp.put(sensor, this.disparityValues.get(sensor).get(slice));
		}
		return instantDisp;
	}
	
	
	
    public Map<Slice, Double> computeDisparityValues_sensorProfile(Map<Integer, Double> slices, String sensorName, List<Slice> listSlice) {
        double total = 0.0;
       // System.out.println(slices);
        Map<Slice, Double> disparityValues = new HashMap<Slice, Double>();
        for (Integer current : slices.keySet()) {

            double valCurrent = slices.get(current);
            try {
            	//system.out.println("sensor profile : "+sensorProfiles.get(sensorName));
                double valProfile = this.sensorProfiles.get(sensorName).get(current%7);
                //System.out.println("val profile: "+ valProfile);
                //System.out.println("val courante: "+ valCurrent);
                double disp = Math.abs(valCurrent - valProfile);
                disparityValues.put(listSlice.get(current), disp);
            } catch (java.util.NoSuchElementException e) {
            } catch (java.lang.NullPointerException e) {

            }

        }
        return disparityValues;
    }   
    
    public Map<Slice, Double> computeSensorDisparityValues(String sensorName, Map<Integer, Double> slices, List<Slice> listSlice)
    {
            this.disparityValues.put(sensorName, computeDisparityValues_sensorProfile(slices, sensorName, listSlice));
           // System.out.println("dispavalues "+disparityValues);
            return this.disparityValues.get(sensorName);
        }


    /* Update valeurs des profils des capteurs*/
	public void update(int sliceHour, Double value, String sensorName) {
		//System.out.println("======update profile : "+ value);
		if (sensorProfiles.get(sensorName).size() < 7) {
			sensorProfiles.get(sensorName).put(sliceHour%7, value);
		} 
		else {
			
			Integer toUpdate = sliceHour%7;
			//System.out.println("======LAMBDA : "+ lambda);
			
			double newValue = value * lambda + sensorProfiles.get(sensorName).get(toUpdate) * (1 - lambda);
			
			sensorProfiles.get(sensorName).put(toUpdate, newValue);
		}

	}
	
	


    


    

	
}
