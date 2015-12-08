/*
 * Copyright 2015 Benedikt Vogler.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * If this software is used for a game the official „Wurfel Engine“ logo or its name must be
 *   visible in an intro screen or main menu.
 * * Redistributions of source code must retain the above copyright notice, 
 *   this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 * * Neither the name of Benedikt Vogler nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software without specific
 *   prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.bombinggames.wurfelengine.extension.shooting;

import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.bombinggames.wurfelengine.WE;
import com.bombinggames.wurfelengine.core.Controller;
import com.bombinggames.wurfelengine.core.Gameobjects.AbstractEntity;
import com.bombinggames.wurfelengine.core.Gameobjects.Explosion;
import com.bombinggames.wurfelengine.core.Gameobjects.MovableEntity;
import com.bombinggames.wurfelengine.core.Gameobjects.Particle;
import com.bombinggames.wurfelengine.core.Gameobjects.ParticleType;
import com.bombinggames.wurfelengine.core.Map.Coordinate;
import java.util.ArrayList;

/**
 * A bullet is a moving object which can destroy and damage entities or the
 * world.
 *
 * @author Benedikt Vogler
 */
public class Bullet extends AbstractEntity {

	private static final long serialVersionUID = 1L;
	private static final String explosionsound = "explosion2";
	/**
	 * direction and speed of the bullet
	 */
	private Vector3 movement = new Vector3();//movement
	private byte damage;
	private int distance = 0;//distance traveled
	private Weapon gun;//no self shooting
	private int maxDistance = 1000;//default maxDistance
	private int explosive = 0;
	private int impactSprite;
	private Coordinate ignoreCoord;
	private int ignoreId;

	/**
	 * You can set a different sprite via {@link #setSpriteId(byte)}. It uses
	 * the engine default sprite.
	 *
	 * @see #setSpriteId(byte)
	 */
	public Bullet() {
		super((byte) 22);
		setName("Bullet");
		setMass(0.002f);
		setSaveToDisk(false);
	}

	@Override
	public void update(float dt) {
		super.update(dt);
		if (!hasPosition()) return;
		
		//apply gravity
		movement.add(new Vector3(0, 0, -WE.getCvars().getValueF("gravity") * dt*0.001f));
		Vector3 dMov = movement.cpy().scl(dt);
		//dMov.z /= 1.414213562f;//mixed screen and game space together?
		getPosition().add(dMov);
		setRotation(getRotation() + dt);

		//only spawn specific distance then destroy self
		distance += dMov.len();
		if (distance > maxDistance) {
			dispose();
			return;
		}

		//block hit -> spawn effect
		if (
			hasPosition()
			&& getPosition().getBlock() != null
			&& getPosition().getBlock().isObstacle()
			&& (ignoreCoord == null
				||
				!ignoreCoord.equals(getPosition().toCoord())
				)
			&& ignoreId != getPosition().getBlock().getId()
		) {
			if (impactSprite != 0) {
				Particle impactPart = new Particle();
				impactPart.setTTL(400);
				impactPart.setColor(new Color(0.4f, 0.3f, 0.2f, 1));
				impactPart.setType(ParticleType.SMOKE);
				impactPart.spawn(getPosition().cpy());
			}
			dispose();
			return;
		}
		
        //check character hit
		ArrayList<MovableEntity> entitylist = Controller.getMap().getEntitysOnCoord(getPosition().toCoord(), MovableEntity.class);
		entitylist.remove(gun);//remove self from list to prevent self shooting
		//remove
		entitylist.removeIf(
			(MovableEntity item) -> !item.isObstacle() || item.getPosition().toCoord().equals(ignoreCoord)
		);
		if (!entitylist.isEmpty()) {
			entitylist.get(0).takeDamage(damage);//damage only the first unit on the list
			Particle blood = new Particle();
			blood.setColor(new Color(1,0,0,1));
			blood.setTTL(400);
			blood.setType(ParticleType.REGULAR);//blood
			blood.spawn(getPosition().cpy());
			dispose();
		}
	}

	/**
	 *
	 * @param dir
	 */
	public void setDirection(Vector3 dir) {
		float speed = movement.len();
		this.movement = dir.scl(speed);
	}

	/**
	 *
	 * @param speed
	 */
	public void setSpeed(float speed) {
		this.movement.nor().scl(speed);
	}

	/**
	 * Set the firing gun so it does not hit itself
	 * @param weapon
	 */
	public void setGun(Weapon weapon) {
		this.gun = weapon;
	}

	/**
	 *
	 * @param maxDistance
	 */
	public void setMaxDistance(int maxDistance) {
		this.maxDistance = maxDistance;
	}

	/**
	 *
	 * @param damage
	 */
	public void setDamage(byte damage) {
		this.damage = damage;
	}

	/**
	 *
	 * @param ex
	 */
	public void setExplosive(int ex) {
		explosive = ex;
	}

	/**
	 * Spawns explosion.
	 */
	private void explode(int radius) {
		new Explosion(radius, (byte) 80, WE.getGameplay().getView().getCameras().get(0)).spawn(getPosition());
	}

	@Override
	public void dispose() {
		if (explosive > 0) {
			explode(3);
		}
		super.dispose();
	}

	/**
	 * Set the sprite which get spawned when the bullet hits.
	 *
	 * @param id if you don't want an impact sprite set id to0.
	 */
	public void setImpactSprite(int id) {
		impactSprite = id;
	}

	/**
	 *
	 * @return the distance traveled.
	 */
	public int getDistance() {
		return distance;
	}

	/**
	 * 
	 * @param coord 
	 */
	public void ignoreCoord(Coordinate coord) {
		ignoreCoord = coord;
	}

	void ignoreBlock(int ignoreId) {
		this.ignoreId = ignoreId;
	}

	void setMovement(Vector3 mov) {
		this.movement = mov;
	}
	
	@Override
	public boolean handleMessage(Telegram msg) {
		return true;
	}

}
