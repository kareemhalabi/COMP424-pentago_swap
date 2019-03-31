package student_player;

import boardgame.Board;
import pentago_swap.PentagoBoardState;
import pentago_swap.PentagoMove;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

class UCTNode {

	// Create a random number generator for the class
	private static Random rng = new Random();

	private double winScore;
	private int numSims;

	//TODO might only store moves and not states at each step (space/memory tradeoff)
	private PentagoBoardState state;
	private PentagoMove move;
	private UCTNode parent;
	private ArrayList<UCTNode> children;

	private static final double EXPLOITATION_PARAM = Math.sqrt(2);

	UCTNode(PentagoBoardState state, PentagoMove move) {
		numSims = 0;
		winScore = 0;
		this.state = state;
		this.move = move;
		this.children = new ArrayList<>();
	}

	void backPropagate(int winner) {

		UCTNode currentNode = this;
		while(currentNode != null) {
			currentNode.numSims++;
			// Board always toggles to opponent after the end of the game
			if(currentNode.getState().getOpponent() == winner)
				currentNode.winScore += 1;
			else if(winner == Board.DRAW)
				currentNode.winScore += 0.5;

			currentNode = currentNode.parent;
		}
	}

	double getStateValue() {

		if (this.numSims == 0)
			return Double.MAX_VALUE;

		return (this.winScore / (double) this.numSims) + EXPLOITATION_PARAM * Math.sqrt(Math.log(this.parent.numSims)/this.numSims);
	}

	ArrayList<UCTNode> getChildren() {
		return children;
	}

	UCTNode getRandomChild() {
		return children.get(rng.nextInt(children.size()));
	}

	UCTNode getMaxSimsChild() {
		return Collections.max(this.children, Comparator.comparing(c -> c.numSims));
	}

	void addChild(UCTNode child) {
		this.children.add(child);
		child.parent = this;
	}

	PentagoBoardState getState() {
		return state;
	}

	public PentagoMove getMove() {
		return move;
	}

	public double getWinScore() {
		return winScore;
	}

	public int getNumSims() {
		return numSims;
	}
}
