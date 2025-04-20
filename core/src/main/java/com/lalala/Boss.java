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

    private World world;
    private Body body;
    private Vector2 position;
    private Rectangle bounds;
    private int health = 5;
    private boolean alive = true;
    private float hitCooldown = 0f;
    private final float HIT_INTERVAL = 0.5f;

    private int actionCD = 0;
    private int jumpCD = 0;
    private int dashCD = 0;
    private int jumpFinalCD = 0;

    private float stateTime = 0f;
    private boolean isDashing = false;
    private boolean isJumping = false;
    private boolean isIdleWaiting = false;
    private boolean isJumpDashing = false;
    private boolean hasJumpedForDash = false;
    private boolean facingRight = true;

    private float width;
    private float height;

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
    private float directionForJumpDash = 1f;
    private Vector2 jumpDashVelocity = new Vector2();


    public Boss(World world, float x, float y) {
        this.world = world;
        this.position = new Vector2(x, y);

        idleLeftAnimation = loadAnimation("Boss/Idle", 1, 0.1f, Animation.PlayMode.LOOP);
        idleRightAnimation = loadAnimation("Boss/IdleR", 1, 0.1f, Animation.PlayMode.LOOP);

        TextureRegion firstFrame = idleLeftAnimation.getKeyFrame(0f);
        float pixelsPerUnit = 100f;
        this.width = firstFrame.getRegionWidth() / pixelsPerUnit;
        this.height = firstFrame.getRegionHeight() / pixelsPerUnit;

        this.bounds = new Rectangle(x - width / 2f, y - height / 2f, width, height);

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

        stateTime += delta;

        if (hitCooldown > 0) hitCooldown -= delta;
        if (actionCD > 0) actionCD--;
        if (jumpCD > 0) jumpCD--;
        if (dashCD > 0) dashCD--;
        if (jumpFinalCD > 0) jumpFinalCD--;

        position.set(body.getPosition());

        if (isDashing) {
            currentAnimation = facingRight ? dashRightAnimation : dashLeftAnimation;
            currentFrame = currentAnimation.getKeyFrame(stateTime);
            if (currentAnimation.isAnimationFinished(stateTime)) {
                isDashing = false;
                body.setGravityScale(1);
                isIdleWaiting = true;
                stateTime = 0f;
                body.setLinearVelocity(0, 0);
            }
            return;
        }

        if (isJumpDashing) {
            float vy = body.getLinearVelocity().y;
            if (!hasJumpedForDash) {
                body.setLinearVelocity(body.getLinearVelocity().x, 12f);
                hasJumpedForDash = true;
                stateTime = 0f;
                float dx = playerPos.x - position.x;
                directionForJumpDash = dx >= 0 ? 1f : -1f;
                facingRight = dx >= 0;
                Vector2 dashDir = new Vector2(playerPos.x - position.x, 0).nor();
                jumpDashVelocity.set(dashDir.scl(15f).x, -5f);  // 冲刺方向和速度只记录一次
            } else if (vy < 0 && stateTime > 0.15f) {
                body.setLinearVelocity(jumpDashVelocity);
                currentAnimation = facingRight ? jumpDashRightAnimation : jumpDashLeftAnimation;
                currentFrame = currentAnimation.getKeyFrame(stateTime);
                if (isOnGround() || currentAnimation.isAnimationFinished(stateTime)) {
                    isJumpDashing = false;
                    hasJumpedForDash = false;
                    isIdleWaiting = true;
                    stateTime = 0f;
                    body.setLinearVelocity(0, 0);
                }
                return;
            }
            currentAnimation = facingRight ? jumpDashRightAnimation : jumpDashLeftAnimation;
            currentFrame = currentAnimation.getKeyFrame(stateTime);
            return;
        }


        if (isJumping) {
            float vy = body.getLinearVelocity().y;
            if (vy == 0) {
                isJumping = false;
                isIdleWaiting = true;
                stateTime = 0f;
            } else if (vy < 0) {
                currentAnimation = facingRight ? landRightAnimation : landLeftAnimation;
                currentFrame = currentAnimation.getKeyFrame(stateTime);
                if (currentAnimation.isAnimationFinished(stateTime)) {
                    currentFrame = currentAnimation.getKeyFrames()[currentAnimation.getKeyFrames().length - 1];
                }
                return;
            } else {
                currentAnimation = facingRight ? jumpRightAnimation : jumpLeftAnimation;
                currentFrame = currentAnimation.getKeyFrame(stateTime);
                return;
            }
        }

        if (isIdleWaiting) {
            currentAnimation = facingRight ? idleRightAnimation : idleLeftAnimation;
            currentFrame = currentAnimation.getKeyFrame(stateTime, true);
            if (stateTime >= 0.5f) {
                isIdleWaiting = false;
                stateTime = 0f;
            }
            return;
        }

        if (actionCD == 0) {
            int index = random.nextInt(3);
            float distanceX = playerPos.x - position.x;
            facingRight = distanceX >= 0;

            if (random.nextFloat() < 0.25f) { // 25% 概率 idle
                isIdleWaiting = true;
                stateTime = 0f;
                actionCD = 30; // 空闲等待一小段时间
                return;
            }

            if (Math.abs(distanceX) < 3f) {
                Vector2 vel = body.getLinearVelocity();
                body.setLinearVelocity((facingRight ? 6f : -6f), vel.y);
                Animation<TextureRegion> newAnimation = facingRight ? walkRightAnimation : walkLeftAnimation;
                if (currentAnimation != newAnimation) {
                    currentAnimation = newAnimation;
                    stateTime = 0f;
                }
                actionCD = 30;
            } else if (index == 0 && jumpCD == 0) {
                isJumping = true;
                stateTime = 0f;
                Vector2 vel = body.getLinearVelocity();
                body.setLinearVelocity(facingRight?7:-7, 10f);
                jumpCD = 60;
                actionCD = 50;
            } else if (index == 1 && dashCD == 0) {
                isDashing = true;
                stateTime = 0f;
                body.setLinearVelocity((facingRight ? 15f : -15f), 0f);
                body.setGravityScale(0);
                dashCD = (int)(dashLeftAnimation.getAnimationDuration() / delta);
                actionCD = 500;
            } else if (index == 2 && jumpFinalCD == 0) {
                isJumpDashing = true;
                hasJumpedForDash = false;
                stateTime = 0f;
                jumpFinalCD = 100;
                actionCD = 500;
            } else {
                isIdleWaiting = true;
                stateTime = 0f;
            }
        } else {
            if (currentAnimation == walkLeftAnimation || currentAnimation == walkRightAnimation) {
                currentFrame = currentAnimation.getKeyFrame(stateTime, true);
            } else {
                currentAnimation = facingRight ? idleRightAnimation : idleLeftAnimation;
                currentFrame = currentAnimation.getKeyFrame(stateTime, true);
            }
        }

        position.set(body.getPosition());
        bounds.set(position.x - width / 2f, position.y - height / 2f, width, height);
    }

    public void draw(SpriteBatch batch) {
        if (!alive || currentFrame == null) return;
        batch.draw(currentFrame, position.x - width / 2f, position.y - height / 2f, width, height);
    }

    public void tryHit(Rectangle hitbox) {
        if (!alive || hitCooldown > 0) return;
        if (hitbox.overlaps(bounds)) {
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
}
