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



public class Profile {

	private Map<String, Map<Integer,Double>> sensorProfiles = new HashMap<String, Map<Integer,Double>>();
	private String name;
	private double lambda;
	private AnoMASly classifier;
	private List<Double> lastDA = new ArrayList<Double>();
	private Double DA_mean;
	private Map<String, Map<Slice, Double>> disparityValues = new HashMap<String, Map<Slice, Double>>();
	private List<String> anomalySensors = new ArrayList<String>();

	public Profile(String profileName, double lambda, List<String> sensorsNames, int alertThreshold, int nbr_sensors) {
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
	
    public void addSensorProfile(String sensorName, Double value, Slice slice) {
	    if (this.sensorProfiles.containsKey(sensorName) && (sensorProfiles.get(sensorName).size() < 24)){
	    	Map<Integer,Double> profile = this.sensorProfiles.get(sensorName);
	    	profile.put(slice.getBegin().getHour(), value);
	    	this.sensorProfiles.put(sensorName, profile);
	    }
	    else if (this.sensorProfiles.containsKey(sensorName)) {
	    	update(slice, value, sensorName);
	    }
	    else {
	    	Map<Integer,Double> profile = new HashMap<Integer,Double>();
	    	profile.put(slice.getBegin().getHour(),value);
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
	
	
	
    public Map<Slice, Double> computeDisparityValues_sensorProfile(Map<Slice, Double> slices, String sensorName) {
        double total = 0.0;
        
        Map<Slice, Double> disparityValues = new HashMap<Slice, Double>();
        for (Slice current : slices.keySet()) {

            double valCurrent = slices.get(current);
            try {
            	//system.out.println("sensor profile : "+sensorProfiles.get(sensorName));
                double valProfile = this.sensorProfiles.get(sensorName).get(current.getBegin().getHour());
                //system.out.println("val profile: "+ valProfile);
                //system.out.println("val courante: "+ valCurrent);
                double disp = Math.abs(valCurrent - valProfile);
                disparityValues.put(current, disp);
            } catch (java.util.NoSuchElementException e) {
            } catch (java.lang.NullPointerException e) {

            }

        }
        return disparityValues;
    }   
    
    public Map<Slice, Double> computeSensorDisparityValues(String sensorName, Map<Slice, Double> slices) {
            this.disparityValues.put(sensorName, computeDisparityValues_sensorProfile(slices, sensorName));
            return this.disparityValues.get(sensorName);
        }

    
    
	public String ProfileSensorToString(String sensorName) {
		String profileTostring = "";
	
		profileTostring +=sensorName+"; ";
		for (Entry <Integer,Double> profileValues : sensorProfiles.get(sensorName).entrySet()) {
			profileTostring+=profileValues.getValue()+"; ";
		}
	
		return profileTostring;

	}
	


    /* Update valeurs des profils des capteurs*/
	public void update(Slice slice, Double value, String sensorName) {
		//System.out.println("======update profile : "+ value);
		if (sensorProfiles.get(sensorName).size() < 24) {
			sensorProfiles.get(sensorName).put(slice.getBegin().getHour(), value);
		} else {
			
			Integer toUpdate = slice.getBegin().getHour();
			//System.out.println("======LAMBDA : "+ lambda);
			
			double newValue = value * lambda + sensorProfiles.get(sensorName).get(toUpdate) * (1 - lambda);
			
			sensorProfiles.get(sensorName).put(toUpdate, newValue);
		}

	}

	public List<String> getAnomalySensors() {
		return anomalySensors;
	}

	public void setAnomalySensors(List<String> anomalySensors) {
		this.anomalySensors = anomalySensors;
	}

	public Double getDA_mean() {
	
		int nbElem = 0;
		DA_mean = 0.0;
		for (Double da : lastDA ) {
			DA_mean+=da;
			nbElem+=1;
		}
		return DA_mean/nbElem;
	}

	public List<Double> getlastDA() {
		return lastDA;
	}
	
	
	public void clearLastDA() {
		this.lastDA.clear();
	}
	
	


    


    

	
}
