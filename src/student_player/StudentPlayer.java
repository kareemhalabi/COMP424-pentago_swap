package student_player;

import boardgame.Move;
import pentago_swap.PentagoBoardState;
import pentago_swap.PentagoMove;
import pentago_swap.PentagoPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import static student_player.MyTools.efficientGetRandomMove;

/** A player file submitted by a student. */
public class StudentPlayer extends PentagoPlayer {

	// Create a single random number generator for the program
	static Random rng = new Random();

    private static final int TIMEOUT = 2000;


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
    	long endTime = startTime + TIMEOUT - 600;

    	UCTNode root = new UCTNode(null);

		while (System.currentTimeMillis() < endTime) {
			//----------- Descent (and Growth) phase -----------
			UCTNode promisingNode = selectPromisingNode(root);

			if(!promisingNode.getState(boardState).gameOver()) {
				expandNode(promisingNode, boardState);
			}

			//----------- Rollout phase -----------
			UCTNode nodeToExplore = promisingNode;
			if(promisingNode.getChildren().size() > 0) {
				nodeToExplore = promisingNode.getRandomChild();
			}
			int[] result = simulateRandomPlayout(nodeToExplore, boardState);

			//----------- Update phase -----------
			nodeToExplore.backPropagate(result);
		}

		UCTNode finalSelection = root.getMaxSimsChild();
		return finalSelection.getMove();
    }

	private int[] simulateRandomPlayout(UCTNode start, PentagoBoardState startState) {

		PentagoBoardState state = start.getState(startState);
		while(!state.gameOver()) {
			state.processMove(efficientGetRandomMove(state));
		}
		// Returns who won and who was last to play
		return new int[] {state.getWinner(), state.getOpponent()};
	}

	private UCTNode selectPromisingNode(UCTNode root) {
    	UCTNode currentNode = root;
    	while(currentNode.getChildren().size() != 0) {
			currentNode = Collections.max(currentNode.getChildren(), Comparator.comparing(UCTNode::getStateValue));
		}
    	return currentNode;
	}

	private void expandNode(UCTNode growthNode, PentagoBoardState startState) {
		ArrayList<PentagoMove> availableMoves = growthNode.getState(startState).getAllLegalMoves();
		availableMoves.forEach(move -> growthNode.addChild(new UCTNode(move)));
	}
}