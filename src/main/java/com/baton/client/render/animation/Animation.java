package com.baton.client.render.animation;

import net.minecraft.Util;

public final class Animation {
	private static final float SETTLE_DISTANCE = 1.0E-4F;
	private static final float SETTLE_VELOCITY = 1.0E-3F;

	private final float omega;
	private float value;
	private float target;
	private float velocity;
	private long lastNanos = Util.getNanos();

	public Animation(float value, float smoothTime) {
		if (smoothTime <= 0.0F) {
			throw new IllegalArgumentException("smoothTime must be positive");
		}
		this.omega = 2.0F / smoothTime;
		this.value = value;
		this.target = value;
	}

	public Animation target(float target) {
		this.target = target;
		return this;
	}

	public Animation snap(float value) {
		this.value = value;
		this.target = value;
		this.velocity = 0.0F;
		return this;
	}

	public float update() {
		long now = Util.getNanos();
		float delta = (now - lastNanos) * 1.0E-9F;
		lastNanos = now;
		if (value == target && velocity == 0.0F) {
			return value;
		}

		float x = omega * delta;
		float decay = 1.0F / (1.0F + x + 0.48F * x * x + 0.235F * x * x * x);
		float offset = value - target;
		float impulse = (velocity + omega * offset) * delta;
		float next = target + (offset + impulse) * decay;
		velocity = (velocity - omega * impulse) * decay;

		boolean overshot = (next - target) * offset < 0.0F;
		boolean settled = Math.abs(next - target) < SETTLE_DISTANCE && Math.abs(velocity) < SETTLE_VELOCITY;
		if (overshot || settled) {
			next = target;
			velocity = 0.0F;
		}
		value = next;
		return value;
	}

	public float value() {
		return value;
	}

	public float target() {
		return target;
	}
}
