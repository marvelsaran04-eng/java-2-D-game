package com.saran.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.utils.Array;



public class PlayScreen implements Screen {

    private static final float WORLD_WIDTH = 800;
    private static final float WORLD_HEIGHT = 480;

    private MyGdxGame game;

    private OrthographicCamera camera;
    private Viewport viewport;
    //
    private Texture playerTexture;
    private Texture wallTexture;
    private Texture goalTexture;

    private Sound winSound;

    private Animation<TextureRegion> walkAnimation;
    private float stateTime;

    //
    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private Array<Rectangle> walls;

    private SpriteBatch batch;
    private BitmapFont font;

    private Rectangle player;

    private Rectangle goal;

    private float speed = 200;
    private boolean gameWon = false;
    private TextureRegion idleFrame;

    public PlayScreen(MyGdxGame game) {
        this.game = game;


        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        viewport.apply();
        camera.position.set(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, 0);

        //add pictures to asserts
        playerTexture = new Texture("player.png");
        wallTexture = new Texture("wall.png");
        goalTexture = new Texture("goal.png");

        winSound = Gdx.audio.newSound(Gdx.files.internal("win.wav"));

        map = new TmxMapLoader().load("level1.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map);

        walls = new Array<>();





        TextureRegion[][] tmp = TextureRegion.split(
            playerTexture,
            playerTexture.getWidth() / 4,
            playerTexture.getHeight()
        );
        idleFrame = tmp[0][0]; // first frame as idle


        TextureRegion[] frames = new TextureRegion[4];
        for (int i = 0; i < 4; i++) {
            frames[i] = tmp[0][i];
        }

        walkAnimation = new Animation<>(0.15f, frames);
        stateTime = 0f;


        batch = new SpriteBatch();
        font = new BitmapFont();

        resetGame();

        MapObjects objects = map.getLayers().get("objects").getObjects();

        for (MapObject obj : objects) {
            if (obj instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject) obj).getRectangle();

                if (obj.getName().equals("playerSpawn")) {
                    player.setPosition(rect.x, rect.y);
                }

                if (obj.getName().equals("goal")) {
                    goal.set(rect);
                }
            }
        }

        int tileSize = 32;
        MapObjects wallObjects = map.getLayers().get("walls").getObjects();

        for (MapObject obj : wallObjects) {
            Rectangle rect = ((RectangleMapObject) obj).getRectangle();
            walls.add(rect);
        }
    }

    private void resetGame() {
        player = new Rectangle(50, 50, 40, 40);
        goal = new Rectangle(700, 400, 50, 50);
        gameWon = false;
    }


    private void moveWithCollision(float dx, float dy) {
        float step = 1f; // 1 pixel steps

        while (dx != 0 || dy != 0) {
            float moveX = Math.abs(dx) > step ? Math.signum(dx) * step : dx;
            float moveY = Math.abs(dy) > step ? Math.signum(dy) * step : dy;

            float oldX = player.x;
            float oldY = player.y;

            player.x += moveX;
            for (Rectangle w : walls) {
                if (player.overlaps(w)) {
                    player.x = oldX;
                    dx = 0;
                    break;
                }
            }

            player.y += moveY;
            for (Rectangle w : walls) {
                if (player.overlaps(w)) {
                    player.y = oldY;
                    dy = 0;
                    break;
                }
            }

            dx -= moveX;
            dy -= moveY;
        }
    }

    @Override
    public void render(float delta) {

        boolean moving = false;
        if (Gdx.input.isKeyPressed(Input.Keys.A) ||
            Gdx.input.isKeyPressed(Input.Keys.D) ||
            Gdx.input.isKeyPressed(Input.Keys.W) ||
            Gdx.input.isKeyPressed(Input.Keys.S)) {
            moving = true;
        }

        TextureRegion currentFrame;
        if (moving) {
            stateTime += delta;
            currentFrame = walkAnimation.getKeyFrame(stateTime, true);
        } else {
            currentFrame = idleFrame;
        }



        if (!gameWon) {
            float oldX = player.x;
            float oldY = player.y;

            // MOVE X
            float dx = 0;
            float dy = 0;

            if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= speed * delta;
            if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += speed * delta;
            if (Gdx.input.isKeyPressed(Input.Keys.W)) dy += speed * delta;
            if (Gdx.input.isKeyPressed(Input.Keys.S)) dy -= speed * delta;

            moveWithCollision(dx, dy);

            for (Rectangle w : walls) {
                if (player.overlaps(w)) {
                    player.x = oldX;
                    break;

                }
            }

            for (Rectangle w : walls) {
                if (player.overlaps(w)) {
                    player.y = oldY;
                    break;

                }
            }

            // WIN CHECK (ONLY ONCE)
            if (player.overlaps(goal)) {
                gameWon = true;
                winSound.play();
            }
        }


        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            resetGame();
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();

        batch.setProjectionMatrix(camera.combined);


        mapRenderer.setView(camera);
        mapRenderer.render();

        batch.begin();

        batch.draw(currentFrame, player.x, player.y, player.width, player.height);


        batch.draw(goalTexture, goal.x, goal.y, goal.width, goal.height);

        if (gameWon) {
            font.draw(batch, "YOU WIN! Press R to Restart", 280, 240);
        }

        batch.end();

    }

    @Override public void resize(int width, int height) { viewport.update(width, height); }
    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void dispose() {

        playerTexture.dispose();

        goalTexture.dispose();
        winSound.dispose();
        batch.dispose();
        font.dispose();
        map.dispose();
        mapRenderer.dispose();


    }
}
