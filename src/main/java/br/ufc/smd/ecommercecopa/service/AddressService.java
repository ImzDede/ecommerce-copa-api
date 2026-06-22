package br.ufc.smd.ecommercecopa.service;

import br.ufc.smd.ecommercecopa.dto.address.AddressListResponse;
import br.ufc.smd.ecommercecopa.dto.address.AddressResponse;
import br.ufc.smd.ecommercecopa.dto.address.CreateAddressRequest;
import br.ufc.smd.ecommercecopa.dto.address.UpdateAddressRequest;
import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.Address;
import br.ufc.smd.ecommercecopa.model.Client;
import br.ufc.smd.ecommercecopa.repository.AddressRepository;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddressService {

    private final AuthService authService;
    private final AddressRepository addressRepository;

    public AddressService(AuthService authService, AddressRepository addressRepository) {
        this.authService = authService;
        this.addressRepository = addressRepository;
    }

    @Transactional(readOnly = true)
    public AddressListResponse list(HttpSession session) {
        Client client = authService.requireClient(session);
        return new AddressListResponse(addressRepository.findByClient_UserIdAndDeletedAtIsNullOrderByDefaultAddressDescCityAscNeighborhoodAsc(client.getUserId()).stream()
                .map(this::toResponse)
                .toList());
    }

    @Transactional(readOnly = true)
    public AddressResponse findById(UUID id, HttpSession session) {
        Client client = authService.requireClient(session);
        return toResponse(requireOwnAddress(id, client.getUserId()));
    }

    @Transactional
    public AddressResponse create(CreateAddressRequest request, HttpSession session) {
        Client client = authService.requireClient(session);

        Address address = new Address();
        address.setClient(client);
        address.setName(normalizeRequiredText(request.name(), "Nome do endereço é obrigatório"));
        address.setStreet(normalizeRequiredText(request.street(), "Endereço é obrigatório"));
        address.setNumber(normalizeRequiredText(request.number(), "Número é obrigatório"));
        address.setState(normalizeRequiredText(request.state(), "Estado é obrigatório"));
        address.setCity(normalizeRequiredText(request.city(), "Cidade é obrigatória"));
        address.setNeighborhood(normalizeRequiredText(request.neighborhood(), "Bairro é obrigatório"));
        address.setComplement(normalizeOptionalText(request.complement()));
        address.setPostalCode(normalizeRequiredText(request.postalCode(), "CEP é obrigatório"));

        var activeAddresses = addressRepository.findByClient_UserIdAndDeletedAtIsNullOrderByDefaultAddressDescCityAscNeighborhoodAsc(client.getUserId());
        boolean shouldBeDefault = activeAddresses.isEmpty() || Boolean.TRUE.equals(request.isDefault());
        if (shouldBeDefault) {
            clearDefault(activeAddresses);
        }
        address.setDefaultAddress(shouldBeDefault);

        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse update(UUID id, UpdateAddressRequest request, HttpSession session) {
        Client client = authService.requireClient(session);
        boolean hasName = request.name() != null;
        boolean hasStreet = request.street() != null;
        boolean hasNumber = request.number() != null;
        boolean hasState = request.state() != null;
        boolean hasCity = request.city() != null;
        boolean hasNeighborhood = request.neighborhood() != null;
        boolean hasComplement = request.complement() != null;
        boolean hasPostalCode = request.postalCode() != null;
        boolean hasDefault = request.isDefault() != null;

        if (!hasName && !hasStreet && !hasNumber && !hasState && !hasCity && !hasNeighborhood && !hasComplement && !hasPostalCode && !hasDefault) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Informe ao menos um campo para atualizar", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Address address = requireOwnAddress(id, client.getUserId());
        if (hasName) {
            address.setName(normalizeRequiredText(request.name(), "Nome do endereço é obrigatório"));
        }
        if (hasStreet) {
            address.setStreet(normalizeRequiredText(request.street(), "Endereço é obrigatório"));
        }
        if (hasNumber) {
            address.setNumber(normalizeRequiredText(request.number(), "Número é obrigatório"));
        }
        if (hasState) {
            address.setState(normalizeRequiredText(request.state(), "Estado é obrigatório"));
        }
        if (hasCity) {
            address.setCity(normalizeRequiredText(request.city(), "Cidade é obrigatória"));
        }
        if (hasNeighborhood) {
            address.setNeighborhood(normalizeRequiredText(request.neighborhood(), "Bairro é obrigatório"));
        }
        if (hasComplement) {
            address.setComplement(normalizeOptionalText(request.complement()));
        }
        if (hasPostalCode) {
            address.setPostalCode(normalizeRequiredText(request.postalCode(), "CEP é obrigatório"));
        }
        if (hasDefault) {
            if (Boolean.TRUE.equals(request.isDefault())) {
                var activeAddresses = addressRepository.findByClient_UserIdAndDeletedAtIsNullOrderByDefaultAddressDescCityAscNeighborhoodAsc(client.getUserId());
                clearDefault(activeAddresses);
            }
            address.setDefaultAddress(Boolean.TRUE.equals(request.isDefault()));
        }

        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public void delete(UUID id, HttpSession session) {
        Client client = authService.requireClient(session);
        Address address = requireOwnAddress(id, client.getUserId());
        address.setDeletedAt(LocalDateTime.now());
    }

    private void clearDefault(Iterable<Address> addresses) {
        for (Address address : addresses) {
            address.setDefaultAddress(false);
        }
    }

    private Address requireOwnAddress(UUID id, UUID clientId) {
        return addressRepository.findByIdAndClient_UserIdAndDeletedAtIsNull(id, clientId)
                .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "Endereço não encontrado", HttpStatus.NOT_FOUND));
    }

    private String normalizeRequiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new AppException("BUSINESS_RULE_VIOLATION", message, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getName(),
                address.getStreet(),
                address.getNumber(),
                address.getState(),
                address.getCity(),
                address.getNeighborhood(),
                address.getComplement(),
                address.getPostalCode(),
                address.isDefaultAddress()
        );
    }
}
