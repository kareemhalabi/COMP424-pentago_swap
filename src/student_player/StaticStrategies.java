package student_player;

import static student_player.PentagoBitBoard.QUAD_SWAPS;

/**
 * This class contains static strategies that are applied
 */
class StaticStrategies {

	/**
	 * Tries to play an winning move from the current state
	 * @param bitBoardState current state of the game
	 * @return a winning move if found, 0 otherwise
	 */
	static long checkOffensiveMove(PentagoBitBoard bitBoardState) {

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

	/**
	 * Tries to block an opponent from winning from the current state
	 * @param bitBoardState Current state of the game
	 * @return an effective defensive move if found, 0 otherwise
	 */
	static long checkDefensiveMove(PentagoBitBoard bitBoardState) {

		PentagoBitBoard bitBoardStateClone = (PentagoBitBoard) bitBoardState.clone();

		// Pretend we are opponent
		bitBoardStateClone.togglePlayer();

    	// Try to win as opponent
		long opponentMove = checkOffensiveMove(bitBoardStateClone);
		bitBoardStateClone.togglePlayer();
		if(opponentMove != 0) {
			// We'll put a piece where the opponent would have to win
			long defensivePlacement = opponentMove ^ (1L << 40);

			// Pick a quadrant swap that ensures the opponent can't win on the next turn anyway
			long quadrantClearMask = ~(0b1111L << 36);
			for(byte[] swap : QUAD_SWAPS) {
				long smallerQuad = (long)swap[0] << 38;
				long largerQuad = (long)swap[1] << 36;

				long defensiveMove = (defensivePlacement & quadrantClearMask) | smallerQuad | largerQuad;
				bitBoardState.processMove(defensiveMove);
				long opponentCounterMeasure = checkOffensiveMove(bitBoardStateClone);
				bitBoardState.undoMove(defensiveMove);

				// Opponent can't win, found an effective defensive move!
				if(opponentCounterMeasure == 0) {
					return defensiveMove;
				}
			}
		}

		// No defensive move found
		return 0;
	}
}
