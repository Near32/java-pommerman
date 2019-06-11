import core.Game;
import core.GameState;
import players.*;
import players.heuristics.*;
import players.mcts.LobsterParams;
import players.mcts.LobsterPlayer;

import utils.Types;
import players.rhea.utils.Constants;
import objects.Avatar;
import players.mcts.MCTSPlayer;
import players.mcts.MCTSParams;
import players.rhea.RHEAPlayer;
import players.rhea.utils.RHEAParams;
import players.rhea.RHEALobsterPlayer;
import players.rhea.utils.RHEALobsterParams;
import utils.*;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

public class Test {

    public static void main(String[] args) {

        // Game parameters
        long seed = System.currentTimeMillis();
        int boardSize = Types.BOARD_SIZE;
        Types.GAME_MODE gameMode = Types.GAME_MODE.FFA;
        boolean useSeparateThreads = false;                 //true may be unstable, false is recommended.

        Game game = new Game(seed, boardSize, gameMode, "");

        // Key controllers for human player s (up to 2 so far).
        KeyController ki1 = new KeyController(true);
        KeyController ki2 = new KeyController(false);


        // Create players
        ArrayList<Player> players = new ArrayList<>();
        int playerID = Types.TILETYPE.AGENT0.getKey();

        // Define some heuristics
        MCTSParams mctsParams = new MCTSParams();
        mctsParams.stop_type = mctsParams.STOP_ITERATIONS;
        mctsParams.heuristic_method = mctsParams.CUSTOM_HEURISTIC;

        // Define our heuristics
        MCTSParams OurMCTSParams = new MCTSParams();
        OurMCTSParams.K = 10;//Math.sqrt(2);
        OurMCTSParams.rollout_depth = 6;
        OurMCTSParams.stop_type = mctsParams.STOP_ITERATIONS;
        OurMCTSParams.heuristic_method = mctsParams.OUR_HEURISTIC;


        MCTSParams CollapseMCTSParams = new MCTSParams();
        CollapseMCTSParams.K = 10;//Math.sqrt(2);
        CollapseMCTSParams.rollout_depth = 6;
        CollapseMCTSParams.stop_type = mctsParams.STOP_ITERATIONS;
        CollapseMCTSParams.heuristic_method = mctsParams.OUR_HEURISTIC;
        CollapseMCTSParams.collapsing = true;
        CollapseMCTSParams.nbrUpdates2Uniform = 100;
        CollapseMCTSParams.maxClusterRatio = 0.25f;
        CollapseMCTSParams.nbrClustererCycles = 100;
        List<Integer> lch = new ArrayList<Integer>();
        lch.add(0);
        lch.add(1);
        lch.add(2);
        lch.add(3);
        CollapseMCTSParams.ClusteringHeuristicFunction = gs->
        {
            List<Float> ret = new ArrayList<>();
            for(int shk: lch)
            {
                StateHeuristic sh;
                if (shk == mctsParams.CUSTOM_HEURISTIC)
                    sh = new CustomHeuristic(gs);
                else if (shk == mctsParams.OUR_HEURISTIC)
                    sh = new OurHeuristic();
                else if (shk == mctsParams.ADVANCED_HEURISTIC) {
                    Random rnd = new Random();
                    sh = new AdvancedHeuristic(gs, rnd);
                }
                else
                    sh = new PlayerCountHeuristic();

                ret.add( (float) sh.evaluateState(gs));
            }
            return ret;
        };

        RHEAParams rheaParams = new RHEAParams();
        rheaParams.heurisic_type = Constants.CUSTOM_HEURISTIC;
/*
        LobsterParams lobsterParams = new LobsterParams();
        lobsterParams.stop_type = lobsterParams.STOP_ITERATIONS;
        lobsterParams.heuristic_method = lobsterParams.LOBSTER_HEURISTIC;

        // Different available players

//        players.add(new HumanPlayer(ki1, playerID++));
//        players.add(new HumanPlayer(ki2, playerID++));
//        players.add(new DoNothingPlayer(playerID++));
*/
        players.add(new MCTSPlayer(seed, playerID++, CollapseMCTSParams));
        players.add(new MCTSPlayer(seed, playerID++, OurMCTSParams));
        //players.add(new SimpleEvoAgent(seed, playerID++));
        //players.add(new RHEAPlayer(seed, playerID++, rheaParams));
        players.add(new MCTSPlayer(seed, playerID++, OurMCTSParams));
        players.add(new MCTSPlayer(seed, playerID++, OurMCTSParams));

/*
//        players.add(new DoNothingPlayer(playerID++));
//        players.add(new DoNothingPlayer(playerID++));
//        players.add(new OSLAPlayer(seed, playerID++));
//        players.add(new LobsterPlayer(seed, playerID++, lobsterParams));
        players.add(new MCTSPlayer(seed, playerID++, mctsParams));
//        players.add(new SimplePlayer(seed, playerID++));
        players.add(new SimpleEvoAgent(seed, playerID++));
        players.add(new RHEAPlayer(seed, playerID++, rheaParams));

        RHEALobsterParams rheaLobsterParams = new RHEALobsterParams();
        players.add(new RHEALobsterPlayer(seed, playerID++, rheaLobsterParams));
*/

        // Make sure we have exactly NUM_PLAYERS players
        assert players.size() == Types.NUM_PLAYERS : "There should be " + Types.NUM_PLAYERS +
                                                     " added to the game, but there are " + players.size();

        //Assign players and run the game.
        game.setPlayers(players);

        //Run a single game with the players
        Run.runGame(game, ki1, ki2, useSeparateThreads);

        /* Uncomment to run the replay of the previous game: */
//        if (game.isLogged()){
//            Game replay = Game.getLastReplayGame();
//            Run.runGame(replay, ki1, ki2, useSeparateThreads);
//            assert(replay.getGameState().equals(game.getGameState()));
//        }

        /* Run with no visuals, N repetitions TIMES number-seeds: */
//        int N = 20;
//        Run.runGames(game, new long[]{seed}, N, useSeparateThreads);

    }

}
