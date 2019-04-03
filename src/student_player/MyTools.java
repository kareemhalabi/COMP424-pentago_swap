package student_player;

import pentago_swap.PentagoBoardState;
import pentago_swap.PentagoCoord;
import pentago_swap.PentagoMove;

import java.util.ArrayList;
import java.util.Arrays;

class MyTools {


    /**
     * More efficient method of getting random move. Instead of generating all legal moves
     * and picking a random one, we simply try random positions until an empty slot is found
     * and return this move
     * @param state current state of the game
     * @return a random legal move to play
     */
    public static PentagoMove efficientGetRandomMove(PentagoBoardState state) {

        // Find a random placement
        PentagoCoord randomPlacement;
        do {
            randomPlacement = new PentagoCoord(StudentPlayer.rng.nextInt(PentagoBoardState.BOARD_SIZE), StudentPlayer.rng.nextInt(PentagoBoardState.BOARD_SIZE));
        } while(!state.isPlaceLegal(randomPlacement));

        // Pick a random swap
        ArrayList<PentagoBoardState.Quadrant> quadrants = new ArrayList<>(Arrays.asList(PentagoBoardState.Quadrant.values()));

        PentagoBoardState.Quadrant firstQuadrant = quadrants.remove(StudentPlayer.rng.nextInt(quadrants.size()));
        PentagoBoardState.Quadrant secondQuadrant = quadrants.remove(StudentPlayer.rng.nextInt(quadrants.size()));

        return new PentagoMove(randomPlacement, firstQuadrant, secondQuadrant, state.getTurnPlayer());

    }

}