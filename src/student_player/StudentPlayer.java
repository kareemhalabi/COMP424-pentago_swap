package student_player;

import boardgame.Move;
import pentago_swap.PentagoBoardState;
import pentago_swap.PentagoPlayer;

import java.util.ArrayList;
import java.util.Random;

import static student_player.PentagoBitBoard.longToPentagoMove;

/** A player file submitted by a student. */
public class StudentPlayer extends PentagoPlayer {

	// Create a single random number generator for the program
	static Random rng = new Random();

    private static final int TIMEOUT = 1500;


    /**
     * You must modify this constructor to return your student number. This is
     * important, because this is what the code that runs the competition uses to
     * associate you with your agent. The constructor should do nothing else.
     */
    public StudentPlayer() {
        super("260616162");
    }

    /**
     * This is the primary method that you need to implement. The ``boardState``
     * object contains the current state of the game, which your agent must use to
     * make decisions.
     */

    public Move chooseMove(PentagoBoardState boardState) {

    	// ----------- Setup -----------
    	long startTime = System.currentTimeMillis();
    	long endTime = startTime + TIMEOUT;

    	PentagoBitBoard bitBoardState = new PentagoBitBoard(boardState);

    	UCTNode root = new UCTNode(bitBoardState, 0L);

		while (System.currentTimeMillis() < endTime) {
			//----------- Descent (and Growth) phase -----------
			UCTNode promisingNode = selectPromisingNode(root);

			PentagoBitBoard promisingState = promisingNode.getState();
			if(!promisingState.gameOver()) {
				expandNode(promisingNode);
			}

			//----------- Rollout phase -----------
			UCTNode nodeToExplore = promisingNode;
			if(promisingNode.hasChildren()) {
				nodeToExplore = promisingNode.getRandomChild();
			}
			byte[] result = simulateRandomPlayout(nodeToExplore);

			//----------- Update phase -----------
			nodeToExplore.backPropagate(result);
		}

		UCTNode finalSelection = root.getMaxSimsChild();
		return longToPentagoMove(finalSelection.getMove());
    }

	private byte[] simulateRandomPlayout(UCTNode start) {

		PentagoBitBoard state = start.getState();
		while(!state.gameOver()) {
			state.processMove(state.getRandomMove());
		}
		// Returns who won and who was last to play
		return new byte[] {state.getWinner(), state.getOpponent()};
	}

	private UCTNode selectPromisingNode(UCTNode root) {
    	UCTNode currentNode = root;

    	while (currentNode.hasChildren()) {
			double maxValue = Double.MIN_VALUE;
			int maxIndex = -1;

			for(int i = 0; i < currentNode.getChildren().length; i++) {
				double value = currentNode.getChildren()[i].getStateValue();
				if(value > maxValue) {
					maxValue = value;
					maxIndex = i;
				}
			}

			currentNode = currentNode.getChildren()[maxIndex];
		}
    	return currentNode;
	}

	private void expandNode(UCTNode growthNode) {
		ArrayList<Long> availableMoves = growthNode.getState().getAllLegalNonSymmetricMoves();
		UCTNode[] children = new UCTNode[availableMoves.size()];
		for(int i = 0; i < availableMoves.size(); i++) {
			PentagoBitBoard newState = (PentagoBitBoard) growthNode.getState().clone();
			newState.processMove(availableMoves.get(i));
			UCTNode child = new UCTNode(newState, availableMoves.get(i));
			child.setParent(growthNode);
			children[i] = child;
		}
		growthNode.setChildren(children);
	}
}