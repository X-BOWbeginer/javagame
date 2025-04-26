// Player.java
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

public class Player {
    private final World world;
    private final Body body;

    private Animation<TextureRegion> idleAnimation, walkAnimation, jumpUpAnimation, jumpLoopAnimation, landAnimation;
    private Animation<TextureRegion> doubleJumpAnimation, dashAnimation, dashEffectAnimation;
    private Animation<TextureRegion> attack1Animation, attack2Animation, attackDownAnimation;

    private float stateTime = 0f;
    private TextureRegion currentFrame;

    private boolean grounded = false;
    private boolean canDoubleJump = true;
    private boolean justDoubleJumped = false;
    private int groundContactCount = 0;
    private boolean isDashing = false;
    private float dashTimer = 0f;
    private int facingDirection = 1;
    private int health = 20;
    private int maxHealth = 20;
    private float hitCooldown = 0f;
    private final float HIT_INTERVAL = 0.5f;


    private boolean playingLand = false;
    private boolean justLanded = false;

    private boolean playingDashEffect = false;
    private float dashEffectTime = 0f;

    private boolean isAttacking = false;
    private int attackCombo = 0;
    private float attackTimer = 0f;
    //private final float attackCooldown = 0.5f;

    private TextureRegion attackEffect1;
    private TextureRegion attackEffect2;
    private TextureRegion attackEffectDown;
    private boolean showAttackEffect = false;
    private float attackEffectTimer = 0f;
    private final float attackEffectDuration = 0.12f;

    private Rectangle currentHitbox = new Rectangle();

    private final float moveSpeed = 5f;
    private final float jumpVelocity = 10f;
    private final float dashSpeed = 15f;
    private final float dashDuration = 0.2f;

    public Player(World world, float x, float y) {
        this.world = world;

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.fixedRotation = true;
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

        idleAnimation = loadAnimation("Idle/Idle_", 1, 0.1f, Animation.PlayMode.LOOP);
        walkAnimation = loadAnimation("Walk/Walk_", 8, 0.08f, Animation.PlayMode.LOOP);
        jumpUpAnimation = loadAnimation("Jump/上升/Jump_", 9, 0.08f, Animation.PlayMode.LOOP);
        jumpLoopAnimation = loadAnimation("Jump/空中循环/JumpLoop_", 3, 0.12f, Animation.PlayMode.LOOP);
        landAnimation = loadAnimation("Jump/落地/Land_", 3, 0.05f, Animation.PlayMode.NORMAL);
        doubleJumpAnimation = loadAnimation("DoubleJump/DoubleJump_", 4, 0.06f, Animation.PlayMode.NORMAL);
        dashAnimation = loadAnimation("Dash/Dash_", 4, 0.05f, Animation.PlayMode.NORMAL);
        dashEffectAnimation = loadAnimation("DashEffect/dash_effect_02000", 5, 0.03f, Animation.PlayMode.NORMAL);
        attack1Animation = loadAnimation("Attack/1/Attack_", 5, 0.06f, Animation.PlayMode.NORMAL);
        attack2Animation = loadAnimation("Attack/2/AttackTwice_", 5, 0.06f, Animation.PlayMode.NORMAL);
        attackDownAnimation = loadAnimation("Down/AttackBottom_", 5, 0.06f, Animation.PlayMode.NORMAL);

        Texture fullTex1 = new Texture("Attack/1/lr1.png");
        Texture fullTex2 = new Texture("Attack/2/lr2.png");
        Texture fullTex3 = new Texture("Down/down.png");
        attackEffect1 = new TextureRegion(fullTex1, 0, 0, fullTex1.getWidth(), fullTex1.getHeight() / 2);
        attackEffect2 = new TextureRegion(fullTex2, 0, 0, fullTex2.getWidth(), fullTex2.getHeight() / 2);
        attackEffectDown = new TextureRegion(fullTex3, 0, 0, fullTex3.getWidth(), fullTex3.getHeight() / 2);
    }

    private Animation<TextureRegion> loadAnimation(String basePrefix, int count, float frameDuration, Animation.PlayMode mode) {
        Array<TextureRegion> frames = new Array<>();
        for (int i = 1; i <= count; i++) {
            Texture tex = new Texture(basePrefix + i + ".PNG");
            frames.add(new TextureRegion(tex));
        }
        Animation<TextureRegion> anim = new Animation<>(frameDuration, frames);
        anim.setPlayMode(mode);
        return anim;
    }
    public boolean isInAir() {
        return !grounded;
    }

    public void triggerDownAttack() {
        if (!isAttacking && isInAir()) {
            isAttacking = true;
            attackCombo = -1;
            attackTimer = 0f;
            stateTime = 0f;
        }
    }

    public void update(boolean moveLeft, boolean moveRight, boolean jumpPressed, boolean dashPressed, boolean attackPressed, boolean downAttackPressed, float delta) {
        Vector2 velocity = body.getLinearVelocity();
        stateTime += delta;
        if (hitCooldown > 0) hitCooldown -= delta;


        if (downAttackPressed) {
            triggerDownAttack();
        }

        if (playingDashEffect) {
            dashEffectTime += delta;
            if (dashEffectAnimation.isAnimationFinished(dashEffectTime)) {
                playingDashEffect = false;
            }
        }

        if (isDashing) {
            dashTimer -= delta;
            if (dashTimer <= 0) {
                isDashing = false;
                body.setGravityScale(1);
            }
        }

        currentHitbox.set(0, 0, 0, 0); // clear hitbox in default

        if (attackPressed) {
            if (!isAttacking) {
                isAttacking = true;
                attackCombo = 1;
                attackTimer = 0f;
                stateTime = 0f;
            } else if (attackCombo == 1 && attackTimer < 0.3f) {
                attackCombo = 2;
                attackTimer = 0f;
                stateTime = 0f;
            }
        }

        if (isAttacking) {
            attackTimer += delta;
            Vector2 pos = body.getPosition();
            if (attackCombo == 1) {
                currentFrame = attack1Animation.getKeyFrame(stateTime);
                if (attack1Animation.getKeyFrameIndex(stateTime) == 1) {
                    showAttackEffect = true;
                    attackEffectTimer = 0f;
                    updateCurrentHitbox(attackEffect1, 1, pos);
                }
                if (attack1Animation.isAnimationFinished(stateTime)) {
                    if (attackCombo == 2) {
                        stateTime = 0f;
                    } else {
                        isAttacking = false;
                    }
                }
                return;
            } else if (attackCombo == 2) {
                currentFrame = attack2Animation.getKeyFrame(stateTime);
                if (attack2Animation.getKeyFrameIndex(stateTime) == 1) {
                    showAttackEffect = true;
                    attackEffectTimer = 0f;
                    updateCurrentHitbox(attackEffect2, 2, pos);
                }
                if (attack2Animation.isAnimationFinished(stateTime)) {
                    isAttacking = false;
                    attackCombo = 0;
                }
                return;
            } else if (attackCombo == -1) {
                currentFrame = attackDownAnimation.getKeyFrame(stateTime);
                if (attackDownAnimation.getKeyFrameIndex(stateTime) == 1) {
                    showAttackEffect = true;
                    attackEffectTimer = 0f;
                    updateCurrentHitbox(attackEffectDown, -1, pos);
                }
                if (attackDownAnimation.isAnimationFinished(stateTime)) {
                    isAttacking = false;
                    attackCombo = 0;
                }
                return;
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
            body.setGravityScale(0);
            stateTime = 0f;
            playingDashEffect = true;
            dashEffectTime = 0f;
            return;
        }

        if (isDashing) {
            currentFrame = dashAnimation.getKeyFrame(stateTime);
        } else if (playingLand) {
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
        if (playingDashEffect) {
            TextureRegion effect = dashEffectAnimation.getKeyFrame(dashEffectTime);
            if (effect.isFlipX() != (facingDirection == 1)) {
                effect.flip(true, false);
            }
            Vector2 pos = body.getPosition();
            float w = effect.getRegionWidth() / 150f;
            float h = effect.getRegionHeight() / 150f;
            float x = pos.x;
            float y = pos.y;
            batch.draw(effect, x - w / 2f, y - h / 2f, w / 2f, h / 2f, w, h, 1f, 1f, 0f);
        }

        if (showAttackEffect) {
            attackEffectTimer += Gdx.graphics.getDeltaTime();
            if (attackEffectTimer > attackEffectDuration) {
                showAttackEffect = false;
            } else {
                TextureRegion effect;
                if (attackCombo == -1) {
                    effect = attackEffectDown;
                } else if (attackCombo == 1) {
                    effect = attackEffect1;
                } else {
                    effect = attackEffect2;
                }

                if (effect != null) {
                    Vector2 pos = body.getPosition();
                    float w = effect.getRegionWidth() / 80f;
                    float h = effect.getRegionHeight() / 80f;
                    float x = pos.x;
                    float y = pos.y;

                    if (attackCombo == -1) {
                        y -= h * 0.5f;
                    } else {
                        float offsetX = (facingDirection == 1) ? w * 0.3f : -w * 0.3f;
                        x += offsetX;
                        if (effect.isFlipX() != (facingDirection == 1)) {
                            effect.flip(true, false);
                        }
                    }

                    batch.draw(effect, x - w / 2f, y - h / 2f, w / 2f, h / 2f, w, h, 1f, 1f, 0f);

                    // set hitbox
                    currentHitbox.set(x - w / 2f, y - h / 2f, w, h);
                }
            }
        }

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
            if (hitCooldown > 0) {
                // blinking
                if ((int)(hitCooldown * 10) % 2 == 0) {
                    batch.setColor(1f, 0.3f, 0.3f, 1f);
                }
            }
            batch.draw(currentFrame, drawX, drawY, drawW, drawH);
            batch.setColor(1f, 1f, 1f, 1f);

        }
    }

    public void drawHitbox(ShapeRenderer renderer) {
        if (!showAttackEffect) return;

        renderer.setColor(1, 0, 0, 1);
        renderer.rect(currentHitbox.x, currentHitbox.y, currentHitbox.width, currentHitbox.height);
    }
    private void updateCurrentHitbox(TextureRegion effect, int combo, Vector2 pos) {
        float w = effect.getRegionWidth() / 80f;
        float h = effect.getRegionHeight() / 80f;
        float x = pos.x;
        float y = pos.y;

        if (combo == -1) {
            y -= h * 0.5f;
        } else {
            float offsetX = (facingDirection == 1) ? w * 0.3f : -w * 0.3f;
            x += offsetX;
        }

        currentHitbox.set(x - w / 2f, y - h / 2f, w, h);
    }



    public void dispose() {
        for (TextureRegion region : idleAnimation.getKeyFrames()) region.getTexture().dispose();
        for (TextureRegion region : walkAnimation.getKeyFrames()) region.getTexture().dispose();
        for (TextureRegion region : jumpUpAnimation.getKeyFrames()) region.getTexture().dispose();
        for (TextureRegion region : jumpLoopAnimation.getKeyFrames()) region.getTexture().dispose();
        for (TextureRegion region : landAnimation.getKeyFrames()) region.getTexture().dispose();
        for (TextureRegion region : doubleJumpAnimation.getKeyFrames()) region.getTexture().dispose();
        for (TextureRegion region : dashAnimation.getKeyFrames()) region.getTexture().dispose();
        for (TextureRegion region : dashEffectAnimation.getKeyFrames()) region.getTexture().dispose();
        for (TextureRegion region : attack1Animation.getKeyFrames()) region.getTexture().dispose();
        for (TextureRegion region : attack2Animation.getKeyFrames()) region.getTexture().dispose();
    }

    public Body getBody() {
        return body;
    }
    public Rectangle getCurrentHitbox() {
        return currentHitbox;
    }
    public Vector2 getPosition() {
        return body.getPosition();
    }
    public void drawHealthBar(ShapeRenderer renderer) {
        Vector2 pos = body.getPosition();
        float barWidth = 1f;
        float barHeight = 0.1f;
        float x = pos.x - barWidth / 2f;
        float y = pos.y + 0.7f;

        float ratio = (float) health / maxHealth;

        renderer.setColor(0.3f, 0.3f, 0.3f, 1f);
        renderer.rect(x, y, barWidth, barHeight);

        renderer.setColor(0.1f, 1f, 0.2f, 1f);
        renderer.rect(x, y, barWidth * ratio, barHeight);
    }
    public void tryHit(Rectangle bossHitbox) {
        if (hitCooldown > 0 || isDashing) return; // during dash, player is invincible.


        Vector2 pos = body.getPosition();
        Rectangle playerBodyBox = new Rectangle(pos.x - 0.5f, pos.y - 0.5f, 1f, 1f);

        if (bossHitbox.overlaps(playerBodyBox)) {
            health--;
            hitCooldown = HIT_INTERVAL;
            System.out.println("Player hit! HP: " + health);
            if (health <= 0) {
                //dead!
            }
        }
    }

    public boolean isDead() {
        return health <= 0;
    }





}
