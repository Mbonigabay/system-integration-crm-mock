package dev.mbonigabay.crm_mock_api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.mbonigabay.crm_mock_api.model.Product;
import dev.mbonigabay.crm_mock_api.service.DataLoadService;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/products")
@Tag(name = "Inventory System", description = "Mock Inventory for product data")
public class InventoryController {

    private final DataLoadService dataLoadService;

    public InventoryController(DataLoadService dataLoadService) {
        this.dataLoadService = dataLoadService;
    }

    @GetMapping
    public List<Product> getProducts() {
        return dataLoadService.getProducts();
    }
}