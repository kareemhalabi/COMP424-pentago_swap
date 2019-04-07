package student_player;

import boardgame.Board;
import pentago_swap.PentagoBoardState;
import pentago_swap.PentagoMove;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

import static pentago_swap.PentagoBoardState.*;

public class PentagoBitBoard {

	private static final int NUM_QUADS = 4;
	private static final int MAX_TURNS = 36;
	private static final int QUAD_SIZE = 3;

	// Supposedly faster than java.util.Random
	// https://lemire.me/blog/2016/02/01/default-random-number-generators-are-slow/
	private static final ThreadLocalRandom rand = ThreadLocalRandom.current();
	public static final byte DRAW = Byte.MAX_VALUE;
	public static final byte NOBODY = Byte.MAX_VALUE - 1;

	private long[] pieces;

	private byte turnPlayer;
	private byte turnNumber;
	private byte winner;


	private static final long[] quadrantMasks = {
		0b111000111000111000000000000000000000L,
		0b000111000111000111000000000000000000L,
		0b000000000000000000111000111000111000L,
		0b000000000000000000000111000111000111L
	};

	private static final long[] winningMasks = new long[32];

	// Generates the winningMasks
	static {
		// Generate the rows
		long baseRowMask = 0b111110000000000000000000000000000000L;
		int i = 0;
		winningMasks[i++] = baseRowMask;
		for(; i < 6; i++) {
			baseRowMask = baseRowMask >> 6;
			winningMasks[i] = baseRowMask;
		}
		baseRowMask = baseRowMask >> 1;
		winningMasks[i++] = baseRowMask;
		for(; i < 12; i++) {
			baseRowMask = baseRowMask << 6;
			winningMasks[i] = baseRowMask;
		}

		// Generate the columns
		long baseColumnMask = 0b100000100000100000100000100000000000L;
		winningMasks[i++] = baseColumnMask;
		for(; i < 24; i++) {
			baseColumnMask = baseColumnMask >> 1;
			winningMasks[i] = baseColumnMask;
		}

		// Diagonal masks (hardcoded since the logic to generate them would be too complex)
		winningMasks[24] = 0b100000010000001000000100000010000000L;
		winningMasks[25] = 0b010000001000000100000010000001000000L;
		winningMasks[26] = 0b000000010000001000000100000010000001L;
		winningMasks[27] = 0b000000100000010000001000000100000010L;
		winningMasks[28] = 0b000001000010000100001000010000000000L;
		winningMasks[29] = 0b000010000100001000010000100000000000L;
		winningMasks[30] = 0b000000000010000100001000010000100000L;
		winningMasks[31] = 0b000000000001000010000100001000010000L;

	}


	/**
	 * Maps a source and destination quadrant to
	 * how many bits need to be shifted
	 */
	private static final int[][] quadrantBitShifts = {
		{-1, 3, 18, 21},  // From Quad 0 -> Quad 1 is 3 bits, Quad 0 -> Quad 2 is 18 bits and Quad 0 -> Quad 3 is 21 bits
		{-1,-1, 15, 18},  // From Quad 1 -> Quad 2 is 15 bits, Quad 1 -> Quad 3 is 18 bits
		{-1,-1,-1, 3},    // From Quad 2 -> Quad 3 is 3 bits
		{-1,-1,-1, 0}     // From Quad 3 -> Quad 3 is 0 bits (not used as an actual swap since it would be invalid)
	};

	PentagoBitBoard(PentagoBoardState board) {

		this.pieces = new long[2];

		// Get the pieces
		for(int x = 0; x < BOARD_SIZE; x++) {
			for(int y = 0; y < BOARD_SIZE; y++) {

				//Shift the last pieces down
				this.pieces[BLACK] = this.pieces[BLACK] << 1;
				this.pieces[WHITE] = this.pieces[WHITE] << 1;

				PentagoBoardState.Piece p = board.getPieceAt(x, y);
				if(p == PentagoBoardState.Piece.BLACK) {
					this.pieces[BLACK] = this.pieces[BLACK] | 1;
				} else if (p == PentagoBoardState.Piece.WHITE) {
					this.pieces[WHITE] = this.pieces[WHITE] | 1;
				}

			}
		}

		this.turnPlayer = (byte) board.getTurnPlayer();

		// I use turn number as number of plays on the board (makes for faster allocation of legalmoves array)
		this.turnNumber = (byte) (board.getTurnNumber() * 2 + this.turnPlayer);
		if(board.getWinner() == Board.DRAW) {
			this.winner = DRAW;
		} else if(board.getWinner() == Board.NOBODY) {
			this.winner = NOBODY;
		} else {
			this.winner = (byte) board.getWinner();
		}
	}

	private PentagoBitBoard() {
		this.pieces = new long[2];
		this.winner = NOBODY;
	}

	// For Cloning
	private PentagoBitBoard(PentagoBitBoard board) {
		this.pieces = new long[2];
		this.pieces[0] = board.pieces[0];
		this.pieces[1] = board.pieces[1];
		this.winner = board.winner;
		this.turnPlayer = board.turnPlayer;
		this.turnNumber = board.turnNumber;
	}

	@Override
	public Object clone() {
		return new PentagoBitBoard(this);
	}

	// For Debug
	private PentagoBitBoard(long[] pieces, byte winner, byte turnPlayer, byte turnNumber) {
		this.pieces = pieces;
		this.winner = winner;
		this.turnPlayer = turnPlayer;
		this.turnNumber = turnNumber;
	}

	/**
	 * Returns all legal moves as a long:
	 *
	 *       |                       |P|     |                                    |
	 *       |                       |I|4 bit|                                    |
	 * 	     |        unused         |D|quads|       36 bits for coordinate       |
	 * 	     ----------------------------------------------------------------------
	 *       |.......................|p|sq|lq|cccccccccccccccccccccccccccccccccccc|
	 *
	 * 	Note that sq must be less than lq
	 *
	 * @return All legal moves as longs
	 */
	private ArrayList<Long> getAllLegalMoves(long availableSpots, byte[][] quadrantSwaps, int intialSize) {

		ArrayList<Long> moves = new ArrayList<>(intialSize);

		long mask = 1;
		for(int i = 0; i < BOARD_SIZE * BOARD_SIZE; i++) {
			if((availableSpots & mask) == mask) {
				for(byte[] quadrantSwap : quadrantSwaps) {
					moves.add((((((((long)turnPlayer << 2) | quadrantSwap[0]) << 2) | quadrantSwap[1]) << 36) | mask));
				}
			}
			mask = mask << 1;
		}

		return moves;
	}

	public ArrayList<Long> getAllLegalNonSymmetricMoves() {

		ArrayList<ArrayList<Byte>> equalQuadrants = partitionQuadrants();

		ArrayList<Long> moves;

		byte[][] swaps = {{0, 1}, {0, 2}, {0, 3}, {1, 2}, {1, 3}, {2, 3}};
		long placements = ~(this.pieces[WHITE] | this.pieces[BLACK]);
		int initialCapacity = ((BOARD_SIZE * BOARD_SIZE) - this.turnNumber) * swaps.length;

		switch (equalQuadrants.size()) {

			case 1:
				// 4 identical quadrants (Q0=Q1=Q2=Q3):
				// Try a move for each free spot in Q0 and just swap Q1 -> Q2

				byte Q0 = equalQuadrants.get(0).get(0);

				placements = placements & quadrantMasks[Q0];
				swaps = new byte[][]{{1, 2}};
				initialCapacity = (QUAD_SIZE * QUAD_SIZE) * swaps.length;

				moves = getAllLegalMoves(placements, swaps, initialCapacity);
				break;

			case 2:
				// 2 pairs of identical quadrants (Q0=Q2, Q1=Q3):
				// Try a move for each free spot in Q0 and Q1 and do all the swaps
				if (equalQuadrants.get(0).size() == 2) {

					Q0 = equalQuadrants.get(0).get(0);
					byte Q1 = equalQuadrants.get(1).get(0);

					placements = placements & (quadrantMasks[Q0] | quadrantMasks[Q1]);
					initialCapacity = (QUAD_SIZE * QUAD_SIZE) * 2 * swaps.length;

					moves = getAllLegalMoves(placements, swaps, initialCapacity);
				}
				// 1 unique, 3 identical quadrants(Q0, Q1=Q2=Q3):
				// Try a move for each free spot in Q0 and do a swap for Q0 -> Q1, Q0 -> Q2, Q0 -> Q2, Q1 -> Q2
				// Try a move for each free spot in Q1 and do all the swaps
				else {

					ArrayList<Byte> uniqueQuadrant = equalQuadrants.get(0).size() < equalQuadrants.get(1).size() ? equalQuadrants.get(0) : equalQuadrants.get(1);
					ArrayList<Byte> identicalQuadrants = equalQuadrants.get(0).size() > equalQuadrants.get(1).size() ? equalQuadrants.get(0) : equalQuadrants.get(1);

					long placementsQ0 = placements & quadrantMasks[uniqueQuadrant.get(0)];
					byte[][] swapsQ0 = new byte[4][2];
					int initialCapacityQ0 = (QUAD_SIZE * QUAD_SIZE) * swaps.length;

					Q0 = equalQuadrants.get(0).get(0);

					// Generate first 3 swaps
					for(int i = 0; i < identicalQuadrants.size(); i++) {
						byte quadrant = identicalQuadrants.get(i);
						byte largerQuadrant = quadrant > Q0 ? quadrant : Q0;
						byte smallerQuadrant = quadrant < Q0 ? quadrant : Q0;
						swapsQ0[i][0] = smallerQuadrant;
						swapsQ0[i][1] = largerQuadrant;
					}

					// Generate last swap
					byte largerQuadrant = identicalQuadrants.get(0) > identicalQuadrants.get(1) ? identicalQuadrants.get(0) : identicalQuadrants.get(1);
					byte smallerQuadrant = identicalQuadrants.get(0) < identicalQuadrants.get(1) ? identicalQuadrants.get(0) : identicalQuadrants.get(1);
					swapsQ0[swapsQ0.length-1][0] = smallerQuadrant;
					swapsQ0[swapsQ0.length-1][1] = largerQuadrant;

					moves = getAllLegalMoves(placementsQ0, swapsQ0, initialCapacityQ0);

					long placementsQ1 = placements & quadrantMasks[identicalQuadrants.get(0)];
					byte[][] swapsQ1 = swaps;
					int initialCapacityQ1 = (QUAD_SIZE * QUAD_SIZE) * swapsQ1.length;

					moves.addAll(getAllLegalMoves(placementsQ1, swapsQ1, initialCapacityQ1));
				}

				break;

			// Default is no identical pairs of quadrants: return all legal moves as usual
			default:
				moves = getAllLegalMoves(placements, swaps, initialCapacity);
				break;
		}

		return moves;
	}

	private ArrayList<ArrayList<Byte>> partitionQuadrants() {

		// Put each quadrant (the two piece longs) into a list
		ArrayList<ArrayList<Long>> quadrants = new ArrayList<>(NUM_QUADS);

		for(int i = 0; i < NUM_QUADS; i++) {
			quadrants.add(new ArrayList<>());
			for(long pieceLong : this.pieces) {
				// Shift all quadrants to the same position
				quadrants.get(i).add(pieceLong >> quadrantBitShifts[i][NUM_QUADS-1]);
			}
		}

		// Partition quadrants into buckets where they are equal
		HashMap<ArrayList<Long>, ArrayList<Byte>> equalQuadrants = new HashMap<>();

		for(byte j = 0; j < NUM_QUADS; j++) {

			// Found a similar quadrant, add quadrant number to the partition
			if(equalQuadrants.containsKey(quadrants.get(j))) {
				equalQuadrants.get(quadrants.get(j)).add(j);
			}
			// New unique quadrant, create a new partition with the quadrant number
			else {
				ArrayList<Byte> newPartition = new ArrayList<>();
				newPartition.add(j);
				equalQuadrants.put(quadrants.get(j), newPartition);
			}
		}

		return new ArrayList<>(equalQuadrants.values());
	}

	public long getRandomMove() {

		long availableSpots = ~(this.pieces[WHITE] | this.pieces[BLACK]);

		long move;
		// Pick a random empty coordinate
		do {
			move = 1L << rand.nextInt(BOARD_SIZE * BOARD_SIZE);
		} while((move & availableSpots) != move);

		// Pick random quadrants
		int firstQuad = rand.nextInt(4);
		int secondQuad;
		do {
			secondQuad = rand.nextInt(4);
		} while(firstQuad == secondQuad);

		int largerQuad = firstQuad > secondQuad ? firstQuad : secondQuad;
		int smallerQuad = firstQuad < secondQuad ? firstQuad : secondQuad;

		return ((((((long)turnPlayer << 2) | smallerQuad) << 2) | largerQuad) << 36) | move;
	}

	public void processMove(long move) {

		//Extract info from move
		int player = (int) ((move >> 40) & 1);
		int smallerQuad = (int) ((move >> 38) & 0b11);
		int largerQuad = (int) ((move >> 36) & 0b11);
		long coord = move & 0b111111111111111111111111111111111111L;

		//Place the coordinate based on player
		this.pieces[player] = this.pieces[player] | coord;
		this.swapQuadrants(smallerQuad, largerQuad);
		this.turnNumber++;

		this.updateWinner();

		this.turnPlayer = (byte) (1 - this.turnPlayer);

	}

	public void undoMove(long move) {

		//Extract info from move
		int player = (int) ((move >> 40) & 1);
		int smallerQuad = (int) ((move >> 38) & 0b11);
		int largerQuad = (int) ((move >> 36) & 0b11);
		long coord = move & 0b111111111111111111111111111111111111L;

		// Re-swap the quadrants
		this.swapQuadrants(smallerQuad, largerQuad);

		// Undo the placement
		this.pieces[player] = this.pieces[player] & ~coord;
		this.turnNumber--;

		this.updateWinner();

		this.turnPlayer = (byte) (1 - this.turnPlayer);
	}

	public void swapQuadrants(int smallerQuad, int largerQuad) {

		for(int i = 0; i < this.pieces.length; i++) {
			// Shift the smaller quad down to make for the new larger quad

			long newLarge = (this.pieces[i] & quadrantMasks[smallerQuad]) >> quadrantBitShifts[smallerQuad][largerQuad];

			// Shift the larger quad up to make the new smaller quad
			long newSmall = (this.pieces[i] & quadrantMasks[largerQuad]) << quadrantBitShifts[smallerQuad][largerQuad];

			// Perform the update
			long newConfig = newLarge | newSmall;
			long updateMask = ~ (quadrantMasks[smallerQuad] | quadrantMasks[largerQuad]);

			this.pieces[i] = (this.pieces[i] & updateMask) | newConfig;
		}
	}

	private void updateWinner() {
		boolean playerWin = checkWin(this.turnPlayer);
		boolean otherWin = checkWin((byte) (1 - this.turnPlayer));

		if (playerWin) { // Current player has won
			this.winner = otherWin ? DRAW : this.turnPlayer;
		} else if (otherWin) { // Player's move caused the opponent to win
			this.winner = (byte) (1 - this.turnPlayer);
		} else if (gameOver()) {
			this.winner = DRAW;
		}
	}

	private boolean checkWin(byte turnPlayer) {
		for(long mask: winningMasks) {
			if((mask & this.pieces[turnPlayer]) == mask) {
				return true;
			}
		}
		return false;
	}

	public boolean gameOver() {
		return (this.turnNumber >= MAX_TURNS) || this.winner != NOBODY;
	}

	public static long xyToBit(int x, int y) {

		// Get x in correct position:
		long bit = (1 << (BOARD_SIZE - 1 - x));

		// Get y in correct position
		bit = bit << (BOARD_SIZE * (BOARD_SIZE - 1 - y));

		return bit;
	}

	public static int[] bitToXY(long move) {

		int bitPosition = 0;
		long mask = 1;
		while((move & mask) != mask) {
			bitPosition++;
			mask = mask << 1;
		}

		int x = (BOARD_SIZE*BOARD_SIZE - 1 - bitPosition) % BOARD_SIZE;
		int y = (BOARD_SIZE*BOARD_SIZE - 1 - bitPosition) / BOARD_SIZE;

		return new int[] {x, y};
	}

	public static PentagoMove longToPentagoMove(long move) {

		int player = (int) ((move >> 40) & 1);
		int smallerQuad = (int) ((move >> 38) & 0b11);
		int largerQuad = (int) ((move >> 36) & 0b11);
		long coord = move & 0b111111111111111111111111111111111111L;

		int[] coordXY = bitToXY(coord);

		// I accidentally used a different coordinate system so return x -> y, y -> x
		return new PentagoMove(coordXY[1], coordXY[0], Quadrant.values()[smallerQuad], Quadrant.values()[largerQuad], player);
	}

	public byte getWinner() {
		return winner;
	}

	public byte getOpponent() {
		return (byte) (1 - this.turnPlayer);
	}

	public void togglePlayer() {
		this.turnPlayer = getOpponent();
	}

	public byte getTurnNumber() {
		return this.turnNumber;
	}

	public byte getTurnPlayer() {
		return this.turnPlayer;
	}

	@Override
	public String toString() {
		StringBuilder boardString = new StringBuilder();
		String rowMarker = "--------------------------\n";
		boardString.append(rowMarker);
		for(int y = 0; y < BOARD_SIZE; y++) {
			boardString.append("|");
			for(int x = 0; x < BOARD_SIZE; x++) {
				boardString.append(" ");

				long xy = xyToBit(x, y);
				if((pieces[WHITE] & xy) == xy) {
					boardString.append('W');
				} else if((pieces[BLACK] & xy) == xy) {
					boardString.append('B');
				} else {
					boardString.append(" ");
				}

				boardString.append(" |");
				if(x == BOARD_SIZE/2 - 1) {
					boardString.append("|");
				}
			}
			boardString.append("\n");
			if(y == BOARD_SIZE/2 - 1) {
				boardString.append(rowMarker);
			}
		}
		boardString.append(rowMarker);
		return boardString.toString();
	}

	public static void main(String[] args) {
//		maskTest();
		PentagoBitBoard pbs = new PentagoBitBoard();

		Scanner scanner = new Scanner(System.in);
		int id = 0;
		while(pbs.winner == NOBODY) {
			System.out.print("Enter move (x y a b): ");
			String moveStr = scanner.nextLine();
			String[] moveStrs = moveStr.split(" ");

			long m = ((((((long)id << 2) | Integer.parseInt(moveStrs[2])) << 2) | Integer.parseInt(moveStrs[3])) << 36) | xyToBit(Integer.parseInt(moveStrs[0]), Integer.parseInt(moveStrs[1]));

			pbs.processMove(m);
			System.out.println(pbs);
			id = 1 - id;
		}

		switch(pbs.winner) {
			case WHITE:
				System.out.println("White wins.");
				break;
			case BLACK:
				System.out.println("Black wins.");
				break;
			case DRAW:
				System.out.println("Draw.");
				break;
			case NOBODY:
				System.out.println("Nobody has won.");
				break;
			default:
				System.out.println("Unknown error.");
		}
	}

	public static void maskTest() {
		for (long mask: winningMasks) {
			System.out.println(new PentagoBitBoard(new long[] {mask, 0L}, (byte)0, (byte)0, (byte)0));
		}
	}
}
