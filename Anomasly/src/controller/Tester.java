package controller;

import java.io.*;

public class Tester {
	public static void main(String[] args)
    {
        Runtime runtime = Runtime.getRuntime();     //getting Runtime object
 
        try
        {
        	System.out.println("Hello");
//            runtime.exec("cd C:\\Users\\ander\\OneDrive\\Documents\\Nicolas Baudin Internship\\Python Code\\GraphicalInterface");
//            runtime.exec("pipenv run python main.py");
//            String cmd = "python\\C:\\Users\\ander\\OneDrive\\Documents\\Nicolas Baudin Internship\\Python Code\\GraphicalInterface\\main.py";
//        	String cmd = "C:/Users/ander/OneDrive/Documents/\"Nicolas Baudin Internship\"/\"Python Code\"/GraphicalInterface/run_interface.bat";
        	String cmd = "run_interface.bat";
        	System.out.println(cmd);
            
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(cmd);
             
            // retrieve output from python script
            BufferedReader bfr = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line = "";
            while((line = bfr.readLine()) != null) {
            	// display each output line form python script
            	System.out.println(line);
            }
//            runtime.exec("pipenv run python main.py C:\\Users\\ander\\OneDrive\\Documents\\Nicolas Baudin Internship\\Python Code\\GraphicalInterface");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
// pipenv run python main.py C:\Users\ander\OneDrive\Documents\Nicolas Baudin Internship\Python Code\GraphicalInterface
//C:\Users\ander\OneDrive\Documents\"Nicolas Baudin Internship"\"Python Code"\GraphicalInterface\run_interface.bat