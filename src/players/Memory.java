package players;

import core.GameState;

public class Memory {

    private GameState gameState;

    public GameState update(GameState gameState) {
        this.gameState = gameState;
        return gameState;
    }
}
