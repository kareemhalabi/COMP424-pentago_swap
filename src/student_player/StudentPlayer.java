package student_player;

import boardgame.Move;
import pentago_swap.PentagoBoardState;
import pentago_swap.PentagoMove;
import pentago_swap.PentagoPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/** A player file submitted by a student. */
public class StudentPlayer extends PentagoPlayer {

    private boolean firstMove = true;

    private final int FIRST_MOVE_TIMEOUT = 30000;
    private final int TIMEOUT = 2000;


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
    	long endTime = firstMove ? startTime + TIMEOUT - 300 : startTime + TIMEOUT - 300; //TODO Change back to StartTimeout
    	if(firstMove) firstMove = false;

    	System.gc(); // Might help with the random timeouts

    	UCTNode root = new UCTNode(boardState, null);

		while (System.currentTimeMillis() < endTime) {
			//----------- Descent (and Growth) phase -----------
			UCTNode promisingNode = selectPromisingNode(root);

			if(!promisingNode.getState().gameOver()) {
				expandNode(promisingNode);
			}

			//----------- Rollout phase -----------
			UCTNode nodeToExplore = promisingNode;
			if(promisingNode.getChildren().size() > 0) {
				nodeToExplore = promisingNode.getRandomChild();
			}
			int result = simulateRandomPlayout(nodeToExplore);

			//----------- Update phase -----------
			nodeToExplore.backPropagate(result);
		}

		UCTNode finalSelection = root.getMaxSimsChild();
		return finalSelection.getMove();
    }

	private int simulateRandomPlayout(UCTNode start) {

		PentagoBoardState state = (PentagoBoardState) start.getState().clone();
		while(!state.gameOver()) {
			state.processMove((PentagoMove) state.getRandomMove());
		}
		return state.getWinner();
	}

	private UCTNode selectPromisingNode(UCTNode root) {
    	UCTNode currentNode = root;
    	while(currentNode.getChildren().size() != 0) {
			currentNode = Collections.max(currentNode.getChildren(), Comparator.comparing(UCTNode::getStateValue));
		}
    	return currentNode;
	}

	private void expandNode(UCTNode growthNode) {
		ArrayList<PentagoMove> availableMoves = growthNode.getState().getAllLegalMoves();
		availableMoves.forEach(move -> {
			PentagoBoardState nextState = (PentagoBoardState)growthNode.getState().clone();
			nextState.processMove(move);
			growthNode.addChild(new UCTNode(nextState, move));
		});
	}
}