package players.heuristics;


import core.GameState;
import utils.Types;

import java.util.ArrayList;
import java.util.List;

public class MultiHeuristicA {

    private int increaseParam = 2;
    private double lambda = 0.99;
    private double lambdaDeltaVSDXY = 0.99;
    private double lambdaEnemiesVSPlayer = 0.99;

    public List<Double> evaluateState(GameState gs) {
        double value = 0;
        double valueP;
        double valueCloseToB;
        List<Double> values = new ArrayList<>();

        if(gs.winner() == Types.RESULT.WIN)
            value = 100;
        else if (gs.winner() == Types.RESULT.LOSS)
            value = -100;
        else if (gs.winner() == Types.RESULT.TIE)
            value = -50;

        values.add(value);

        int pCount = 0;
        for (Types.TILETYPE id : gs.getAliveAgentIDs()) {
            if (id.getKey() != gs.getPlayerId()) {
                pCount+=increaseParam;
            }
        }
        valueP = 1.0 / (pCount+1);
        values.add(valueP);

        Types.TILETYPE[][] board = gs.getBoard();
        int[][] bombBS = gs.getBombBlastStrength();
        double sumDelta = 0;
        int countBomb = 0;
        double sumDXY = 0;
        for (int ip=0; ip<board.length; ip++)
        {
            for (int jp = 0; jp < board.length; jp++)
            {
                if (board[ip][jp] == Types.TILETYPE.AGENT0 || board[ip][jp] == Types.TILETYPE.AGENT1 || board[ip][jp] == Types.TILETYPE.AGENT2 || board[ip][jp] == Types.TILETYPE.AGENT3)
                {
                    for (int i = 0; i < gs.getBoard().length; i++)
                    {
                        for (int j = 0; j < gs.getBoard().length; j++)
                        {
                            if (bombBS[i][j] != 0)
                            // if there is a bomb here:
                            {
                                countBomb += 1;
                                int dx = Math.abs(i - ip);
                                int dy = Math.abs(j - jp);
                                double delta = Math.sqrt(dx * dx + dy * dy);
                                if (delta < 1) {
                                    delta = 100;
                                }
                                if (delta > 3) {
                                    delta = 20;
                                }

                                if(board[ip][jp].getKey() == gs.getPlayerId())
                                {
                                    sumDelta += (1-lambdaEnemiesVSPlayer)*delta;
                                    sumDXY += Math.abs(dx - 1) + Math.abs(dy - 1);
                                }
                                else
                                {
                                    sumDelta += lambdaEnemiesVSPlayer*delta;
                                    sumDXY += dx + dy;
                                }
                            }
                        }
                    }
                }
            }
        }

        if(countBomb == 0){ countBomb=1;}
        //Let us incentivise the agent to have agents (in general) to be close to bombs while being itself on some diagonals rather on the direct line of blast (and have enemies on the direct line of blast):
        double v1 = 1.0 / (0.001+sumDelta/countBomb);
        double v2 = 1.0 / (0.001+sumDXY/countBomb);
        valueCloseToB = (lambdaDeltaVSDXY)*v1+ (1-lambdaDeltaVSDXY)*v2;
        values.add(v1);
        values.add(v2);
        values.add(valueCloseToB);


        value = (1.0-lambda)*valueP+lambda*valueCloseToB;
        values.add(value);

        return values;
    }
}