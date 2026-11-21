package com.tacz.guns.compat.jei.entry;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.GunTabType;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.ItemStack;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import com.google.common.collect.ImmutableList;

/** A single registration's shared stacks and compatibility results for both query directions. */
public final class AttachmentQueryData {
    private final List<AttachmentQueryEntry> attachmentQueries;
    private final List<GunAttachmentQueryEntry> gunAttachmentQueries;
    private final int gunCount;
    private final int attachmentCount;
    private final long compatibilityChecks;

    public AttachmentQueryData(List<AttachmentQueryEntry> attachmentQueries, List<GunAttachmentQueryEntry> gunAttachmentQueries, int gunCount, int attachmentCount, long compatibilityChecks) {
        this.attachmentQueries = ImmutableList.copyOf(attachmentQueries);
        this.gunAttachmentQueries = ImmutableList.copyOf(gunAttachmentQueries);
        this.gunCount = gunCount;
        this.attachmentCount = attachmentCount;
        this.compatibilityChecks = compatibilityChecks;
    }

    public List<AttachmentQueryEntry> attachmentQueries() {
        return attachmentQueries;
    }

    public List<GunAttachmentQueryEntry> gunAttachmentQueries() {
        return gunAttachmentQueries;
    }

    public int gunCount() {
        return gunCount;
    }

    public int attachmentCount() {
        return attachmentCount;
    }

    public long compatibilityChecks() {
        return compatibilityChecks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AttachmentQueryData)) return false;
        AttachmentQueryData that = (AttachmentQueryData) o;
        return java.util.Objects.equals(attachmentQueries, that.attachmentQueries) && java.util.Objects.equals(gunAttachmentQueries, that.gunAttachmentQueries) && java.util.Objects.equals(gunCount, that.gunCount) && java.util.Objects.equals(attachmentCount, that.attachmentCount) && java.util.Objects.equals(compatibilityChecks, that.compatibilityChecks);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(attachmentQueries, gunAttachmentQueries, gunCount, attachmentCount, compatibilityChecks);
    }

    @Override
    public String toString() {
        return "AttachmentQueryData[" + "attachmentQueries=" + attachmentQueries + ", " + "gunAttachmentQueries=" + gunAttachmentQueries + ", " + "gunCount=" + gunCount + ", " + "attachmentCount=" + attachmentCount + ", " + "compatibilityChecks=" + compatibilityChecks + "]";
    }

    private static final Map<String, GunTabType> GUN_TABS = Arrays.stream(GunTabType.values())
            .collect(Collectors.collectingAndThen(Collectors.toMap(type -> type.name().toLowerCase(Locale.US), type -> type),
                    Collections::unmodifiableMap));
    private static final Comparator<Gun> GUN_ORDER = Comparator.comparingInt(Gun::sort)
            .thenComparing(gun -> gun.id().toString());
    private static final Comparator<Attachment> ATTACHMENT_ORDER = Comparator.comparingInt(Attachment::sort)
            .thenComparing(attachment -> attachment.id().toString());

    public static AttachmentQueryData load() {
        List<Gun> guns = new ArrayList<>();
        TimelessAPI.getAllCommonGunIndex().forEach(entry -> {
            ItemStack stack = GunItemBuilder.create().setId(entry.getKey()).build();
            if (stack.getItem() instanceof IGun) {
                guns.add(new Gun(entry.getKey(), entry.getValue().getType(), entry.getValue().getSort(), stack));
            }
        });
        List<Attachment> attachments = new ArrayList<>();
        TimelessAPI.getAllCommonAttachmentIndex().forEach(entry -> {
            if (!entry.getValue().getPojo().isHidden()) {
                attachments.add(new Attachment(entry.getKey(), entry.getValue().getType(), entry.getValue().getSort(),
                        AttachmentItemBuilder.create().setId(entry.getKey()).build()));
            }
        });
        return create(guns, attachments,
                (gun, attachment) -> ((IGun) gun.stack().getItem()).allowAttachment(gun.stack(), attachment.stack()),
                (gun, type) -> ((IGun) gun.stack().getItem()).allowAttachmentType(gun.stack(), type));
    }

    static AttachmentQueryData create(List<Gun> guns, List<Attachment> attachments,
                                       BiPredicate<Gun, Attachment> compatible,
                                       BiPredicate<Gun, AttachmentType> allowsType) {
        List<Attachment> sortedAttachments = attachments.stream().sorted(ATTACHMENT_ORDER).collect(Collectors.toList());
        Set<AttachmentType> presentTypes = EnumSet.noneOf(AttachmentType.class);
        for (Attachment attachment : attachments) {
            if (attachment.type() != AttachmentType.NONE) {
                presentTypes.add(attachment.type());
            }
        }
        Map<Attachment, Map<GunTabType, List<ItemStack>>> gunsByAttachment = new IdentityHashMap<>();
        Map<Gun, Map<AttachmentType, List<ItemStack>>> attachmentsByGun = new IdentityHashMap<>();
        long checks = 0;
        for (Gun gun : guns) {
            GunTabType gunTab = GUN_TABS.get(gun.type());
            Set<AttachmentType> allowedTypes = EnumSet.noneOf(AttachmentType.class);
            for (AttachmentType type : presentTypes) {
                if (allowsType.test(gun, type)) {
                    allowedTypes.add(type);
                }
            }
            for (Attachment attachment : sortedAttachments) {
                boolean reverseEligible = allowedTypes.contains(attachment.type());
                if (gunTab == null && !reverseEligible) {
                    continue;
                }
                checks++;
                if (!compatible.test(gun, attachment)) {
                    continue;
                }
                // The attachment view historically does not require allowAttachmentType.
                if (gunTab != null) {
                    gunsByAttachment.computeIfAbsent(attachment, key -> new EnumMap<>(GunTabType.class))
                            .computeIfAbsent(gunTab, key -> new ArrayList<>()).add(gun.stack());
                }
                if (reverseEligible) {
                    attachmentsByGun.computeIfAbsent(gun, key -> new EnumMap<>(AttachmentType.class))
                            .computeIfAbsent(attachment.type(), key -> new ArrayList<>()).add(attachment.stack());
                }
            }
        }

        List<AttachmentQueryEntry> attachmentQueries = new ArrayList<>();
        // Preserve attachment iteration order and the original gun order within each tab.
        for (Attachment attachment : attachments) {
            Map<GunTabType, List<ItemStack>> groups = gunsByAttachment.get(attachment);
            if (groups != null) {
                for (GunTabType type : GunTabType.values()) {
                    List<ItemStack> matches = groups.get(type);
                    if (matches != null) {
                        attachmentQueries.add(new AttachmentQueryEntry(attachment.stack(), matches));
                    }
                }
            }
        }
        List<GunAttachmentQueryEntry> gunQueries = new ArrayList<>();
        for (Gun gun : guns.stream().sorted(GUN_ORDER).collect(Collectors.toList())) {
            Map<AttachmentType, List<ItemStack>> groups = attachmentsByGun.get(gun);
            if (groups != null) {
                for (AttachmentType type : AttachmentType.values()) {
                    List<ItemStack> matches = groups.get(type);
                    if (matches == null) {
                        continue;
                    }
                    int total = matches.size();
                    int pageSize = GunAttachmentQueryEntry.ATTACHMENTS_PER_PAGE;
                    int pages = (total + pageSize - 1) / pageSize;
                    for (int page = 0; page < pages; page++) {
                        gunQueries.add(new GunAttachmentQueryEntry(gun.stack(), type,
                                matches.subList(page * pageSize, Math.min((page + 1) * pageSize, total)),
                                total, page + 1, pages));
                    }
                }
            }
        }
        return new AttachmentQueryData(attachmentQueries, gunQueries, guns.size(), attachments.size(), checks);
    }

    static final class Gun {
        private final ResourceLocation id;
        private final String type;
        private final int sort;
        private final ItemStack stack;

        public Gun(ResourceLocation id, String type, int sort, ItemStack stack) {
            this.id = id;
            this.type = type;
            this.sort = sort;
            this.stack = stack;
        }

        public ResourceLocation id() {
            return id;
        }

        public String type() {
            return type;
        }

        public int sort() {
            return sort;
        }

        public ItemStack stack() {
            return stack;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Gun)) return false;
            Gun that = (Gun) o;
            return java.util.Objects.equals(id, that.id) && java.util.Objects.equals(type, that.type) && java.util.Objects.equals(sort, that.sort) && java.util.Objects.equals(stack, that.stack);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, type, sort, stack);
        }

        @Override
        public String toString() {
            return "Gun[" + "id=" + id + ", " + "type=" + type + ", " + "sort=" + sort + ", " + "stack=" + stack + "]";
        }
 }

    static final class Attachment {
        private final ResourceLocation id;
        private final AttachmentType type;
        private final int sort;
        private final ItemStack stack;

        public Attachment(ResourceLocation id, AttachmentType type, int sort, ItemStack stack) {
            this.id = id;
            this.type = type;
            this.sort = sort;
            this.stack = stack;
        }

        public ResourceLocation id() {
            return id;
        }

        public AttachmentType type() {
            return type;
        }

        public int sort() {
            return sort;
        }

        public ItemStack stack() {
            return stack;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Attachment)) return false;
            Attachment that = (Attachment) o;
            return java.util.Objects.equals(id, that.id) && java.util.Objects.equals(type, that.type) && java.util.Objects.equals(sort, that.sort) && java.util.Objects.equals(stack, that.stack);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, type, sort, stack);
        }

        @Override
        public String toString() {
            return "Attachment[" + "id=" + id + ", " + "type=" + type + ", " + "sort=" + sort + ", " + "stack=" + stack + "]";
        }
 }
}
