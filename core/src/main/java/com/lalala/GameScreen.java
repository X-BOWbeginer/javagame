package com.lalala;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.viewport.FitViewport;

import static com.lalala.Boss.*;

public class GameScreen implements Screen, ContactListener {

    private final MainGame game;

    private World world;
    private Box2DDebugRenderer debugRenderer;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private FitViewport viewport;
    private ShapeRenderer shapeRenderer;
    private Player player;
    private Boss boss;

    private boolean left, right, jump, dash, attack, downAttack;
    private boolean paused = false;
    private float elapsedTime = 0f;  // 用于记录游戏时间

    public GameScreen(MainGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        world = new World(new Vector2(0, -25f), true);
        world.setContactListener(this);

        debugRenderer = new Box2DDebugRenderer();
        camera = new OrthographicCamera();
        viewport = new FitViewport(16, 9, camera);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        player = new Player(world, 8, 5);
        boss = new Boss(world, 1, 5);

        createBounds(0.5f);
    }

    private void input() {
        left = Gdx.input.isKeyPressed(Input.Keys.A);
        right = Gdx.input.isKeyPressed(Input.Keys.D);
        jump = Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.K);
        dash = Gdx.input.isKeyJustPressed(Input.Keys.L);
        attack = Gdx.input.isKeyJustPressed(Input.Keys.J);
        downAttack = attack && Gdx.input.isKeyPressed(Input.Keys.S);
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            paused = !paused;
        }
    }

    private void logic(float delta) {
        player.update(left, right, jump, dash, attack, downAttack, delta);

        if (boss != null) {
            boss.update(player.getPosition(), delta);
            boss.tryHit(player.getCurrentHitbox());
            player.tryHit(boss.getCurrentHitbox());

            if (!boss.isAlive()) {
                boss.dispose();
                boss = null;
                game.setScreen(new WinScreen(elapsedTime));  // 传递时间到 WinScreen
                return;
            }

        }

        if (player.isDead()) {
            game.setScreen(new LostScreen());
            return;
        }

        world.step(delta, 6, 2);
    }

    private void draw() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        player.draw(batch);
        if (boss != null) {
            boss.draw(batch);
        }
        if (paused) {
            // 字体较小可用 BitmapFont，或者放大一点
            BitmapFont font = new BitmapFont();
            font.setColor(1f, 1f, 1f, 1f);
            font.draw(batch, "Paused - Press P to Resume", 300, 500);
        }
        batch.end();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        player.drawHitbox(shapeRenderer);
        if (boss != null) {
            boss.drawHealthBar(shapeRenderer);
        }
        player.drawHealthBar(shapeRenderer);

        // 调试用的 hitbox
        shapeRenderer.setColor(1f, 1f, 0f, 1f);
        if (boss != null) {
            Rectangle bossBox = boss.getCurrentHitbox();
            shapeRenderer.rect(bossBox.x, bossBox.y, bossBox.width, bossBox.height);
        }
        Vector2 pos = player.getBody().getPosition();
        shapeRenderer.setColor(0, 1, 0, 1);
        shapeRenderer.rect(pos.x - 0.5f, pos.y - 0.5f, 1f, 1f);

        shapeRenderer.end();

        debugRenderer.render(world, camera.combined);
    }

    @Override
    public void render(float delta) {
        input();

        if (!paused) {
            logic(delta);
            elapsedTime += delta;
        }

        draw();
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
        fixtureDef.filter.categoryBits = CATEGORY_GROUND;
        fixtureDef.filter.maskBits = CATEGORY_PLAYER | CATEGORY_BOSS;

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

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        batch.dispose();
        player.dispose();
        world.dispose();
        debugRenderer.dispose();
        shapeRenderer.dispose();
    }

    @Override public void beginContact(Contact contact) { player.beginContact(contact); }
    @Override public void endContact(Contact contact) { player.endContact(contact); }
    @Override public void preSolve(Contact contact, Manifold manifold) {}
    @Override public void postSolve(Contact contact, ContactImpulse impulse) {}
}
