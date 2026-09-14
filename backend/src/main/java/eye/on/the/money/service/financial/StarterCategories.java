package eye.on.the.money.service.financial;

import eye.on.the.money.model.financial.CategoryColor;

import java.util.List;

public final class StarterCategories {

    public record Definition(String name, CategoryColor color, List<String> patterns) {
    }

    public static final List<Definition> DEFINITIONS = List.of(
            new Definition("Groceries", CategoryColor.BLUE,
                    List.of("LIDL", "TESCO", "SPAR", "ECOFAMILY", "ABC", "KIFLI", "FARMER CENTER", "EURO MIX")),
            new Definition("Pharmacy", CategoryColor.ORANGE,
                    List.of("GYOGYSZERT", "GYO GYSZERT", "PATIKA", "GYOG")),
            new Definition("Drugstore", CategoryColor.AQUA, List.of("ROSSMANN")),
            new Definition("Fuel", CategoryColor.YELLOW, List.of("MOL")),
            new Definition("Transport", CategoryColor.MAGENTA, List.of("SIMPLEP MAV", "MOBILPARKOLAS")),
            new Definition("Utilities", CategoryColor.GREEN, List.of("MVM NEXT", "SIMPLEP DIJNET")),
            new Definition("Home & DIY", CategoryColor.VIOLET,
                    List.of("FESS", "MEGOLDAS", "MU SZAKI", "MUSZAKI")),
            new Definition("Clothing & Sport", CategoryColor.RED,
                    List.of("KIK", "PEPCO", "DECATHLON", "RUHA", "CARING", "MIXBAZ")),
            new Definition("Post & Parcel", CategoryColor.AQUA, List.of("POSTA", "GLS")),
            new Definition("Entertainment", CategoryColor.MAGENTA, List.of("BATTLE.NET", "BLIZZARD")));

    private StarterCategories() {
    }
}
