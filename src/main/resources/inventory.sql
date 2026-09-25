CREATE TABLE inventory (
    id UUID PRIMARY KEY,

    product_id UUID NOT NULL UNIQUE,

    quantity INTEGER NOT NULL DEFAULT 0,

    reserved_quantity INTEGER NOT NULL DEFAULT 0,

    available_quantity INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_inventory_quantity
        CHECK (quantity >= 0),

    CONSTRAINT chk_inventory_reserved_quantity
        CHECK (reserved_quantity >= 0),

    CONSTRAINT chk_inventory_available_quantity
        CHECK (available_quantity >= 0),

    CONSTRAINT chk_inventory_reserved_not_greater
        CHECK (reserved_quantity <= quantity)
);

CREATE INDEX idx_inventory_product_id
    ON inventory(product_id);

CREATE INDEX idx_inventory_created_at
    ON inventory(created_at);