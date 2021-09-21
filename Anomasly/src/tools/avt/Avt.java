package tools.avt;

import java.io.Serializable;

/**
 * For more information about AVT, please refers to work of Sylvain Lemouzy -
 * Syst�mes interactifs auto-adaptatifs par syst�mes multi-agents
 * auto-organisateurs : application � la personnalisation de l'acc�s �
 * l'information PhD Thesis - French
 * 
 * @author Verstaevel Nicolas - nicolas.a.verstaevel@sogeti.com
 * @since 02/2015
 */

public class Avt implements Serializable, FeedbackFunctionInterface {

	private static final long serialVersionUID = -7361906860711782367L;
	/**
	 * Acceleration value.
	 */
	private double _acceleration = 2.0;
	/**
	 * Decceleration value.
	 */
	private double _decceleration = 1. / 3.;
	/**
	 * Current value.
	 */
	private double _value;
	/**
	 * Delta value.
	 */
	private double _delta;
	/**
	 * Last feedback.
	 */
	private AvtFeedback _lastFeedback = AvtFeedback.GOOD;

	/**
	 * Constructor.
	 * 
	 * @param value
	 *            Initial value of the AVT.
	 * @param initialDelta
	 *            Initial delta value of the AVT.
	 */
	public Avt(Double value, Double initialDelta) {
		this._value = value;
		this._delta = initialDelta;
		// this._delta = 0;
	}

	/**
	 * Updating current value according to the feedback.
	 * 
	 * @param f
	 *            Current feedback.
	 */
	public void update(AvtFeedback f) {
		switch (_lastFeedback) {
		case GREATER:
			switch (f) {
			case GREATER:
				_delta = _delta * _acceleration;
				_value = _value + _delta;
				_lastFeedback = AvtFeedback.GREATER;
				break;
			case LOWER:
				_delta = _delta * _decceleration;
				_value = _value - _delta;
				_lastFeedback = AvtFeedback.LOWER;
				break;
			case GOOD:
				_delta = _delta * _decceleration;
				_lastFeedback = AvtFeedback.GOOD;
				break;
			}
			break;
		case LOWER:
			switch (f) {
			case GREATER:
				_delta = _delta * _decceleration;
				_value = _value + _delta;
				_lastFeedback = AvtFeedback.GREATER;
				break;
			case LOWER:
				_delta = _delta * _acceleration;
				_value = _value - _delta;
				_lastFeedback = AvtFeedback.LOWER;
				break;
			case GOOD:
				_delta = _delta * _decceleration;
				_lastFeedback = AvtFeedback.GOOD;
				break;
			}
			break;
		case GOOD:
			switch (f) {
			case GREATER:
				_value = _value + _delta;
				_lastFeedback = AvtFeedback.GREATER;
				break;
			case LOWER:
				_value = _value - _delta;
				_lastFeedback = AvtFeedback.LOWER;
				break;
			case GOOD:
				_delta = _delta * _decceleration;
				_lastFeedback = AvtFeedback.GOOD;
				break;
			}
			break;
		}
	}

	/**
	 * Get the current delta value.
	 * 
	 * @return The current delta value.
	 */
	public Double getDelta() {
		return _delta;
	}

	/**
	 * Get the current value.
	 * 
	 * @return The current value.
	 */
	public Double getValue() {
		return _value;
	}

	/**
	 * Update the AVT values with new ones.
	 * 
	 * @param value
	 *            The new value of the AVT
	 * @param delta
	 *            The new value of the delta parameter.
	 */
	public void updateValue(double value, Double delta) {
		_value = value;
		_delta = delta;
	}

	@Override
	public void increase() {
		this.update(AvtFeedback.GREATER);
	}

	@Override
	public void decrease() {
		this.update(AvtFeedback.LOWER);
	}

	@Override
	public void setValue(Double value) {
		this._value = value;
	}

	@Override
	public void confirm() {
		this.update(AvtFeedback.GOOD);
	}
}
