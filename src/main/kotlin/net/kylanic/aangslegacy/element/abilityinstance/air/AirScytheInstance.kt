package net.kylanic.aangslegacy.element.abilityinstance.air

import net.kylanic.aangslegacy.AangsLegacy
import net.kylanic.aangslegacy.element.abilityinstance.AbilityInstance
import net.kylanic.aangslegacy.element.abilityinstance.ShiftAbility
import net.kylanic.aangslegacy.event.Event
import net.kylanic.aangslegacy.event.EventType
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Damageable
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.math.cos
import kotlin.math.sin

class AirScytheInstance(
    override val owner: Player
) : AbilityInstance("al:air_scythe", owner), ShiftAbility {

    private var activeTicks = 0
    private val duration = 16

    private val maxRange = 4.0
    private val arcAngle = Math.toRadians(50.0)
    private val damage = 1.5
    private val knockbackStrength = 0.5

    private val hitEntities = mutableSetOf<Int>()

    override fun onEvent(event: Event) {
        if (event.type != EventType.Shift) return
        if (isActive) return

        AangsLegacy.logger.info("[AirScythe] Shift received from ${owner.name}")
        start()
    }

    override fun onStart() {
        activeTicks = 0
        hitEntities.clear()

        owner.world.playSound(
            owner.location,
            Sound.ENTITY_PLAYER_ATTACK_SWEEP,
            1.0f,
            1.5f
        )
    }

    override fun onTick() {
        activeTicks++

        val progress = activeTicks.toDouble() / duration
        val currentRange = maxRange * progress

        spawnScytheEffect(currentRange)
        checkHits(currentRange)

        if (activeTicks >= duration) {
            end()
        }
    }

    private fun spawnScytheEffect(currentRange: Double) {
        val world = owner.world
        val origin = owner.eyeLocation.add(0.0, -1.0, 0.0)

        val direction = origin.direction
        val forward = Vector(direction.x, 0.0, direction.z)
        if (forward.lengthSquared() < 0.001) return
        forward.normalize()

        val right = Vector(-forward.z, 0.0, forward.x)

        val points = (8 * currentRange).toInt().coerceAtLeast(3)
        val halfArc = arcAngle / 2.0

        for (i in 0..points) {
            val angle = -halfArc + (arcAngle * i / points)

            val cosVal = cos(angle)
            val sinVal = sin(angle)

            val offset = forward.clone().multiply(cosVal * currentRange)
                .add(right.clone().multiply(sinVal * currentRange))

            val location = origin.clone().add(offset)

            world.spawnParticle(
                Particle.CLOUD,
                location,
                1,
                0.05, 0.05, 0.05,
                0.01
            )

            if (i % 3 == 0) {
                world.spawnParticle(
                    Particle.SWEEP_ATTACK,
                    location,
                    1,
                    0.0, 0.0, 0.0,
                    0.0
                )
            }
        }
    }

    private fun checkHits(currentRange: Double) {
        val origin = owner.eyeLocation
        val direction = Vector(origin.direction.x, 0.0, origin.direction.z)
        if (direction.lengthSquared() < 0.001) return
        direction.normalize()

        val nearby = owner.world.getNearbyEntities(
            owner.location,
            maxRange + 1.0,
            2.5,
            maxRange + 1.0
        )

        val halfCos = cos(arcAngle / 2.0)

        for (entity in nearby) {
            if (entity == owner) continue
            if (entity !is Damageable) continue
            if (entity.entityId in hitEntities) continue

            val relative = entity.location.clone().subtract(owner.location).toVector()
            val horizontal = Vector(relative.x, 0.0, relative.z)
            val distance = horizontal.length()

            if (distance !in (currentRange - 1.0)..(currentRange + 0.5)) continue

            horizontal.normalize()
            val dot = direction.dot(horizontal)

            if (dot < halfCos) continue

            hit(entity)
        }
    }

    private fun hit(entity: Entity) {
        if (entity !is Damageable) return

        hitEntities += entity.entityId
        entity.damage(damage, owner)

        val knockbackDir = entity.location.clone().subtract(owner.location).toVector()
        if (knockbackDir.lengthSquared() > 0.001) {
            knockbackDir.normalize()
            entity.velocity = entity.velocity.clone().add(
                knockbackDir.multiply(knockbackStrength).setY(0.2)
            )
        }

        owner.world.spawnParticle(
            Particle.CLOUD,
            entity.location.clone().add(0.0, 1.0, 0.0),
            10,
            0.2, 0.3, 0.2,
            0.05
        )

        owner.world.playSound(
            entity.location,
            Sound.ENTITY_PLAYER_ATTACK_SWEEP,
            0.6f,
            2.0f
        )
    }

    override fun onEnd() {
        AangsLegacy.logger.info("[AirScythe] ended for ${owner.name}")
        hitEntities.clear()
    }
}