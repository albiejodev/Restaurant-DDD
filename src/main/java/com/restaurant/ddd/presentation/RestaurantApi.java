package com.restaurant.ddd.presentation;

import jakarta.validation.Valid;
import com.restaurant.ddd.application.port.in.RestaurantUseCases;
import com.restaurant.ddd.application.command.RestaurantCommands.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
class RestaurantApi {
    private final RestaurantUseCases service;

    RestaurantApi(RestaurantUseCases service) { this.service = service; }

    @PostMapping("/central/branches")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String,Object> createBranch(@Valid @RequestBody BranchInput in) { return service.createBranch(in); }

    @PutMapping("/central/branches/{id}")
    Map<String,Object> updateBranch(@PathVariable UUID id, @Valid @RequestBody BranchInput in) { return service.updateBranch(id,in); }

    @GetMapping("/central/branches")
    List<Map<String,Object>> branches() { return service.branches(); }

    @GetMapping("/central/branches/{id}")
    Map<String,Object> getBranch(@PathVariable UUID id) { return service.getBranch(id); }

    @PostMapping("/central/masters")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String,Object> createMaster(@Valid @RequestBody MasterInput in) { return service.createMaster(in); }

    @PostMapping("/central/master-menus")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String,Object> createMasterMenu(@Valid @RequestBody MasterMenuInput in) { return service.createMasterMenu(in); }

    @GetMapping("/central/masters")
    List<Map<String,Object>> masters() { return service.masters(); }

    @GetMapping("/central/masters/{id}")
    Map<String,Object> getMaster(@PathVariable UUID id) { return service.getMaster(id); }

    @PostMapping("/central/branches/{branchId}/masters/{masterId}/enable")
    Map<String,Object> enable(@PathVariable UUID branchId,@PathVariable UUID masterId) { return service.enable(branchId,masterId); }

    @DeleteMapping("/central/branches/{branchId}/masters/{masterId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void disable(@PathVariable UUID branchId,@PathVariable UUID masterId) { service.disable(branchId,masterId); }

    @GetMapping("/central/branches/{branchId}/masters")
    List<Map<String,Object>> assigned(@PathVariable UUID branchId) { return service.assigned(branchId); }

    @PostMapping("/central/menus")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String,Object> createMenu(@Valid @RequestBody MenuInput in) { return service.createMenu(in); }

    @PostMapping("/central/menus/{menuId}/categories")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String,Object> category(@PathVariable UUID menuId,@Valid @RequestBody CategoryInput in) { return service.category(menuId,in); }

    @PostMapping("/central/categories/{categoryId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String,Object> item(@PathVariable UUID categoryId,@Valid @RequestBody ItemInput in) { return service.item(categoryId,in); }

    @PostMapping("/central/items/{itemId}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String,Object> variant(@PathVariable UUID itemId,@Valid @RequestBody VariantInput in) { return service.variant(itemId,in); }

    @GetMapping("/branches/{branchId}/menu")
    List<Map<String,Object>> menu(@PathVariable UUID branchId) { return service.menu(branchId); }

    @PostMapping("/central/branches/{branchId}/prices")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String,Object> setPrice(@PathVariable UUID branchId,@Valid @RequestBody PriceInput in) { return service.setPrice(branchId,in); }

    @PutMapping("/central/branches/{branchId}/prices/{priceId}")
    Map<String,Object> updatePrice(@PathVariable UUID branchId,@PathVariable UUID priceId,@Valid @RequestBody PriceInput in) { return service.updatePrice(branchId,priceId,in); }

    @GetMapping("/branches/{branchId}/prices")
    List<Map<String,Object>> prices(@PathVariable UUID branchId) { return service.prices(branchId); }

    @PostMapping("/branches/{branchId}/orders")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String,Object> order(@PathVariable UUID branchId,@Valid @RequestBody OrderInput in) { return service.order(branchId,in); }

    @GetMapping("/branches/{branchId}/orders")
    List<Map<String,Object>> orders(@PathVariable UUID branchId) { return service.orders(branchId); }

    @GetMapping("/branches/{branchId}/orders/{orderId}")
    Map<String,Object> getOrder(@PathVariable UUID branchId,@PathVariable UUID orderId) { return service.getOrder(branchId,orderId); }

    @PostMapping("/branches/{branchId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String,Object> payment(@PathVariable UUID branchId,@Valid @RequestBody PaymentInput in) { return service.payment(branchId,in); }

    @GetMapping("/branches/{branchId}/payments")
    List<Map<String,Object>> payments(@PathVariable UUID branchId) { return service.payments(branchId); }

    @GetMapping("/central/payments")
    List<Map<String,Object>> centralPayments() { return service.centralPayments(); }

    @GetMapping("/central/reports/branches/{branchId}")
    Map<String,Object> report(@PathVariable UUID branchId) { return service.report(branchId); }

    @GetMapping("/central/reports/branches")
    List<Map<String,Object>> reports() { return service.reports(); }
}

