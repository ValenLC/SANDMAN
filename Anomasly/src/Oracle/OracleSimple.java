/**
 * 
 */
package Oracle;

import java.time.LocalDateTime;
import java.util.SortedSet;
import java.util.TreeSet;

import time.Slice;

/**
 * @author Maxime Houssin - maxime.houssin@irit.fr
 * Oracle intended to be used along ReaderTsimulus and ProfileManagerSimple
 */
public class OracleSimple implements Oracle {
	/**
	 * Ordered set of anomalies timestamps
	 */
	private SortedSet<LocalDateTime> anomalies;
	
	public OracleSimple() {
		anomalies = new TreeSet<LocalDateTime>();
	}
	
	@Override
	public void addAnomaly(LocalDateTime timestamp) {
		anomalies.add(timestamp);
	}
	
	/**
	 * Prints timestamp -> sensorNames[] list
	 */
//	public String toString() {
//		String s = new String("Anomalies : \n");
//		for (LocalDateTime timestamp : anomalies.keySet()) {
//			s += timestamp.toString() + " : [";
//			for (String sensorName : anomalies.get(timestamp)) {
//				s += sensorName + ",";
//			}
//			s = s.substring(0, s.length()-1);
//			s += "]\n";
//		}
//		return s;
//	}
	
	@Override
	public boolean isAnomaly(Slice slice) {
		return !anomalies.subSet(slice.getBegin(), slice.nextSlice().getBegin()).isEmpty();
	}
}
