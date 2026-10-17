package com.dethrace.carmageddon;

import org.libsdl.app.SDLActivity;

/**
 * Runs dethrace (libmain.so) in its own process. The game id ("c1" or "sp")
 * selects which data directory is passed to the engine.
 */
public class GameActivity extends SDLActivity {

    public static final String EXTRA_GAME_ID = "game_id";

    @Override
    protected String[] getLibraries() {
        return new String[] { "SDL2", "main" };
    }

    @Override
    protected String[] getArguments() {
        String gameId = getIntent().getStringExtra(EXTRA_GAME_ID);
        if (gameId == null) {
            gameId = "c1";
        }
        String dir = GameData.gameDir(this, gameId).getAbsolutePath();
        return new String[] { "--dir", dir, "--no-signal-handler", "-hires" };
    }
}
