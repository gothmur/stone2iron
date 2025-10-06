package sk.gothmur.mod.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import sk.gothmur.mod.stone2steel;

// GeckoLib (minimal)
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class GrindstoneBlockEntity extends BlockEntity implements GeoAnimatable {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Držanie RMB – klientsky buffer (udrží "true" ešte pár tickov)
    public int clientHoldBufTicks = 0;

    // Po pustení RMB nech dobehne do najbližšej 90° polohy
    public boolean finishToQuarter = false;

    // Absolútny uhol (stupne), ktorý priamo použijeme v modeli
    private float angleDeg = 0f;

    // Zvukový cooldown, aby sme neprehrávali zvuk každý tick
    private int soundCooldown = 0;
    private static final int SOUND_INTERVAL_TICKS = 8; // ako jedna „štvťotáčka“
    private static final float SOUND_VOL = 0.6f;

    // Nastavenia rýchlostí
    private static final float SPIN_SPEED_DEG_PER_TICK = 12f; // rýchlosť pri držaní
    private static final float SNAP_STEP_DEG_PER_TICK = 6f;   // rýchlosť dorovnania na násobok 90°

    public GrindstoneBlockEntity(BlockPos pos, BlockState state) {
        super(stone2steel.GRINDSTONE_BE.get(), pos, state);
    }

    /* --------------------------
             CLIENT TICK
       -------------------------- */
    public static void clientTick(Level level, BlockPos pos, BlockState state, GrindstoneBlockEntity be) {
        boolean spinningNow = false;

        // Držíme RMB -> plynulo toč
        if (be.clientHoldBufTicks > 0) {
            be.angleDeg = (be.angleDeg - SPIN_SPEED_DEG_PER_TICK) % 360f; // mínus = smer doľava
            be.clientHoldBufTicks--;
            spinningNow = true;
            // ak počas držania bežalo "dovyrovnávanie", vypni keď už je veľmi blízko
            if (be.finishToQuarter) {
                float mod = ((be.angleDeg % 90f) + 90f) % 90f;
                if (mod < 1f || mod > 89f) be.finishToQuarter = false;
            }
        } else if (be.finishToQuarter) {
            // Nedržíme -> ak treba, dorovnaj na násobok 90°
            float mod = ((be.angleDeg % 90f) + 90f) % 90f; // 0..90
            if (mod == 0f) {
                be.finishToQuarter = false;
            } else {
                float toGo = 90f - mod;
                float step = Math.min(SNAP_STEP_DEG_PER_TICK, toGo);
                be.angleDeg = (be.angleDeg - step) % 360f; // točíme v rovnakom smere
                spinningNow = true;
            }
        }

        // Zvuk: prehrávaj krátky „grind“ každých X tickov počas otáčania
        if (spinningNow) {
            if (be.soundCooldown <= 0) {
                float pitch = 0.95f + (level.getRandom().nextFloat() * 0.1f); // jemná variácia
                level.playLocalSound(
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.GRINDSTONE_USE, // vanilla grindstone zvuk
                        SoundSource.BLOCKS,
                        SOUND_VOL, pitch, false
                );
                be.soundCooldown = SOUND_INTERVAL_TICKS;
            } else {
                be.soundCooldown--;
            }
        } else {
            // keď sa netočí, cooldown odrátaj (aby sa ďalší zvuk neprehral hneď pri krátkom ťuknutí)
            if (be.soundCooldown > 0) be.soundCooldown--;
        }
    }

    /* --------------------------
             SERVER TICK (no-op)
       -------------------------- */
    public static void serverTick(Level level, BlockPos pos, BlockState state, GrindstoneBlockEntity be) {
        // zámerne prázdne – server nič nerieši v tejto verzii
    }

    /* --------------------------
                 API
       -------------------------- */
    public void clientBumpSpin() {
        if (this.level != null && this.level.isClientSide) {
            this.clientHoldBufTicks = 2; // drž pár tickov "true", kým klient tickuje
        }
    }

    public void clientOnRelease() {
        if (this.level != null && this.level.isClientSide) {
            float mod = ((angleDeg % 90f) + 90f) % 90f;
            this.finishToQuarter = (mod != 0f);
        }
    }

    // spätná kompatibilita s pôvodným networkingom – no-op
    public void setSpinning(UUID playerId, boolean start) {
        // nič – ovládame len klientsky
    }

    /* --------------------------
           GeckoLib (minimum)
       -------------------------- */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // žiadne animácie – rotáciu robíme priamo v modeli cez angleDeg
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object animatable) {
        return this.level != null ? this.level.getGameTime() : 0;
    }

    public float getAngleDeg() {
        return angleDeg;
    }

    /* --------------------------
                 NBT (1.21.1)
       -------------------------- */
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("AngleDeg", angleDeg);
        tag.putBoolean("FinishToQuarter", finishToQuarter);
        tag.putInt("SoundCooldown", soundCooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("AngleDeg")) angleDeg = tag.getFloat("AngleDeg");
        finishToQuarter = tag.getBoolean("FinishToQuarter");
        soundCooldown = tag.getInt("SoundCooldown");
    }
}
