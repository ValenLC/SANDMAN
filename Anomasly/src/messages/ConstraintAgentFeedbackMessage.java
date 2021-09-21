package messages;

import agent.SituationAgent;
import agent.SituationAgent.Relation;

/**
 * @author Nicolas Verstaevel - nicolas.verstaevel@irit.fr This class models the
 *         feedbacks that are sent by a Constraint agent to a Weight agent. It
 *         contains all the required information by the Weight agent to decide
 *         of its action.
 *
 */
public class ConstraintAgentFeedbackMessage implements Comparable<ConstraintAgentFeedbackMessage> {

	/**
	 * The criticality of the Constraint agent.
	 */
	public Double criticality;
	/**
	 * The relation type of the inequality of the Constraint agent.
	 */
	public Relation type;
	/**
	 * The Weight influence of the Weight agent recipient of this message.
	 */
	public double weightInfluence;
	/**
	 * A reference to the transmitter Constraint agent.
	 */
	public SituationAgent c;

	/**
	 * The value of the disparity associated to the Weight agent in the inequality.
	 */
	public double disparity;
	
	public boolean sncNoCritChange;

	/**
	 * Build a new Constraint Agent Feedback message.
	 * 
	 * @param mycrit
	 *            The criticality of the Constraint agent
	 * @param type
	 *            The relation type of the inequality of the Constraint agent.
	 * @param weightInfluence
	 *            The Weight influence of the Weight agent recipient of this
	 *            message.
	 * @param sncNoCritChange 
	 * @param c
	 *            A reference to the transmitter Constraint agent.
	 */
	public ConstraintAgentFeedbackMessage(double mycrit, Relation type, double weightInfluence, double disparity,
			boolean sncNoCritChange, SituationAgent c) {
		this.criticality = mycrit;
		this.disparity = disparity;
		this.type = type;
		this.weightInfluence = weightInfluence;
		this.sncNoCritChange = sncNoCritChange;
		this.c = c;
	}

	@Override
	public int compareTo(ConstraintAgentFeedbackMessage arg0) {
		return criticality.compareTo(arg0.criticality);
	}
}
