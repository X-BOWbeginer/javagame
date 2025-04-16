// Player.java
package com.lalala;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;

public class Player {
    private final World world;
    private final Body body;

    private Animation<TextureRegion> idleAnimation;
    private Animation<TextureRegion> walkAnimation;
    private Animation<TextureRegion> jumpUpAnimation;
    private Animation<TextureRegion> jumpLoopAnimation;
    private Animation<TextureRegion> landAnimation;
    private Animation<TextureRegion> doubleJumpAnimation;

    private float stateTime = 0f;
    private TextureRegion currentFrame;

    private boolean grounded = false;
    private boolean canDoubleJump = true;
    private boolean justDoubleJumped = false;
    private int groundContactCount = 0;
    private boolean isDashing = false;
    private float dashTimer = 0f;
    private int facingDirection = 1;

    private boolean playingLand = false;
    private boolean justLanded = false;

    private final float moveSpeed = 5f;
    private final float jumpVelocity = 10f;
    private final float dashSpeed = 20f;
    private final float dashDuration = 0.2f;

    public Player(World world, float x, float y) {
        this.world = world;

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);
        this.body = world.createBody(bodyDef);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(0.5f, 0.5f);
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1f;
        fixtureDef.friction = 0f;
        body.createFixture(fixtureDef);
        shape.dispose();

        PolygonShape footShape = new PolygonShape();
        footShape.setAsBox(0.45f, 0.1f, new Vector2(0, -0.5f), 0);
        FixtureDef footFixture = new FixtureDef();
        footFixture.shape = footShape;
        footFixture.isSensor = true;
        Fixture sensor = body.createFixture(footFixture);
        sensor.setUserData("foot");
        footShape.dispose();

        idleAnimation = loadAnimation("Idle/Idle_", 9, 0.1f, Animation.PlayMode.LOOP);
        walkAnimation = loadAnimation("Walk/Walk_", 8, 0.08f, Animation.PlayMode.LOOP);
        jumpUpAnimation = loadAnimation("Jump/上升/Jump_", 9, 0.08f, Animation.PlayMode.LOOP);
        jumpLoopAnimation = loadAnimation("Jump/空中循环/JumpLoop_", 3, 0.12f, Animation.PlayMode.LOOP);
        landAnimation = loadAnimation("Jump/落地/Land_", 3, 0.05f, Animation.PlayMode.NORMAL);
        doubleJumpAnimation = loadAnimation("DoubleJump/DoubleJump_", 4, 0.08f, Animation.PlayMode.NORMAL);
    }

    private Animation<TextureRegion> loadAnimation(String basePath, int count, float frameDuration, Animation.PlayMode mode) {
        Array<TextureRegion> frames = new Array<>();
        for (int i = 1; i <= count; i++) {
            frames.add(new TextureRegion(new Texture(basePath + i + ".PNG")));
        }
        Animation<TextureRegion> anim = new Animation<>(frameDuration, frames);
        anim.setPlayMode(mode);
        return anim;
    }

    public void update(boolean moveLeft, boolean moveRight, boolean jumpPressed, boolean dashPressed, float delta) {
        Vector2 velocity = body.getLinearVelocity();
        stateTime += delta;

        if (isDashing) {
            dashTimer -= delta;
            if (dashTimer <= 0) {
                isDashing = false;
            }
        }

        if (!isDashing) {
            if (moveLeft) {
                body.setLinearVelocity(-moveSpeed, velocity.y);
                facingDirection = -1;
            } else if (moveRight) {
                body.setLinearVelocity(moveSpeed, velocity.y);
                facingDirection = 1;
            } else {
                body.setLinearVelocity(0, velocity.y);
            }

            if (jumpPressed) {
                if (grounded) {
                    body.setLinearVelocity(body.getLinearVelocity().x, jumpVelocity);
                    grounded = false;
                    canDoubleJump = true;
                    playingLand = false;
                    justDoubleJumped = false;
                } else if (canDoubleJump) {
                    body.setLinearVelocity(body.getLinearVelocity().x, jumpVelocity);
                    canDoubleJump = false;
                    justDoubleJumped = true;
                    stateTime = 0f;
                }
            }
        }

        if (dashPressed && !isDashing) {
            isDashing = true;
            dashTimer = dashDuration;
            body.setLinearVelocity(facingDirection * dashSpeed, 0);
            return;
        }

        // 设置动画帧
        if (playingLand) {
            currentFrame = landAnimation.getKeyFrame(stateTime);
            if (landAnimation.isAnimationFinished(stateTime)) {
                playingLand = false;
            }
        } else if (!grounded) {
            justLanded = true;
            if (justDoubleJumped && !doubleJumpAnimation.isAnimationFinished(stateTime)) {
                currentFrame = doubleJumpAnimation.getKeyFrame(stateTime);
            } else if (velocity.y > 0.5f) {
                currentFrame = jumpUpAnimation.getKeyFrame(stateTime);
            } else if (velocity.y < -0.5f) {
                currentFrame = jumpLoopAnimation.getKeyFrame(stateTime);
            } else {
                currentFrame = jumpLoopAnimation.getKeyFrame(stateTime);
            }
        } else {
            if (justLanded) {
                stateTime = 0f;
                playingLand = true;
                justLanded = false;
                currentFrame = landAnimation.getKeyFrame(stateTime);
            } else if (moveLeft || moveRight) {
                currentFrame = walkAnimation.getKeyFrame(stateTime);
            } else {
                currentFrame = idleAnimation.getKeyFrame(stateTime);
            }
        }
    }

    public void beginContact(Contact contact) {
        handleSensorContact(contact, true);
    }

    public void endContact(Contact contact) {
        handleSensorContact(contact, false);
    }

    private void handleSensorContact(Contact contact, boolean begin) {
        Fixture a = contact.getFixtureA();
        Fixture b = contact.getFixtureB();

        boolean aIsFoot = "foot".equals(a.getUserData());
        boolean bIsFoot = "foot".equals(b.getUserData());

        if (aIsFoot || bIsFoot) {
            if (begin) {
                groundContactCount++;
            } else {
                groundContactCount = Math.max(0, groundContactCount - 1);
            }
            grounded = groundContactCount > 0;
        }
    }

    public void draw(SpriteBatch batch) {
        Vector2 pos = body.getPosition();
        if (currentFrame != null) {
            float pixelsPerUnit = 100f;
            float texW = currentFrame.getRegionWidth();
            float texH = currentFrame.getRegionHeight();
            float drawW = texW / pixelsPerUnit;
            float drawH = texH / pixelsPerUnit;
            if (currentFrame.isFlipX() != (facingDirection == 1)) {
                currentFrame.flip(true, false);
            }
            float drawX = pos.x - drawW / 2f;
            float drawY = pos.y - 0.5f;
            batch.draw(currentFrame, drawX, drawY, drawW, drawH);
        }
    }

    public void dispose() {
        for (TextureRegion region : idleAnimation.getKeyFrames()) region.getTexture().dispose();
        for (TextureRegion region : walkAnimation.getKeyFrames()) region.getTexture().dispose();
        for (TextureRegion region : jumpUpAnimation.getKeyFrames()) region.getTexture().dispose();
        for (TextureRegion region : jumpLoopAnimation.getKeyFrames()) region.getTexture().dispose();
        for (TextureRegion region : landAnimation.getKeyFrames()) region.getTexture().dispose();
        for (TextureRegion region : doubleJumpAnimation.getKeyFrames()) region.getTexture().dispose();
    }

    public Body getBody() {
        return body;
    }
}
