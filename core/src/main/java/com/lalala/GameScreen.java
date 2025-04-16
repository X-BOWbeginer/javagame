// GameScreen.java
package com.lalala;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class GameScreen implements Screen, ContactListener {
    private World world;
    private Box2DDebugRenderer debugRenderer;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private Player player;
    private FitViewport viewport;

    @Override
    public void show() {
        world = new World(new Vector2(0, -25f), true);
        world.setContactListener(this);

        debugRenderer = new Box2DDebugRenderer();
        camera = new OrthographicCamera();
        viewport = new FitViewport(16, 9, camera);
        batch = new SpriteBatch();

        player = new Player(world, 8, 5);

        createBounds(0.5f);
    }

    private void createBounds(float margin) {
        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();

        float left = margin;
        float right = worldWidth - margin;
        float bottom = margin;
        float top = worldHeight - margin;

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        Body bounds = world.createBody(bodyDef);

        EdgeShape edge = new EdgeShape();
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = edge;
        fixtureDef.friction = 0.8f;

        edge.set(new Vector2(left, bottom), new Vector2(right, bottom));
        bounds.createFixture(fixtureDef);
        edge.set(new Vector2(left, top), new Vector2(right, top));
        bounds.createFixture(fixtureDef);
        edge.set(new Vector2(left, bottom), new Vector2(left, top));
        bounds.createFixture(fixtureDef);
        edge.set(new Vector2(right, bottom), new Vector2(right, top));
        bounds.createFixture(fixtureDef);

        edge.dispose();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();

        boolean left = Gdx.input.isKeyPressed(Input.Keys.A);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.D);
        boolean jump = Gdx.input.isKeyJustPressed(Input.Keys.W);
        boolean dash = Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT);

        player.update(left, right, jump, dash, delta);
        world.step(delta, 6, 2);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        player.draw(batch);
        batch.end();

        debugRenderer.render(world, camera.combined);
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { batch.dispose(); player.dispose(); world.dispose(); debugRenderer.dispose(); }
    @Override public void beginContact(Contact contact) { player.beginContact(contact); }
    @Override public void endContact(Contact contact) { player.endContact(contact); }
    @Override public void preSolve(Contact contact, Manifold manifold) {}
    @Override public void postSolve(Contact contact, ContactImpulse impulse) {}
}
