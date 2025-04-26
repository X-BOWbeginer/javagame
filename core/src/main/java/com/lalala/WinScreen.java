package com.lalala;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class WinScreen implements Screen {
    private SpriteBatch batch;
    private BitmapFont font;
    private GlyphLayout layout;
    private float elapsedTime;
    private List<Float> topTimes;

    public WinScreen(float elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("font.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 30;  //
        parameter.color.set(1f, 1f, 1f, 1f);
        parameter.borderWidth = 2f;
        parameter.borderColor.set(0f, 0f, 0f, 1f);
        font = generator.generateFont(parameter);
        generator.dispose();

        layout = new GlyphLayout();

        saveElapsedTimeToDatabase();
        loadTopTimes();
    }

    private void saveElapsedTimeToDatabase() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:game_data.db")) {
            Statement stmt = conn.createStatement();
            String createTable = "CREATE TABLE IF NOT EXISTS win_times (id INTEGER PRIMARY KEY AUTOINCREMENT, time REAL)";
            stmt.execute(createTable);

            String insertSQL = "INSERT INTO win_times (time) VALUES (?)";
            PreparedStatement pstmt = conn.prepareStatement(insertSQL);
            pstmt.setDouble(1, elapsedTime);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadTopTimes() {
        topTimes = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:game_data.db")) {
            String selectSQL = "SELECT time FROM win_times ORDER BY time ASC LIMIT 5";
            PreparedStatement pstmt = conn.prepareStatement(selectSQL);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                topTimes.add(rs.getFloat("time"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float currentY = screenHeight / 2f + 150;  //

        String winText = "You Win!";
        layout.setText(font, winText);
        font.draw(batch, layout, (screenWidth - layout.width) / 2f, currentY);

        currentY -= 50;
        String timeText = "Your Time: " + String.format("%.2f", elapsedTime) + " s";
        layout.setText(font, timeText);
        font.draw(batch, layout, (screenWidth - layout.width) / 2f, currentY);

        if (!topTimes.isEmpty()) {
            currentY -= 70;
            String topText = "Top 5 Times:";
            layout.setText(font, topText);
            font.draw(batch, layout, (screenWidth - layout.width) / 2f, currentY);

            for (int i = 0; i < topTimes.size(); i++) {
                currentY -= 40;
                String rankText = (i + 1) + ". " + String.format("%.2f", topTimes.get(i)) + " s";
                layout.setText(font, rankText);
                font.draw(batch, layout, (screenWidth - layout.width) / 2f, currentY);
            }
        }

        batch.end();
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
