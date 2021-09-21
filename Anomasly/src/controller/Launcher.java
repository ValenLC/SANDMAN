package controller;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import JSON.ReadJSON;
import JSON.WriteJSON;
import agent.WeightAgent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import core.AnoMASly.EventClass;
import core.ModuleContext;
import core.Sandman;
import profile.SensorProfile;
import time.Slice;

public class Launcher {
	private static Slice starterSlice = null;
	private WriteJSON saveJSON = new WriteJSON();
	private static ReadJSON readJSON = new ReadJSON();
	private static Sandman sandman;
	private static String dataFilePath;
	private Path outputDirectory, inputDirectory;
	private Slice firstSlice, lastSlice, currentSlice;
	private static List<String> sensors;
	private int counter;
	private boolean running = false;
	private Long timer;
	private boolean feedbackReceived = false;
	private Long totalExecutionTime;
	Scanner scanner;
	private WatchService watchService;
	private WatchKey key;
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	public Long getTimer() {
		return timer;
	}

	public void setTimer(Long newTimerValue) {
		this.timer = newTimerValue;
	}

	public static void main(String[] args) throws FileNotFoundException {
		
		Launcher launcher = new Launcher();
		
		// Launch the python interface
		String cmd = "run_interface.bat"; // For Windows
//		String cmd = "run_interface_unix.bash"; // For Mac/Unix
		launcher.runInterface(cmd);	
		
		launcher.setDataFilePath(Paths.get(System.getProperty("user.dir"), "Data") + 
				File.separator + "20Sensors_7profiles.xlsx - Data1_8mois.csv5%&15%_bruit.csv");
				//File.separator + "data4-double.csv");
		System.out.println("TRUC = " + Paths.get(System.getProperty("user.dir", "Output").toString()));
		
		
		launcher.setOutputDirectory(Paths.get(System.getProperty("user.dir"), "Output"));
		launcher.setInputDirectory(Paths.get(System.getProperty("user.dir"), "Input"));
		
		// Is this necessary/best way to handle?
		if(!java.nio.file.Files.exists(launcher.outputDirectory)) {
			System.out.println("Directory does not exist");
			throw new IllegalArgumentException();
		}
		File f = new File(dataFilePath+".json");
		launcher.init("2016-01-01T00:00");
		if(f.exists() && !f.isDirectory()) { 

			starterSlice = readJSON.readJson(dataFilePath+".json", sandman.getProfile(), sensors, sandman.getContext());
			System.out.println(starterSlice);
		}
	
		launcher.run();
	}
	
	// Function to manually launch python interface using batch or bash file
	private void runInterface(String cmd) {
		try {
			Runtime rt = Runtime.getRuntime();
			rt.exec(cmd);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// All initialisation functions for sandman are called here
	private void init(String firstTimeStamp) throws FileNotFoundException {
		System.out.println("Initialising with output directory: " + outputDirectory);
		File dataFile = new File(dataFilePath);

        scanner = new Scanner(dataFile);
        String line = scanner.nextLine();
        String lineSplit[] = line.split(",");
        int nbSensors = lineSplit.length - 2;
    
        
		// Initialise sandman
		sandman = new Sandman();
		
		// Initialise watcher
		try {
			watchService = FileSystems.getDefault().newWatchService();
			inputDirectory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		counter = 0;
		
		sensors = new ArrayList<String>();
		for (int i=0; i<nbSensors; i++ ) {
			sensors.add(i, "daily_"+(i+1));
		}
		System.out.println(sensors);
	
		
//		firstSlice = new Slice(LocalDateTime.parse("2016-01-01T00:00"), Duration.ofHours(1));
		firstSlice = new Slice(LocalDateTime.parse(firstTimeStamp.replace("T", " "), formatter), Duration.ofHours(1));
		starterSlice = new Slice(LocalDateTime.parse(firstTimeStamp.replace("T", " "), formatter), Duration.ofHours(1));
//		firstSlice = new Slice(LocalDateTime.parse("2016-01-31T10:00"), Duration.ofHours(1));
		lastSlice = new Slice(LocalDateTime.parse("2016-05-03T23:00"), Duration.ofHours(1));
		
		currentSlice = firstSlice;
		
		sandman.init(firstSlice, sensors);
		sandman.addSensorFile(dataFile);
		
	}
	
	private void run(){
		EventClass currentAnom;
		Double currentDA;
		
		totalExecutionTime = new Long(0);
		
//		1024 for the multiplication and division of speed by 2
		setTimer(new Long(1024));
		
//		currentSlice = firstSlice;
		String line = scanner.nextLine();
	    String lineSplit[] = line.split(",");
	    currentSlice = new Slice(LocalDateTime.parse(lineSplit[0].replace("T", " "), formatter), Duration.ofHours(1));
	    
	    while (currentSlice.isBefore(starterSlice)) {
	    	
	    	line = scanner.nextLine();
		    lineSplit= line.split(",");
		    currentSlice = new Slice(LocalDateTime.parse(lineSplit[0].replace("T", " "), formatter), Duration.ofHours(1));
	    }
	
		while(true) {
			System.out.println(currentSlice);
			long startTime = System.currentTimeMillis();
			
			checkForInput();
	     
			System.out.println("System Running: " + running);
			
			if(running && currentSlice.isBefore(lastSlice)) {
				
				System.out.println(currentSlice);
				System.out.println(line);
			    lineSplit = line.split(",");
			    
				currentAnom = sandman.newMeasure(currentSlice, lineSplit, currentSlice);
				currentDA = sandman.getContext().getHistoryDA().get(currentSlice.getBegin());
				line = scanner.nextLine();
				// Write results to output file
				writeOutput(currentSlice, currentAnom);				
				currentSlice = currentSlice.nextSlice();
				counter++;

				long endTime = System.currentTimeMillis();
				totalExecutionTime += (endTime - startTime);
				System.out.println("Temps = " + totalExecutionTime.toString());
				
			}
			try {
				Thread.sleep(getTimer());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
	}

	// Have there been any events seen since last update
	private void checkForInput() {
		if(((key = watchService.poll()) != null)) {
			for (WatchEvent<?> event : key.pollEvents()) {
				if(event.context().toString().equals("play.json")) {
					// Play/Pause file found
					System.out.println("Play/Pause File Found");
					String fileName = inputDirectory + File.separator + event.context();
					running = readPlayFile(fileName);
					deleteFile(fileName);
				} else if(event.context().toString().equals("start.json")){
					System.out.println("Start File Found");
					String fileName = inputDirectory + File.separator + event.context();
					readStartFile(fileName);
					deleteFile(fileName);
				} else if(event.context().toString().equals("feedback.json")){
					// Feedback file found
					System.out.println("============================================================================================================");
					System.out.println("Feedback File Found");
					String fileName = inputDirectory + File.separator + event.context();
					readFeedbackFile(fileName);
					System.out.println(currentSlice);
					saveJSON.writeJSON(dataFilePath+".json", sandman.getProfile().listProfiles, sandman.getContext().getSensorDB().getSensors(), currentSlice, sandman.getContext(), sandman.getProfile());
					deleteFile(fileName);
				} else if(event.context().toString().equals("timer.json")){
					// Timer file found
					System.out.println("Timer update Found");
					String fileName = inputDirectory + File.separator + event.context();
					readTimerFile(fileName);
					deleteFile(fileName);
				} else {
					System.out.println("Unknown File Type");
				}
			}
			
			key.reset();
		}
	}
	
	@SuppressWarnings("unchecked")
	// Read a play/pause file to play/pause the system
	private boolean readPlayFile(String filePath) {
		JSONParser jsonParser = new JSONParser();
		boolean inputRunning = false;
		
		try(FileReader file = new FileReader(filePath)){
			//Read JSON file
			Object obj = jsonParser.parse(file);
			JSONArray array = new JSONArray();
			array.add(obj);
			JSONObject data = (JSONObject) array.get(0);
			inputRunning = (boolean) data.get("running");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return inputRunning;
	}
	
	@SuppressWarnings("unchecked")
	// Read a play/pause file to play/pause the system
	private void readStartFile(String filePath) {
		JSONParser jsonParser = new JSONParser();
		String timeStamp;
		
		try(FileReader file = new FileReader(filePath)){
			//Read JSON file
			Object obj = jsonParser.parse(file);
			JSONArray array = new JSONArray();
			array.add(obj);
			JSONObject data = (JSONObject) array.get(0);
			timeStamp = (String) data.get("timeStamp");
			
			//init(timeStamp);
			running = true;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	// Read a feedback file
	private void readFeedbackFile(String filePath) {
		JSONParser jsonParser = new JSONParser();
		JSONArray feedbackArray = new JSONArray();
		JSONObject feedbackPoint = new JSONObject();
		Slice feedbackSlice;
		
		this.feedbackReceived = true;
		
		try(FileReader file = new FileReader(filePath)){
			//Read JSON file
			Object obj = jsonParser.parse(file);
			JSONArray array = new JSONArray();
			array.add(obj);
			JSONObject data = (JSONObject) array.get(0);
			
			long arrayLength = (long) data.get("arrayLength");
			System.out.println(arrayLength + " feedback points found");
			
			feedbackArray = (JSONArray) data.get("feedbackArray");
			
			for(int i = (int) arrayLength-1; i >= 0; i--) {
				feedbackPoint = (JSONObject) feedbackArray.get(i);
				String timeStamp = (String) feedbackPoint.get("timeStamp");
				System.out.println("timeStampFeedback : "+timeStamp);
				String expertFeedback = (String) feedbackPoint.get("expertFeedback");
				System.out.println("expertFeedback : "+expertFeedback);
				boolean isAnom = (boolean) feedbackPoint.get("isAnom");				
				feedbackSlice = new Slice(LocalDateTime.parse(timeStamp), Duration.ofHours(1));
				// todo
				// GIVE FEEDBACK TO SYSTEM HERE
				// Convert from expert feedback to boolean isanomaly??
				// Need to access history somehow
				sandman.newFeedback(feedbackSlice, isAnom, expertFeedback); // This is the line that causes the sandman system to crash
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	// Read a timer file to update the speed of the system
	private void readTimerFile(String filePath) {
		JSONParser jsonParser = new JSONParser();
		Long newTimerValue;
		
		try(FileReader file = new FileReader(filePath)){
			//Read JSON file
			JSONObject data = (JSONObject) jsonParser.parse(file);
			newTimerValue = (Long) data.get("newTimerValue");
			setTimer(newTimerValue);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	// Function to delete any file given the path
	private void deleteFile(String filePath) {
		File f;
		f = new File(filePath);
		f.delete();
	}
	
	@SuppressWarnings("unchecked")
	private void writeOutput(Slice currentSlice, EventClass anomaly) {
		// Construct the array of data
		ArrayList<Double> weights = new ArrayList<>();
		HashMap<String, WeightAgent> weightAgentMap = sandman.getProfile().getProfile(sandman.getContext().currentProfil).getClassifierSituation().getWeightAgentMap();
		List<Integer> sensorsNumbers = new ArrayList<>();
		for(String weightAgent : weightAgentMap.keySet()) {
			sensorsNumbers.add(Integer.parseInt(weightAgent.substring(6)));
		}
		Collections.sort(sensorsNumbers);
		for(Integer weightAgent : sensorsNumbers) {
			weights.add(weightAgentMap.get("daily_" + weightAgent.toString()).getMyWeight());
		}
		JSONObject data = new JSONObject();
		Double altertThreshold = sandman.getContext().classifier.getAlertThreshold();
		Double currentDA = sandman.getContext().getHistoryDA().get(currentSlice);
		ArrayList<Double> profileValues = new ArrayList<>();
		for (String sensor : sensors) {
			profileValues.add(sandman.getContext().getProfile(sensor).get(currentSlice.getBegin().getHour()));
		}
		
		data.put("timeStamp", currentSlice.toString());
		data.put("anomalyState", anomaly.toString());
		data.put("alertThreshold", altertThreshold.toString());
		data.put("anomalyDegree", currentDA.toString());
		data.put("sensorsProfiles", profileValues.toString());
		data.put("weights", weights.toString());
		data.put("totalExecutionTime", totalExecutionTime.toString());
		System.out.println("--------OUTPUT------------");
		System.out.println("Timestamp : " + currentSlice.toString());
		System.out.println("Weight : " + weights.toString());
		System.out.println("Profile : " + profileValues.toString());
		System.out.println("--------------------------");
		
		// Combine into single array
		JSONObject timeArray = new JSONObject();
		timeArray.put("data", data);
		
		// Create file path
		String fileName = outputDirectory + File.separator + "output" + Integer.toString(counter) +".json";
		
		// Write JSON File
		try(FileWriter file = new FileWriter(fileName)){
			file.write(timeArray.toJSONString());
			System.out.println("Output sent to: " + fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void setDataFilePath(String filePath) {
		dataFilePath = filePath;
	}
	
	public void setOutputDirectory(Path dir) {
		outputDirectory = dir;
	}
	
	public void setInputDirectory(Path dir) {
		inputDirectory = dir;
	}
}
