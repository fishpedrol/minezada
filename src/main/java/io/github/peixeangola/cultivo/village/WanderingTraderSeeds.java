package io.github.peixeangola.cultivo.village;

import io.github.peixeangola.cultivo.core.SeedSources;
import io.github.peixeangola.cultivo.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.event.entity.living.MobSpawnEvent;

/**
 * O mascate (wandering trader) vende sementes do catálogo. No 26.3 as ofertas do mascate saem de três trade sets
 * fixos no código (wandering_trader/buying, uncommon e common) e o Forge 26.3 não tem evento para acrescentar outro;
 * pôr as trocas do mod nas tags vanilla só daria uma chance pequena de aparecer, sem garantir as ofertas. Então,
 * quando um mascate novo nasce ({@link MobSpawnEvent.FinalizeSpawn}: spawn natural, ovo ou /summon), o servidor
 * acrescenta {@link SeedSources#TRADER_OFFERS} ofertas sorteadas por {@link SeedSources#traderOffers} depois das
 * vanilla. As ofertas são salvas com o mascate, então recarregar o mundo ou trocar de dimensão não as duplica (o
 * evento só dispara na criação).
 */
public final class WanderingTraderSeeds {
    private WanderingTraderSeeds() {}

    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getEntity() instanceof WanderingTrader trader && trader.level() instanceof ServerLevel) {
            addSeedOffers(trader);
        }
    }

    /** Acrescenta as ofertas de sementes ao mascate (gerando antes as vanilla, se ainda não existirem). */
    public static List<MerchantOffer> addSeedOffers(WanderingTrader trader) {
        MerchantOffers offers = trader.getOffers();
        RandomSource random = trader.getRandom();
        List<MerchantOffer> added = new ArrayList<>();
        for (SeedSources.TraderOffer offer : SeedSources.traderOffers(random::nextInt)) {
            MerchantOffer merchantOffer = new MerchantOffer(
                new ItemCost(Items.EMERALD, offer.emeralds()),
                new ItemStack(ModItems.SEEDS.get(offer.variety()).get(), offer.count()),
                SeedSources.TRADER_MAX_USES, SeedSources.TRADER_XP, SeedSources.TRADER_PRICE_MULTIPLIER);
            offers.add(merchantOffer);
            added.add(merchantOffer);
        }
        return added;
    }
}
