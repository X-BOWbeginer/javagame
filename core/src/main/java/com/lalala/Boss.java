// Boss.java
package com.lalala;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import java.util.Random;

public class Boss {
    public static final short CATEGORY_PLAYER = 0x0001;
    public static final short CATEGORY_BOSS   = 0x0002;
    public static final short CATEGORY_GROUND = 0x0004;

    // State machine states
    private enum State {
        IDLE,
        WALKING,
        JUMPING,
        LANDING,
        DASHING,
        JUMP_DASHING,
        IDLE_WAITING
    }

    private State currentState = State.IDLE;
    private World world;
    private Body body;
    private Vector2 position;
    private int health = 5;
    private float maxHealth = 5f;

    private boolean alive = true;
    private float hitCooldown = 0f;
    private final float HIT_INTERVAL = 0.5f;

    private Rectangle currentHitbox = new Rectangle();

    private int actionCD = 0;
    private int jumpCD = 0;
    private int dashCD = 0;
    private int jumpFinalCD = 0;

    private float stateTime = 0f;
    private boolean facingRight = true;
    private boolean isMovingWhileWalking = false; // Flag to determine if walking involves movement

    private float width;
    private float height;

    // Jump dash related variables
    private boolean hasJumpedForDash = false;
    private float directionForJumpDash = 1f;
    private Vector2 jumpDashVelocity = new Vector2();

    // Animations
    private Animation<TextureRegion> dashLeftAnimation;
    private Animation<TextureRegion> dashRightAnimation;
    private Animation<TextureRegion> jumpLeftAnimation;
    private Animation<TextureRegion> jumpRightAnimation;
    private Animation<TextureRegion> walkLeftAnimation;
    private Animation<TextureRegion> walkRightAnimation;
    private Animation<TextureRegion> idleLeftAnimation;
    private Animation<TextureRegion> idleRightAnimation;
    private Animation<TextureRegion> landLeftAnimation;
    private Animation<TextureRegion> landRightAnimation;
    private Animation<TextureRegion> jumpDashLeftAnimation;
    private Animation<TextureRegion> jumpDashRightAnimation;

    private Animation<TextureRegion> currentAnimation;
    private TextureRegion currentFrame;

    private Random random = new Random();

    public Boss(World world, float x, float y) {
        this.world = world;
        this.position = new Vector2(x, y);

        // Load animations
        idleLeftAnimation = loadAnimation("Boss/Idle", 1, 0.1f, Animation.PlayMode.LOOP);
        idleRightAnimation = loadAnimation("Boss/IdleR", 1, 0.1f, Animation.PlayMode.LOOP);

        TextureRegion firstFrame = idleLeftAnimation.getKeyFrame(0f);
        float pixelsPerUnit = 100f;
        this.width = firstFrame.getRegionWidth() / pixelsPerUnit;
        this.height = firstFrame.getRegionHeight() / pixelsPerUnit;

        // Create physics body
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.fixedRotation = true;
        bodyDef.position.set(x, y);
        body = world.createBody(bodyDef);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width / 2f, height / 2f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1f;
        fixtureDef.friction = 0.2f;
        fixtureDef.filter.categoryBits = CATEGORY_BOSS;
        fixtureDef.filter.maskBits = CATEGORY_GROUND;

        body.createFixture(fixtureDef);
        shape.dispose();

        // Load all animations
        dashLeftAnimation = loadAnimation("Boss/Dash", 11, 0.05f, Animation.PlayMode.NORMAL);
        dashRightAnimation = loadAnimation("Boss/DashR", 11, 0.05f, Animation.PlayMode.NORMAL);
        jumpLeftAnimation = loadAnimation("Boss/Jump", 28, 0.05f, Animation.PlayMode.NORMAL);
        jumpRightAnimation = loadAnimation("Boss/JumpR", 28, 0.05f, Animation.PlayMode.NORMAL);
        walkLeftAnimation = loadAnimation("Boss/Walk", 10, 0.08f, Animation.PlayMode.LOOP);
        walkRightAnimation = loadAnimation("Boss/WalkR", 10, 0.08f, Animation.PlayMode.LOOP);
        landLeftAnimation = loadAnimation("Boss/Land", 5, 0.05f, Animation.PlayMode.NORMAL);
        landRightAnimation = loadAnimation("Boss/LandR", 5, 0.05f, Animation.PlayMode.NORMAL);
        jumpDashLeftAnimation = loadAnimation("Boss/JumpDash", 28, 0.05f, Animation.PlayMode.NORMAL);
        jumpDashRightAnimation = loadAnimation("Boss/JumpDashR", 28, 0.05f, Animation.PlayMode.NORMAL);

        // Initialize with idle animation
        currentAnimation = idleRightAnimation;
        currentFrame = currentAnimation.getKeyFrame(0);
    }

    private Animation<TextureRegion> loadAnimation(String path, int count, float frameDuration, Animation.PlayMode playMode) {
        Array<TextureRegion> frames = new Array<>();
        for (int i = 0; i <= count; i++) {
            String filename = path + "/" + i + ".png";
            Texture tex = new Texture(Gdx.files.internal(filename));
            frames.add(new TextureRegion(tex));
        }
        Animation<TextureRegion> anim = new Animation<>(frameDuration, frames.toArray(TextureRegion.class));
        anim.setPlayMode(playMode);
        return anim;
    }

    private boolean isOnGround() {
        final boolean[] result = {false};
        Vector2 start = new Vector2(body.getPosition().x, body.getPosition().y - height / 2f);
        Vector2 end = new Vector2(start.x, start.y - 0.1f);
        world.rayCast((fixture, point, normal, fraction) -> {
            if (fixture.getFilterData().categoryBits == CATEGORY_GROUND) {
                result[0] = true;
                return 0;
            }
            return 1;
        }, start, end);
        return result[0];
    }

    public void update(Vector2 playerPos, float delta) {
        if (!alive) return;

        // Update hitbox
        Vector2 pos = body.getPosition();
        if (currentFrame != null) {
            float pixelsPerUnit = 100f;
            float drawW = currentFrame.getRegionWidth() / pixelsPerUnit;
            float drawH = currentFrame.getRegionHeight() / pixelsPerUnit;
            currentHitbox.set(pos.x - drawW / 2f, pos.y - 1f, drawW, drawH);
        }

        stateTime += delta;

        // Update cooldowns
        if (hitCooldown > 0) hitCooldown -= delta;
        if (actionCD > 0) actionCD--;
        if (jumpCD > 0) jumpCD--;
        if (dashCD > 0) dashCD--;
        if (jumpFinalCD > 0) jumpFinalCD--;

        position.set(body.getPosition());

        // State machine implementation
        switch (currentState) {
            case DASHING:
                updateDashingState();
                break;

            case JUMP_DASHING:
                updateJumpDashingState(playerPos);
                break;

            case JUMPING:
                updateJumpingState();
                break;

            case IDLE_WAITING:
                updateIdleWaitingState();
                break;

            case WALKING:
                if (isMovingWhileWalking) {
                    Vector2 vel = body.getLinearVelocity();
                    body.setLinearVelocity((facingRight ? 6f : -6f), vel.y);
                }
                currentFrame = currentAnimation.getKeyFrame(stateTime, true);

                //
                if (actionCD == 0) {
                    currentState = State.IDLE_WAITING;
                    isMovingWhileWalking = false;
                    stateTime = 0f;
                }
                break;


            case IDLE:
            default:
                // Choose next action if timer is up
                if (actionCD == 0) {
                    chooseNextAction(playerPos);
                } else {
                    // Continue with current animation
                    if (currentAnimation == walkLeftAnimation || currentAnimation == walkRightAnimation) {
                        currentFrame = currentAnimation.getKeyFrame(stateTime, true);
                    } else {
                        // Default to idle animation
                        currentAnimation = facingRight ? idleRightAnimation : idleLeftAnimation;
                        currentFrame = currentAnimation.getKeyFrame(stateTime, true);
                    }
                }
                break;
        }

        position.set(body.getPosition());
    }

    private void updateDashingState() {
        currentAnimation = facingRight ? dashRightAnimation : dashLeftAnimation;
        currentFrame = currentAnimation.getKeyFrame(stateTime);

        if (currentAnimation.isAnimationFinished(stateTime)) {
            currentState = State.IDLE_WAITING;
            body.setGravityScale(1);
            stateTime = 0f;
            body.setLinearVelocity(0, 0);
        }
    }

    private void updateJumpDashingState(Vector2 playerPos) {
        float vy = body.getLinearVelocity().y;

        if (!hasJumpedForDash) {
            // Initial jump phase
            body.setLinearVelocity(body.getLinearVelocity().x, 12f);
            hasJumpedForDash = true;
            stateTime = 0f;

            float dx = playerPos.x - position.x;
            directionForJumpDash = dx >= 0 ? 1f : -1f;
            facingRight = dx >= 0;

            Vector2 dashDir = new Vector2(playerPos.x - position.x, 0).nor();
            jumpDashVelocity.set(dashDir.scl(15f).x, -5f);
        } else if (vy < 0 && stateTime > 0.15f) {
            // Dash down phase
            body.setLinearVelocity(jumpDashVelocity);
            currentAnimation = facingRight ? jumpDashRightAnimation : jumpDashLeftAnimation;
            currentFrame = currentAnimation.getKeyFrame(stateTime);

            if (isOnGround() || currentAnimation.isAnimationFinished(stateTime)) {
                currentState = State.IDLE_WAITING;
                hasJumpedForDash = false;
                stateTime = 0f;
                body.setLinearVelocity(0, 0);
            }
        }

        currentAnimation = facingRight ? jumpDashRightAnimation : jumpDashLeftAnimation;
        currentFrame = currentAnimation.getKeyFrame(stateTime);
    }

    private void updateJumpingState() {
        float vy = body.getLinearVelocity().y;

        if (vy == 0) {
            currentState = State.IDLE_WAITING;
            stateTime = 0f;
        } else if (vy < 0) {
            // Landing phase
            currentAnimation = facingRight ? landRightAnimation : landLeftAnimation;
            currentFrame = currentAnimation.getKeyFrame(stateTime);

            if (currentAnimation.isAnimationFinished(stateTime)) {
                currentFrame = currentAnimation.getKeyFrames()[currentAnimation.getKeyFrames().length - 1];
            }
        } else {
            // Rising phase
            currentAnimation = facingRight ? jumpRightAnimation : jumpLeftAnimation;
            currentFrame = currentAnimation.getKeyFrame(stateTime);
        }
    }

    private void updateIdleWaitingState() {
        currentAnimation = facingRight ? idleRightAnimation : idleLeftAnimation;
        currentFrame = currentAnimation.getKeyFrame(stateTime, true);

        if (stateTime >= 0.5f) {
            currentState = State.IDLE;
            stateTime = 0f;
        }
    }

    private void chooseNextAction(Vector2 playerPos) {
        int index = random.nextInt(3);
        float distanceX = playerPos.x - position.x;
        facingRight = distanceX >= 0;

        if (random.nextFloat() < 0.25f) { // 25% chance idle
            currentState = State.IDLE_WAITING;
            stateTime = 0f;
            actionCD = 30;
            return;
        }

        if (Math.abs(distanceX) < 3f) {
            // Move away from player when player is close
            Vector2 vel = body.getLinearVelocity();
            body.setLinearVelocity((facingRight ? 6f : -6f), vel.y);
            isMovingWhileWalking = true;

            currentState = State.WALKING;
            Animation<TextureRegion> newAnimation = facingRight ? walkRightAnimation : walkLeftAnimation;
            if (currentAnimation != newAnimation) {
                currentAnimation = newAnimation;
                stateTime = 0f;
            }
            actionCD = 120;
        } else if (index == 0 && jumpCD == 0) {
            // Jump
            currentState = State.JUMPING;
            stateTime = 0f;
            Vector2 vel = body.getLinearVelocity();
            body.setLinearVelocity(facingRight ? 7 : -7, 10f);
            jumpCD = 60;
            actionCD = 50;
        } else if (index == 1 && dashCD == 0) {
            // Dash
            currentState = State.DASHING;
            stateTime = 0f;
            body.setLinearVelocity((facingRight ? 15f : -15f), 0f);
            body.setGravityScale(0);
            dashCD = (int)(dashLeftAnimation.getAnimationDuration() / Gdx.graphics.getDeltaTime());
            actionCD = 500;
        } else if (index == 2 && jumpFinalCD == 0) {
            // Jump dash
            currentState = State.JUMP_DASHING;
            hasJumpedForDash = false;
            stateTime = 0f;
            jumpFinalCD = 100;
            actionCD = 500;
        } else {
            // Just play walking animation without moving (matches original behavior)
            currentState = State.WALKING;
            isMovingWhileWalking = false;
            Animation<TextureRegion> newAnimation = facingRight ? walkRightAnimation : walkLeftAnimation;
            if (currentAnimation != newAnimation) {
                currentAnimation = newAnimation;
                stateTime = 0f;
            }
            actionCD = 30;
        }
    }

    public void draw(SpriteBatch batch) {
        if (!alive || currentFrame == null) return;

        if (hitCooldown > 0 && ((int)(hitCooldown * 10) % 2 == 0)) {
            batch.setColor(1f, 0.3f, 0.3f, 1f); // blinking red
        }

        batch.draw(currentFrame, position.x - width / 2f, position.y - height / 2f, width, height);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    public void tryHit(Rectangle hitbox) {
        if (!alive || hitCooldown > 0) return;
        if (hitbox.overlaps(currentHitbox)) {
            health--;
            hitCooldown = HIT_INTERVAL;
            if (health <= 0) alive = false;
        }
    }

    public void dispose() {
        world.destroyBody(body);
    }

    public Vector2 getPosition() {
        return position;
    }

    public boolean isAlive() {
        return alive;
    }

    public void drawHealthBar(ShapeRenderer shapeRenderer) {
        if (!alive) return;

        float barWidth = width;
        float barHeight = 0.1f;
        float x = position.x - barWidth / 2f;
        float y = position.y + height / 2f + 0.1f; // pos of health bar

        float healthRatio = health / maxHealth;

        shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1);
        shapeRenderer.rect(x, y, barWidth, barHeight);

        shapeRenderer.setColor(1, 0, 0, 1);
        shapeRenderer.rect(x, y, barWidth * healthRatio, barHeight);
    }

    public Rectangle getCurrentHitbox() {
        return currentHitbox;
    }
}
