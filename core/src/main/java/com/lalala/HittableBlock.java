// HittableBlock.java
package com.lalala;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class HittableBlock {
    private Rectangle bounds;
    private boolean isHit = false;
    private float hitTimer = 0f;


    public HittableBlock(float x, float y, float width, float height) {
        bounds = new Rectangle(x - width / 2f, y - height / 2f, width, height);
    }

    public void update(Rectangle attackHitbox) {
        if (attackHitbox.overlaps(bounds)) {
            isHit = true;
            hitTimer = 0.5f;  // 0.5秒内显示红色
        }
    }
    public void tick(float delta) {
        if (isHit) {
            hitTimer -= delta;
            if (hitTimer <= 0f) {
                isHit = false;
                hitTimer = 0f;
            }
        }
    }

    public void draw(ShapeRenderer renderer) {
        if (isHit) {
            renderer.setColor(1, 0, 0, 1); // 红色
        } else {
            renderer.setColor(0, 1, 0, 1); // 绿色
        }
        renderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

}
