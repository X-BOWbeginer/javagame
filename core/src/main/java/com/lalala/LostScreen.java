package com.lalala;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class LostScreen implements Screen {

    private SpriteBatch batch;
    private BitmapFont font;
    private GlyphLayout layout;
    private OrthographicCamera camera;
    private Viewport viewport;

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(5f);
        layout = new GlyphLayout();

        camera = new OrthographicCamera();
        viewport = new FitViewport(800, 600, camera);
        viewport.apply();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f); // 黑色背景
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        String message = "You Lost! Tap to exit";
        layout.setText(font, message);
        float x = (viewport.getWorldWidth() - layout.width) / 2f;
        float y = (viewport.getWorldHeight() + layout.height) / 2f;

        batch.begin();
        font.draw(batch, layout, x, y);
        batch.end();

        if (Gdx.input.isTouched()) {
            Gdx.app.exit(); // 点击退出
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
