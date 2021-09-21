/**
 * 
 */
package time;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;


/**
 * @author comhoussin
 * Slice for normalized data points
 */
public class Slice implements Comparable<Slice> {
	private LocalDateTime begin; 
	private Duration duration; 
	
	public Slice(LocalDateTime begin, Duration duration) {
		this.begin = begin;
		this.duration = duration;
	}
	
	public Slice(Slice s) {
		this.begin = s.getBegin();
		this.duration = s.getDuration();
	}
	
	public LocalDateTime nextSliceBegin() {
		return begin.plus(duration);
	}

	public LocalDateTime getBegin() {
		return begin;
	}

	public boolean isBefore(Slice lastSlice) {
		return begin.isBefore(lastSlice.getBegin());
	}

	public boolean isBefore(LocalDateTime timestamp) {
		return begin.isBefore(timestamp);
	}
	
	public boolean isAfter(Slice lastSlice) {
		return begin.isAfter(lastSlice.getBegin());
	}

	public boolean isAfter(LocalDateTime timestamp) {
		return begin.isAfter(timestamp);
	}
	
	public Slice nextSlice() {
		return new Slice(begin.plus(duration), duration);
	}

	public Slice previousSlice() {
		return new Slice(begin.minus(duration), duration);
	}
	
	public boolean contains(LocalDateTime timestamp) {
//		return begin.isBefore(timestamp) && 
		return  begin.truncatedTo(ChronoUnit.SECONDS).isEqual(timestamp.truncatedTo(ChronoUnit.SECONDS)) ||
				(begin.isBefore(timestamp) && begin.plus(duration).isAfter(timestamp));
	}

	@Override
	public int compareTo(Slice slice) {
		if (begin.truncatedTo(ChronoUnit.SECONDS).isEqual(slice.getBegin().truncatedTo(ChronoUnit.SECONDS))) {
			return 0;
		}
		if (begin.truncatedTo(ChronoUnit.SECONDS).isBefore(slice.getBegin().truncatedTo(ChronoUnit.SECONDS))) {
			return -1;
		}
		return 1;
	}

	public Duration getDuration() {
		return duration;
	}
/*
	public Slice modulo(Slice first, int mod) {
		int distance = relativeDistanceTo(first);
		Duration toAdd = duration.multipliedBy(distance % mod);
		return first.plus(toAdd);		
	}

	public Slice plus(Duration duration) {
		return new Slice(begin.plus(duration), duration);
	}
	
	public Slice minus(Duration duration) {
		return new Slice(begin.minus(duration), duration);
	}
*/
	public Slice addSlices(int nbSlice) {
		Slice slice = new Slice(this);
		for (int i=0; i<nbSlice; i++) {
			slice = slice.nextSlice();
		}
		return slice; 
	}
	
	public Slice substractSlices(int nbSlice) {
		Slice slice = new Slice(this);
		for (int i=0; i<nbSlice; i++) {
			slice = slice.previousSlice();
		}
		return slice; 
	}

	public int relativeDistanceTo(Slice slice) {
		Duration dur = Duration.between(begin, slice.getBegin());
		long dist = dur.toMillis() / duration.toMillis();
		if (begin.isBefore(slice.getBegin())) {
			return (int) dist;
		}
		return (int) -dist;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o.getClass().equals(this.getClass())) {
			
			Slice other = (Slice) o;
			return (begin.getYear() == other.begin.getYear() &&
					begin.getMonth().equals(other.begin.getMonth()) &&
					begin.getDayOfMonth() == other.begin.getDayOfMonth() &&
					begin.getHour() == other.begin.getHour());
		}
		return false;
	}

	public String toString() {
		return begin.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(begin.getYear(), begin.getMonth(), begin.getDayOfMonth(), begin.getHour());
	}
	
}
