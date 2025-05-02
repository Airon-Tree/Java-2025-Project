package com.project.test;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;


public class StartGame extends Game {

	public SpriteBatch batch;
	public BitmapFont font;
	public FitViewport viewport;


	public void create() {
		
		batch = new SpriteBatch();
		
		// use libGDX's default font
		font = new BitmapFont();
		viewport = new FitViewport(24, 15);
		
		//font has 15pt, but we need to scale it to our viewport by ratio of viewport height to screen height 
		font.setUseIntegerPositions(false);
//		font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight());
		float base = viewport.getWorldHeight() / Gdx.graphics.getHeight();
		font.getData().setScale(base * 2f);      // ×2 → bigger text everywhere
		
		this.setScreen(new MainMenuScreen(this));
	}
	
	@Override
	public void render() {
		super.render(); // important!
	}
	
	@Override
	public void dispose() {
		batch.dispose();
		font.dispose();
	}

}