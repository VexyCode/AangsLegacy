package net.kylanic.aangslegacy.element.ability.instance.fire

import net.kylanic.aangslegacy.element.ability.instance.ArmSwingAbility
import net.kylanic.aangslegacy.element.ability.instance.AbilityInstance
import net.kylanic.aangslegacy.element.ability.instance.KhaleAbilityInstance
import net.kylanic.aangslegacy.event.Event
import net.kylanic.aangslegacy.event.EventType
import net.kylanic.aangslegacy.util.BiomeUtils
import org.bukkit.Color
import org.bukkit.FluidCollisionMode
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.Damageable
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.util.Vector

class FireballInstance(
    override val owner: Player,
) : AbilityInstance("al:fireball", owner), KhaleAbilityInstance, ArmSwingAbility {
    private lateinit var direction: Vector
    private lateinit var location: Vector
    private lateinit var previousLocation: Vector

    private val speed = 16.0
    private val movementPerTick: Double
        get() = speed / 20.0

    private val velocity: Vector
        get() = direction.clone().multiply(movementPerTick)

    private var lifeTimeTicks: Int = 1200

    private val damage = 3.0
    private val explosionPower = 0.6f
    private val knockbackStrength = 0.35

    private var hitEntity: Entity? = null
    private var ended: Boolean = false

    private var lightBlock: Block? = null

    private companion object {
        val rawToCookedMeat: Map<Material, Material> = mapOf(
            Material.PORKCHOP to Material.COOKED_PORKCHOP,
            Material.BEEF to Material.COOKED_BEEF,
            Material.CHICKEN to Material.COOKED_CHICKEN,
            Material.RABBIT to Material.COOKED_RABBIT,
            Material.MUTTON to Material.COOKED_MUTTON,
            Material.COD to Material.COOKED_COD,
            Material.SALMON to Material.COOKED_SALMON,
        )
    }

    override fun onEvent(event: Event) {
        if (event.type == EventType.ArmSwing) {
            start()
        }
    }

    override fun onStart() {
        direction = owner.eyeLocation.direction.clone().normalize()
        location = owner.eyeLocation.toVector()
        previousLocation = location.clone()

        owner.world.playSound(location.toLocation(owner.world), Sound.ITEM_FIRECHARGE_USE, 0.8f, 1.1f)
    }

    override fun onTick() {
        lifeTimeTicks -= 1

        if (lifeTimeTicks <= 0) {
            end()
            return
        }

        val world = owner.world
        previousLocation = location.clone()

        val currentLocation = location.toLocation(world)
        updateLight(world, currentLocation)
        spawnFlightParticles(world, currentLocation)

        if (hasHitBlock(world, previousLocation, currentLocation.toVector())) {
            end()
            return
        }

        if (hasHitEntity(world, currentLocation)) {
            end()
            return
        }

        move()
    }

    override fun onEnd() {
        if (ended) return
        ended = true

        val world = owner.world
        val currentLocation = location.toLocation(world)

        lightBlock?.let {
            if (it.type == Material.LIGHT) it.type = Material.AIR
        }
        lightBlock = null

        hitEntity?.let { entity ->
            if (entity is Damageable) {
                entity.damage(damage, owner)
            }
            val push = direction.clone().multiply(knockbackStrength).setY(0.15)
            entity.velocity = entity.velocity.clone().add(push)

            if (entity.isDead) {
                cookNearbyDrops(entity.location)
            }
        }

        spawnImpactParticles(world, currentLocation)
        createExplosion(world, currentLocation)
    }

    private fun cookNearbyDrops(deathLocation: Location) {
        val world = deathLocation.world ?: return

        val freshDrops = world.getNearbyEntities(deathLocation, 1.5, 1.5, 1.5)
            .filterIsInstance<Item>()
            .filter { it.ticksLived <= 1 }

        for (drop in freshDrops) {
            val stack = drop.itemStack
            val cooked = rawToCookedMeat[stack.type] ?: continue

            stack.type = cooked
            drop.itemStack = stack
        }
    }

    private fun isRaining(location: Location): Boolean {
        val world = location.world
        return world.hasStorm() && location.y >= world.getHighestBlockYAt(location)
    }

    private fun isDampened(location: Location): Boolean {
        return BiomeUtils.isCold(location)
                || location.block.isLiquid
                || isRaining(location)
    }

    private fun spawnFlightParticles(world: World, location: Location) {
        if (isDampened(location)) {
            world.spawnParticle(
                Particle.DUST, location, 20, 0.2, 0.2, 0.2, 0.05,
                Particle.DustOptions(Color.fromRGB(70, 70, 70), 1.5f)
            )
        } else {
            world.spawnParticle(Particle.FLAME, location, 8, 0.08, 0.08, 0.08, 0.01)
        }
    }

    private fun hasHitBlock(world: World, from: Vector, to: Vector): Boolean {
        val step = to.clone().subtract(from)
        if (step.lengthSquared() < 1.0e-6) return to.toLocation(world).block.isSolid

        val hit = world.rayTraceBlocks(
            from.toLocation(world),
            step.clone().normalize(),
            step.length(),
            FluidCollisionMode.NEVER,
            true
        )

        return hit?.hitBlock?.type?.isSolid == true
    }

    private fun hasHitEntity(world: World, location: Location): Boolean {
        val hit = world.rayTraceEntities(location, direction, movementPerTick, 0.5)
        val entity = hit?.hitEntity ?: return false

        if (entity == owner) return false

        hitEntity = entity
        return true
    }

    private fun move() {
        location.add(velocity)
    }

    private fun spawnImpactParticles(world: World, location: Location) {
        if (isDampened(location)) {
            world.spawnParticle(
                Particle.DUST, location, 20, 0.2, 0.2, 0.2, 0.05,
                Particle.DustOptions(Color.fromRGB(70, 70, 70), 1.5f)
            )
        } else {
            world.spawnParticle(Particle.FLAME, location, 15, 0.15, 0.15, 0.15, 0.02)
        }

        world.spawnParticle(Particle.SMOKE, location, 5, 0.1, 0.1, 0.1, 0.02)
        world.spawnParticle(Particle.EXPLOSION, location, 1)
        world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 1.7f)
    }

    private fun createExplosion(world: World, location: Location) {
        val power = if (isDampened(location)) explosionPower * 0.5f else explosionPower
        world.createExplosion(location, power, true)
    }

    private fun updateLight(world: World, loc: Location) {
        lightBlock?.let {
            if (it.type == Material.LIGHT) it.type = Material.AIR
        }
        lightBlock = null

        val block = loc.block
        if (block.type == Material.AIR) {
            block.type = Material.LIGHT
            val data = block.blockData as org.bukkit.block.data.Levelled
            data.level = 15
            block.blockData = data
            lightBlock = block
        }
    }
}