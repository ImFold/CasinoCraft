# Changelog

All notable changes to CasinoCraft.

## Unreleased

* Added client-side reel trails, landing/win glows, bounded gold spark bursts and a three-sevens jackpot effect. Respects the Minecraft particle setting.

* Shop prices, catalogue and cashier previews now use per-screen server price snapshots.
* Price and data-pack reloads rebuild the price cache and refresh open casino screens.
* Purchases with an outdated displayed price are rejected without charging.
* Corrected the documented slot return to 84.5946% (house edge 15.4054%); payouts are unchanged.
* Added regression tests for slot returns, price snapshots and packet serialization.
* Requires updating both server and clients because the purchase packet format changed.

## 1.5.1

* Smithing recipes are now read (via access widener), so netherite gear is priced from
  diamond item + netherite ingot + template. A netherite sword is worth 6217, not 9000.
* Closed an exploit: selling a netherite sword and buying scrap for profit.

## 1.5.0

* The cashier accepts almost everything: tools, weapons and armor are priced by recipe.
* Durability is taken into account when selling.
* Fixed prices for netherite, chainmail and horse armor (no ingredients in 1.20.1 recipes).

## 1.4.2

* Automatic anti-crafting-arbitrage clamp for all sell prices. Logs vs planks, ingots vs
  nuggets and similar chains can no longer print money, even with a legacy config.

## 1.4.1

* Ores and converter blocks (cobweb, amethyst cluster, gilded blackstone, wet sponge) are
  excluded from the shop.
* Server-side validation of purchased item IDs.

## 1.4.0

* The shop now contains virtually the whole item registry (1118 items in vanilla 1.20.1).
* Search field, mouse-wheel scrolling and a scrollbar.
* Prices for uncraftable items fall back to rarity; craftable items are priced by recipes.

## 1.3.0

* Prices adopted from the Upgrader 1.3.1 base table (diamond 400, emerald 140, etc.).
* Derived prices for blocks, nuggets, planks and simple recipes.

## 1.2.0

* Added the shop with a 25% markup over the sell price.

## 1.1.0

* Physical chips (100 and 10) with 1:1 withdraw/deposit and a per-click limit.

## 1.0.0

* Initial release: cashier, slots, roulette, `/casino` commands, JSON prices.

