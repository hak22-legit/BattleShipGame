package com.battleship.model;

/** ship classes with their fixed length in cells. */
public enum ShipType {
    PATROL_BOAT(2, "destroyer"),
    DESTROYER(2, "destroyer"),
    SUBMARINE(3, "submarine"),
    CRUISER(3, "cruiser"),
    BATTLESHIP(4, "battleship"),
    CARRIER(5, "carrier"),
    FREIGHTER(3, "cruiser");

    private final int size;
    private final String assetName;

    ShipType(int size, String assetName) {
        this.size = size;
        this.assetName = assetName;
    }

    public int getSize() {
        return size;
    }

    public String getAssetName() {
        return assetName;
    }
}

