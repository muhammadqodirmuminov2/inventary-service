CREATE TABLE stock_items (
                             id          UUID        PRIMARY KEY,
                             company_id  UUID        NOT NULL,
                             product_id  UUID        NOT NULL,
                             quantity    INTEGER     NOT NULL DEFAULT 0 CHECK (quantity >= 0),
                             reserved    INTEGER     NOT NULL DEFAULT 0 CHECK (reserved >= 0 AND reserved <= quantity),
                             updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                             version     BIGINT      NOT NULL DEFAULT 0,

                             CONSTRAINT uk_stock_company_product UNIQUE (company_id, product_id)
);