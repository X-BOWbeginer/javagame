// Player.java
package com.lalala;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

public class Player {
    private final World world;
    private final Body body;
    private final Texture texture;

    private boolean grounded = false;
    private boolean canDoubleJump = true;
    private int groundContactCount = 0;

    private final float moveSpeed = 5f;
    private final float jumpVelocity = 10f;

    public Player(World world, float x, float y) {
        this.world = world;

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);
        this.body = world.createBody(bodyDef);

        // 主体
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(0.5f, 0.5f);
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1f;
        fixtureDef.friction = 0f; // ✅ 禁用自身摩擦
        body.createFixture(fixtureDef);
        shape.dispose();

        // ✅ 脚底感应器
        PolygonShape footShape = new PolygonShape();
        footShape.setAsBox(0.45f, 0.1f, new Vector2(0, -0.5f), 0);
        FixtureDef footFixture = new FixtureDef();
        footFixture.shape = footShape;
        footFixture.isSensor = true;
        Fixture sensor = body.createFixture(footFixture);
        sensor.setUserData("foot");
        footShape.dispose();

        texture = new Texture("player.png");
    }

    public void update(boolean moveLeft, boolean moveRight, boolean jumpPressed) {
        Vector2 velocity = body.getLinearVelocity();

        if (moveLeft) {
            body.setLinearVelocity(-moveSpeed, velocity.y);
        } else if (moveRight) {
            body.setLinearVelocity(moveSpeed, velocity.y);
        } else {
            body.setLinearVelocity(0, velocity.y);
        }

        if (jumpPressed) {
            if (grounded) {
                body.setLinearVelocity(velocity.x, jumpVelocity);
                grounded = false;
                canDoubleJump = true;
            } else if (canDoubleJump) {
                body.setLinearVelocity(velocity.x, jumpVelocity);
                canDoubleJump = false;
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
        batch.draw(texture, pos.x - 0.5f, pos.y - 0.5f, 1f, 1f);
    }

    public void dispose() {
        texture.dispose();
    }

    public Body getBody() {
        return body;
    }
}
