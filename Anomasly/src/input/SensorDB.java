/**
 * 
 */
package input;

import java.awt.List;
import java.io.File;
import java.io.FileNotFoundException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import time.Slice;

/**
 * @author Maxime Houssin - maxime.houssin@irit.fr
 *
 */
public class SensorDB {
	private File file;
	private static HashMap<Slice, Map<String, String>> sensors = new HashMap<Slice, Map<String, String>>();
	private int nbFields;
	private static int nbSensors;
	private int lineStartBuffer;
	private int lineEndBuffer;
	private String[][] splitsBuffer;
	private String delimiter = ",";
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	
	// list[0] == lower limit, list [1] == upper limit
	private Map<String, List> normalisationLimits = new HashMap<String, List>();

	public Slice computeEarliestSlice() {
		// TODO : do it properly
		return new Slice(LocalDateTime.of(2016, 1, 1, 0, 0), Duration.ofHours(1));
	}

	public Slice computeLatestSlice() {
		// TODO : do it properly
		return new Slice(LocalDateTime.of(2016, 1, 31, 23, 0), Duration.ofHours(1));
	}

	public void addSensorFile(File file) {
		this.file = file;
		try {
			Scanner scanner = new Scanner(file);
			String line = scanner.nextLine();
			String lineSplit[] = line.split(delimiter);
			this.nbFields = lineSplit.length;
			this.nbSensors = lineSplit.length - 2;
			Map valueSensor = new HashMap<String, String>();
			for (int i = 1; i <= nbSensors; i++) {
				valueSensor.put("daily_"+i, lineSplit[i]); 
			}
			//sensors.put( new Slice(LocalDateTime.parse(lineSplit[0].replace("T", " "), formatter),
			//		Duration.ofHours(1)), valueSensor);
		//	scanner.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * fromLine, toLine included both
	 */
	private String[][] lineSplits(int fromLine, int toLine) {
		if (lineStartBuffer == fromLine && lineEndBuffer == toLine) {
			return splitsBuffer;
		}
		String[][] splits = new String[toLine - fromLine + 1][nbFields];
		try {
			Scanner scanner = new Scanner(this.file);
			int count = -1;
			while (count < fromLine - 1) {
				scanner.nextLine();
				count++;
			}
			int i = 0;
			while (count++ < toLine) {
				String line = scanner.nextLine();
				String lineSplit[] = line.split(delimiter);
				splits[i++] = lineSplit;
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		lineStartBuffer = fromLine;
		lineEndBuffer = toLine;
		splitsBuffer = splits;
		return splits;
	}

	private int computeLineNumber(Slice slice) {
		return Math.abs(slice.relativeDistanceTo(computeEarliestSlice())) + 1;
	}

	/*
	 * This is wonky, splitting doesn't split empty fields
	 */
	public boolean isAnomaly(Slice slice) {
		int lineNumber = computeLineNumber(slice);
		String splits[][] = lineSplits(lineNumber, lineNumber);
		// return !(splits[0][splits[0].length - 2].isEmpty());
		return (splits[0].length > nbSensors);
	}

	public Double getSensorValue(Slice slice,String sensorName) {
		return Double.parseDouble( sensors.get(slice).get(sensorName));
	}
	
	public Map<String, String> getSliceValues(Slice slice) {
		return sensors.get(slice);
	}

    public Map<Slice, Double> getSensorLastValues(String sensorName, Slice slice, int nbSliceDisparity) {
        Slice sliceEnd = slice;
        Slice sliceBegin = slice.addSlices(nbSliceDisparity-1);
     
        HashMap<Slice, Double> values = new HashMap<Slice, Double>();

   
        while( sliceBegin.isBefore(sliceEnd) || sliceBegin.equals(sliceEnd)) {
        	
            values.put(sliceBegin, Double.parseDouble(sensors.get(sliceBegin).get(sensorName)));    
            sliceBegin = sliceBegin.addSlices(1);
        }
        return values;
    }

	public static void addNewValuesSensor(String lineSplit[], Slice slice) {
		Map valueSensor = new HashMap<String, String>();

		for (int i = 1; i <= nbSensors; i++) {
		
           	if (lineSplit[i].equals(" ")) { 	
    			String  value= sensors.get(slice.substractSlices(1)).get("daily_"+i);
    			valueSensor.put("daily_"+i, value);
           	}
        	else {
        		valueSensor.put("daily_"+i, lineSplit[i]); 
        	}
        	
        
        }
        sensors.put(slice, valueSensor);
       
		
	}
	public HashMap<Slice, Map<String, String>> getSensors() {
		return this.sensors;
	}
	
	public void setSensors(HashMap<Slice, Map<String, String>> sensors) {
		this.sensors=sensors;
	}
}
