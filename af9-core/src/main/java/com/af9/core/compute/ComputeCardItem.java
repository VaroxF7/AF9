package com.af9.core.compute;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;

/** A rack card ({@link ComputeCard}); its tooltip shows what it does. */
public class ComputeCardItem extends Item {

    public final ComputeCard card;

    public ComputeCardItem(ComputeCard card, Properties properties) {
        super(properties);
        this.card = card;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("af9.compute.card.kind." + card.kind.name().toLowerCase(Locale.ROOT),
                Component.translatable("af9.compute.card.tier." + ComputeCard.TIER_NAMES[card.tier]), card.tier)
                .withStyle(ChatFormatting.GRAY));
        if (card.kind.isProcessor()) {
            tooltip.add(Component.translatable("af9.compute.card.cwut", card.cwut,
                    String.format(Locale.ROOT, "%.2f", card.cwut / 4.0)).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("af9.compute.card.needs_ram", card.tier)
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("af9.compute.card.feeds", card.tier).withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("af9.compute.card.heat", card.heat).withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("af9.compute.card.eut", card.eut,
                GTValues.VNF[GTUtil.getTierByVoltage(card.eut)]).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable(card.tier <= ComputerRackPartMachine.MV_MAX_CARD_TIER ?
                "af9.compute.card.any_rack" : "af9.compute.card.luv_rack").withStyle(ChatFormatting.DARK_GRAY));
    }
}
