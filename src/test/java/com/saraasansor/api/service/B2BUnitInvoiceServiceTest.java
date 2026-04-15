package com.saraasansor.api.service;

import com.saraasansor.api.dto.InvoiceLineRequest;
import com.saraasansor.api.dto.InvoiceResponse;
import com.saraasansor.api.dto.PurchaseInvoiceCreateRequest;
import com.saraasansor.api.dto.SalesInvoiceCreateRequest;
import com.saraasansor.api.model.B2BUnit;
import com.saraasansor.api.model.B2BUnitInvoice;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.Facility;
import com.saraasansor.api.model.Part;
import com.saraasansor.api.model.User;
import com.saraasansor.api.model.Warehouse;
import com.saraasansor.api.repository.B2BUnitInvoiceRepository;
import com.saraasansor.api.repository.B2BUnitRepository;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.repository.FacilityRepository;
import com.saraasansor.api.repository.PartRepository;
import com.saraasansor.api.repository.UserRepository;
import com.saraasansor.api.repository.WarehouseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class B2BUnitInvoiceServiceTest {

    @Mock
    private B2BUnitInvoiceRepository invoiceRepository;

    @Mock
    private B2BUnitRepository b2bUnitRepository;

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private ElevatorRepository elevatorRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private PartRepository partRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private B2BUnitTransactionService transactionService;

    private B2BUnitInvoiceService service;

    @BeforeEach
    void setUp() {
        service = new B2BUnitInvoiceService(
                invoiceRepository,
                b2bUnitRepository,
                facilityRepository,
                elevatorRepository,
                warehouseRepository,
                partRepository,
                userRepository,
                transactionService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPurchaseInvoiceShouldSucceedAndCalculateTotals() {
        authenticateAs("staff-user");
        when(userRepository.findByUsername("staff-user")).thenReturn(Optional.of(staffUser("staff-user")));
        when(b2bUnitRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(activeB2BUnit(10L)));
        when(warehouseRepository.findByIdAndActiveTrue(7L)).thenReturn(Optional.of(warehouse(7L)));
        when(partRepository.findByIdAndActiveTrue(1001L)).thenReturn(Optional.of(part(1001L, "Motor")));
        when(invoiceRepository.save(any(B2BUnitInvoice.class))).thenAnswer(invocation -> {
            B2BUnitInvoice invoice = invocation.getArgument(0);
            invoice.setId(101L);
            long lineId = 1L;
            for (var line : invoice.getLines()) {
                line.setId(lineId++);
            }
            return invoice;
        });

        PurchaseInvoiceCreateRequest request = new PurchaseInvoiceCreateRequest();
        request.setWarehouseId(7L);
        request.setInvoiceDate(LocalDate.of(2026, 3, 1));
        request.setDescription("purchase test");
        request.setLines(List.of(line(1001L, "2", "100", "18")));

        InvoiceResponse response = service.createPurchaseInvoice(10L, request);

        assertThat(response.getInvoiceType()).isEqualTo(B2BUnitInvoice.InvoiceType.PURCHASE);
        assertThat(response.getSubTotal()).isEqualByComparingTo("200.00");
        assertThat(response.getVatTotal()).isEqualByComparingTo("36.00");
        assertThat(response.getGrandTotal()).isEqualByComparingTo("236.00");
        assertThat(response.getLines()).hasSize(1);
        assertThat(response.getLines().get(0).getStockId()).isEqualTo(1001L);
        assertThat(response.getLines().get(0).getStockName()).isEqualTo("Motor");
        assertThat(response.getLines().get(0).getProductName()).isEqualTo("Motor");

        ArgumentCaptor<B2BUnitInvoice> invoiceCaptor = ArgumentCaptor.forClass(B2BUnitInvoice.class);
        verify(invoiceRepository, times(1)).save(invoiceCaptor.capture());
        B2BUnitInvoice persisted = invoiceCaptor.getValue();
        assertThat(persisted.getFacility()).isNull();
        assertThat(persisted.getElevator()).isNull();
        assertThat(persisted.getWarehouse().getId()).isEqualTo(7L);

        verify(transactionService, times(1)).onPurchaseInvoicePosted(
                eq(10L),
                eq(LocalDate.of(2026, 3, 1)),
                eq(new BigDecimal("236.00")),
                eq("PUR-101"),
                eq("purchase test")
        );
    }

    @Test
    void createSalesInvoiceShouldSucceedAndCalculateTotals() {
        authenticateAs("staff-admin");
        when(userRepository.findByUsername("staff-admin")).thenReturn(Optional.of(staffUser("staff-admin")));

        B2BUnit b2bUnit = activeB2BUnit(5L);
        Facility facility = facility(20L, "Facility A", b2bUnit);
        Elevator elevator = elevator(30L, "Facility A");

        when(b2bUnitRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(b2bUnit));
        when(facilityRepository.findByIdAndActiveTrue(20L)).thenReturn(Optional.of(facility));
        when(elevatorRepository.findById(30L)).thenReturn(Optional.of(elevator));
        when(warehouseRepository.findByIdAndActiveTrue(9L)).thenReturn(Optional.of(warehouse(9L)));
        when(partRepository.findByIdAndActiveTrue(2001L)).thenReturn(Optional.of(part(2001L, "Service A")));
        when(partRepository.findByIdAndActiveTrue(2002L)).thenReturn(Optional.of(part(2002L, "Service B")));
        when(invoiceRepository.save(any(B2BUnitInvoice.class))).thenAnswer(invocation -> {
            B2BUnitInvoice invoice = invocation.getArgument(0);
            invoice.setId(202L);
            return invoice;
        });

        SalesInvoiceCreateRequest request = new SalesInvoiceCreateRequest();
        request.setFacilityId(20L);
        request.setElevatorId(30L);
        request.setWarehouseId(9L);
        request.setInvoiceDate(LocalDate.of(2026, 3, 2));
        request.setDescription("sales test");
        request.setLines(List.of(
                line(2001L, "1", "50", "10"),
                line(2002L, "3", "20", "20")
        ));

        InvoiceResponse response = service.createSalesInvoice(5L, request);

        assertThat(response.getInvoiceType()).isEqualTo(B2BUnitInvoice.InvoiceType.SALES);
        assertThat(response.getSubTotal()).isEqualByComparingTo("110.00");
        assertThat(response.getVatTotal()).isEqualByComparingTo("17.00");
        assertThat(response.getGrandTotal()).isEqualByComparingTo("127.00");
        assertThat(response.getFacilityId()).isEqualTo(20L);
        assertThat(response.getElevatorId()).isEqualTo(30L);
        assertThat(response.getWarehouseId()).isEqualTo(9L);
        assertThat(response.getLines()).hasSize(2);
        assertThat(response.getLines().get(0).getStockId()).isEqualTo(2001L);
        assertThat(response.getLines().get(0).getStockName()).isEqualTo("Service A");

        verify(transactionService, times(1)).onSalesInvoicePosted(
                eq(5L),
                eq(LocalDate.of(2026, 3, 2)),
                eq(new BigDecimal("127.00")),
                eq("SAL-202"),
                eq("sales test")
        );
    }

    @Test
    void createPurchaseInvoiceShouldFailWhenLinesEmpty() {
        authenticateAs("staff-user");
        when(userRepository.findByUsername("staff-user")).thenReturn(Optional.of(staffUser("staff-user")));
        when(b2bUnitRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(activeB2BUnit(10L)));
        when(warehouseRepository.findByIdAndActiveTrue(7L)).thenReturn(Optional.of(warehouse(7L)));

        PurchaseInvoiceCreateRequest request = new PurchaseInvoiceCreateRequest();
        request.setWarehouseId(7L);
        request.setInvoiceDate(LocalDate.of(2026, 3, 1));
        request.setLines(List.of());

        assertThatThrownBy(() -> service.createPurchaseInvoice(10L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("lines must not be empty");

        verify(invoiceRepository, never()).save(any(B2BUnitInvoice.class));
    }

    @Test
    void createPurchaseInvoiceShouldFailWhenStockIdMissing() {
        authenticateAs("staff-user");
        when(userRepository.findByUsername("staff-user")).thenReturn(Optional.of(staffUser("staff-user")));
        when(b2bUnitRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(activeB2BUnit(10L)));
        when(warehouseRepository.findByIdAndActiveTrue(7L)).thenReturn(Optional.of(warehouse(7L)));

        PurchaseInvoiceCreateRequest request = new PurchaseInvoiceCreateRequest();
        request.setWarehouseId(7L);
        request.setInvoiceDate(LocalDate.of(2026, 3, 1));
        InvoiceLineRequest line = new InvoiceLineRequest();
        line.setQuantity(new BigDecimal("1"));
        line.setUnitPrice(new BigDecimal("100"));
        line.setVatRate(new BigDecimal("18"));
        request.setLines(List.of(line));

        assertThatThrownBy(() -> service.createPurchaseInvoice(10L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("stockId is required");

        verify(invoiceRepository, never()).save(any(B2BUnitInvoice.class));
    }

    @Test
    void createSalesInvoiceShouldFailWhenElevatorNotBelongsToFacility() {
        authenticateAs("staff-user");
        when(userRepository.findByUsername("staff-user")).thenReturn(Optional.of(staffUser("staff-user")));

        B2BUnit b2bUnit = activeB2BUnit(5L);
        Facility facility = facility(20L, "Facility A", b2bUnit);
        Elevator elevator = elevator(30L, "Facility B");

        when(b2bUnitRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(b2bUnit));
        when(facilityRepository.findByIdAndActiveTrue(20L)).thenReturn(Optional.of(facility));
        when(elevatorRepository.findById(30L)).thenReturn(Optional.of(elevator));
        when(warehouseRepository.findByIdAndActiveTrue(9L)).thenReturn(Optional.of(warehouse(9L)));

        SalesInvoiceCreateRequest request = new SalesInvoiceCreateRequest();
        request.setFacilityId(20L);
        request.setElevatorId(30L);
        request.setWarehouseId(9L);
        request.setInvoiceDate(LocalDate.of(2026, 3, 2));
        request.setLines(List.of(line(2001L, "1", "50", "10")));

        assertThatThrownBy(() -> service.createSalesInvoice(5L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("does not belong to selected facility");

        verify(invoiceRepository, never()).save(any(B2BUnitInvoice.class));
    }

    @Test
    void createInvoiceShouldBeForbiddenForCariUser() {
        authenticateAs("cari-user");
        User cariUser = new User();
        cariUser.setUsername("cari-user");
        cariUser.setRole(User.Role.CARI_USER);
        B2BUnit ownB2BUnit = activeB2BUnit(10L);
        cariUser.setB2bUnit(ownB2BUnit);
        when(userRepository.findByUsername("cari-user")).thenReturn(Optional.of(cariUser));

        PurchaseInvoiceCreateRequest request = new PurchaseInvoiceCreateRequest();
        request.setWarehouseId(7L);
        request.setInvoiceDate(LocalDate.of(2026, 3, 1));
        request.setLines(List.of(line(1001L, "1", "100", "18")));

        assertThatThrownBy(() -> service.createPurchaseInvoice(10L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("cannot create invoices");

        verify(b2bUnitRepository, never()).findByIdAndActiveTrue(any());
        verify(invoiceRepository, never()).save(any(B2BUnitInvoice.class));
    }

    private void authenticateAs(String username) {
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(username, "x", Collections.emptyList());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User staffUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setRole(User.Role.STAFF_USER);
        user.setUserType(User.UserType.STAFF);
        return user;
    }

    private B2BUnit activeB2BUnit(Long id) {
        B2BUnit b2bUnit = new B2BUnit();
        b2bUnit.setId(id);
        b2bUnit.setActive(true);
        return b2bUnit;
    }

    private Facility facility(Long id, String name, B2BUnit b2bUnit) {
        Facility facility = new Facility();
        facility.setId(id);
        facility.setName(name);
        facility.setB2bUnit(b2bUnit);
        facility.setActive(true);
        return facility;
    }

    private Elevator elevator(Long id, String buildingName) {
        Elevator elevator = new Elevator();
        elevator.setId(id);
        elevator.setBuildingName(buildingName);
        return elevator;
    }

    private Warehouse warehouse(Long id) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(id);
        warehouse.setName("Main Warehouse");
        warehouse.setActive(true);
        return warehouse;
    }

    private Part part(Long id, String name) {
        Part part = new Part();
        part.setId(id);
        part.setName(name);
        part.setActive(true);
        return part;
    }

    private InvoiceLineRequest line(Long stockId, String quantity, String unitPrice, String vatRate) {
        InvoiceLineRequest line = new InvoiceLineRequest();
        line.setStockId(stockId);
        line.setQuantity(new BigDecimal(quantity));
        line.setUnitPrice(new BigDecimal(unitPrice));
        line.setVatRate(new BigDecimal(vatRate));
        return line;
    }
}
