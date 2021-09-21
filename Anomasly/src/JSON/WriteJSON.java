package JSON;
 
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.gson.Gson;

import agent.SituationAgent;
import agent.WeightAgent;
import core.ModuleContext;
import core.ModuleProfile;
import javafx.util.Pair;
import profile.Profile;
import time.Slice;
 
public class WriteJSON{
	public void writeJSON(String FileName, Map <String, Profile>listProfiles,HashMap<Slice, Map<String, String>> sensorsValues, Slice lastSlice, ModuleContext moduleContext,  ModuleProfile moduleProfile) {

	JSONArray jsonList = new JSONArray();
	
	 // create a new Gson instance
		 Gson gson = new Gson();
	for (String profilName: listProfiles.keySet()) {
		


    	Map<String, Pair<Double,Double>> weightAgentMap_SAVE = new HashMap<String, Pair<Double,Double>>();
    	Map<String, WeightAgent> WeightAgentMap = listProfiles.get(profilName).getClassifierSituation().getWeightAgentMap();
    	for(String sensorName : WeightAgentMap.keySet()) {
    		Pair <Double,Double> tmp  = new Pair <Double,Double>(WeightAgentMap.get(sensorName).getMyWeight(),WeightAgentMap.get(sensorName).getDelta());
    		weightAgentMap_SAVE.put(sensorName,tmp);
    	
    	}
    	
    	Map<String, Map<String, Double>> situationNormalMap_SAVE = new HashMap<String, Map<String, Double>>();
    	ArrayList<SituationAgent> situationNormalMap = listProfiles.get(profilName).getClassifierSituation().getSituationAgentsNormal();
    	for(SituationAgent situationAgent : situationNormalMap) {
    		situationNormalMap_SAVE.put(situationAgent.getSlice().toString(),situationAgent.getDisparityList());
    	
    	}
    	
     	Map<String, Map<String, Double>> situationAnormalMap_SAVE = new HashMap<String, Map<String, Double>>();
    	ArrayList<SituationAgent> situationAnormalMap = listProfiles.get(profilName).getClassifierSituation().getSituationAgentsAnomalous();
    	for(SituationAgent situationAgent : situationAnormalMap) {
    		situationAnormalMap_SAVE.put(situationAgent.getSlice().toString(),situationAgent.getDisparityList());
    	
    	}
    
    	Map<String, Map<Integer,Double>> sensorProfiles_SAVE = listProfiles.get(profilName).getSensorProfiles();
    	HashMap<String, Map<String, String>> sensorsValues_SAVE = new HashMap<String, Map<String, String>>();
    	for(Slice slice : sensorsValues.keySet())	{
    		sensorsValues_SAVE.put(slice.toString(),sensorsValues.get(slice));
    	}
    	
    	Map<String, Double> disparityValuesALL_SAVE = new HashMap<String, Double>();
    	Map<Slice, Double> disparityValuesALL = moduleContext.getdisparityValuesALL();
    	for(Slice slice : disparityValuesALL.keySet())	{
    		disparityValuesALL_SAVE.put(slice.toString(),disparityValuesALL.get(slice));
    	}
    	
    	
    	Map<String, Map<Integer,Double>>tmpSensorProfile_SAVE = moduleProfile.getTmpSensorProfile();
    	
  		 // convert your list to json
  		 String jsonSitNormales = gson.toJson(situationNormalMap_SAVE);
  		 String jsonSitAnormales = gson.toJson(situationAnormalMap_SAVE);
  		 String jsonListPoids = gson.toJson(weightAgentMap_SAVE);
  		 String jsonsensorProfiles =  gson.toJson(sensorProfiles_SAVE);
  		 String jsonLastSlice =  gson.toJson(lastSlice.toString());
  		 String jsonSensorsValues =  gson.toJson(sensorsValues_SAVE);
  		 String jsonDispaValues =  gson.toJson(disparityValuesALL_SAVE);
  		 String jsontmpSensorProfile = gson.toJson(tmpSensorProfile_SAVE);
  		 // print your generated json
  		// System.out.println("jsonSitAnormales: " + jsonSitAnormales);
  		// System.out.println("jsonSitNormales: " + jsonSitNormales);
  		// System.out.println("jsonListPoids: " + jsonListPoids);
	      //First PROFIL
	      JSONObject profilDetails = new JSONObject();
	     
	      profilDetails.put("name", profilName);
	      profilDetails.put("listSituationNormales", jsonSitNormales);
	      profilDetails.put("listSituationAnormales", jsonSitAnormales);
	      profilDetails.put("listPoids", jsonListPoids);
	      profilDetails.put("listsensorProfiles", jsonsensorProfiles);
	      profilDetails.put("slice", jsonLastSlice);
	      profilDetails.put("sensorsValues", jsonSensorsValues);
	      profilDetails.put("dispaValues", jsonDispaValues);
	      profilDetails.put("tmpSensorProfile", jsontmpSensorProfile);
	       
	      JSONObject profilObject = new JSONObject(); 
	      profilObject.put("profil", profilDetails);

	      //Add PROFILS to list
	      jsonList.add(profilObject);
       
	}
	 
      //Write JSON file
      try (FileWriter file = new FileWriter(FileName)) {
          //We can write any JSONArray or JSONObject instance to the file
          file.write(jsonList.toJSONString()); 
          file.flush();

      } catch (IOException e) {
          e.printStackTrace();
      }
    
    }
}