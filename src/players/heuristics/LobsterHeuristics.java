package players.heuristics;

import core.ForwardModel;
import core.GameState;
import objects.GameObject;
import utils.Types;
import utils.Vector2d;

public class LobsterHeuristics extends StateHeuristic {
    private BoardStats rootBoardStats;

    public LobsterHeuristics(GameState root) {
        rootBoardStats = new BoardStats(root);
    }

    @Override
    public double evaluateState(GameState gs) {
        boolean gameOver = gs.isTerminal();
        Types.RESULT win = gs.winner();

        // Compute a score relative to the root's state.
        BoardStats lastBoardState = new BoardStats(gs);
        double rawScore = rootBoardStats.score(lastBoardState);

        if(gameOver && win == Types.RESULT.LOSS)
            rawScore = -1;

        if(gameOver && win == Types.RESULT.WIN)
            rawScore = 1;

        return rawScore;
    }

    public static class BoardStats
    {
        int tick, nTeammates, nEnemies, blastStrength;
        boolean canKick;
        int nWoods;
        static double maxWoods = -1;
        static double maxBlastStrength = 10;

        double FACTOR_ENEMY;
        double FACTOR_TEAM;
        double FACTOR_WOODS = 0.1;
        double FACTOR_CANKCIK = 0.15;
        double FACTOR_BLAST = 0.15;
        GameState gameState;

        BoardStats(GameState gs) {
            gameState = gs;
            nEnemies = gs.getAliveEnemyIDs().size();

            // Init weights based on game mode
            if (gs.getGameMode() == Types.GAME_MODE.FFA) {
                FACTOR_TEAM = 0;
                FACTOR_ENEMY = 0.5;
            } else {
                FACTOR_TEAM = 0.1;
                FACTOR_ENEMY = 0.4;
                nTeammates = gs.getAliveTeammateIDs().size();  // We only need to know the alive teammates in team modes
                nEnemies -= 1;  // In team modes there's an extra Dummy agent added that we don't need to care about
            }

            // Save game state information
            this.tick = gs.getTick();
            this.blastStrength = gs.getBlastStrength();
            this.canKick = gs.canKick();

            // Count the number of wood walls
            this.nWoods = 1;
            for (Types.TILETYPE[] gameObjectsTypes : gs.getBoard()) {
                for (Types.TILETYPE gameObjectType : gameObjectsTypes) {
                    if (gameObjectType == Types.TILETYPE.WOOD)
                        nWoods++;
                }
            }
            if (maxWoods == -1) {
                maxWoods = nWoods;
            }
        }

        /**
         * Computes score for a game, in relation to the initial state at the root.
         * Minimizes number of opponents in the game and number of wood walls. Maximizes blast strength and
         * number of teammates, wants to kick.
         * @param futureState the stats of the board at the end of the rollout.
         * @return a score [0, 1]
         */

        boolean isBlockerTile(Types.TILETYPE type)
        {
            if(type == Types.TILETYPE.FLAMES || type == Types.TILETYPE.RIGID)
                return true;

            return false;
        }

        double score(BoardStats futureState)
        {
            int diffTeammates = futureState.nTeammates - this.nTeammates;
            int diffEnemies = - (futureState.nEnemies - this.nEnemies);
            int diffWoods = - (futureState.nWoods - this.nWoods);
            int diffCanKick = futureState.canKick ? 1 : 0;
            int diffBlastStrength = futureState.blastStrength - this.blastStrength;

            double score = (diffEnemies / 3.0) * FACTOR_ENEMY + diffTeammates * FACTOR_TEAM + (diffWoods / maxWoods) * FACTOR_WOODS
                    + diffCanKick * FACTOR_CANKCIK + (diffBlastStrength / maxBlastStrength) * FACTOR_BLAST;

            int playerId = futureState.gameState.getPlayerId();
            Types.TILETYPE[][] tiles = futureState.gameState.getBoard();

            int xLength = tiles.length;
            int yLength = tiles[0].length;
            Vector2d pos = gameState.getPosition();

            int numOccupiedTiles = 0;
            if(pos.x == 0 || isBlockerTile(tiles[pos.x-1][pos.y]))
                    numOccupiedTiles++;

            if(pos.y == 0 || isBlockerTile(tiles[pos.x][pos.y-1]))
                numOccupiedTiles++;

            if(pos.x == xLength-1 || isBlockerTile(tiles[pos.x+1][pos.y]))
                numOccupiedTiles++;

            if(pos.y == yLength-1 || isBlockerTile(tiles[pos.x][pos.y+1]))
                numOccupiedTiles++;

            if(numOccupiedTiles >=3)
                score = 0;


            return score;

        }
    }
}
