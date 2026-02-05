package dev.mbonigabay.crm_mock_api.model;

import java.util.List;

public record DataWrapper(Crm crm, Inventory inventory) {
    public record Crm(List<Customer> customers) {
    }

    public record Inventory(List<Product> products) {
    }
}
