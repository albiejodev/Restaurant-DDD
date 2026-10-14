package com.restaurant.ddd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:restaurant-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class RestaurantApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;

    @BeforeEach
    void cleanDatabase() {
        db.update("delete from payment");
        db.update("delete from order_item");
        db.update("delete from restaurant_order");
        db.update("delete from branch_price");
        db.update("delete from branch_master_assignment");
        db.update("delete from menu_variant");
        db.update("delete from menu_item");
        db.update("delete from menu_category");
        db.update("delete from menu");
        db.update("delete from master_data");
        db.update("delete from branch");
    }

    @Test
    void branchCodeMustBeUnique() throws Exception {
        createBranch("TVM", "Trivandrum");
        mvc.perform(post("/api/central/branches").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"TVM\",\"name\":\"Duplicate\",\"city\":\"Trivandrum\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void masterAssignmentIsBranchSpecificAndDisablingItRemovesBranchAvailability() throws Exception {
        UUID tvm=createBranch("TVM", "Trivandrum"), kochi=createBranch("KOC", "Kochi");
        UUID master=createMaster("MENU-PIZZA", "Pizza");
        enable(tvm, master);

        JsonNode tvmMasters=fetch("/api/central/branches/"+tvm+"/masters");
        JsonNode kochiMasters=fetch("/api/central/branches/"+kochi+"/masters");
        assertThat(tvmMasters.toString()).contains("Pizza");
        assertThat(kochiMasters.size()).isZero();

        mvc.perform(delete("/api/central/branches/"+tvm+"/masters/"+master)).andExpect(status().isNoContent());
        assertThat(fetch("/api/central/branches/"+tvm+"/masters").size()).isZero();
    }

    @Test
    void pricesAreIndependentByBranchAndCustomerType() throws Exception {
        UUID tvm=createBranch("TVM", "Trivandrum"), kochi=createBranch("KOC", "Kochi");
        UUID master=createMaster("MENU-BIRYANI", "Biryani");
        enable(tvm,master); enable(kochi,master);
        UUID variant=createVariant(master,"Biryani","Regular");

        createPrice(tvm,variant,"STAFF","180.00"); createPrice(tvm,variant,"GUEST","200.00");
        createPrice(kochi,variant,"STAFF","130.00"); createPrice(kochi,variant,"GUEST","150.00");
        assertThat(priceAmount(tvm,variant,"STAFF")).isEqualByComparingTo("180.00");
        assertThat(priceAmount(tvm,variant,"GUEST")).isEqualByComparingTo("200.00");
        assertThat(priceAmount(kochi,variant,"STAFF")).isEqualByComparingTo("130.00");
        assertThat(priceAmount(kochi,variant,"GUEST")).isEqualByComparingTo("150.00");
    }

    @Test
    void branchMenuOnlyIncludesVariantsFromEnabledMasters() throws Exception {
        UUID tvm=createBranch("TVM", "Trivandrum"), kochi=createBranch("KOC", "Kochi");
        UUID master=createMaster("MENU-PIZZA", "Pizza");
        enable(tvm,master);
        UUID variant=createVariant(master,"Pizza","Medium Pepperoni");

        JsonNode tvmMenu=fetch("/api/branches/"+tvm+"/menu");
        JsonNode kochiMenu=fetch("/api/branches/"+kochi+"/menu");
        assertThat(tvmMenu.toString()).contains(variant.toString());
        assertThat(kochiMenu.size()).isZero();
    }

    @Test
    void orderKeepsPurchaseTimePriceAfterBranchPriceChanges() throws Exception {
        UUID branch=createBranch("TVM", "Trivandrum"), master=createMaster("MENU-PIZZA", "Pizza");
        enable(branch,master); UUID variant=createVariant(master,"Pizza","Medium Pepperoni");
        UUID price=createPrice(branch,variant,"GUEST","220.00");

        JsonNode order=postJson("/api/branches/"+branch+"/orders",
                "{\"customerType\":\"GUEST\",\"items\":[{\"menuVariantId\":\"%s\",\"quantity\":2}]}".formatted(variant));
        UUID orderId=UUID.fromString(order.path("id").asText());
        mvc.perform(put("/api/central/branches/"+branch+"/prices/"+price).contentType(MediaType.APPLICATION_JSON)
                .content(priceBody(variant,"GUEST","250.00"))).andExpect(status().isOk());

        JsonNode historic=fetch("/api/branches/"+branch+"/orders/"+orderId);
        assertThat(new BigDecimal(historic.path("items").get(0).path("unit_price").asText())).isEqualByComparingTo("220.00");
        assertThat(new BigDecimal(historic.path("total_amount").asText())).isEqualByComparingTo("440.00");
        assertThat(priceAmount(branch,variant,"GUEST")).isEqualByComparingTo("250.00");
    }

    @Test
    void branchCannotOrderVariantWithoutAnEnabledMasterAssignment() throws Exception {
        UUID branch=createBranch("TVM", "Trivandrum"), master=createMaster("MENU-PIZZA", "Pizza");
        UUID variant=createVariant(master,"Pizza","Medium Pepperoni");

        mvc.perform(post("/api/branches/"+branch+"/orders").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerType\":\"GUEST\",\"items\":[{\"menuVariantId\":\"%s\",\"quantity\":1}]}".formatted(variant)))
                .andExpect(status().isConflict());
    }

    @Test
    void aBranchCannotReadAnotherBranchesOrdersOrPayForThem() throws Exception {
        UUID tvm=createBranch("TVM", "Trivandrum"), kochi=createBranch("KOC", "Kochi");
        UUID master=createMaster("MENU-TEA", "Tea"); enable(tvm,master);
        UUID variant=createVariant(master,"Tea","Cup");createPrice(tvm,variant,"GUEST","20.00");
        JsonNode order=postJson("/api/branches/"+tvm+"/orders",
                "{\"customerType\":\"GUEST\",\"items\":[{\"menuVariantId\":\"%s\",\"quantity\":1}]}".formatted(variant));
        String orderId=order.path("id").asText();

        assertThat(fetch("/api/branches/"+kochi+"/orders").size()).isZero();
        mvc.perform(get("/api/branches/"+kochi+"/orders/"+orderId)).andExpect(status().isNotFound());
        mvc.perform(post("/api/branches/"+kochi+"/payments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":\"%s\",\"amount\":20.00,\"method\":\"CASH\"}".formatted(orderId)))
                .andExpect(status().isNotFound());

        postJson("/api/branches/"+tvm+"/payments",
                "{\"orderId\":\"%s\",\"amount\":20.00,\"method\":\"CASH\",\"status\":\"COMPLETED\"}".formatted(orderId));
        assertThat(fetch("/api/branches/"+tvm+"/payments").size()).isEqualTo(1);
        assertThat(fetch("/api/branches/"+kochi+"/payments").size()).isZero();
        JsonNode centralPayments=fetch("/api/central/payments");
        assertThat(centralPayments.size()).isEqualTo(1);
        assertThat(centralPayments.get(0).path("branch_code").asText()).isEqualTo("TVM");
        mvc.perform(get("/api/central/payments").header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    private UUID createBranch(String code,String city) throws Exception {
        return UUID.fromString(postJson("/api/central/branches", "{\"code\":\"%s\",\"name\":\"%s Branch\",\"city\":\"%s\"}".formatted(code,city,city)).path("id").asText());
    }
    private UUID createMaster(String code,String name) throws Exception {
        return UUID.fromString(postJson("/api/central/masters", "{\"code\":\"%s\",\"name\":\"%s\",\"type\":\"MENU\"}".formatted(code,name)).path("id").asText());
    }
    private void enable(UUID branch,UUID master) throws Exception {
        mvc.perform(post("/api/central/branches/"+branch+"/masters/"+master+"/enable")).andExpect(status().isOk());
    }
    private UUID createVariant(UUID master,String categoryName,String variantName) throws Exception {
        UUID menu=UUID.fromString(postJson("/api/central/menus", "{\"masterId\":\"%s\",\"name\":\"%s Menu\"}".formatted(master,categoryName)).path("id").asText());
        UUID category=UUID.fromString(postJson("/api/central/menus/"+menu+"/categories", "{\"name\":\"%s\"}".formatted(categoryName)).path("id").asText());
        UUID item=UUID.fromString(postJson("/api/central/categories/"+category+"/items", "{\"name\":\"%s\"}".formatted(categoryName)).path("id").asText());
        return UUID.fromString(postJson("/api/central/items/"+item+"/variants", "{\"name\":\"%s\"}".formatted(variantName)).path("id").asText());
    }
    private UUID createPrice(UUID branch,UUID variant,String customer,String amount) throws Exception {
        return UUID.fromString(postJson("/api/central/branches/"+branch+"/prices",priceBody(variant,customer,amount)).path("id").asText());
    }
    private String priceBody(UUID variant,String customer,String amount) {
        return "{\"menuVariantId\":\"%s\",\"customerType\":\"%s\",\"amount\":%s,\"currency\":\"INR\"}".formatted(variant,customer,amount);
    }
    private BigDecimal priceAmount(UUID branch,UUID variant,String customer) throws Exception {
        for(JsonNode p:fetch("/api/branches/"+branch+"/prices"))
            if(p.path("menu_variant_id").asText().equals(variant.toString())&&p.path("customer_type").asText().equals(customer)) return new BigDecimal(p.path("amount").asText());
        throw new AssertionError("Price not returned for "+customer);
    }
    private JsonNode fetch(String path) throws Exception {
        MvcResult result=mvc.perform(get(path)).andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }
    private JsonNode postJson(String path,String body) throws Exception {
        MvcResult result=mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }
}



