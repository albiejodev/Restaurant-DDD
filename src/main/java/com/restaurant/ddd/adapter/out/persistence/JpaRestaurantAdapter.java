package com.restaurant.ddd.adapter.out.persistence;

import com.restaurant.ddd.application.command.RestaurantCommands.*;
import com.restaurant.ddd.application.port.out.RestaurantPersistencePort;
import com.restaurant.ddd.adapter.out.persistence.entity.BranchJpaEntity;
import com.restaurant.ddd.adapter.out.persistence.repository.SpringDataBranchRepository;
import com.restaurant.ddd.domain.order.OrderItem;
import com.restaurant.ddd.domain.order.RestaurantOrder;
import com.restaurant.ddd.domain.branch.Branch;
import com.restaurant.ddd.domain.pricing.BranchPrice;
import com.restaurant.ddd.domain.shared.CustomerType;
import com.restaurant.ddd.domain.payment.PaymentStatus;
import org.springframework.stereotype.Repository;
import jakarta.validation.constraints.*;

import org.springframework.http.HttpStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.hibernate.query.NativeQuery;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.*;

@Repository
@Transactional
public class JpaRestaurantAdapter implements RestaurantPersistencePort {
    private final SpringDataBranchRepository branchRepository;

    public JpaRestaurantAdapter(SpringDataBranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    @PersistenceContext
    private EntityManager entityManager;

    public Map<String, Object> createBranch(Branch branch) {
        branchRepository.save(BranchJpaEntity.from(branch));
        return branch(branch.id());
    }
    public Map<String, Object> updateBranch(UUID id, Branch branch) {
        BranchJpaEntity entity = branchRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
        entity.updateFrom(branch);
        branchRepository.save(entity);
        return branch(id);
    }
    public List<Map<String, Object>> branches() {
        return queryForList("select * from branch order by name");
    }
    public Map<String, Object> getBranch(UUID id) {
        return branch(id);
    }

    private Map<String, Object> branch(UUID id) {
        return one("select * from branch where id=?", id, "Branch");
    }
    public Map<String, Object> createMaster(MasterInput in) {
        UUID id = UUID.randomUUID();
        executeUpdate("insert into master_data(id,code,name,type) values(?,?,?,?)", id, in.code(), in.name(), in.type());
        return one("select * from master_data where id=?", id, "Master");
    }
    public List<Map<String, Object>> masters() {
        return queryForList("select * from master_data order by name");
    }
    public Map<String, Object> getMaster(UUID id) {
        return one("select * from master_data where id=?", id, "Master");
    }
    @Transactional
    public Map<String, Object> enable(UUID branchId, UUID masterId) {
        branch(branchId);
        master(masterId);
        Integer existing = queryForObject(
                "select count(*) from branch_master_assignment where branch_id=? and master_id=?",
                Integer.class, branchId, masterId);
        if (existing != null && existing > 0) {
            executeUpdate("update branch_master_assignment set enabled=true where branch_id=? and master_id=?", branchId, masterId);
        } else {
            executeUpdate("insert into branch_master_assignment(id,branch_id,master_id,enabled) values(?,?,?,true)",
                    UUID.randomUUID(), branchId, masterId);
        }
        return one("select a.* from branch_master_assignment a where branch_id=? and master_id=?", branchId, masterId,
                "Assignment");
    }
    public void disable(UUID branchId, UUID masterId) {
        if (executeUpdate("update branch_master_assignment set enabled=false where branch_id=? and master_id=?", branchId,
                masterId) == 0)
            missing("Assignment");
    }
    public List<Map<String, Object>> assigned(UUID branchId) {
        branch(branchId);
        return queryForList(
                "select m.*,a.enabled from master_data m join branch_master_assignment a on a.master_id=m.id where a.branch_id=? and a.enabled=true and m.active=true order by m.name",
                branchId);
    }
    public Map<String, Object> createMenu(MenuInput in) {
        master(in.masterId());
        UUID id = UUID.randomUUID();
        executeUpdate("insert into menu(id,master_id,name) values(?,?,?)", id, in.masterId(), in.name());
        return one("select * from menu where id=?", id, "Menu");
    }
    public Map<String, Object> category(UUID menuId, CategoryInput in) {
        one("select * from menu where id=?", menuId, "Menu");
        UUID id = UUID.randomUUID();
        executeUpdate("insert into menu_category(id,menu_id,name,sort_order) values(?,?,?,?)", id, menuId, in.name(),
                in.sortOrder() == null ? 0 : in.sortOrder());
        return one("select * from menu_category where id=?", id, "Category");
    }
    public Map<String, Object> item(UUID categoryId, ItemInput in) {
        one("select * from menu_category where id=?", categoryId, "Category");
        UUID id = UUID.randomUUID();
        executeUpdate("insert into menu_item(id,category_id,name,description) values(?,?,?,?)", id, categoryId, in.name(),
                in.description());
        return one("select * from menu_item where id=?", id, "Menu item");
    }
    public Map<String, Object> variant(UUID itemId, VariantInput in) {
        one("select * from menu_item where id=?", itemId, "Menu item");
        UUID id = UUID.randomUUID();
        executeUpdate("insert into menu_variant(id,menu_item_id,name,size,flavour) values(?,?,?,?,?)", id, itemId,
                in.name(), in.size(), in.flavour());
        return one("select * from menu_variant where id=?", id, "Menu variant");
    }
    public List<Map<String, Object>> menu(UUID branchId) {
        branch(branchId);
        return queryForList(
                "select m.id menu_id,m.name menu_name,c.id category_id,c.name category_name,i.id item_id,i.name item_name,v.id variant_id,v.name variant_name,v.size,v.flavour from branch_master_assignment a join menu m on m.master_id=a.master_id join menu_category c on c.menu_id=m.id join menu_item i on i.category_id=c.id join menu_variant v on v.menu_item_id=i.id where a.branch_id=? and a.enabled=true and m.active=true and i.active=true and v.active=true order by m.name,c.sort_order,c.name,i.name,v.name",
                branchId);
    }
    public Map<String, Object> setPrice(UUID branchId, BranchPrice price) {
        branch(branchId);
        requireEnabledVariant(branchId, price.menuVariantId());
        executeUpdate(
                "insert into branch_price(id,branch_id,menu_variant_id,customer_type,amount,currency) values(?,?,?,?,?,?)",
                price.id(), branchId, price.menuVariantId(), price.customerType().name(), price.amount(), price.currency());
        return one("select * from branch_price where id=?", price.id(), "Price");
    }
    public Map<String, Object> updatePrice(UUID branchId, UUID priceId,
            BranchPrice price) {
        requireEnabledVariant(branchId, price.menuVariantId());
        if (executeUpdate(
                "update branch_price set menu_variant_id=?,customer_type=?,amount=?,currency=?,updated_at=now() where id=? and branch_id=?",
                price.menuVariantId(), price.customerType().name(), price.amount(),
                price.currency(), priceId, branchId) == 0)
            missing("Price");
        return one("select * from branch_price where id=? and branch_id=?", priceId, branchId, "Price");
    }
    public List<Map<String, Object>> prices(UUID branchId) {
        branch(branchId);
        return queryForList(
                "select p.* from branch_price p join menu_variant v on v.id=p.menu_variant_id join menu_item i on i.id=v.menu_item_id join menu_category c on c.id=i.category_id join menu m on m.id=c.menu_id join branch_master_assignment a on a.master_id=m.master_id and a.branch_id=p.branch_id and a.enabled=true where p.branch_id=? and p.active=true and m.active=true and i.active=true and v.active=true order by p.menu_variant_id,p.customer_type",
                branchId);
    }
    public boolean branchExists(UUID branchId) {
        return branchRepository.existsById(branchId);
    }

    public boolean variantEnabled(UUID branchId, UUID variantId) {
        Integer count = queryForObject(
                "select count(*) from menu_variant v join menu_item i on i.id=v.menu_item_id join menu_category c on c.id=i.category_id join menu m on m.id=c.menu_id join branch_master_assignment a on a.master_id=m.master_id where v.id=? and a.branch_id=? and a.enabled=true and m.active=true and i.active=true and v.active=true",
                Integer.class, variantId, branchId);
        return count != null && count > 0;
    }

    public BigDecimal currentPrice(UUID branchId, UUID variantId, CustomerType customerType) {
        Map<String,Object> row = one(
                "select amount from branch_price where branch_id=? and menu_variant_id=? and customer_type=? and active=true",
                branchId, variantId, customerType.name(), "Active price");
        return (BigDecimal) row.get("amount");
    }

    @Transactional
    public void saveOrder(RestaurantOrder order) {
        executeUpdate(
                "insert into restaurant_order(id,branch_id,customer_type,status,total_amount) values(?,?,?,'PLACED',?)",
                order.id(), order.branchId(), order.customerType().name(), order.totalAmount());
        for (OrderItem line : order.items())
            executeUpdate(
                    "insert into order_item(id,order_id,menu_variant_id,quantity,unit_price,line_total) values(?,?,?,?,?,?)",
                    UUID.randomUUID(), order.id(), line.menuVariantId(), line.quantity(), line.unitPrice(),
                    line.lineTotal());
    }
    public List<Map<String, Object>> orders(UUID branchId) {
        branch(branchId);
        return queryForList("select * from restaurant_order where branch_id=? order by created_at desc", branchId);
    }
    public Map<String, Object> orderDetails(UUID branchId, UUID orderId) {
        Map<String, Object> o = one("select * from restaurant_order where branch_id=? and id=?", branchId, orderId,
                "Order");
        o.put("items", queryForList("select * from order_item where order_id=?", orderId));
        return o;
    }
    public boolean orderBelongsToBranch(UUID branchId, UUID orderId) {
        Integer count = queryForObject("select count(*) from restaurant_order where branch_id=? and id=?", Integer.class, branchId, orderId);
        return count != null && count > 0;
    }

    @Transactional
    public Map<String, Object> savePayment(UUID branchId, PaymentInput in) {
        UUID id = UUID.randomUUID();
        PaymentStatus status = in.status() == null ? PaymentStatus.COMPLETED : in.status();
        executeUpdate(
                "insert into payment(id,branch_id,order_id,amount,method,status,paid_at) values(?,?,?,?,?,?,case when ?='COMPLETED' then now() else null end)",
                id, branchId, in.orderId(), in.amount(), in.method().name(), status.name(), status.name());
        return one("select * from payment where id=? and branch_id=?", id, branchId, "Payment");
    }
    public List<Map<String, Object>> payments(UUID branchId) {
        branch(branchId);
        return queryForList("select * from payment where branch_id=? order by created_at desc", branchId);
    }
    public List<Map<String, Object>> allPayments() {
        return queryForList("select p.*,b.code branch_code,b.name branch_name from payment p join branch b on b.id=p.branch_id order by p.created_at desc");
    }
    public Map<String, Object> report(UUID branchId) {
        branch(branchId);
        return reportData(branchId);
    }
    public List<Map<String, Object>> reports() {
        return queryForList("select id branch_id,code,name,city from branch order by name").stream().map(b -> {
            UUID id = (UUID) b.get("branch_id");
            Map<String, Object> r = reportData(id);
            r.put("branch", b);
            return r;
        }).toList();
    }

    private Map<String, Object> reportData(UUID id) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("branchId", id);
        r.put("totalOrders",
                queryForObject("select count(*) from restaurant_order where branch_id=?", Long.class, id));
        r.put("totalSales", queryForObject(
                "select coalesce(sum(total_amount),0) from restaurant_order where branch_id=?", BigDecimal.class, id));
        r.put("totalPayments", queryForObject("select coalesce(sum(amount),0) from payment where branch_id=?",
                BigDecimal.class, id));
        r.put("paymentTotalsByStatus", queryForList(
                "select status,count(*) count,coalesce(sum(amount),0) amount from payment where branch_id=? group by status",
                id));
        r.put("salesByCustomerType", queryForList(
                "select customer_type,count(*) orders,coalesce(sum(total_amount),0) sales from restaurant_order where branch_id=? group by customer_type",
                id));
        r.put("salesByMenuItem", queryForList(
                "select i.id menu_item_id,i.name,sum(oi.quantity) quantity,sum(oi.line_total) sales from restaurant_order o join order_item oi on oi.order_id=o.id join menu_variant v on v.id=oi.menu_variant_id join menu_item i on i.id=v.menu_item_id where o.branch_id=? group by i.id,i.name",
                id));
        return r;
    }

    private void requireEnabledVariant(UUID branchId, UUID variantId) {
        Integer n = queryForObject(
                "select count(*) from menu_variant v join menu_item i on i.id=v.menu_item_id join menu_category c on c.id=i.category_id join menu m on m.id=c.menu_id join branch_master_assignment a on a.master_id=m.master_id where v.id=? and a.branch_id=? and a.enabled=true and m.active=true and i.active=true and v.active=true",
                Integer.class, variantId, branchId);
        if (n == null || n == 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Menu variant is not enabled for this branch");
    }

    private Map<String, Object> master(UUID id) {
        return one("select * from master_data where id=?", id, "Master");
    }

    private Map<String, Object> one(String sql, Object a, Object b, Object c, String label) {
        List<Map<String, Object>> rows = queryForList(sql, a, b, c);
        if (rows.isEmpty())
            missing(label);
        return rows.getFirst();
    }

    private Map<String, Object> one(String sql, Object a, Object b, String label) {
        List<Map<String, Object>> rows = queryForList(sql, a, b);
        if (rows.isEmpty())
            missing(label);
        return rows.getFirst();
    }

    private Map<String, Object> one(String sql, Object a, String label) {
        List<Map<String, Object>> rows = queryForList(sql, a);
        if (rows.isEmpty())
            missing(label);
        return rows.getFirst();
    }

    private void missing(String what) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, what + " not found");
    }

    private int executeUpdate(String sql, Object... parameters) {
        Query query = entityManager.createNativeQuery(sql);
        bind(query, parameters);
        return query.executeUpdate();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> queryForList(String sql, Object... parameters) {
        NativeQuery<Object[]> query = entityManager.createNativeQuery(sql).unwrap(NativeQuery.class);
        query.setTupleTransformer((tuple, aliases) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 0; i < aliases.length; i++) {
                Object value = tuple[i];
                if (value instanceof byte[] bytes && bytes.length == 16) {
                    ByteBuffer buffer = ByteBuffer.wrap(bytes);
                    value = new UUID(buffer.getLong(), buffer.getLong());
                }
                row.put(aliases[i].toLowerCase(Locale.ROOT), value);
            }
            return row;
        });
        bind(query, parameters);
        return (List<Map<String, Object>>) (List<?>) query.getResultList();
    }

    private <T> T queryForObject(String sql, Class<T> resultType, Object... parameters) {
        List<Map<String, Object>> rows = queryForList(sql, parameters);
        if (rows.isEmpty()) return null;
        Object value = rows.getFirst().values().iterator().next();
        if (value == null || resultType.isInstance(value)) return resultType.cast(value);
        if (Number.class.isAssignableFrom(resultType) && value instanceof Number number) {
            if (resultType == Integer.class) return resultType.cast(number.intValue());
            if (resultType == Long.class) return resultType.cast(number.longValue());
        }
        return resultType.cast(value);
    }

    private void bind(Query query, Object[] parameters) {
        for (int i = 0; i < parameters.length; i++) query.setParameter(i + 1, parameters[i]);
    }
}




