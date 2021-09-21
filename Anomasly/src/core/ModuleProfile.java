package core;

import java.util.HashMap;
import java.util.Map;

import javafx.util.Pair;
import profile.Profile;
import time.Slice;

import core.AnoMASly.EventClass;

public class ModuleProfile {
	
	public Map<String, Profile> listProfiles = new HashMap<String, Profile>();
	private String switchProfile = "";
	private Map<String, Integer> profileSwitchHistory= new HashMap<String, Integer>();
	private Pair<Slice,String> lastProfileSwitch ;
	private Map<String, Map<Integer,Double>> tmpSensorProfile = new HashMap<String, Map<Integer,Double>>();
	private ModuleContext moduleContext;

	/**
	 * Manage the profile switch
	 * 
	 * @param situationToAdd
	 *            Slice at which to switch 
	 * @param profileName
	 *            Current profile name	
	 *            
	 * @return The name of the new profile
	 */
    public String switchProfile(Slice situationToAdd, String profileName) {
    	String currentProfile = profileName;
    	System.out.println("\n-> switch profile "+situationToAdd);
    	Map<String,Double> disparityTotal = new HashMap<>();
    	String newProfile = currentProfile;
    	Slice lastSituationToAdd = situationToAdd;
    	Map<String,Double> potentialProfiles = new HashMap<String,Double>() ;
    	Double minDisparity = 1000.0;
    	for (String pName : listProfiles.keySet()) {
    		System.out.println("\n"+pName);
			Pair<EventClass,Double> classification_DA = moduleContext.computeClassificationProfile(situationToAdd, pName);
			disparityTotal.put( pName, sumDisparityValues(listProfiles.get(pName).getDisparityValues(),situationToAdd ));
			EventClass feedbackClassification =EventClass.REGULAR;
			if (moduleContext.getFeedbackHistory().containsKey(situationToAdd)) {
				feedbackClassification = moduleContext.getFeedbackHistory().get(situationToAdd);
			
			}

			if (classification_DA.getValue()<1000  && !pName.equals(currentProfile) && !feedbackClassification.equals("ALERT") && disparityTotal.get(pName)<moduleContext.getdisparityValuesALL().get(situationToAdd) && disparityTotal.get(pName)<minDisparity) {
				potentialProfiles.clear();
				potentialProfiles.put(pName,classification_DA.getValue());
				minDisparity = disparityTotal.get(pName);
				newProfile = pName;
				
			}
			
			else if ((pName.equals(currentProfile)|| classification_DA.getValue()>=1000) && feedbackClassification.equals("ALERT") &&  disparityTotal.get(pName)<=moduleContext.getdisparityValuesALL().get(situationToAdd) && disparityTotal.get(pName)<minDisparity) {
				potentialProfiles.put(pName,classification_DA.getValue());
				System.out.println(newProfile);
				minDisparity = disparityTotal.get(pName);
				newProfile = pName;
			}
			

    	}
    	
    	//if no better profile has been found
    	if (potentialProfiles.isEmpty()) {
    		System.out.println("\n"+currentProfile + " -> " + newProfile +"\n");
    		for (String sensorName : listProfiles.get(currentProfile).getSensorProfiles().keySet()) {
    			tmpSensorProfile.put(sensorName, new HashMap<Integer,Double> (listProfiles.get(currentProfile).getSensorProfiles().get(sensorName)));
        	}
    		return profileName;
    	}

    	
    	else if (potentialProfiles.size()>0) {
    		Map<String,Double> PreviousPotentialProfiles = new HashMap<String, Double>();
    		while (potentialProfiles.size()>0) {
    			PreviousPotentialProfiles  = new HashMap<String, Double>(potentialProfiles);
    		
    			potentialProfiles.clear();
 
    			situationToAdd=situationToAdd.substractSlices(1);
    			System.out.println("\n-> switch profile "+situationToAdd);
    			
    			Double SUMdispartityCurrent=sumDisparityValues(listProfiles.get(currentProfile).getDisparityValues(),situationToAdd);
    			
	        	for (String pName : PreviousPotentialProfiles.keySet()) {

	    			Pair<EventClass,Double> classification_DA = moduleContext.computeClassificationProfile(situationToAdd, pName);
	    
	    			Double SUMdispartityNew=sumDisparityValues(listProfiles.get(pName).getDisparityValues(),situationToAdd );
	
	    			disparityTotal.put( pName,disparityTotal.get(pName)+SUMdispartityNew);
	    			
	    			EventClass feedbackHistory_situationToAdd = EventClass.REGULAR;
	    			
	    			if (moduleContext.getFeedbackHistory().containsKey(situationToAdd)) {
	    				
	    				feedbackHistory_situationToAdd= moduleContext.getFeedbackHistory().get(situationToAdd);
	    				}
	    			else {
	    				System.out.println("lalaal");
	    				//feedbackHistory_situationToAdd=moduleContext.getClassificationHistory().get(situationToAdd);
	    				feedbackHistory_situationToAdd = EventClass.REGULAR;	
	    			}
	    			
	    			System.out.println(moduleContext.getClassificationHistory());
	  
	    			if (((classification_DA.getValue()<1000 && feedbackHistory_situationToAdd.equals(EventClass.REGULAR) && moduleContext.getdisparityValuesALL().get(situationToAdd)>SUMdispartityNew) || 
	    					(classification_DA.getValue()>=1000 && feedbackHistory_situationToAdd.equals(EventClass.ALERT) && moduleContext.getHistoryDA().get(situationToAdd)<=1000 )) 
	    					&& !pName.equals(currentProfile)){
	    				
	    				potentialProfiles.put(pName,classification_DA.getValue());
	    				newProfile=pName;
	    				
	    			}
	
	        	}
        	}
    		
    		
    		if (potentialProfiles.size()==0) {
    			System.out.println(PreviousPotentialProfiles);
		        double minValue = Double.MAX_VALUE;
		        String minProfile = null;
		        for(String key : PreviousPotentialProfiles.keySet()) {
		            double value = disparityTotal.get(key);
		            if(value <= minValue) {
		                minValue = value;
		                minProfile = key;
		            }
		        }
		        situationToAdd=situationToAdd.addSlices(1);
		        newProfile= minProfile;
		        
		     
    		}
    		
    		else if (potentialProfiles.size()==1) {
    			situationToAdd=situationToAdd.addSlices(1);
    		}
	        
    	}
    	System.out.println(situationToAdd);
    	switchProfile+=situationToAdd+" : "+currentProfile + " -> " + newProfile +"\n";
    	if (!currentProfile.equals(newProfile)) {
    		int jourSemaineSituationToAdd = (situationToAdd.getBegin().getDayOfWeek().getValue()+2)%7;
    		System.out.println(jourSemaineSituationToAdd);
    		if (profileSwitchHistory.containsKey("jour"+jourSemaineSituationToAdd+newProfile)) {
    			profileSwitchHistory.put("jour"+jourSemaineSituationToAdd+newProfile, profileSwitchHistory.get("jour"+jourSemaineSituationToAdd+newProfile)+1);
    		}
    		else { 
    			profileSwitchHistory.put("jour"+jourSemaineSituationToAdd+newProfile, 1);}
				switchProfile+=currentProfile + ", " + situationToAdd.getBegin().getDayOfWeek().getValue() +", "+ newProfile +"\n";
		}
    	lastProfileSwitch = new Pair<Slice, String>(situationToAdd, currentProfile);
    	System.out.println("\n"+currentProfile + " -> " + newProfile +"\n");
 
    	
    	System.out.println(tmpSensorProfile);
    	remove_incorrect_Profiles(tmpSensorProfile, currentProfile, situationToAdd, lastSituationToAdd );
    	currentProfile = newProfile;
    	while (situationToAdd.isBefore(lastSituationToAdd) ) {	
    		System.out.println("* situationToAdd : "+situationToAdd);
    		EventClass feedbackHistory_situationToAdd;
    		
			if (moduleContext.getFeedbackHistory().containsKey(situationToAdd)) {
				feedbackHistory_situationToAdd= moduleContext.getFeedbackHistory().get(situationToAdd);
				}
			else {
    			feedbackHistory_situationToAdd=moduleContext.getClassificationHistory().get(situationToAdd);
				}
    		if (feedbackHistory_situationToAdd.equals("REGULAR")) {
	    		Pair<EventClass, Double> classification_DA = moduleContext.computeClassificationProfile(situationToAdd, newProfile);
		       
	    		moduleContext.getClassificationHistory().put(situationToAdd, classification_DA.getKey());
	    		Map<Slice, Double> historyDA = moduleContext.getHistoryDA();
	    		historyDA.put(situationToAdd, classification_DA.getValue());
		        moduleContext.setHistoryDA(historyDA);
		        moduleContext.newFeedback(situationToAdd, moduleContext.getExpertClassificationHistory().get(situationToAdd),currentProfile);
		    	
    		}
    		situationToAdd=situationToAdd.addSlices(1);
    		
    	}

    	
  
    	for (String sensorName : listProfiles.get(currentProfile).getSensorProfiles().keySet()) {
			tmpSensorProfile.put(sensorName, new HashMap<Integer,Double> (listProfiles.get(currentProfile).getSensorProfiles().get(sensorName)));
    	}
    	
    	return currentProfile;
    }
    
    
    public void remove_incorrect_Profiles(Map<String, Map<Integer,Double>> tempSensorProfile, String profileName, Slice sBegin, Slice sEnd ) {

    	Map<String, Map<Integer,Double>> currentSensorProfile = listProfiles.get(profileName).getSensorProfiles();
    	while (sBegin.isBefore(sEnd) ) {
    		
    		for(String sensorName : currentSensorProfile.keySet()) {
    		
    			currentSensorProfile.get(sensorName).put(sBegin.getBegin().getHour(), tempSensorProfile.get(sensorName).get(sBegin.getBegin().getHour()));
    			listProfiles.get(profileName).getClassifierSituation().removeSituation(sBegin);
    		}
    		sBegin=sBegin.addSlices(1);
    	}
   
    	listProfiles.get(profileName).setSensorProfiles (currentSensorProfile);
    	
    }
    
    
    public Double sumDisparityValues(Map<String, Map<Slice, Double>> dispartityMap ,Slice slice) {
    	Double SUMdispartityCurrent =0.0;
    
			for (String sensorName : dispartityMap.keySet()) {
				try {
					
				SUMdispartityCurrent+= dispartityMap.get(sensorName).get(slice);
				}
				catch(java.lang.NullPointerException exception){
					
				}
			}
		return SUMdispartityCurrent;
    }

    
   
	public Profile getProfile(String profileName) {
		// TODO Auto-generated method stub
		return listProfiles.get(profileName);
	}
	
	public void setProfile(String profileName, Profile profile) {
		// TODO Auto-generated method stub
		listProfiles.put(profileName, profile);
	}

	public void setContext(ModuleContext context) {
		this.moduleContext = context;
		// TODO Auto-generated method stub
		
	}


	public Map<String, Profile> getListprofile() {
		return this.listProfiles;
		// TODO Auto-generated method stub
		
	}


	public void setlistProfiles(Map<String, Profile> listProfiles) {
		this.listProfiles=listProfiles;
		// TODO Auto-generated method stub
		
	}
    
	
	public  Map<String, Map<Integer,Double>> getTmpSensorProfile(){
		return this.tmpSensorProfile;
		
	}
    
	
	public  void setTmpSensorProfile(Map<String, Map<Integer,Double>> tmpSensorProfile){
		this.tmpSensorProfile= tmpSensorProfile;
		
	}
}
