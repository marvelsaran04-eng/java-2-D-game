package com.saran.game;

import com.badlogic.gdx.Game;

public class MyGdxGame extends Game {

    @Override
    public void create() {
        setScreen(new PlayScreen(this));
    }
}
