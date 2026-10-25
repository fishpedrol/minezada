package io.github.peixeangola.cultivo.registry;

import io.github.peixeangola.cultivo.Cultivo;
import io.github.peixeangola.cultivo.core.ContentIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Eventos de som do Cultivo, um por ID de {@link ContentIds#SOUNDS}. O áudio vem de eventos vanilla (o sounds.json
 * gerado aponta para eles com "type": "event"); o mod só dá nome, volume, tom e a legenda em português. Toda chamada
 * acontece no servidor com {@code level.playSound(null, ...)}, para todos os jogadores por perto ouvirem.
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Cultivo.MOD_ID);

    /** Tragada, no começo do uso de qualquer aparelho de fumo. */
    public static final RegistryObject<SoundEvent> SMOKE_INHALE = register("smoke.inhale");
    /** Fumaça soprada, no fim de uma sessão que deu certo. */
    public static final RegistryObject<SoundEvent> SMOKE_EXHALE = register("smoke.exhale");
    /** Água do bong borbulhando durante o uso (legenda "Bong borbulha"). */
    public static final RegistryObject<SoundEvent> BONG_BUBBLE = register("bong.bubble");
    /** Água do narguilé borbulhando durante o uso, mais grave (legenda própria, "Narguilé borbulha"). */
    public static final RegistryObject<SoundEvent> HOOKAH_BUBBLE = register("hookah.bubble");
    /** Isqueiro acendendo, no começo do uso. */
    public static final RegistryObject<SoundEvent> LIGHTER_CLICK = register("lighter.click");
    /** Maços farfalhando no suporte de secagem que está trabalhando. */
    public static final RegistryObject<SoundEvent> DRYING_RACK_RUSTLE = register("drying_rack.rustle");
    /** Peneira sacudindo enquanto trabalha. */
    public static final RegistryObject<SoundEvent> SIEVE_SIFT = register("sieve.sift");
    /** Prensa apertando, quando a placa chega embaixo. */
    public static final RegistryObject<SoundEvent> PRESS_SQUEEZE = register("press.squeeze");
    /** Fim de um ciclo de qualquer estação. */
    public static final RegistryObject<SoundEvent> STATION_FINISH = register("station.finish");
    /** Baseados enrolados na bandeja. */
    public static final RegistryObject<SoundEvent> TRAY_ROLL = register("tray.roll");
    /** Pote aberto (retirar o conteúdo). */
    public static final RegistryObject<SoundEvent> JAR_OPEN = register("jar.open");
    /** Pote fechado (guardar algo). */
    public static final RegistryObject<SoundEvent> JAR_CLOSE = register("jar.close");
    /** Cura concluída no pote. */
    public static final RegistryObject<SoundEvent> JAR_CURED = register("jar.cured");
    /** Dichavador moendo (instalar, tirar e peneira trabalhando com ele). */
    public static final RegistryObject<SoundEvent> GRINDER_GRIND = register("grinder.grind");
    /** Cinza caindo no cinzeiro. */
    public static final RegistryObject<SoundEvent> ASHTRAY_ASH = register("ashtray.ash");
    /** Cinzeiro esvaziado (sai farinha de osso). */
    public static final RegistryObject<SoundEvent> ASHTRAY_EMPTY = register("ashtray.empty");

    private ModSounds() {}

    private static RegistryObject<SoundEvent> register(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(Cultivo.id(id)));
    }
}
