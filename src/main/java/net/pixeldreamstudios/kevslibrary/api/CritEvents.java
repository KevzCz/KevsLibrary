package net.pixeldreamstudios.kevslibrary.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;

public final class CritEvents {
    public interface CritListener {
        void onCrit(Context ctx);
    }

    public static final Event<CritListener> CRIT = EventFactory.createArrayBacked(
            CritListener.class,
            listeners -> ctx -> {
                for (CritListener l : listeners) l.onCrit(ctx);
            }
    );

    public record Context(
            LivingEntity target,
            LivingEntity attacker,
            DamageSource source,
            float finalDamage,
            boolean isCrit
    ) { }
}
