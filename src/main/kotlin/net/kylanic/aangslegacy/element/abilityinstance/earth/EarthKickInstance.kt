package net.kylanic.aangslegacy.element.abilityinstance.earth

import net.kylanic.aangslegacy.element.abilityinstance.AbilityInstance
import net.kylanic.aangslegacy.element.abilityinstance.ShiftAbility
import net.kylanic.aangslegacy.event.Event
import net.kylanic.aangslegacy.event.EventType
import net.kylanic.aangslegacy.util.ConfigLoader
import net.kyori.adventure.text.Component
import org.bukkit.FluidCollisionMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Damageable
import org.bukkit.entity.Entity
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.math.sin

class EarthKickInstance(
    override val owner: Player,
) : AbilityInstance("al:earth_kick", owner), ShiftAbility {
    private enum class Phase { SELECTING, FLOATING, LAUNCHED }

    private var phase = Phase.SELECTING

    private var sourceBlockLoc: Location? = null
    private var fallingBlock: FallingBlock? = null

    private val selectRange = 5.0
    private val launchTargetRange = 8.0
    private val floatHeightOffset = 2.0
    private val launchSpeed = 1.6
    private val damage = 4.0
    private val knockbackStrength = 0.4
    private val kickRecoil = 0.25

    private var lifeTimeTicks: Int = 200

    private var floatCenter: Location? = null
    private var floatTicks: Int = 0
    private val bobHeight = 0.15
    private val bobSpeed = 0.1

    private var lastAirLocation: Location? = null

    override fun onEvent(event: Event) = when (event.type) {
        EventType.Shift if phase == Phase.SELECTING -> selectAndSpawnBlock()
        EventType.ArmSwing if phase == Phase.FLOATING -> if (isBeingAimedAt()) launch() else {}
        else -> {}
    }


    private fun isBeingAimedAt(): Boolean {
        val fb = fallingBlock ?: return false

        val hit = owner.world.rayTraceEntities(
            owner.eyeLocation,
            owner.eyeLocation.direction,
            launchTargetRange,
            0.5
        ) { entity -> entity is FallingBlock }

        val hitEntity = hit?.hitEntity ?: return false
        return hitEntity.entityId == fb.entityId
    }

    override fun onStart() {}

    private fun selectAndSpawnBlock() {
        val earthbendable = ConfigLoader.getEarthbendableBlocks()

        val hit = owner.world.rayTraceBlocks(
            owner.eyeLocation,
            owner.eyeLocation.direction,
            selectRange,
            FluidCollisionMode.NEVER,
            true
        )

        val block = hit?.hitBlock
        if (block == null) {
            owner.sendActionBar(Component.text("§cNo earthbendable block in range."))
            end()
            return
        }

        if (block.type !in earthbendable) {
            owner.sendActionBar(Component.text("§cNo earthbendable block in range."))
            end()
            return
        }

        sourceBlockLoc = block.location.clone()
        val blockData = block.blockData.clone()

        val groundCenter = block.location.clone().add(0.5, 0.5, 0.5)
        owner.world.spawnParticle(Particle.BLOCK_CRUMBLE, groundCenter, 25, 0.3, 0.2, 0.3, 0.0, blockData)
        owner.world.spawnParticle(Particle.CLOUD, groundCenter, 6, 0.25, 0.1, 0.25, 0.01)

        block.setType(Material.AIR, false)

        val spawnLoc = block.location.clone().add(0.5, floatHeightOffset, 0.5)
        val fb = owner.world.spawnFallingBlock(spawnLoc, blockData)
        fb.setGravity(false)
        fb.dropItem = false
        fb.setHurtEntities(false)
        fb.velocity = Vector(0.0, 0.0, 0.0)


        fallingBlock = fb
        floatCenter = spawnLoc.clone()
        floatTicks = 0
        lastAirLocation = spawnLoc.clone()
        phase = Phase.FLOATING

        owner.world.playSound(spawnLoc, Sound.BLOCK_STONE_BREAK, 0.8f, 0.9f)
        owner.world.playSound(spawnLoc, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.4f, 1.6f)
    }

    private fun launch() {
        val fb = fallingBlock ?: return

        val direction = owner.eyeLocation.direction.clone().normalize()
        fb.velocity = direction.multiply(launchSpeed)

        val recoil = direction.clone().multiply(-kickRecoil).setY(0.05)
        owner.velocity = owner.velocity.clone().add(recoil)

        phase = Phase.LAUNCHED
        owner.world.playSound(fb.location, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.8f)
        owner.world.spawnParticle(Particle.BLOCK_CRUMBLE, fb.location, 15, 0.15, 0.15, 0.15, 0.05, fb.blockData)
    }

    override fun onTick() {
        val fb = fallingBlock
        if (fb == null || !fb.isValid) {
            if (phase != Phase.SELECTING) {
                end()
            }
            return
        }

        when (phase) {
            Phase.SELECTING -> {}
            Phase.FLOATING -> {
                lifeTimeTicks -= 1
                if (lifeTimeTicks <= 0) {
                    end()
                    return
                }

                floatTicks += 1
                val center = floatCenter
                if (center != null) {
                    val offset = sin(floatTicks * bobSpeed) * bobHeight
                    val bobbed = center.clone().add(0.0, offset, 0.0)
                    fb.teleport(bobbed)
                    lastAirLocation = bobbed.clone()
                }

                owner.world.spawnParticle(Particle.BLOCK_CRUMBLE, fb.location, 3, 0.1, 0.1, 0.1, fb.blockData)

                if (floatTicks % 10 == 0) {
                    owner.world.spawnParticle(Particle.CLOUD, fb.location.clone().add(0.0, -0.3, 0.0), 2, 0.15, 0.02, 0.15, 0.005)
                }
            }
            Phase.LAUNCHED -> {
                owner.world.spawnParticle(Particle.BLOCK_CRUMBLE, fb.location, 4, 0.1, 0.1, 0.1, 0.02, fb.blockData)

                val hitEntity = owner.world.getNearbyEntities(fb.location, 0.6, 0.6, 0.6)
                    .filterIsInstance<Entity>()
                    .firstOrNull { it != owner && it is Damageable }

                if (hitEntity != null) {
                    (hitEntity as Damageable).damage(damage, owner)
                    val push = fb.velocity.clone().normalize().multiply(knockbackStrength).setY(0.2)
                    hitEntity.velocity = hitEntity.velocity.clone().add(push)

                    spawnImpactEffects(fb.location, fb.blockData)
                    end()
                    return
                }

                if (fb.location.block.type.isSolid) {
                    placeAtImpact(fb.blockData)
                    spawnImpactEffects(fb.location, fb.blockData)
                    end()
                    return
                }

                lastAirLocation = fb.location.clone()
            }
        }
    }

    private fun placeAtImpact(blockData: org.bukkit.block.data.BlockData) {
        val placeLoc = lastAirLocation ?: return
        val block = placeLoc.block

        if (block.type != Material.AIR) return

        block.blockData = blockData
    }

    private fun spawnImpactEffects(location: Location, blockData: org.bukkit.block.data.BlockData) {
        val world = location.world ?: return
        world.spawnParticle(Particle.BLOCK_CRUMBLE, location, 30, 0.3, 0.3, 0.3, 0.1, blockData)
        world.spawnParticle(Particle.CLOUD, location, 8, 0.2, 0.1, 0.2, 0.03)
        world.playSound(location, Sound.BLOCK_STONE_BREAK, 1f, 0.7f)
        world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 0.3f, 1.8f)
    }

    override fun onEnd() {
        fallingBlock?.let {
            if (it.isValid) it.remove()
        }
        fallingBlock = null
    }
}