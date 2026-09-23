package net.kylanic.aangslegacy.element.ability.instance.water

import net.kylanic.aangslegacy.element.ability.instance.ArmSwingAbility
import net.kylanic.aangslegacy.element.ability.instance.AbilityInstance
import net.kylanic.aangslegacy.event.Event
import net.kylanic.aangslegacy.event.EventType
import net.kylanic.aangslegacy.util.BiomeUtils
import org.bukkit.FluidCollisionMode
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector

class WaterSplashInstance(
    override val owner: Player,
) : AbilityInstance("al:water_splash", owner), ArmSwingAbility {
    private val range: Int = 3
    private val speed: Double = 1.1
    private val gravity: Double = 0.045
    private val maxTravel: Double = 20.0
    private val splashRadius: Double = 3.25
    private val damage: Double = 4.0
    private val knockbackStrength: Double = 1.15
    private val soakedSlowTicks: Int = 40
    private val puddleLifeTicks: Int = 25
    private var lifeTimeTicks: Int = 600

    private enum class Phase { IDLE, CHARGING, LAUNCHED, DONE }
    private var phase: Phase = Phase.IDLE

    private var hasWater: Boolean = false
    private var frozen: Boolean = false
    private lateinit var location: Vector
    private lateinit var previousLocation: Vector
    private lateinit var velocity: Vector
    private var travelled: Double = 0.0
    private var ticksElapsed: Int = 0
    private val trailLength: Int = 4
    private val trailQueue: ArrayDeque<Vector> = ArrayDeque()

    private data class TempBlock(val pos: Vector, val revertAtTick: Int)
    private val tempBlocks: MutableList<TempBlock> = mutableListOf()

    override fun onEvent(event: Event) {
        if (event.type != EventType.ArmSwing) return

        when (phase) {
            Phase.IDLE -> {
                if (!hasWater && isLookingAtWater()) {
                    hasWater = true
                    phase = Phase.CHARGING
                    start()
                }
            }
            Phase.CHARGING -> launch()
            else -> Unit
        }
    }

    override fun onStart() {
        frozen = BiomeUtils.isCold(owner.location)

        location = chargeSpot()
        previousLocation = location.clone()

        val sound = if (frozen) Sound.BLOCK_GLASS_PLACE else Sound.ITEM_BUCKET_FILL
        owner.world.playSound(location.toLocation(owner.world), sound, 0.6f, 1.4f)
    }

    private val activeMaterial: Material
        get() = if (frozen) Material.ICE else Material.WATER

    override fun onTick() {
        ticksElapsed++
        lifeTimeTicks -= 1

        revertDueTempBlocks()

        if (lifeTimeTicks <= 0 && phase != Phase.DONE) {
            fizzle()
            return
        }

        when (phase) {
            Phase.CHARGING -> tickCharging()
            Phase.LAUNCHED -> tickLaunched()
            else -> Unit
        }
    }

    override fun onEnd() {
        val world = owner.world
        for (block in tempBlocks) {
            val b = world.getBlockAt(block.pos.toLocation(world))
            if (b.type == Material.WATER || b.type == Material.ICE) b.type = Material.AIR
        }
        tempBlocks.clear()
        clearTrail()
    }

    private fun tickCharging() {
        val world = owner.world

        world.getBlockAt(previousLocation.toLocation(world)).let {
            if (it.type == activeMaterial) it.type = Material.AIR
        }

        location = chargeSpot()
        previousLocation = location.clone()

        val block = world.getBlockAt(location.toLocation(world))
        if (block.type == Material.AIR) {
            block.setType(activeMaterial, false)
        }

        val loc = location.toLocation(world)
        if (frozen) {
            world.spawnParticle(Particle.SNOWFLAKE, loc, 5, 0.15, 0.15, 0.15, 0.01)
            world.spawnParticle(Particle.ITEM_SNOWBALL, loc, 2, 0.1, 0.1, 0.1, 0.0)
        } else {
            world.spawnParticle(Particle.DRIPPING_WATER, loc, 4, 0.15, 0.15, 0.15, 0.0)
            world.spawnParticle(Particle.BUBBLE_COLUMN_UP, loc, 2, 0.1, 0.1, 0.1, 0.01)
        }
    }

    private fun chargeSpot(): Vector {
        return owner.eyeLocation.toVector()
            .add(owner.eyeLocation.direction.clone().normalize().multiply(range.toDouble()))
    }

    private fun launch() {
        phase = Phase.LAUNCHED

        val world = owner.world
        world.getBlockAt(location.toLocation(world)).let {
            if (it.type == activeMaterial) it.type = Material.AIR
        }

        velocity = owner.eyeLocation.direction.clone().normalize().multiply(speed)
        travelled = 0.0

        val sound = if (frozen) Sound.BLOCK_GLASS_BREAK else Sound.ENTITY_PLAYER_SPLASH
        world.playSound(location.toLocation(world), sound, 1.0f, 0.8f)
    }

    private fun tickLaunched() {
        val world = owner.world
        previousLocation = location.clone()

        velocity = velocity.clone().also { it.y -= gravity }
        val step = velocity.clone()

        val from = previousLocation.toLocation(world)
        val hit = world.rayTraceBlocks(from, step.clone().normalize(), step.length(), FluidCollisionMode.NEVER, true)
        val hitBlock = hit?.hitBlock

        if (hitBlock != null && hitBlock.type.isSolid && !isOwnTrailBlock(hitBlock)) {
            splash(hit.hitPosition.toLocation(world))
            end()
            return
        }

        location = location.clone().add(step)
        travelled += step.length()

        advanceTrail()
        spawnTrailParticles(location.toLocation(world))

        val hitEntity = findEntityNear(location, 0.9)
        if (hitEntity != null) {
            splash(location.toLocation(world))
            end()
            return
        }

        if (travelled >= maxTravel) {
            splash(location.toLocation(world))
            end()
        }
    }

    private fun spawnTrailParticles(loc: org.bukkit.Location) {
        val world = owner.world
        if (frozen) {
            world.spawnParticle(Particle.SNOWFLAKE, loc, 6, 0.08, 0.08, 0.08, 0.02)
            world.spawnParticle(Particle.ITEM_SNOWBALL, loc, 3, 0.05, 0.05, 0.05, 0.0)
            if (ticksElapsed % 4 == 0) {
                world.playSound(loc, Sound.BLOCK_GLASS_STEP, 0.4f, 1.6f)
            }
        } else {
            world.spawnParticle(Particle.FALLING_WATER, loc, 6, 0.08, 0.08, 0.08, 0.02)
            world.spawnParticle(Particle.BUBBLE_POP, loc, 3, 0.05, 0.05, 0.05, 0.0)
            if (ticksElapsed % 4 == 0) {
                world.playSound(loc, Sound.BLOCK_WATER_AMBIENT, 0.4f, 1.6f)
            }
        }
    }

   private fun splash(hitPoint: org.bukkit.Location) {
        val world = hitPoint.world ?: owner.world
        clearTrail()

        if (frozen) {
            world.playSound(hitPoint, Sound.BLOCK_GLASS_BREAK, 1.3f, 1.0f)
            world.playSound(hitPoint, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 1.1f)
            world.spawnParticle(Particle.SNOWFLAKE, hitPoint, 40, 0.6, 0.4, 0.6, 0.1)
            world.spawnParticle(Particle.ITEM_SNOWBALL, hitPoint, 30, 0.6, 0.3, 0.6, 0.05)
            world.spawnParticle(Particle.BLOCK_CRUMBLE, hitPoint, 25, 0.5, 0.3, 0.5, 0.0, Material.ICE.createBlockData())
        } else {
            world.playSound(hitPoint, Sound.ENTITY_GENERIC_SPLASH, 1.3f, 1.0f)
            world.playSound(hitPoint, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 0.8f, 1.1f)
            world.spawnParticle(Particle.FALLING_WATER, hitPoint, 40, 0.6, 0.4, 0.6, 0.15)
            world.spawnParticle(Particle.SPLASH, hitPoint, 60, 0.7, 0.3, 0.7, 0.2)
        }

        for (entity in world.getNearbyLivingEntities(hitPoint, splashRadius)) {
            if (entity == owner) continue

            val away = entity.location.toVector().subtract(hitPoint.toVector())
            val horizontal = Vector(away.x, 0.0, away.z)
            if (horizontal.lengthSquared() < 1.0e-4) horizontal.setX(0.01)
            horizontal.normalize().multiply(knockbackStrength)

            entity.damage(damage, owner)
            entity.velocity = entity.velocity.clone().add(horizontal).setY(0.35)

            if (frozen) {
                entity.freezeTicks = (entity.freezeTicks + entity.maxFreezeTicks / 2)
                    .coerceAtMost(entity.maxFreezeTicks)
                entity.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, soakedSlowTicks, 2, false, true))
            } else {
                entity.fireTicks = 0
                entity.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, soakedSlowTicks, 1, false, true))
            }
        }

        val center = hitPoint.toVector()
        val radiusBlocks = 1
        for (dx in -radiusBlocks..radiusBlocks) {
            for (dz in -radiusBlocks..radiusBlocks) {
                val pos = Vector(
                    kotlin.math.floor(center.x).toInt() + dx + 0.5,
                    kotlin.math.floor(center.y).toInt().toDouble(),
                    kotlin.math.floor(center.z).toInt() + dz + 0.5,
                )
                val dist2 = dx * dx + dz * dz
                if (dist2 > radiusBlocks * radiusBlocks + 1) continue

                val block = world.getBlockAt(pos.toLocation(world))
                if (block.type == Material.AIR) {
                    block.setType(activeMaterial, false)
                    val lingerBonus = if (frozen) puddleLifeTicks * 2 else 0
                    placeTempBlock(pos, ticksElapsed + puddleLifeTicks + lingerBonus + (dist2 * 3))
                }
            }
        }
    }

    private fun fizzle() {
        val world = owner.world
        world.spawnParticle(Particle.DRIPPING_WATER, location.toLocation(world), 10, 0.2, 0.2, 0.2, 0.0)
        clearTrail()
        end()
    }

    private fun placeTempBlock(pos: Vector, revertAtTick: Int) {
        tempBlocks.add(TempBlock(pos.clone(), revertAtTick))
    }

   private fun advanceTrail() {
        val world = owner.world

        val headBlock = world.getBlockAt(location.toLocation(world))
        if (headBlock.type == Material.AIR) {
            headBlock.setType(activeMaterial, false)
        }
        trailQueue.addLast(location.clone())

        while (trailQueue.size > trailLength) {
            val expired = trailQueue.removeFirst()
            val block = world.getBlockAt(expired.toLocation(world))
            if (block.type == Material.WATER || block.type == Material.ICE) {
                block.type = Material.AIR
            }
        }
    }

    private fun clearTrail() {
        val world = owner.world
        for (pos in trailQueue) {
            val block = world.getBlockAt(pos.toLocation(world))
            if (block.type == Material.WATER || block.type == Material.ICE) {
                block.type = Material.AIR
            }
        }
        trailQueue.clear()
    }

    private fun isOwnTrailBlock(block: org.bukkit.block.Block): Boolean {
        return trailQueue.any { pos ->
            block.x == kotlin.math.floor(pos.x).toInt() &&
                    block.y == kotlin.math.floor(pos.y).toInt() &&
                    block.z == kotlin.math.floor(pos.z).toInt()
        }
    }

    private fun revertDueTempBlocks() {
        if (tempBlocks.isEmpty()) return
        val world = owner.world
        val iterator = tempBlocks.iterator()
        while (iterator.hasNext()) {
            val tb = iterator.next()
            if (tb.revertAtTick <= ticksElapsed) {
                val block = world.getBlockAt(tb.pos.toLocation(world))
                if (block.type == Material.WATER || block.type == Material.ICE) block.type = Material.AIR
                iterator.remove()
            }
        }
    }

    private fun findEntityNear(pos: Vector, radius: Double): LivingEntity? {
        val world = owner.world
        return world.getNearbyLivingEntities(pos.toLocation(world), radius)
            .firstOrNull { it != owner }
    }

    private fun isLookingAtWater(): Boolean {
        val eye = owner.eyeLocation
        val direction = eye.direction.clone().normalize()

        if (owner.location.block.type == Material.WATER) return true

        for (i in 0..60) {
            val distance = i * 0.05
            val block = eye.clone().add(direction.clone().multiply(distance)).block
            if (block.type == Material.WATER) return true
        }

        return false
    }
}