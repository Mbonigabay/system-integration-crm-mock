package dev.mbonigabay.crm_mock_api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.mbonigabay.crm_mock_api.model.Customer;
import dev.mbonigabay.crm_mock_api.service.DataLoadService;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/customers")
@Tag(name = "CRM System", description = "Mock CRM for customer data")
public class CustomerController {

    private final DataLoadService dataLoadService;

    public CustomerController(DataLoadService dataLoadService) {
        this.dataLoadService = dataLoadService;
    }

    @GetMapping
    public List<Customer> getAllCustomers() {
        return dataLoadService.getCustomers();
    }
}