package agent;

import java.util.ArrayList;

import messages.ConstraintAgentFeedbackMessage;

public class WeightAgent {

	private static final double MINWEIGHT = 0.01;

	private double myWeight = 1; // TODO 1 before 

	private double beta = 1;

	public static int lockProtect = -10;

	private ArrayList<ConstraintAgentFeedbackMessage> cMessagesHigher = new ArrayList<ConstraintAgentFeedbackMessage>();

	private ArrayList<ConstraintAgentFeedbackMessage> cMessagesLower = new ArrayList<ConstraintAgentFeedbackMessage>();

	public String decision = "";

	public ConstraintAgentFeedbackMessage selectedFeedback = null;

	private double DELTAMAX = 100;
	private double DELTAMIN = 0.1;

	private double MINIMALWEIGHTADJUSTMENT = 0.1;

	public enum LastAction {
		INCREASE, DECREASE, NOTHING
	};

	private LastAction lastAction = LastAction.NOTHING;

	private final double INFLUENCE_THRESHOLD = 0.95;

	public WeightAgent() {
		
	}

	public WeightAgent(WeightAgent a) {
		this.myWeight = a.myWeight;
		this.beta = a.beta;

	}
	/**
	 * Get the Higher feedback message with the highest criticality.
	 * 
	 * @return This feedback message
	 */
	private ConstraintAgentFeedbackMessage getMaxHigher() {
		ConstraintAgentFeedbackMessage tmp = null;
		if (cMessagesHigher.size() > 0) {
			double tmpMax = cMessagesHigher.get(0).criticality;
			for (ConstraintAgentFeedbackMessage c : cMessagesHigher) {
				if (c.criticality >= tmpMax) {
					tmp = c;
					tmpMax = c.criticality;
				}
			}
		}
		return tmp;
	}

	/**
	 * Get the Lower feedback message with the highest criticality.
	 * 
	 * @return This feedback message
	 */
	private ConstraintAgentFeedbackMessage getMaxLower() {
		ConstraintAgentFeedbackMessage tmp = null;
		if (cMessagesLower.size() > 0) {
			double tmpMax = cMessagesLower.get(0).criticality;
			for (ConstraintAgentFeedbackMessage c : cMessagesLower) {
				if (c.criticality >= tmpMax) {
					tmp = c;
					tmpMax = c.criticality;
				}
			}
		}
		return tmp;
	}
	/**
	 * Perform the weight update.
	 */
	public void doCycle() {
		decision = "";
		selectedFeedback = null;
		//System.out.println("==== Cycle calcul poids doCycle() pour "+this);
		ConstraintAgentFeedbackMessage maxCritCAH = null;
		ConstraintAgentFeedbackMessage maxCritCAL = null;
		//System.out.println("cMessagesHigher : "+cMessagesHigher);
		//System.out.println("cMessagesLower : "+cMessagesLower);
		if (cMessagesHigher.size() > 0)
			maxCritCAH = getMaxHigher();
		if (cMessagesLower.size() > 0)
			maxCritCAL = getMaxLower();

		if (cMessagesHigher.size() > 0 && cMessagesLower.size() > 0) {
			if (maxCritCAH.criticality >= maxCritCAL.criticality) {
				resolveCaseHigher(maxCritCAH, maxCritCAL);
				this.selectedFeedback = maxCritCAH;
			} else {
				resolveCaseLower(maxCritCAH, maxCritCAL);
				this.selectedFeedback = maxCritCAL;
			}
		} else {
			if (cMessagesHigher.size() > 0) {
				if (maxCritCAH.criticality > lockProtect)
					increase(maxCritCAH);
				this.selectedFeedback = maxCritCAH;
			} else {
				if (cMessagesLower.size() > 0) {
					if (maxCritCAL.criticality > lockProtect)
						decrease(maxCritCAL);
				} else {
					this.lastAction = LastAction.NOTHING;
				}
			}
		}
		clearMessages();
	}

	/**
	 * Select the next action (decrease the value of the weight or do nothing)
	 * 
	 * @param maxCritCAH
	 *            The value of the Higher feedback message
	 * @param maxCritCAL
	 *            The value of the Lower feedback message
	 */
	private void resolveCaseLower(ConstraintAgentFeedbackMessage maxCritCAH,
			ConstraintAgentFeedbackMessage maxCritCAL) {
	
		if (maxCritCAL.weightInfluence >= maxCritCAH.weightInfluence || 
			(maxCritCAL.sncNoCritChange && maxCritCAL.weightInfluence >= INFLUENCE_THRESHOLD)) {
			
			decrease(maxCritCAL);
		} else {
			lastAction = LastAction.NOTHING;
		}
	}

	/**
	 * Select the next action (increase the value of the weight or do nothing)
	 * 
	 * @param maxCritCAH
	 *            The value of the Higher feedback message
	 * @param maxCritCAL
	 *            The value of the Lower feedback message
	 */
	private void resolveCaseHigher(ConstraintAgentFeedbackMessage maxCritCAH,
			ConstraintAgentFeedbackMessage maxCritCAL) {
	
		if (maxCritCAH.weightInfluence >= maxCritCAL.weightInfluence ||
			(maxCritCAH.sncNoCritChange && maxCritCAH.weightInfluence >= INFLUENCE_THRESHOLD)) {
			
			increase(maxCritCAH);
		} else {
			lastAction = LastAction.NOTHING;
		}
	}

	private void clearMessages() {
		cMessagesHigher = new ArrayList<ConstraintAgentFeedbackMessage>();
		cMessagesLower = new ArrayList<ConstraintAgentFeedbackMessage>();
	}

	/**
	 * Increase the value of the weight
	 * 
	 */
	private void increase(ConstraintAgentFeedbackMessage constraintAgent) {
		

		if (lastAction.equals(LastAction.INCREASE) ) {
			
			beta = Math.min(DELTAMAX, beta * 2);
		} 
		else {
			if (lastAction.equals(LastAction.DECREASE)) {
				
				beta = Math.max(DELTAMIN, beta * 1 / 3);
			}
		}
		System.out.println("beta : " + beta);
		System.out.println("WEIGHT INCREASE OF: "  +Math.max(MINIMALWEIGHTADJUSTMENT, beta));
		myWeight = myWeight + beta;
		System.out.println("myWeight apres  = "+myWeight);

		
		lastAction = LastAction.INCREASE;
	}

	public double getDelta() {
		return beta;
	}
	
	
	/**
	 * Decrease the value of the weight
	 * 
	 */
	private void decrease(ConstraintAgentFeedbackMessage constraintAgent) {
		System.out.println("myWeight avant  = "+myWeight);
		if (myWeight== MINWEIGHT) {
			lastAction = LastAction.NOTHING;
			return;
		}
		
		System.out.println("myWeight   = "+myWeight);
		if (lastAction.equals(LastAction.DECREASE) ) {
			beta = Math.min(DELTAMAX, beta * 2);
		} 
		else {
			if (lastAction.equals(LastAction.INCREASE)) {
				beta = Math.max(DELTAMIN, beta * 1 / 3);

			}
		}
		System.out.println("WEIGHT DECREASE OF : " + beta);
		myWeight = Math.max(MINWEIGHT, myWeight - beta);
		System.out.println("myWeight apres  = "+myWeight);
		
		lastAction = LastAction.DECREASE;
	}

	/**
	 * Add a new feedback message
	 * 
	 * @param mess
	 *            The feedback message received
	 */
	public void newConstraintFeedbackMessage(ConstraintAgentFeedbackMessage mess) {
		switch (mess.type) {
		case HIGHERTHAN:
			cMessagesHigher.add(mess);
			break;
		case LOWERTHANOREQUAL:
			cMessagesLower.add(mess);
			break;
		default:
			break;
		}
	}

	public void setMyWeight(double myWeight) {
		this.myWeight = myWeight;
	}
	
	public void setBeta(double beta) {
		this.beta = beta;
	}

	public double getMyWeight() {
		return myWeight;
	}
	
	@Override
	public String toString() {
		return "WeightAgent [myWeight=" + myWeight + "]";
	}
	



}