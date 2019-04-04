package student_player;

import boardgame.Board;
import pentago_swap.PentagoBoardState;
import pentago_swap.PentagoMove;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Stack;

class UCTNode {

	private int winScore;
	private int numSims;

	private PentagoMove move;
	private UCTNode parent;
	private ArrayList<UCTNode> children;

	private static final double EXPLOITATION_PARAM = Math.sqrt(2);

	UCTNode(PentagoMove move) {
		this.move = move;
		this.children = new ArrayList<>();
	}

	void backPropagate(int[] result) {

		UCTNode currentNode = this;
		while(currentNode != null) {
			currentNode.numSims += 2;
			// Board always toggles to opponent after the end of the game
			if(result[1] == result[0])
				currentNode.winScore += 2;
			else if(result[0] == Board.DRAW)
				currentNode.winScore += 1;

			// Toggle player
			result[1] = 1 - result[1];

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
		return children.get(StudentPlayer.rng.nextInt(children.size()));
	}

	UCTNode getMaxSimsChild() {
		return Collections.max(this.children, Comparator.comparing(c -> c.numSims));
	}

	void addChild(UCTNode child) {
		this.children.add(child);
		child.parent = this;
	}

	PentagoBoardState getState(PentagoBoardState startState) {

		// If we are at the root, game state is unchanged
		if(this.move == null) return startState;

		// Get the chain of moves from the parent to this move
		Stack<PentagoMove> moveStack = new Stack<>();
		UCTNode currentNode = this;
		while (currentNode != null && currentNode.move != null) {
			moveStack.push(currentNode.move);
			currentNode = currentNode.parent;
		}

		// Apply the moves
		PentagoBoardState endState = (PentagoBoardState) startState.clone();
		while(!moveStack.isEmpty()) {
			endState.processMove(moveStack.pop());
		}

		return endState;
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
