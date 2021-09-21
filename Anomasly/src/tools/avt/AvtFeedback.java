package tools.avt;



/**
 * 
 * Feedbacks for Avt.
 * 
 * @author Verstaevel Nicolas - nicolas.a.verstaevel@sogeti.com
 * @since 02/2015
 *
 */
public enum AvtFeedback {
	/**
	 * Feedback if the value is greater than the current value.
	 */
	GREATER,
	/**
	 * Feedback if the value is lower than the current value.
	 */
	LOWER,
	/**
	 * Feedback if the value is good.
	 */
	GOOD;

	public String toString() {
		switch (this) {
		case GREATER:
			return "Greater";
		case LOWER:
			return "Lower";
		case GOOD:
			return "Good";
		default:
			return "not a feedback";
		}
	}
}