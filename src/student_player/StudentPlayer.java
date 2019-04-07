package student_player;

import boardgame.Move;
import pentago_swap.PentagoBoardState;
import pentago_swap.PentagoMove;
import pentago_swap.PentagoPlayer;

import java.util.ArrayList;
import java.util.Random;

import static student_player.PentagoBitBoard.longToPentagoMove;

/** A player file submitted by a student. */
public class StudentPlayer extends PentagoPlayer {

	// Create a single random number generator for the program
	static Random rng = new Random();

    private static final int TIMEOUT = 1400;


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

    	if(bitBoardState.getTurnNumber() >= 7) {
    		long winningMove = checkOffensiveMove(bitBoardState);
    		if(winningMove != 0) {
    			System.out.println("Found a winning move!");
				return longToPentagoMove(winningMove);
			}
    		long defensiveMove = checkDefensiveMove(bitBoardState);
    		if(defensiveMove != 0) {

    			// TODO find out why some returned moves are illegal
				PentagoMove potentialDefensiveMove = longToPentagoMove(defensiveMove);
				if(boardState.isLegal(potentialDefensiveMove)) {
					System.out.println("Found a defensive move!");
					return longToPentagoMove(defensiveMove);
				}

			}
		}

    	UCTNode root = new UCTNode(0L);

		while (System.currentTimeMillis() < endTime) {
			//----------- Descent (and Growth) phase -----------
			UCTNode promisingNode = selectPromisingNode(root);

			PentagoBitBoard promisingState = promisingNode.getState();
			if(!promisingState.gameOver()) {
				expandNode(promisingNode, promisingState);
			}

			//----------- Rollout phase -----------
			UCTNode nodeToExplore = promisingNode;
			if(promisingNode.hasChildren()) {
				nodeToExplore = promisingNode.getRandomChild();
			}
			byte[] result = simulateRandomPlayout(nodeToExplore, bitBoardState);
//			long rolloutTime = System.nanoTime() - currentTime;

			//----------- Update phase -----------
			nodeToExplore.backPropagate(result);
		}

		UCTNode finalSelection = root.getMaxSimsChild();
		return longToPentagoMove(finalSelection.getMove());
    }

	private long checkOffensiveMove(PentagoBitBoard bitBoardState) {

    	PentagoBitBoard bitBoardStateClone = (PentagoBitBoard) bitBoardState.clone();

    	byte player = bitBoardStateClone.getTurnPlayer();

    	for(long move : bitBoardStateClone.getAllLegalNonSymmetricMoves()) {
			bitBoardStateClone.processMove(move);
    		if(bitBoardStateClone.getWinner() == player) {
    			return move;
			}
			bitBoardStateClone.undoMove(move);
		}
    	// No offensive move found
    	return 0;
	}

	private long checkDefensiveMove(PentagoBitBoard bitBoardState) {

		PentagoBitBoard bitBoardStateClone = (PentagoBitBoard) bitBoardState.clone();

		bitBoardStateClone.togglePlayer();
    	byte opponent = bitBoardStateClone.getTurnPlayer();

    	// Play a random move to toggle to opponent
		long opponentMove = checkOffensiveMove(bitBoardStateClone);
		bitBoardStateClone.undoMove(opponentMove);
		if(opponentMove != 0) {
			// We'll play what the opponent would have to win
			long defensiveMove = opponentMove ^ (1L << 40);

			// Double check we don't let opponent win anyway
			bitBoardStateClone.processMove(defensiveMove);
			if(bitBoardStateClone.getWinner() != opponent) {
				return defensiveMove;
			}
		}
		bitBoardStateClone.togglePlayer();
		return 0;
	}

	private byte[] simulateRandomPlayout(UCTNode start, PentagoBitBoard startState) {

		PentagoBitBoard state = start.getState(startState);
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

	private void expandNode(UCTNode growthNode, PentagoBitBoard startState) {
		ArrayList<Long> availableMoves = growthNode.getState(startState).getAllLegalNonSymmetricMoves();
		UCTNode[] children = new UCTNode[availableMoves.size()];
		for(int i = 0; i < availableMoves.size(); i++) {
			UCTNode child = new UCTNode(availableMoves.get(i));
			child.setParent(growthNode);
			children[i] = child;
		}
		growthNode.setChildren(children);
	}
}