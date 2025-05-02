package com.project.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;


public class MainMenuScreen extends ScreenAdapter {
	
	private final StartGame game;

	public MainMenuScreen(StartGame game) {
		this.game = game;
	}

	@Override
	public void render(float delta) {
		ScreenUtils.clear(Color.BLACK);

		game.viewport.apply();
		game.batch.setProjectionMatrix(game.viewport.getCamera().combined);

		game.batch.begin();
		//draw text. Remember that x and y are in meters
		game.font.draw(game.batch, "Encountering Gear Worshipper! ", 11, 8f);
		game.font.draw(game.batch, "Tap anywhere to begin!", 11, 7);
		game.batch.end();

		if (Gdx.input.isTouched()) {
			game.setScreen(new GearGame(game));
			dispose();
		}
	}
	
	@Override
	public void resize(int width, int height) {
		game.viewport.update(width, height, true);
	}

}
