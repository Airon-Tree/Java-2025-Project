package com.project.test;


import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;

public class GearGame extends ScreenAdapter {
	private final StartGame game;
	
    Texture backgroundTexture;
    Texture hongluTexture;
    Texture gearTexture;
    Sound gearSound;
    Sound sSlash1, sSlash2, sSlash3;
    Sound sGun,   sHurt;
    Music music;
    
    Sprite hongluSprite;
    
    Vector2 touchPos;
    
    Array<Sprite> gearSprites;
    
    float gearTimer;
    
    Rectangle hongluRectangle;
    Rectangle gearRectangle;
    private ShapeRenderer debugRenderer;	// visualizing the hitbox
    private boolean showHitboxes = false;
    private static final float HEAD_WIDTH_RATIO   = 0.4f;	//remove the extra space of hitbox
    private static final float HEAD_HEIGHT_RATIO  = 0.4f;
    
    int enemyTakeDown;
    
    private Animation<TextureRegion> moveAnimation;
    private Animation<TextureRegion> attackAnimation;
    
    // --- state handling ---
    private enum State { STANDING, MOVING, ATTACKING, DAMAGED, DASHING }
    private State currentState = State.STANDING;
    private State previousState = State.STANDING;
    private float stateTimer = 0f;            // seconds spent in currentState
    private boolean facingRight = true;       // last horizontal direction
    private boolean movedThisFrame = false;
    
    // --- textures for every state ---
    Texture standTexture, moveTexture, attackTexture, damageTexture;
    Texture move1Texture, move2Texture;
    Texture attack1Texture;
    Texture gearIdleTex, gearMoveTex, gearDmgTex;
    
    // --- health state ---
    private static final int HONG_LU_MAX_HP = 5;
    private static final int GEAR_HP = 4;      // hp gears
    private int hongluHp = HONG_LU_MAX_HP;
    private static final float DAMAGE_DURATION     = 0.30f;  // seconds the hurt pose lasts
    private static final float KNOCKBACK_DISTANCE  = 2.0f;   // metres pushed back
    
    private static final int MAX_GEARS       = 4;
    private static final float GEAR_SPEED    = 1.5f;
    private static final float GEAR_PAUSE    = 1f;   // seconds to idle when close
    private static final float PURSUE_MIN_TIME    = 1.0f;
    private static final float PURSUE_MAX_TIME    = 2.5f;
    private static final float WANDER_TIME        = 1.0f;   // seconds wander lasts
    
    /* ---------- dash ---------- */
    private static final float DASH_SPEED     = 20f;   // metres per second
    private static final float DASH_DURATION  = 0.18f; // seconds the dash lasts
    private static final float DASH_COOLDOWN  = 0.6f;  // minimum time between dashes
    private boolean dashHitDone = false;
    private Rectangle dashRect = new Rectangle();
    Texture dashTex;
    private float   dashTimer     = 0f;
    private float   dashCooldownT = 0f;
    
    
    private int killCount = 0;     
    private boolean gameOver = false; 
    
    // bullets
    private Texture bulletTex;
    private static final float BULLET_SPEED = 24f;    // m s-¹
    private static final float BULLET_W = 0.6f, BULLET_H = 0.3f;
    
    private static class Bullet {
        Sprite sprite;
        Rectangle rect;
        Bullet(Texture tex, float x, float y) {
            sprite = new Sprite(tex); sprite.setSize(BULLET_W, BULLET_H);
            sprite.setPosition(x, y);
            rect = new Rectangle(x, y, BULLET_W, BULLET_H);
        }
    }
    private Array<Bullet> bullets = new Array<>();
    
    
    /* ---------- melee combo ---------- */
    private static final float MELEE_WINDOW      = 0.4f;   // seconds to press again
    private static final float MELEE_ANIM_TIME   = 0.3f;   // sprite lasts this long
    private static final float KNIFE_W = 1.5f, KNIFE_H = 2.0f;  // hitbox size

    Texture knife1Tex, knife1_1Tex, knife2Tex, knife3Tex;
    private int   meleeStage   = 0;     // 0 = idle, 1-3 = slash number
    private float meleeTimer   = 0f;
    private Rectangle knifeRect = new Rectangle();
    
    /* ---------- slash-1 animation ---------- */
    private Animation<TextureRegion> slash1Anim;
    private float slash1Time = 0f;      // timer for the 2nd frame anim
    private boolean slash1HitDone = false;
    
    
    // Loads a texture or returns a 1×1 dummy so the game never gets null. 
    private Texture safeTex(String file) {
        FileHandle fh = Gdx.files.internal(file);
        if (!fh.exists()) {
            System.err.println("[WARN] texture not found: " + file);
            Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pm.setColor(Color.WHITE); pm.fill();
            return new Texture(pm);          // never null
        }
        return new Texture(fh);
    }
    
    
    
    private static class Gear {
    	
        enum GState { PURSUE, IDLE, WANDER, DAMAGED }
        Sprite sprite;
        Rectangle rect;
        GState state = GState.PURSUE;
        float timer = 0f;         // counts time spent in current state
        int hp = GEAR_HP;
        Texture idleTex, moveTex, damagedTex;
        
        float pursueTargetTime;    // how long current PURSUE should last
        Vector2 wanderDir = new Vector2(); // direction chosen for WANDER
    
        Gear(Texture idleTex, Texture moveTex, Texture dmgTex,
             float x, float y, float w, float h) {
        	
            this.idleTex     = idleTex;
            this.moveTex     = moveTex;
            this.damagedTex = dmgTex;
        	
            sprite = new Sprite(idleTex); 
            sprite.setSize(w, h);
            sprite.setPosition(x, y);
            rect   = new Rectangle(x, y, w, h);
            
            if (dmgTex == null) {
                System.err.println("[WARN] damaged texture missing, using idle instead");
                dmgTex = idleTex;
            }
            chooseNewPursueTime();
            
//            System.out.println("[DEBUG]   new Gear   idle=" + idleTex +
//                    " move=" + moveTex + " dmg=" + dmgTex);
//            
            
        }
        
        void chooseNewPursueTime() {
        	pursueTargetTime = MathUtils.random(PURSUE_MIN_TIME, PURSUE_MAX_TIME);
        }

    }
    
    
    private Array<Gear> gears = new Array<>();

    
    public GearGame(final StartGame game) {
    	this.game = game;
    	
        backgroundTexture = new Texture("battle_gears.png");
        hongluTexture = new Texture("Honglu_move.png");
        gearTexture = new Texture("gear_1_idle.png");
        gearIdleTex  = safeTex("gear_1_idle.png");
        gearMoveTex  = safeTex("gear_1_move.png");
        gearDmgTex   = safeTex("gear_1_damaged.png");
        
        
        
        // --- for debug ---
//        System.out.println("[DEBUG] gearIdleTex  = " + gearIdleTex);
//        System.out.println("[DEBUG] gearMoveTex  = " + gearMoveTex);
//        System.out.println("[DEBUG] gearDmgTex   = " + gearDmgTex);


        
        gearSound = Gdx.audio.newSound(Gdx.files.internal("hurt.wav"));
        sSlash1 = Gdx.audio.newSound(Gdx.files.internal("slash1.wav"));
        sSlash2 = Gdx.audio.newSound(Gdx.files.internal("slash2.wav"));
        sSlash3 = Gdx.audio.newSound(Gdx.files.internal("slash3.wav"));
        sGun    = Gdx.audio.newSound(Gdx.files.internal("shot.wav"));
        sHurt   = Gdx.audio.newSound(Gdx.files.internal("hurt.wav"));
        music = Gdx.audio.newMusic(Gdx.files.internal("The 5th Walpurgis Night Easy Battle Theme.wav"));
        music.setLooping(true);
        music.setVolume(.2f);
        
        
        //hongluSprite = new Sprite(hongluTexture);
        standTexture  = new Texture("Honglu_idle.png");
        moveTexture   = new Texture("Honglu_move1 (1).png");
        attackTexture = new Texture("Honglu_skill1_3.png");
        damageTexture = new Texture("Honglu_dmg.png");
        bulletTex   = new Texture("Fx_T_Shape_Heathcliff_Tamaki_skill3_bullet_Roket.png");
        dashTex = new Texture("Honglu_dash.png");
        
        move1Texture   = new Texture("Honglu_move1 (2).png");
        move2Texture   = new Texture("Honglu_move1 (3).png");
        
        attack1Texture = new Texture("skill1_1.png");
        
        knife1Tex   = new Texture("Honglu_knife1.png");
        knife1_1Tex   = new Texture("Honglu_knife2.png");
        knife2Tex   = new Texture("Honglu_knife3.png");
        knife3Tex   = new Texture("Honglu_knife4.png");
        
        //slash-1 anime
        TextureRegion s1f0 = new TextureRegion(knife1Tex);
        TextureRegion s1f1 = new TextureRegion(knife1_1Tex);
        slash1Anim = new Animation<TextureRegion>(0.15f, s1f0, s1f1);   // 0.08s each
        slash1Anim.setPlayMode(Animation.PlayMode.NORMAL);
        
        //move-anime
        Array<TextureRegion> moveFrames = new Array<>();
        moveFrames.add(new TextureRegion(moveTexture));
        moveFrames.add(new TextureRegion(move1Texture));
        moveFrames.add(new TextureRegion(move2Texture));

        moveAnimation = new Animation<TextureRegion>(0.15f, moveFrames, PlayMode.LOOP);
        
        //shot-anime
        Array<TextureRegion> attackFrames = new Array<>();
        attackFrames.add(new TextureRegion(attack1Texture));
        attackFrames.add(new TextureRegion(attackTexture));
        attackFrames.add(new TextureRegion(attack1Texture));
        
        attackAnimation = new Animation<TextureRegion>(0.1f, attackFrames, PlayMode.NORMAL);
        

        hongluSprite = new Sprite(standTexture);   // start idle
        hongluSprite.setSize(4.4f, 4.4f);
        
        touchPos = new Vector2();
        
        gearSprites = new Array<>();
        
        hongluRectangle = new Rectangle();
        gearRectangle = new Rectangle();
        debugRenderer = new ShapeRenderer();	// visualizing the hitbox
        
        // Spawn up-to-4 gears immediately
        for (int i = 0; i < MAX_GEARS; i++) {
        	creategearEnemy();
        }
        
        
    }

    
	@Override
	public void show() {
		// start the playback of the background music
		// when the screen is shown
		music.play();
	}
	
	@Override
    public void render(float delta) {
		if (!gameOver) {
		    input();
		    logic();
		}
        draw();
    }

    private void input() {
        float speed = 5f;
        float delta = Gdx.graphics.getDeltaTime();
        
        if (gameOver || currentState == State.DAMAGED) return;
        
        float dx = 0, dy = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) { dx =  speed * delta;  facingRight = true;  }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT )) { dx = -speed * delta;  facingRight = false; }
        if (Gdx.input.isKeyPressed(Input.Keys.UP   )) { dy =  speed * delta;  }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN )) { dy = -speed * delta;  }


        movedThisFrame = (dx != 0 || dy != 0);
        hongluSprite.translate(dx, dy);

        // Z for fire key → go into ATTACKING for 0.25 s
        if (Gdx.input.isKeyJustPressed(Input.Keys.Z) && currentState != State.ATTACKING) {
            currentState = State.ATTACKING;
            stateTimer = 0f;
            
            float muzzleX = facingRight ? hongluSprite.getX() + hongluSprite.getWidth() :
                			hongluSprite.getX() - BULLET_W;
            float muzzleY = hongluSprite.getY() + hongluSprite.getHeight() * 0.70f;
            bullets.add(new Bullet(bulletTex, muzzleX, muzzleY));
            sGun.play();
        }
        
        
        /* ---------- dash on C ---------- */
        if (Gdx.input.isKeyJustPressed(Input.Keys.C) &&
            currentState != State.DASHING &&
            dashCooldownT <= 0f) {

        	currentState  = State.DASHING;
        	dashTimer     = 0f;
        	dashCooldownT = DASH_COOLDOWN;
        	dashHitDone   = false;                
        	hongluSprite.setRegion(dashTex);
        }
        
        

        if (Gdx.input.isTouched()) {
            touchPos.set(Gdx.input.getX(), Gdx.input.getY()); // Get where the touch happened on screen
            game.viewport.unproject(touchPos); // Convert the units to the world units of the viewport
            hongluSprite.setCenterX(touchPos.x); // Change the horizontally centered position of the honglu
            hongluSprite.setCenterY(touchPos.y);
        }
        
        // Press F1 for hitbox
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            showHitboxes = !showHitboxes;
        }
        
        
        // Press X for Melee attack
        if (Gdx.input.isKeyJustPressed(Input.Keys.X)) {
            if (meleeStage == 0 || meleeTimer < MELEE_WINDOW) {
                meleeStage = (meleeStage % 3) + 1;
            } else meleeStage = 1;
            meleeTimer = 0f;

            // pick sprite / reset animation timer
            if (meleeStage == 1) {
                slash1Time = 0f;  
                slash1HitDone = false;
                sSlash1.play(0.3f);
            }
            if (meleeStage == 2) {
                sSlash2.play(0.4f);
            }
            if (meleeStage == 3) {
                sSlash3.play(0.5f);
            }
            
            
            /* knife hitbox for stage 2 & 3 (instant hit) */
            if (meleeStage >= 2) {
                float kx = facingRight
                          ? hongluSprite.getX() + hongluSprite.getWidth()
                           : hongluSprite.getX() - KNIFE_W;
                float ky = hongluSprite.getY() + hongluSprite.getHeight()*0.5f - KNIFE_H/2f;
                knifeRect.set(kx, ky, KNIFE_W, KNIFE_H);
            
                for (int i = gears.size - 1; i >= 0; i--) {
                    Gear g = gears.get(i);
                    if (knifeRect.overlaps(g.rect)) {
                        if (--g.hp <= 0) { gears.removeIndex(i); killCount++; }
                        else { g.state = Gear.GState.DAMAGED; g.timer = 0f;
                               g.sprite.setRegion(g.damagedTex); }
                    }
                }
            }
        }
        
        
        
    }

    private void logic() {
    	
    	
    	
        float worldWidth = game.viewport.getWorldWidth();
        float worldHeight = game.viewport.getWorldHeight();
        float hongluWidth = hongluSprite.getWidth();
        float hongluHeight = hongluSprite.getHeight();
        float delta = Gdx.graphics.getDeltaTime();
        
        /* ---------- dash movement ---------- */
        if (currentState == State.DASHING) {
            float dx = DASH_SPEED * delta * (facingRight ? 1 : -1);
            hongluSprite.translateX(dx);
            
            
            /* --- dash damage (once per dash) --- */
            if (!dashHitDone) {
                float kx = facingRight
                           ? hongluSprite.getX() + hongluSprite.getWidth()
                           : hongluSprite.getX() - KNIFE_W;
                float ky = hongluSprite.getY() + hongluSprite.getHeight()*0.5f - KNIFE_H/2f;
                dashRect.set(kx, ky, KNIFE_W, KNIFE_H);        // reuse knife size
        
                for (int i = gears.size - 1; i >= 0; i--) {
                    Gear g = gears.get(i);
                    if (dashRect.overlaps(g.rect)) {
                        if (--g.hp <= 0) { gears.removeIndex(i); killCount++; }
                        else { g.state = Gear.GState.DAMAGED; g.timer = 0f;
                               g.sprite.setRegion(g.damagedTex); }
                    }
               }
                dashHitDone = true;         // damage only once per dash
            }
            
            
            
            dashTimer += delta;
            if (dashTimer >= DASH_DURATION) {
                currentState = State.STANDING;   // or MOVING if key held
            }
        }

        hongluSprite.setX(MathUtils.clamp(hongluSprite.getX(), 0, worldWidth - hongluWidth));
        hongluSprite.setY(MathUtils.clamp(hongluSprite.getY(), 0, 12 - hongluHeight)); 
        
        
    	// ------------- state transitions -------------
    	State previousState = currentState;

    	// MOVING if we walked this frame
    	if (currentState != State.ATTACKING && 
    			currentState != State.DAMAGED &&
    			currentState != State.DASHING) {
//    	    if (Math.abs(hongluSprite.getVelocity().x) > 0.01f ||
//    	    		Math.abs(hongluSprite.getVelocity().y) > 0.01f)
//    	        currentState = State.MOVING;
//    	    else
//    	        currentState = State.STANDING;
    		currentState = movedThisFrame ? State.MOVING : State.STANDING;
    	}

    	// leave ATTACKING after 0.25 s
    	if (previousState == State.ATTACKING && stateTimer > 0.3f) {
    	    currentState = State.STANDING; 
    	    stateTimer = 0f;
    	}
    	
    	// leave DAMAGED after DAMAGE_DURATION
    	if (previousState == State.DAMAGED && stateTimer > DAMAGE_DURATION) {
    		currentState = State.STANDING;
    		stateTimer   = 0f;
    	}
    	
    	if (currentState != previousState) {
    	    stateTimer = 0f;
    	}
    	
    	
    	
    	
    	// ------------- sprite art -------------
    	TextureRegion frame;
    	switch (currentState) {
    	    case STANDING:  hongluSprite.setRegion(standTexture);   break;
    	    //case MOVING:    hongluSprite.setRegion(moveTexture);    break;
    	    case MOVING:
    	        frame = moveAnimation.getKeyFrame(stateTimer, true);    // loop moving animation
    	        hongluSprite.setRegion(frame); 
    	        break;
    	    case ATTACKING: 
    	    	frame = attackAnimation.getKeyFrame(stateTimer, true);
    	    	hongluSprite.setRegion(frame);  
    	    	break;
    	    case DAMAGED:   hongluSprite.setRegion(damageTexture);  break;
    	}
    	
    	/* ---- override with knife sprites if a combo is in progress ---- */
    	if (meleeStage == 1) {
    		TextureRegion S1frame = slash1Anim.getKeyFrame(slash1Time, false);
    		hongluSprite.setRegion(S1frame);
    		hongluSprite.setFlip(!facingRight, false); 
    	} else if (meleeStage == 2) {
    	    hongluSprite.setRegion(knife2Tex);
    	} else if (meleeStage == 3) {
    	    hongluSprite.setRegion(knife3Tex);
    	}

    	hongluSprite.setFlip(!facingRight, false);  // face last direction
    	stateTimer += delta;
        
        
        
        
        // Apply the honglu position and size to the hongluRectangle
    	float headW = hongluWidth  * HEAD_WIDTH_RATIO + 0.5f;
    	float headH = hongluHeight * HEAD_HEIGHT_RATIO;
    	float offsetX;
    	if (facingRight) {
    	    // box starts a little to the right, leaving some room on the back
    	    offsetX = hongluWidth - headW - 0.9f;
    	} else {
    	    // facing left → box flush with sprite’s left edge
    	    offsetX = 0.9f;
    	}

    	hongluRectangle.set(
    	        hongluSprite.getX() + offsetX,
    	        hongluSprite.getY() + (hongluWidth - headH), // vertically centred
    	        headW,
    	        headH
    	);
        
    	
    	
    	// --- move & cull bullets ---
    	for (int i = bullets.size - 1; i >= 0; i--) {
    	    Bullet b = bullets.get(i);
    	    float dx = BULLET_SPEED * delta * (facingRight ? 1 : -1);
    	    b.sprite.translateX(dx);
    	    b.rect.setPosition(b.sprite.getX(), b.sprite.getY());

    	    if (b.sprite.getX() < -BULLET_W || b.sprite.getX() > worldWidth)
    	        bullets.removeIndex(i);
    	}
    	
    	
//    	/* ---------- melee input (C key) ---------- */
//    	if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
//    	    // combo chain: press again within MELEE_WINDOW
//    	    if (meleeStage == 0 || meleeTimer < MELEE_WINDOW) {
//    	        meleeStage = (meleeStage % 3) + 1;     // 1 → 2 → 3 → 1 …
//    	    } else {
//    	        meleeStage = 1;                        // too late → restart combo
//    	    }
//    	    meleeTimer = 0f;
//
//    	    // choose sprite
//    	    switch (meleeStage) {
//    	        case 1: frame = attackAnimation.getKeyFrame(stateTimer, true);
//    	    			hongluSprite.setRegion(frame);  
//    	    			break;
//    	        case 2: hongluSprite.setRegion(knife2Tex);	break;
//    	        case 3: hongluSprite.setRegion(knife3Tex);	break;
//    	    }
//
//    	    /* build knife hit-box in front of Hong Lu */
//    	    float kx = facingRight
//    	               ? hongluSprite.getX() + hongluSprite.getWidth()
//    	               : hongluSprite.getX() - KNIFE_W;
//    	    float ky = hongluSprite.getY() + hongluSprite.getHeight() * 0.5f - KNIFE_H / 2f;
//    	    knifeRect.set(kx, ky, KNIFE_W, KNIFE_H);
//
//    	    /* damage gears once */
//    	    for (int i = gears.size - 1; i >= 0; i--) {
//    	        Gear g = gears.get(i);
//    	        if (knifeRect.overlaps(g.rect)) {
//    	            if (--g.hp <= 0) { gears.removeIndex(i); killCount++; }
//    	            else {
//    	                g.state = Gear.GState.DAMAGED;
//    	                g.timer = 0f;
//    	                g.sprite.setRegion(g.damagedTex);
//    	            }
//    	        }
//    	    }
//    	}

    	
        
    	
    	

    	
    	
    	/* ---------- AI gears ---------- */
    	for (int i = gears.size - 1; i >= 0; i--) {
    	    Gear g = gears.get(i);
    	    g.timer += delta;

    	    switch (g.state) {
    	        /* ---------- PURSUE Hong Lu ---------- */
    	        case PURSUE: {
    	            g.sprite.setRegion(g.moveTex);
    	            float cx = hongluSprite.getX() + hongluSprite.getWidth()  / 2f;
    	            float cy = hongluSprite.getY() + hongluSprite.getHeight() / 2f;
    	            float gx = g.sprite.getX()    + g.sprite.getWidth()  / 2f;
    	            float gy = g.sprite.getY()    + g.sprite.getHeight() / 2f;

    	            float dx = Math.signum(cx - gx) * GEAR_SPEED * delta;
    	            float dy = Math.signum(cy - gy) * GEAR_SPEED * delta;

    	            g.sprite.translate(dx, dy);

    	            if (g.timer >= g.pursueTargetTime) {
    	                g.state = Gear.GState.IDLE;
    	                g.timer = 0f;
    	                g.sprite.setRegion(g.idleTex);
    	            }
    	            break;
    	        }

    	        /* ---------- stand still for 1 s ---------- */
    	        case IDLE: {
    	            if (g.timer >= 1f) {
    	                g.state = Gear.GState.WANDER;
    	                g.timer = 0f;
    	                g.wanderDir.set(MathUtils.random(-1f, 1f),
    	                                MathUtils.random(-1f, 1f)).nor();
    	                g.sprite.setRegion(g.moveTex);
    	            }
    	            break;
    	        }

    	        /* ---------- wander random dir for 1 s ---------- */
    	        case WANDER: {
    	            g.sprite.translate(g.wanderDir.x * GEAR_SPEED * delta,
    	                               g.wanderDir.y * GEAR_SPEED * delta);
    	            if (g.timer >= WANDER_TIME) {
    	                g.state = Gear.GState.PURSUE;
    	                g.timer = 0f;
    	                g.chooseNewPursueTime();
    	            }
    	            break;
    	        }

    	        /* ---------- hurt freeze ---------- */
    	        case DAMAGED: {
    	            if (g.timer >= DAMAGE_DURATION) {
    	                g.state = Gear.GState.PURSUE;
    	                g.timer = 0f;
    	                g.chooseNewPursueTime();
    	                g.sprite.setRegion(g.moveTex);
    	            }
    	            break;
    	        }
    	    }

    	    /* keep inside world bounds */
    	    g.sprite.setX(MathUtils.clamp(g.sprite.getX(), 0,
    	             worldWidth - g.sprite.getWidth()));
    	    g.sprite.setY(MathUtils.clamp(g.sprite.getY(), 0,
    	             10 - g.sprite.getHeight()));

    	    /* update head-only rectangle */
    	    float gh = g.sprite.getHeight(), gw = g.sprite.getWidth();
    	    headW = gw * HEAD_WIDTH_RATIO;
    	    headH = gh * HEAD_HEIGHT_RATIO;
    	    g.rect.set(g.sprite.getX() + (gw - headW) / 2f,
    	               g.sprite.getY() + (gh - headH),
    	               headW, headH);

    	    /* --- bullet → gear --- */
    	    for (int j = bullets.size - 1; j >= 0; j--) {
    	        Bullet b = bullets.get(j);
    	        if (g.rect.overlaps(b.rect)) {
    	            bullets.removeIndex(j);
    	            if (--g.hp <= 0) {
    	                gears.removeIndex(i);
    	                killCount++;                       // existing counter
    	            } else {
    	                g.state = Gear.GState.DAMAGED;
    	                g.timer = 0f;
    	                if (g.damagedTex != null) {
    	                    g.sprite.setRegion(new TextureRegion(g.damagedTex));
    	                }
    	            }
    	            break;
    	        }
    	    }

    	    /* --- gear → player --- */
    	    if (currentState != State.DASHING && 
    	    		i < gears.size && 
    	    		g.rect.overlaps(hongluRectangle) && 
    	    		currentState != State.DAMAGED) {
    	        if (--hongluHp <= 0) { gameOver = true; return; }
    	        currentState = State.DAMAGED; stateTimer = 0f;
    	        sHurt.play();
    	        float knockDir = Math.signum(hongluSprite.getX() - g.sprite.getX());
    	        hongluSprite.translateX(knockDir * KNOCKBACK_DISTANCE);
    	    }
    	}

    	/* ---------- maintain max-4 gears ---------- */
    	while (!gameOver && gears.size < MAX_GEARS) creategearEnemy();
    	
    	
    	
    	
    	/* ---------- melee timer & slash-1 animation ---------- */
    	if (meleeStage > 0) {
    	    meleeTimer += delta;
    	    
    	    // end sprite after MELEE_ANIM_TIME
        	if (meleeTimer > MELEE_WINDOW) {
        	    meleeStage = 0;
        	}
        	
        	if (currentState == State.DASHING) {
        		
        	    hongluSprite.setRegion(dashTex);
        	    hongluSprite.setFlip(!facingRight, false);
        	    
        	} else if (meleeStage == 1) {
    	        slash1Time += delta;
    	        
    	        // update sprite every frame
    	        frame = slash1Anim.getKeyFrame(slash1Time, false);
    	        hongluSprite.setRegion(frame);
    	        hongluSprite.setFlip(!facingRight, false); 

    	        // land the hit exactly on frame index 1 (the 2nd frame) once
    	        if (!slash1HitDone &&
    	            slash1Anim.getKeyFrameIndex(slash1Time) == 1) {
    	            slash1HitDone = true;

    	            float kx = facingRight
    	                       ? hongluSprite.getX() + hongluSprite.getWidth()
    	                       : hongluSprite.getX() - KNIFE_W;
    	            float ky = hongluSprite.getY() + hongluSprite.getHeight()*0.5f - KNIFE_H/2f;
    	            knifeRect.set(kx, ky, KNIFE_W, KNIFE_H);

    	            for (int i = gears.size - 1; i >= 0; i--) {
    	                Gear g = gears.get(i);
    	                if (knifeRect.overlaps(g.rect)) {
    	                    if (--g.hp <= 0) { gears.removeIndex(i); killCount++; }
    	                    else { g.state = Gear.GState.DAMAGED; g.timer = 0f;
    	                           g.sprite.setRegion(g.damagedTex); }
    	                }
    	            }
    	        }
    	    }
    	    // end sprite after MELEE_ANIM_TIME (same as before)
//    	    if (meleeTimer > MELEE_ANIM_TIME) {
//    	    	meleeStage = 0;
//    	    }
    	}
        

    	if (dashCooldownT > 0f) dashCooldownT -= delta;
        if (gameOver) return;
    }

    private void draw() {
    	
        ScreenUtils.clear(Color.BLACK);
        
        
        game.viewport.apply();
        game.batch.setProjectionMatrix(game.viewport.getCamera().combined);
        

        if (gameOver) {
            game.batch.begin();
            game.font.draw(game.batch,
                           "GAME  OVER",
                           game.viewport.getWorldWidth() / 2f - 4f,
                           game.viewport.getWorldHeight() / 2f + 1.5f);
            game.font.draw(game.batch,
                           "Total Kills: " + killCount,
                           game.viewport.getWorldWidth() / 2f - 5f,
                           game.viewport.getWorldHeight() / 2f - 1f);
            game.batch.end();
            return;                                     // don’t draw anything else
        }
        
        
        game.batch.begin();

        float worldWidth = game.viewport.getWorldWidth();
        float worldHeight = game.viewport.getWorldHeight();

        game.batch.draw(backgroundTexture, 0, 0, worldWidth, worldHeight);
        hongluSprite.draw(game.batch);

        // draw each enemy sprite
        for (Gear g : gears)  g.sprite.draw(game.batch);
//        for (Sprite gearSprite : gearSprites) {
//            gearSprite.draw(game.batch);
//        }
        
        // draw bullets
        for (Bullet b : bullets) b.sprite.draw(game.batch);
        
        // HUD: health & kills
        game.font.draw(game.batch,
                       "HP: " + hongluHp,
                       1f, game.viewport.getWorldHeight() - 1f);      // y measured from bottom
        game.font.draw(game.batch,
                       "Kills: " + killCount,
                       1f, game.viewport.getWorldHeight() - 2.5f);


        game.batch.end();
        
        // ---------- DEBUG HITBOXES ----------
        if(showHitboxes) {
            debugRenderer.setProjectionMatrix(game.viewport.getCamera().combined);
            debugRenderer.begin(ShapeRenderer.ShapeType.Line);

            // player hitbox (red)
            debugRenderer.setColor(Color.RED);
            debugRenderer.rect(hongluRectangle.x,
                               hongluRectangle.y,
                               hongluRectangle.width,
                               hongluRectangle.height);

            // gears hitboxes (yellow)
            debugRenderer.setColor(Color.YELLOW);
            for (Gear g : gears) {
            	debugRenderer.rect(g.rect.x, g.rect.y,
            			g.rect.width, g.rect.height);
            }
            
            debugRenderer.setColor(Color.BLUE);
            if (meleeStage > 0) {                            // show knifeRect
                debugRenderer.rect(knifeRect.x, knifeRect.y,
                                   knifeRect.width, knifeRect.height);
            }
            
            debugRenderer.setColor(Color.BLUE);
            if (currentState == State.DASHING && !dashHitDone)
                debugRenderer.rect(dashRect.x, dashRect.y, dashRect.width, dashRect.height);

            debugRenderer.end();
        }
        
        
    }

    private void creategearEnemy() {
    	
//        float gearWidth = 2.7f;
//        float gearHeight = 4.9f;
//        float worldWidth = game.viewport.getWorldWidth();
//        float worldHeight = game.viewport.getWorldHeight();
//
//        
//        gears.add(new Gear(gearTexture, worldWidth, MathUtils.random(0, 10 - gearWidth), 
//        				   gearWidth, gearHeight));
    	float w = 4.4f, h = 4.4f;
        float x = game.viewport.getWorldWidth() + MathUtils.random(0f, 4f);
        float y = MathUtils.random(0, 10 - h);
        gears.add(new Gear(gearIdleTex, gearMoveTex, gearDmgTex, x, y, w, h));

    }
    
	@Override
	public void resize(int width, int height) {
		game.viewport.update(width, height, true);
	}

    @Override
    public void pause() {
        
    }

    @Override
    public void resume() {
        
    }

    @Override
    public void dispose() {
		backgroundTexture.dispose();
		gearSound.dispose();
		music.dispose();
		gearTexture.dispose();
		standTexture.dispose();
		moveTexture.dispose();
		move1Texture.dispose();
		move2Texture.dispose();
		attackTexture.dispose();
		damageTexture.dispose();
		debugRenderer.dispose();
		bulletTex.dispose();
		gearIdleTex.dispose();
		gearMoveTex.dispose();
		gearDmgTex.dispose();
		knife1Tex.dispose();
		knife1_1Tex.dispose();
		knife2Tex.dispose();
		knife3Tex.dispose();
		sSlash1.dispose();  sSlash2.dispose();  sSlash3.dispose();
		sGun.dispose();     sHurt.dispose();
		dashTex.dispose();
    }
}
