package com.lalala;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.Screen;

public class WinScreen implements Screen {
    private SpriteBatch batch;
    private BitmapFont font;
    private float elapsedTime;

    public WinScreen(float elapsedTime) {
        this.elapsedTime = elapsedTime;  // 接收并存储时间
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont(); // 默认字体
        font.getData().setScale(1.5f);  // 可调整字体大小
        font.setColor(1f, 1f, 1f, 1f);  // 白色字体
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();

        // 显示用时
        font.draw(batch, "You Win!,Time: " + String.format("%.2f", elapsedTime) + " seconds", 300, 450);

        batch.end();
    }

    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
