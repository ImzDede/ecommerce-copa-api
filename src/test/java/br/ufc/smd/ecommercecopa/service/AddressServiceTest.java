package br.ufc.smd.ecommercecopa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.ufc.smd.ecommercecopa.dto.address.CreateAddressRequest;
import br.ufc.smd.ecommercecopa.dto.address.UpdateAddressRequest;
import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.Address;
import br.ufc.smd.ecommercecopa.model.Client;
import br.ufc.smd.ecommercecopa.model.User;
import br.ufc.smd.ecommercecopa.repository.AddressRepository;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private HttpSession session;

    private AddressService addressService;
    private Client client;

    @BeforeEach
    void setUp() {
        addressService = new AddressService(authService, addressRepository);
        client = client(UUID.randomUUID());
    }

    @Test
    void createSavesTrimmedAddress() {
        when(authService.requireClient(session)).thenReturn(client);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address address = invocation.getArgument(0);
            ReflectionTestUtils.setField(address, "id", UUID.randomUUID());
            return address;
        });
        when(addressRepository.findByClient_UserIdAndDeletedAtIsNullOrderByDefaultAddressDescCityAscNeighborhoodAsc(client.getUserId()))
                .thenReturn(List.of());

        var response = addressService.create(new CreateAddressRequest(
                " Casa ",
                " Rua A ",
                " 123 ",
                " CE ",
                " Fortaleza ",
                " Centro ",
                " ",
                " 60000-000 ",
                null
        ), session);

        assertNotNull(response.id());
        assertEquals("Casa", response.name());
        assertEquals("Rua A", response.street());
        assertEquals("123", response.number());
        assertEquals("CE", response.state());
        assertEquals(null, response.complement());
        assertEquals(true, response.isDefault());
    }

    @Test
    void createDefaultClearsPreviousDefaultAddress() {
        Address currentDefault = address(UUID.randomUUID(), client);
        currentDefault.setDefaultAddress(true);
        when(authService.requireClient(session)).thenReturn(client);
        when(addressRepository.findByClient_UserIdAndDeletedAtIsNullOrderByDefaultAddressDescCityAscNeighborhoodAsc(client.getUserId()))
                .thenReturn(List.of(currentDefault));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address address = invocation.getArgument(0);
            ReflectionTestUtils.setField(address, "id", UUID.randomUUID());
            return address;
        });

        var response = addressService.create(new CreateAddressRequest(
                "Trabalho",
                "Rua B",
                "456",
                "CE",
                "Fortaleza",
                "Aldeota",
                null,
                "60100-000",
                true
        ), session);

        assertEquals(false, currentDefault.isDefaultAddress());
        assertEquals(true, response.isDefault());
    }

    @Test
    void updateRejectsEmptyBody() {
        when(authService.requireClient(session)).thenReturn(client);

        AppException exception = assertThrows(AppException.class, () -> addressService.update(
                UUID.randomUUID(),
                new UpdateAddressRequest(null, null, null, null, null, null, null, null, null),
                session
        ));

        assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    void deleteSetsDeletedAt() {
        UUID addressId = UUID.randomUUID();
        Address address = address(addressId, client);
        when(authService.requireClient(session)).thenReturn(client);
        when(addressRepository.findByIdAndClient_UserIdAndDeletedAtIsNull(addressId, client.getUserId()))
                .thenReturn(Optional.of(address));

        addressService.delete(addressId, session);

        assertNotNull(address.getDeletedAt());
    }

    private Client client(UUID id) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setName("Maria Silva");

        Client client = new Client();
        ReflectionTestUtils.setField(client, "userId", id);
        client.setUser(user);
        client.setCpf("12345678901");
        client.setDateOfBirth(LocalDate.parse("2000-01-01"));
        return client;
    }

    private Address address(UUID id, Client client) {
        Address address = new Address();
        ReflectionTestUtils.setField(address, "id", id);
        address.setClient(client);
        address.setName("Casa");
        address.setStreet("Rua A");
        address.setNumber("123");
        address.setState("CE");
        address.setCity("Fortaleza");
        address.setNeighborhood("Centro");
        address.setPostalCode("60000-000");
        return address;
    }
}
