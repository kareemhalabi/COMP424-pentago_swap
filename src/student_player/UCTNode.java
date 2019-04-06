package student_player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static student_player.PentagoBitBoard.DRAW;

class UCTNode {

	private int winScore;
	private int numSims;

	private long move;
	private PentagoBitBoard bitBoard;

	private UCTNode parent;
	private ArrayList<UCTNode> children;

	private static final double EXPLOITATION_PARAM = Math.sqrt(2);

	UCTNode(PentagoBitBoard bitBoard, long move) {
		this.bitBoard = bitBoard;
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
		return !(this.children == null || this.children.isEmpty());
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

	public void setChildren(ArrayList<UCTNode> children) {
		this.children = children;
	}


	public PentagoBitBoard getState() {
		return bitBoard;
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
