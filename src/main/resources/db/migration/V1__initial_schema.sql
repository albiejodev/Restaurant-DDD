CREATE TABLE branch (
 id uuid PRIMARY KEY, code varchar(30) NOT NULL UNIQUE, name varchar(160) NOT NULL, city varchar(100) NOT NULL,
 address varchar(300), active boolean NOT NULL DEFAULT true, created_at timestamp with time zone NOT NULL DEFAULT now(), updated_at timestamp with time zone NOT NULL DEFAULT now()
);
CREATE TABLE master_data (
 id uuid PRIMARY KEY, code varchar(60) NOT NULL UNIQUE, name varchar(160) NOT NULL, type varchar(40) NOT NULL,
 version integer NOT NULL DEFAULT 1 CHECK(version > 0), active boolean NOT NULL DEFAULT true,
 created_at timestamp with time zone NOT NULL DEFAULT now(), updated_at timestamp with time zone NOT NULL DEFAULT now()
);
CREATE TABLE branch_master_assignment (
 id uuid PRIMARY KEY, branch_id uuid NOT NULL REFERENCES branch(id), master_id uuid NOT NULL REFERENCES master_data(id),
 enabled boolean NOT NULL DEFAULT true, synced_version integer NOT NULL DEFAULT 0 CHECK(synced_version >= 0), last_synced_at timestamp with time zone,
 UNIQUE(branch_id, master_id)
);
CREATE TABLE menu (
 id uuid PRIMARY KEY, master_id uuid NOT NULL UNIQUE REFERENCES master_data(id), name varchar(160) NOT NULL,
 active boolean NOT NULL DEFAULT true, version integer NOT NULL DEFAULT 1
);
CREATE TABLE menu_category (
 id uuid PRIMARY KEY, menu_id uuid NOT NULL REFERENCES menu(id) ON DELETE CASCADE, name varchar(120) NOT NULL,
 sort_order integer NOT NULL DEFAULT 0, UNIQUE(menu_id,name)
);
CREATE TABLE menu_item (
 id uuid PRIMARY KEY, category_id uuid NOT NULL REFERENCES menu_category(id) ON DELETE CASCADE, name varchar(160) NOT NULL,
 description varchar(500), active boolean NOT NULL DEFAULT true
);
CREATE TABLE menu_variant (
 id uuid PRIMARY KEY, menu_item_id uuid NOT NULL REFERENCES menu_item(id) ON DELETE CASCADE, name varchar(160) NOT NULL,
 size varchar(80), flavour varchar(100), active boolean NOT NULL DEFAULT true, UNIQUE(menu_item_id,name)
);
CREATE TABLE branch_price (
 id uuid PRIMARY KEY, branch_id uuid NOT NULL REFERENCES branch(id), menu_variant_id uuid NOT NULL REFERENCES menu_variant(id),
 customer_type varchar(20) NOT NULL CHECK(customer_type IN ('STAFF','GUEST')), amount numeric(12,2) NOT NULL CHECK(amount >= 0),
 currency char(3) NOT NULL DEFAULT 'INR', active boolean NOT NULL DEFAULT true, created_at timestamp with time zone NOT NULL DEFAULT now(), updated_at timestamp with time zone NOT NULL DEFAULT now(),
 UNIQUE(branch_id,menu_variant_id,customer_type)
);
CREATE TABLE restaurant_order (
 id uuid PRIMARY KEY, branch_id uuid NOT NULL REFERENCES branch(id), customer_type varchar(20) NOT NULL CHECK(customer_type IN ('STAFF','GUEST')),
 status varchar(20) NOT NULL DEFAULT 'PLACED', total_amount numeric(12,2) NOT NULL CHECK(total_amount >= 0), created_at timestamp with time zone NOT NULL DEFAULT now()
);
CREATE TABLE order_item (
 id uuid PRIMARY KEY, order_id uuid NOT NULL REFERENCES restaurant_order(id) ON DELETE CASCADE, menu_variant_id uuid NOT NULL REFERENCES menu_variant(id),
 quantity integer NOT NULL CHECK(quantity > 0), unit_price numeric(12,2) NOT NULL CHECK(unit_price >= 0), line_total numeric(12,2) NOT NULL CHECK(line_total >= 0)
);
CREATE TABLE payment (
 id uuid PRIMARY KEY, branch_id uuid NOT NULL REFERENCES branch(id), order_id uuid NOT NULL REFERENCES restaurant_order(id), amount numeric(12,2) NOT NULL CHECK(amount > 0),
 method varchar(20) NOT NULL CHECK(method IN ('CASH','CARD','UPI')), status varchar(20) NOT NULL CHECK(status IN ('PENDING','COMPLETED','FAILED','REFUNDED')),
 paid_at timestamp with time zone, created_at timestamp with time zone NOT NULL DEFAULT now()
);
CREATE INDEX idx_assignment_branch_enabled ON branch_master_assignment(branch_id,enabled);
CREATE INDEX idx_price_branch_active ON branch_price(branch_id,active);
CREATE INDEX idx_order_branch_date ON restaurant_order(branch_id,created_at);
CREATE INDEX idx_payment_branch_order ON payment(branch_id,order_id);

