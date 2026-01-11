package kuroyale.domain;

/**
 * Represents whether a unit moves on the ground or flies in the air.
 * Ground units cannot attack flying units (unless they have AIR_AND_GROUND target type).
 * Flying units can be attacked by units that target AIR_AND_GROUND.
 */
public enum UnitMovementType {
    GROUND,
    FLYING
}

