package controller;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Watcher {
	private WatchService watchService;
	private Path watchDirectory;
	private WatchKey key;
	private boolean systemRunning = true;
	
	public Watcher(Path dir) {
		try {
			watchService = FileSystems.getDefault().newWatchService();
			watchDirectory = dir;
			watchDirectory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Watcher() {
		// TODO Auto-generated constructor stub
	}

	// Have there been any events seen since last update
	public boolean eventSeen() {
		boolean event = false;
		
		event = ((key = watchService.poll()) != null);
		
		System.out.println(event);
		return event;
	}
	
	// Function to run when there have been events detected
	public void handleEvents() {
		for (WatchEvent<?> event : key.pollEvents()) {
			System.out.println(event.context().toString());
		
			if(event.context().toString().equals("play.json")) {
				// Play/Pause file found
				System.out.println("Play File Found");
				String fileName = watchDirectory + File.separator + event.context();
				systemRunning = readPlayFile(fileName);
				deleteFile(fileName);
			} else {
				System.out.println("ERROR");
				}
		}
		
		key.reset();
	}
	public static void main(String[] args) throws IOException, InterruptedException {
		Watcher test = new Watcher();
		
        WatchService watchService
          = FileSystems.getDefault().newWatchService();
 
        Path path =  Paths.get(System.getProperty("user.dir"), "Input");
        
        
 
        path.register(
          watchService, 
            StandardWatchEventKinds.ENTRY_CREATE, 
              StandardWatchEventKinds.ENTRY_DELETE, 
                StandardWatchEventKinds.ENTRY_MODIFY);
        
        path.register(
                watchService, 
                  StandardWatchEventKinds.ENTRY_CREATE);
 
        WatchKey key;
        while ((key = watchService.take()) != null) {
            for (WatchEvent<?> event : key.pollEvents()) {
            	System.out.println(event.context().toString());
            	if(event.context().toString().equals("play.json")) {
            		System.out.println("Read File Found");
            		String fileName = path + File.separator + event.context();
            		System.out.println(test.readPlayFile(fileName));
            		test.deleteFile(fileName);
            	} else {
            		System.out.println("ERROR");
            	}
            	
            	// Create file path
        		String fileName = path + File.separator + event.context();
        		System.out.println(fileName);
        		
        		// Write JSON File
        		try(FileReader file = new FileReader(fileName)){
        			JSONParser jsonParser = null;
					//Read JSON file
                    Object obj = jsonParser.parse(file);
         
                    JSONArray array = new JSONArray();
                    array.add(obj);
                    JSONObject test1 = (JSONObject) array.get(0);
                    
                    System.out.println(test1.get("running"));
        		} catch (IOException e) {
        			e.printStackTrace();
        		} catch (ParseException e) {
        			e.printStackTrace();
        		}
        		
                System.out.println(
                  "Event kind:" + event.kind() 
                    + ". File affected: " + event.context() + ".");
            }
            key.reset();
        }
    }
	
	private boolean readPlayFile(String filePath) {
		JSONParser jsonParser = new JSONParser();
		boolean running = false;
		
		try(FileReader file = new FileReader(filePath)){
			//Read JSON file
            Object obj = jsonParser.parse(file);
 
            JSONArray array = new JSONArray();
            array.add(obj);
            JSONObject test = (JSONObject) array.get(0);
            
//            System.out.println(test.get("running"));
            running = (boolean) test.get("running");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return running;
	}
	
	private void deleteFile(String filePath) {
		File f;
		f = new File(filePath);
		f.delete();
	}
	
	public boolean getSystemRunning() {
		return systemRunning;
	}
}
