package dev.blitical.jigsawDB.entry.selector.condition;

import dev.blitical.jigsawDB.table.Table;

public abstract class Condition<T extends Table<T, ?>> {

    public enum NodeType {
        COMPARISON,
        AND,
        OR,
        NOT
    }

    private final NodeType type;

    protected Condition(NodeType type) {
        this.type = type;
    }

    protected Condition() {
        this.type = NodeType.COMPARISON;
    }

    public NodeType getType() {
        return type;
    }
}
