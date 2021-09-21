package tools.avt;



public interface FeedbackFunctionInterface {
	
	/**
	 * Get the current value of the function.
	 * @return The vurrent value of the function.
	 */
	public Double getValue();

	/**
	 * Increase confidence value by following the Lambda function law.
	 */
	public void increase();

	/**
	 * Decrease confidence value by following the Lambda function law.
	 */
	public void decrease();

	/**
	 * Set the value.
	 * @param value The value to be set.
	 */
	public void setValue(Double value);

	/**
	 * Confirm the current value. 
	 */
	public void confirm();
}
