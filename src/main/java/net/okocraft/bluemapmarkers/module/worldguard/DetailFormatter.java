package net.okocraft.bluemapmarkers.module.worldguard;

import com.google.common.html.HtmlEscapers;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

class DetailFormatter {

    @Contract("_ -> new")
    static @NotNull DetailFormatter compile(@NotNull String original) {
        var builder = new StringBuilder();
        var parts = new ArrayList<Part>();
        for (int i = 0, l = original.length(); i < l; i++) {
            char c = original.charAt(i);

            if (c == '{') {
                parts.add(createPart(builder.toString()));
                builder.setLength(0);
                builder.append(c);
            } else if (c == '}') {
                builder.append(c);
                parts.add(createPart(builder.toString()));
                builder.setLength(0);
            } else {
                builder.append(c);
            }
        }
        return new DetailFormatter(parts);
    }

    private static Part createPart(@NotNull String content) {
        return switch (content) {
            case "{region_id}" -> RegionPlaceholder.of((builder, region) -> builder.append(region.getId()));
            case "{region_displayname}" ->
                    RegionPlaceholder.of((builder, region) -> builder.append(getEffectiveDisplayName(region)));
            case "{region_owners}" ->
                    RegionPlaceholder.of(((builder, region) -> appendMembers(builder, region.getOwners())));
            case "{region_members}" ->
                    RegionPlaceholder.of(((builder, region) -> appendMembers(builder, region.getMembers())));
            case "{region_parents}" -> RegionPlaceholder.of(DetailFormatter::appendParents);
            default -> new StringPart(content);
        };
    }

    private static String getEffectiveDisplayName(@NotNull ProtectedRegion region) {
        var flag = region.getFlag(WorldGuardRenderer.DISPLAY_FLAG);
        return flag != null ? HtmlEscapers.htmlEscaper().escape(flag) : region.getId();
    }

    private static void appendMembers(@NotNull StringBuilder builder, @NotNull DefaultDomain domain) {
        boolean first = true;

        for (var uuid : domain.getUniqueIds()) {
            var name = Bukkit.getOfflinePlayer(uuid).getName();
            if (first) {
                first = false;
            } else {
                builder.append(", ");
            }
            builder.append(name != null && !name.isEmpty() ? name : uuid.toString());
        }
    }

    private static void appendParents(@NotNull StringBuilder builder, @NotNull ProtectedRegion region) {
        var parent = region.getParent();

        if (parent == null) {
            return;
        }

        builder.append(parent.getId());
        parent = parent.getParent();

        while (parent != null) {
            builder.append(" - ").append(parent.getId());
            parent = parent.getParent();
        }
    }

    private final List<Part> parts;

    private DetailFormatter(@NotNull List<Part> parts) {
        this.parts = parts;
    }

    @NotNull String format(@NotNull ProtectedRegion region) {
        var builder = new StringBuilder();

        this.parts.forEach(part -> {
            if (part instanceof StringPart(String content)) {
                builder.append(content);
            } else if (part instanceof RegionPlaceholder regionPlaceholder) {
                regionPlaceholder.appendValue(builder, region);
            }
        });

        return builder.toString();
    }

    private sealed interface Part permits StringPart, RegionPlaceholder {
    }

    private record StringPart(@NotNull String content) implements Part {
    }

    private non-sealed interface RegionPlaceholder extends Part {

        static RegionPlaceholder of(@NotNull RegionPlaceholder placeholder) {
            return placeholder;
        }

        void appendValue(@NotNull StringBuilder builder, @NotNull ProtectedRegion region);
    }
}
