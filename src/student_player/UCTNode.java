package student_player;

import java.util.Stack;

import static student_player.PentagoBitBoard.DRAW;

class UCTNode {

	private int winScore;
	private int numSims;

	private long move;

	private UCTNode parent;
	private UCTNode[] children;

	private static final double EXPLOITATION_PARAM = Math.sqrt(2);

	UCTNode(long move) {
		this.move = move;
	}

	void backPropagate(byte[] result) {

		UCTNode currentNode = this;
		while(currentNode != null) {
			currentNode.numSims += 2;
			// Board always toggles to opponent after the end of the game
			if(result[1] == result[0])
				currentNode.winScore += 2;
			else if(result[0] == DRAW)
				currentNode.winScore += 1;

			// Toggle player
			result[1] = (byte) (1 - result[1]);

			currentNode = currentNode.parent;
		}
	}

	double getStateValue() {

		if (this.numSims == 0)
			return Double.MAX_VALUE;

		return (this.winScore / (double) this.numSims) + EXPLOITATION_PARAM * Math.sqrt(Math.log(this.parent.numSims)/this.numSims);
	}

	public void setParent(UCTNode parent) {
		this.parent = parent;
	}

	public boolean hasChildren() {
		return !(this.children == null || this.children.length == 0);
	}

	UCTNode[] getChildren() {
		return children;
	}

	UCTNode getRandomChild() {
		return children[StudentPlayer.rng.nextInt(children.length)];
	}

	UCTNode getMaxSimsChild() {
		int maxSims = Integer.MIN_VALUE;
		int maxIndex = -1;

		for(int i = 0; i < this.children.length; i++) {
			if(this.children[i].numSims > maxSims) {
				maxSims = this.children[i].numSims;
				maxIndex = i;
			}
		}

		return this.children[maxIndex];
	}

	public void setChildren(UCTNode[] children) {
		this.children = children;
	}


	public PentagoBitBoard getState(PentagoBitBoard startState) {

		// If we are at the root, game state is unchanged
		if(this.move == 0) return startState;

		// Get the chain of moves from the parent to this move
		Stack<Long> moveStack = new Stack<>();
		UCTNode currentNode = this;
		while(currentNode != null && currentNode.move != 0) {
			moveStack.push(currentNode.move);
			currentNode = currentNode.parent;
		}

		// Apply the moves
		PentagoBitBoard endState = (PentagoBitBoard) startState.clone();
		while(!moveStack.isEmpty()) {
			endState.processMove(moveStack.pop());
		}

		return endState;
	}

	public long getMove() {
		return move;
	}

	public double getWinScore() {
		return winScore;
	}

	public int getNumSims() {
		return numSims;
	}
}
