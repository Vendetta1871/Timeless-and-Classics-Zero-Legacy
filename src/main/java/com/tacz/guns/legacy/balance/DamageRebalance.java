package com.tacz.guns.legacy.balance;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tacz.guns.GunMod;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 1.12.2 addition (com.tacz.guns.legacy): per-shot damage rebalance for the default balance.
 * <p>
 * The base damage of each gun is derived from its real cartridge with the Gosselain number
 * (temporary wound cavity volume, proportional to muzzle energy x caliber), compressed by a power
 * law {@code D = 8 * (V / V(5.56x45))^0.8} and rounded to 0.5. The .50 BMG guns are capped by hand
 * (the formula would give ~95), and the minigun keeps a low per-bullet damage on purpose because of
 * its 1200 rpm. Explosive weapons (m320, rpg7) are out of the model and not listed.
 * <p>
 * The overrides are applied to the gun pack JSON ({@link #rebalance(Map)}) before it is parsed into
 * {@link com.tacz.guns.resource.pojo.data.gun.GunData} and before it is stored in the network cache,
 * so the logical server, the synced clients and the local client reload all see the same values.
 * No file of the (CC BY-NC-ND licensed) default pack is modified or redistributed by this.
 * <p>
 * Config: {@code config/tacz-rebalance.cfg}. The server owns the values; in multiplayer the synced
 * pack data overwrites whatever the client has configured locally. Edit the file and run
 * {@code /tacz reload} (or restart) to apply changes.
 */
public final class DamageRebalance {
    private static final String CATEGORY = "rebalance";
    /** Suffix of the gun data resource ids (the index files reference {@code tacz:<gun>_data}). */
    private static final String DATA_SUFFIX = "_data";

    /**
     * Gosselain @0.8 damage per shot, per default-pack gun id (without the {@code _data} suffix).
     * Ordered by weapon class like the balance tables; guns not listed keep their pack damage.
     */
    private static final Map<String, Float> DEFAULTS = buildDefaults();

    private static volatile boolean enabled = true;
    private static volatile Map<ResourceLocation, Float> overrides = new LinkedHashMap<>();

    private DamageRebalance() {
    }

    private static Map<String, Float> buildDefaults() {
        Map<String, Float> map = new LinkedHashMap<>();
        // pistols
        map.put("taurus943", 1.5f);   // .22 WMR
        map.put("glock_17", 4.5f);    // 9x19
        map.put("cz75", 4.5f);
        map.put("m9a4", 4.5f);
        map.put("p320", 4.5f);        // .45 ACP
        map.put("m1911", 5f);
        map.put("rhino357", 5.5f);    // .357 Mag
        map.put("hk_mk23", 6f);
        map.put("deagle_golden", 6f);
        map.put("deagle", 15.5f);     // .50 AE
        map.put("timeless50", 19f);
        map.put("taurus500", 19f);    // .500 S&W
        // SMGs
        map.put("p90", 3f);           // 5.7x28, its niche is the 0.7 armor penetration
        map.put("uzi", 4.5f);
        map.put("hk_mp5a5", 4.5f);
        map.put("b93r", 5f);
        map.put("vector45", 5f);      // .45 ACP
        map.put("ump45", 5.5f);
        // assault rifles
        map.put("hk416d", 7f);        // 5.56x45, short barrel
        map.put("g36k", 7.5f);
        map.put("m4a1", 8f);
        map.put("m16a4", 8f);         // anchor of the scale
        map.put("spr15hb", 8f);
        map.put("qbz_95", 8f);        // 5.8x42
        map.put("scar_l", 8.5f);
        map.put("qbz_191", 8.5f);
        map.put("aug", 8.5f);
        map.put("m16a1", 8.5f);       // 20" barrel
        map.put("type_81", 12f);      // 7.62x39
        map.put("ak47", 12f);
        map.put("sks_tactical", 12.5f);
        map.put("rpk", 13f);
        // machine guns
        // minigun: the formula gives 17, cut by hand - at 1200 rpm its DPM would be absurd
        map.put("minigun", 10f);
        // battle rifles / DMRs
        map.put("fn_evolys", 16.5f);  // 7.62x51
        map.put("scar_h", 17f);
        map.put("hk_g3", 17.5f);
        map.put("lonetrail", 18f);    // .30-06
        map.put("fn_fal", 18f);
        map.put("mk14", 18.5f);
        map.put("m700", 19f);
        // bolt actions / big bores
        map.put("springfield1873", 16.5f); // .45-70
        map.put("kar98", 20.5f);           // 7.92x57
        map.put("ai_awp", 32f);            // .338 Lapua
        // .50 BMG: the formula gives ~95, capped by hand to stay a (strong) two-shot weapon
        map.put("m107", 50f);
        map.put("m95", 55f);
        // shotguns: damage is the total per shell, divided over the pellets at runtime
        map.put("db_short", 7f);
        map.put("aa12", 11f);
        map.put("db_long", 12f);
        map.put("m870", 12f);
        map.put("m1014", 13.5f);
        map.put("spas_12", 15.5f);
        return map;
    }

    private static String[] defaultEntries() {
        List<String> list = new ArrayList<>(DEFAULTS.size());
        for (Map.Entry<String, Float> entry : DEFAULTS.entrySet()) {
            list.add(GunMod.MOD_ID + ":" + entry.getKey() + "=" + fmt(entry.getValue()));
        }
        return list.toArray(new String[0]);
    }

    /**
     * (Re)loads {@code config/tacz-rebalance.cfg}. Called before every gun pack reload, so an edited
     * file takes effect on {@code /tacz reload}.
     */
    public static void reload() {
        File file = new File(Loader.instance().getConfigDir(), "tacz-rebalance.cfg");
        Configuration config = new Configuration(file);
        config.setCategoryComment(CATEGORY,
                "Per-shot damage rebalance based on the real muzzle energy x caliber (Gosselain) of each "
                + "cartridge, see the javadoc of DamageRebalance for the exact formula. "
                + "Server-side: in multiplayer the synced values from the server win.");
        enabled = config.getBoolean("enabled", CATEGORY, true,
                "When false, guns keep the damage from the gun pack files.");
        String[] raw = config.getStringList("damage_overrides", CATEGORY, defaultEntries(),
                "Damage per shot as '<gun id>=<damage>', e.g. 'tacz:ak47=12'. "
                + "The distance falloff (damage_adjust) and the fire mode offsets (fire_mode_adjust) "
                + "are scaled by the same new/old ratio. Remove an entry to keep that gun's pack damage.");
        Map<ResourceLocation, Float> parsed = new LinkedHashMap<>();
        for (String entry : raw) {
            if (entry == null || entry.trim().isEmpty()) {
                continue;
            }
            int sep = entry.indexOf('=');
            if (sep <= 0 || sep == entry.length() - 1) {
                GunMod.LOGGER.warn("Damage rebalance: bad entry '{}', expected <gun id>=<damage>", entry);
                continue;
            }
            String idText = entry.substring(0, sep).trim().toLowerCase(Locale.ROOT);
            ResourceLocation gunId = idText.indexOf(':') >= 0
                    ? new ResourceLocation(idText)
                    : new ResourceLocation(GunMod.MOD_ID, idText);
            try {
                float damage = Float.parseFloat(entry.substring(sep + 1).trim());
                if (damage <= 0f) {
                    throw new NumberFormatException("damage must be > 0");
                }
                parsed.put(gunId, damage);
            } catch (NumberFormatException e) {
                GunMod.LOGGER.warn("Damage rebalance: bad damage in entry '{}': {}", entry, e.getMessage());
            }
        }
        overrides = parsed;
        config.save();
    }

    /**
     * Rewrites the listed guns in the scanned {@code data/guns} JSON before parsing.
     * Must run before {@code JsonDataManager#apply}, so both the parsed data and the network cache
     * built from the same elements carry the rebalanced values.
     */
    public static void rebalance(Map<ResourceLocation, JsonElement> gunDataJson) {
        if (!enabled || overrides.isEmpty()) {
            return;
        }
        int applied = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : gunDataJson.entrySet()) {
            if (applyToGunJson(entry.getKey(), entry.getValue())) {
                applied++;
            }
        }
        GunMod.LOGGER.info("Damage rebalance: applied {} of {} configured damage overrides",
                applied, overrides.size());
    }

    private static boolean applyToGunJson(ResourceLocation dataId, JsonElement element) {
        Float value = overrides.get(stripDataSuffix(dataId));
        if (value == null) {
            return false;
        }
        if (!element.isJsonObject()) {
            GunMod.LOGGER.warn("Damage rebalance: {} is not a JSON object, skipped", dataId);
            return false;
        }
        JsonObject root = element.getAsJsonObject();
        JsonObject bullet = asObject(root.get("bullet"));
        JsonElement damageElement = bullet != null ? bullet.get("damage") : null;
        if (damageElement == null || !damageElement.isJsonPrimitive()) {
            GunMod.LOGGER.warn("Damage rebalance: no bullet damage in {}, skipped", dataId);
            return false;
        }
        float oldDamage = damageElement.getAsFloat();
        if (oldDamage <= 0f) {
            GunMod.LOGGER.warn("Damage rebalance: non-positive damage in {}, skipped", dataId);
            return false;
        }
        float ratio = value / oldDamage;
        bullet.addProperty("damage", round(value));

        // distance falloff: absolute damage per segment, scale by the same ratio
        JsonObject extraDamage = asObject(bullet.get("extra_damage"));
        if (extraDamage != null && extraDamage.get("damage_adjust") != null && extraDamage.get("damage_adjust").isJsonArray()) {
            for (JsonElement segment : extraDamage.getAsJsonArray("damage_adjust")) {
                JsonObject pair = asObject(segment);
                if (pair != null && pair.get("damage") != null && pair.get("damage").isJsonPrimitive()) {
                    pair.addProperty("damage", round(pair.get("damage").getAsFloat() * ratio));
                }
            }
        }

        // fire mode damage values are offsets added to the base damage (see DamageModifier):
        // rescale the effective per-mode damage (old + offset) and turn it back into an offset
        JsonObject fireModeAdjust = asObject(root.get("fire_mode_adjust"));
        if (fireModeAdjust != null) {
            for (Map.Entry<String, JsonElement> mode : fireModeAdjust.entrySet()) {
                JsonObject adjust = asObject(mode.getValue());
                if (adjust != null && adjust.get("damage") != null && adjust.get("damage").isJsonPrimitive()) {
                    float offset = adjust.get("damage").getAsFloat();
                    adjust.addProperty("damage", round((oldDamage + offset) * ratio - value));
                }
            }
        }
        GunMod.LOGGER.debug("Damage rebalance: {} damage {} -> {}", dataId, fmt(oldDamage), fmt(value));
        return true;
    }

    private static JsonObject asObject(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static ResourceLocation stripDataSuffix(ResourceLocation id) {
        return id.getPath().endsWith(DATA_SUFFIX)
                ? new ResourceLocation(id.getNamespace(), id.getPath().substring(0, id.getPath().length() - DATA_SUFFIX.length()))
                : id;
    }

    private static float round(float value) {
        return Math.round(value * 10f) / 10f;
    }

    private static String fmt(float value) {
        return value == Math.floor(value) ? String.valueOf((int) value) : String.format(Locale.ROOT, "%.1f", value);
    }
}
