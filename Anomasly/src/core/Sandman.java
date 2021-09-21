/**
 * 
 */
package core;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import Oracle.Oracle;
import Oracle.OracleSimple;
import agent.SituationAgent;
import core.AnoMASly.EventClass;
import core.logging.LogSandman;
import input.Sensor;
import input.SensorDB;
import profile.SensorProfile;
import time.Slice;

/**
 * @author Maxime Houssin - maxime.houssin@irit.fr
 *
 */

public class Sandman {
	
	private ModuleProfile profile; // S1
	private ModuleContext context; // S2
	private SensorDB sensorDB;
	
	private LogSandman logSandman;
	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	private final double PROFILE_MANAGER_LAMBDA = 0.25;
	private final Duration SLICE_DURATION = Duration.ofHours(1);
	private final int ALERT_THRESHOLD = 1000;
	private final int CLASSIFIER_MAX_TRIES = 100;
	private final double CLASSIFIER_PRECISION = 8;
	public static final int NB_SLICE_DISPARITY = 1;
	
	public final Slice firstSlice = new Slice(LocalDateTime.parse("2016-01-01T00:00".replace("T", " "), formatter), Duration.ofHours(1));
	private final Slice CONTEXT_SLICES_BEFORE_LEARNING =  firstSlice.addSlices(24*7);
	
//	private final int CONTEXT_SLICES_BEFORE_LEARNING = 0;
	//public static final double SENSOR_NOISE = 0.0001;
	public static final double SENSOR_NOISE = 0.0;
	public static final int PREVIOUS_DISPARITY_TOTAL_LIMIT = 8;
	public static final double BACK_TO_NORMAL_THRESHOLD = 100;
	public static final double SIMILARITY_THRESHOLD = 0.4;
	
	
	public Sandman() {
		
		this.logSandman = new LogSandman();

		this.sensorDB = new SensorDB();
		this.profile = new ModuleProfile();
		this.context = new ModuleContext(sensorDB, logSandman, CLASSIFIER_PRECISION, CLASSIFIER_MAX_TRIES, 
										CONTEXT_SLICES_BEFORE_LEARNING, NB_SLICE_DISPARITY, PREVIOUS_DISPARITY_TOTAL_LIMIT);
		
		this.profile.setContext(context);
		this.context.setProfile(profile);
	}

	
	// Modified function
	public void newFeedback(Slice currentSlice, boolean isAnom, String expertFeedback) {
		// both modules are independant
		
		context.readFeedback(currentSlice, isAnom, expertFeedback);
	}

	public EventClass newMeasure(Slice currentSlice,  String lineSplit[], Slice slice) {
		SensorDB.addNewValuesSensor(lineSplit, slice);
		System.out.println(currentSlice.getBegin().toString());
		return context.runClassification(currentSlice);
	}
	
	public void init(Slice firstSlice, List selectedSensorsNames) {
	
		context.init(selectedSensorsNames, ALERT_THRESHOLD);
	}

	private void initSensors(Slice firstSlice, List<Sensor> selectedSensors) {
		for (Sensor s : selectedSensors) {
			s.sliceValues(firstSlice);
		}
	}

	public Slice computeEarliestSlice(List selectedSensors) {
		return sensorDB.computeEarliestSlice();
	}

	public int getALERT_THRESHOLD() {
		return ALERT_THRESHOLD;
	}

	public Slice getEarliestSlice(List selectedSensors) {
		return sensorDB.computeEarliestSlice();
	}

	public Slice computeLatestSlice(List selectedSensors) {
		return sensorDB.computeLatestSlice();
	}
	
	public List<String> getSensorsNames() {
		return new ArrayList<String>(sensorDB.getAllSensors());
	}

	public LogSandman getLogSandman() {
		return logSandman;
	}
	
	// I ADDED
	public ModuleContext getContext() {
		return context;
	}

	public ModuleProfile getProfile() {
		return profile;
	}
	public void addSensorFile(File file) {
		sensorDB.addSensorFile(file);
	}
	
	public void setUseContextUpdate(boolean useContextUpdate) {
		context.setUseContextUpdate(useContextUpdate);
	}
	
	public void setUseProfileUpdate(boolean useProfileUpdate) {
		context.setUseProfileUpdate(useProfileUpdate);
	}

	public String profilesToCSV() {
		return profile.profilesToCSV();
	}

	public void loadProfiles(String filename, String content) {
		profile.loadProfiles(filename, content);
	}

	public String cyclesPerSituationToCSV() {
		return logSandman.cyclesPerSituationToCSV();
	}
}
