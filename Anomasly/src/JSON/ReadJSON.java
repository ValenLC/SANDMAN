package JSON;
 
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.Gson;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import agent.SituationAgent;
import agent.WeightAgent;
import agent.SituationAgent.Relation;
import core.AnoMASly;
import core.ModuleContext;
import core.ModuleProfile;
import core.AnoMASly.EventClass;
import input.SensorDB;
import javafx.util.Pair;
import profile.Profile;
import time.Slice;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReadJSON
{
	static private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	static private Slice lastSlice;
	static private NavigableMap<Slice, EventClass> feedbackHistory = new TreeMap<Slice, EventClass>();
	
    public static Slice readJson(String filename, ModuleProfile moduleProfile, List<String> sensorNames, ModuleContext moduleContext) 
    {
        //JSON parser object to parse read file
        JSONParser jsonParser = new JSONParser();
         
        try (FileReader reader = new FileReader(filename))
        {
            //Read JSON file
            Object obj = jsonParser.parse(reader);
 
            JSONArray profilList = (JSONArray) obj;
            System.out.println(profilList);
            Map<String, Profile> listProfiles = moduleProfile.getListprofile();
            profilList.forEach( profil -> parseProfilesObject( (JSONObject) profil, listProfiles, sensorNames, moduleContext.getSensorDB(),moduleContext, moduleProfile) );
            
            moduleProfile.setlistProfiles(listProfiles);
            moduleContext.setFeedbackHistory(feedbackHistory);
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
		return lastSlice;
    }
 
    
    private static void parseProfilesObject(JSONObject profil, Map<String, Profile> listProfiles, List<String>sensorNames, SensorDB sensorDB, ModuleContext moduleContext, ModuleProfile moduleProfile) 
    {
        //Get profil object within list
        JSONObject profilObject = (JSONObject) profil.get("profil");

        
        //Get last Slice
        String slice = (String) profilObject.get("slice");  
        System.out.println(slice);
        lastSlice = new Slice(LocalDateTime.parse(slice.replace("T", " ").replace("\"", ""), formatter), Duration.ofHours(1));

        //Get profil name
        String name = (String) profilObject.get("name");  
        System.out.println(name);
        
        
        //Get sensorValues 
        Type type = new TypeToken<Map<String, Map<String,String>> >(){}.getType();
        Map<String, Map<String, String>>  sensorsValues_SAVED = new Gson().fromJson((String) profilObject.get("sensorsValues"), type);
    	
        HashMap<Slice, Map<String, String>> sensorsValues = new HashMap<Slice, Map<String, String>>();
    	for(String sliceString : sensorsValues_SAVED.keySet())	{
    		sensorsValues.put(new Slice(LocalDateTime.parse(sliceString.replace("T", " ").replace("\"", ""), formatter), Duration.ofHours(1)),sensorsValues_SAVED.get(sliceString));
    	}
        sensorDB.setSensors(sensorsValues);
        
        //Get Temporary sensor profile 
        type = new TypeToken<Map<String, Map<Integer,Double>> >(){}.getType();
        Map<String, Map<Integer,Double>>  tmpSensorProfile_SAVED = new Gson().fromJson((String) profilObject.get("tmpSensorProfile"), type);
    	
        
        //Get profil listSituationNormales
        type = new TypeToken<Map<String, Map<String, Double>> >(){}.getType();
        Map<String, Map<String, Double>>  listSituationNormales = new Gson().fromJson((String) profilObject.get("listSituationNormales"), type);
        
        System.out.println(listSituationNormales);
         
        //Get profil listSituationAnormales
        type = new TypeToken<Map<String, Map<String, Double>> >(){}.getType();
        Map<String, Map<String, Double>>  listSituationAnormales = new Gson().fromJson((String) profilObject.get("listSituationAnormales"), type);
        
        System.out.println(listSituationAnormales);
        
        //Get profil listPoids
        type = new TypeToken<Map<String, Pair<Double, Double>> >(){}.getType();
        Map<String, Pair<Double, Double>> listPoids = new Gson().fromJson((String) profilObject.get("listPoids"), type);
       
        System.out.println(listPoids);
        
      //Get profil listsensorProfiles
        type = new TypeToken<Map<String, Map<Integer,Double>> >(){}.getType();
        Map<String, Map<Integer,Double>> listsensorProfiles = new Gson().fromJson((String) profilObject.get("listsensorProfiles"), type);
        
        Profile newProfile = new Profile(name, 0.25, sensorNames, 1000,sensorNames.size());
        newProfile.setSensorProfiles(listsensorProfiles);
     
    	HashMap<String, WeightAgent> mapWeights = new HashMap<String, WeightAgent>();
    	for(String sensorName :  listPoids.keySet()) {
    		WeightAgent wa = new WeightAgent();
    		wa.setMyWeight(listPoids.get(sensorName).getKey());
    		wa.setBeta(listPoids.get(sensorName).getValue());
    		mapWeights.put(sensorName,wa);
    	}

    	ArrayList<SituationAgent> listSAA = new ArrayList<SituationAgent>();
    	for(String sliceString :  listSituationAnormales.keySet()) {
    		feedbackHistory.put(new Slice(LocalDateTime.parse(sliceString.replace("T", " "), formatter), Duration.ofHours(1)), EventClass.ALERT);
    		listSAA.add(new SituationAgent(listSituationAnormales.get(sliceString), mapWeights, SituationAgent.Relation.LOWERTHANOREQUAL, newProfile.getClassifierSituation(), new Slice(LocalDateTime.parse(sliceString.replace("T", " "), formatter), Duration.ofHours(1)))); 
    	}
    	
    	ArrayList<SituationAgent> listSAN = new ArrayList<SituationAgent>();
    	for(String sliceString :  listSituationNormales.keySet()) {
    		feedbackHistory.put(new Slice(LocalDateTime.parse(sliceString.replace("T", " "), formatter), Duration.ofHours(1)), EventClass.REGULAR);
    		listSAN.add(new SituationAgent(listSituationNormales.get(sliceString), mapWeights,SituationAgent.Relation.HIGHERTHAN, newProfile.getClassifierSituation(), new Slice(LocalDateTime.parse(sliceString.replace("T", " "), formatter), Duration.ofHours(1)))); 
    	}
    	
    	type = new TypeToken<Map<String, Double> >(){}.getType();
	    Map<String, Double> dispaValuesALL_SAVED = new Gson().fromJson((String) profilObject.get("dispaValues"), type);
	    Map<Slice, Double> dispaValuesALL=new  HashMap<Slice, Double>();
	 	for(String sliceString :  dispaValuesALL_SAVED.keySet()) {
     		dispaValuesALL.put(new Slice(LocalDateTime.parse(sliceString.replace("T", " ").replace("\"", ""), formatter), Duration.ofHours(1)), dispaValuesALL_SAVED.get(sliceString));
     	}
    	
	 	
    	moduleContext.setdisparityValuesALL(dispaValuesALL);
        newProfile.getClassifierSituation().setweightAgentsMap(mapWeights);
        newProfile.getClassifierSituation().setSituationAgentsAnomalous(listSAA);
        newProfile.getClassifierSituation().setSituationAgentsNormal(listSAN);
        moduleProfile.setTmpSensorProfile(tmpSensorProfile_SAVED);
        listProfiles.put(name, newProfile);
        
        
        
       
    }
}
